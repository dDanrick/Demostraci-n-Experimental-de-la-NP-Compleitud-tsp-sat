# TSP ≤p SAT — Reducción polinomial del Problema del Viajante (decisión) a SAT

Programa en Java 11+ que recibe una instancia del TSP (matriz de distancias de N ciudades y umbral K),
la **traduce a una fórmula CNF** (formato DIMACS), la entrega a un **SAT-Solver (Sat4j)** y
**reconstruye la ruta** a partir del modelo booleano. El código **no resuelve el TSP**: no hay fuerza
bruta, programación dinámica ni metaheurísticas; solo traducción y lectura del resultado.

## Estructura

```
src/main/java/tspsat/
  Main.java                      CLI y orquestación de las 3 fases
  model/TspInstance.java         instancia (N, matriz, K)
  io/InstanceReader.java         lectura de archivo / argumentos / matriz aleatoria
  cnf/CnfFormula.java            fórmula CNF + escritura DIMACS
  reduction/TspToSatReducer.java reducción TSP -> SAT (Fase 1)
  reduction/VariableMap.java     numeración de variables X(i,t) y A(s,v)
  solver/SatSolver.java          interfaz con el solver
  solver/Sat4jSolver.java        adaptador a Sat4j (Fase 2)
  solver/BuiltinCdclSolver.java  SAT-Solver CDCL genérico alternativo (sin dependencias)
  decode/SolutionDecoder.java    modelo -> ruta + costo real (Fase 3)
  bench/Benchmark.java           pruebas de rendimiento
examples/                        instancias de ejemplo
tools/validate_oracle.py         validación externa contra enumeración exhaustiva (solo pruebas)
tools/run_examples.sh            ejecuta todos los ejemplos
docs/ejemplo_n3.cnf              ejemplo de archivo DIMACS generado
```

## Requisitos

* JDK 11 o superior y Maven 3.6+ (descarga Sat4j `org.ow2.sat4j.core:2.3.6` desde Maven Central).

## Compilar

```bash
mvn clean package            # genera target/tsp-sat.jar (incluye Sat4j)
```

Sin Maven: descargue `org.ow2.sat4j.core-2.3.6.jar` y ejecute
`javac -cp org.ow2.sat4j.core-2.3.6.jar -d out $(find src -name "*.java")`
y luego `java -cp out:org.ow2.sat4j.core-2.3.6.jar tspsat.Main ...` (en Windows use `;` en lugar de `:`).

## Ejecutar

Archivo de entrada (`#` = comentario): `N`, `K` y la matriz N×N (puede ser asimétrica).

```
4
80
0 10 15 20
10 0 35 25
15 35 0 30
20 25 30 0
```

```bash
java -jar target/tsp-sat.jar --file examples/ejemplo_n4_sat.txt
java -jar target/tsp-sat.jar --file examples/ejemplo_n4_sat.txt --k 79          # sobrescribe K
java -jar target/tsp-sat.jar --n 3 --k 16 --matrix "0 5 4  5 0 7  4 7 0"        # por consola
java -jar target/tsp-sat.jar --file examples/ejemplo_n3.txt --dimacs salida.cnf  # guarda la CNF
```

Opciones: `--solver sat4j|builtin` (por defecto `sat4j`), `--timeout segundos`, `--no-symmetry`.

Salida esperada:

* SAT → ruta, costo real y `Verificación del certificado: OK`.
* UNSAT → `NO existe ninguna ruta con costo menor o igual a K`.

## Pruebas

```bash
tools/run_examples.sh sat4j                         # todos los ejemplos
python3 tools/validate_oracle.py "java -jar target/tsp-sat.jar" --solver sat4j
```

`validate_oracle.py` compara el veredicto del programa con una enumeración exhaustiva **independiente
y externa** (N ≤ 8, K = óptimo−1, óptimo, óptimo+3, con y sin ruptura de simetría). Es solo una herramienta
de verificación; no forma parte del flujo del programa.

## Pruebas de rendimiento

```bash
# K = costo de un recorrido arbitrario (SAT por construcción)
java -jar target/tsp-sat.jar --bench 3,4,10 --maxdist 20 --ratio 1.0 --seed 42 --timeout 60
# Punto de quiebre: bisección sobre K con el propio SAT-Solver; mide último SAT (K*) y último UNSAT (K*-1)
java -jar target/tsp-sat.jar --bench 8,10,11,12,13,14,15 --tight --maxdist 20 --seed 42 --timeout 120
```

## Modelo (resumen)

* `X(i,t)`: la ciudad *i* se visita en el paso *t* (N² variables).
* `A(s,v)`: el costo acumulado de los primeros *s* tramos es ≥ *v* (contador secuencial ponderado).
* Visita única y ocupación única: *al menos uno* + *a lo sumo uno* por pares.
* Costo: `¬X(i,t) ∨ ¬X(j,t+1) ∨ ¬A(t,v) ∨ A(t+1, min(v+d(i,j), K+1))` y la unitaria `¬A(N,K+1)`.
* Ruptura de simetría: `X(0,0)`.

Detalle matemático y análisis de complejidad en el informe técnico.
