# TSP ≤p SAT — Reducción polinomial del Problema del Viajante a SAT

Este repositorio contiene **dos formas de ejecutar el mismo proyecto**: una versión para trabajar desde la consola y otra preparada para abrir y ejecutar cómodamente desde Apache NetBeans. Ambas convierten una instancia del TSP de decisión en una fórmula CNF y consultan un SAT-Solver para determinar si existe un recorrido que visite cada ciudad una sola vez, regrese al inicio y tenga un costo total menor o igual que `K`. El programa no resuelve el TSP mediante fuerza bruta, programación dinámica ni metaheurísticas: realiza la reducción a SAT y reconstruye la ruta a partir del modelo booleano.

## Versiones disponibles

- **Versión de consola (`consola/`)**: se ejecuta con comandos desde una terminal. Es la opción indicada para reproducir ejemplos, generar archivos DIMACS, automatizar pruebas y ejecutar benchmarks mediante argumentos de línea de comandos.
- **Versión NetBeans (`netbeans/`)**: se abre como proyecto Maven en Apache NetBeans. Permite seleccionar el caso de prueba y los modos de ejecución editando la configuración de `Ejecutar.java`, y consultar la salida en la ventana **Output**.

> **Organización en GitHub:** se recomienda colocar este `README.md` en la raíz del repositorio y guardar cada proyecto completo en una carpeta separada, por ejemplo `consola/` y `netbeans/`. Cada carpeta debe conservar su propio `pom.xml`, `src/`, ejemplos y archivos auxiliares. Si eliges otros nombres para las carpetas, adapta las rutas de los comandos de esta guía.

La opción de solver principal es **Sat4j 2.3.6**, declarada como dependencia Maven. También se incluye `BuiltinCdclSolver`, un solver SAT genérico alternativo integrado en el código. Al informar resultados, especifica qué solver se utilizó: las mediciones históricas del informe se obtuvieron con `BuiltinCdclSolver`, no con Sat4j.

## 1. Requisitos generales

- JDK 11 o superior.
- Maven 3.6 o superior para compilar desde la consola.
- Conexión a Internet durante la primera compilación, para descargar Sat4j 2.3.6 y los complementos de Maven.
- Apache NetBeans solo es necesario para la versión `netbeans/`. Puedes instalarlo desde [netbeans.apache.org](https://netbeans.apache.org/).

## 2. Versión de consola

### 2.1 Compilar

Abre una terminal en la carpeta `consola/`, donde se encuentra su `pom.xml`, y ejecuta:

```bash
mvn clean package
```

Si la compilación termina correctamente, Maven genera `target/tsp-sat.jar`. La primera compilación necesita conexión a Internet para descargar Sat4j y los complementos de Maven.

### 2.2 Ejecutar una instancia desde un archivo

Cada archivo de entrada contiene `N` en la primera línea, `K` en la segunda y después una matriz de distancias de tamaño `N × N`. Las filas de la matriz se separan por espacios. Se permiten comentarios que comiencen con `#`. Por ejemplo, el archivo `examples/ejemplo_n4_sat.txt` representa una instancia con cuatro ciudades y umbral 80.

Desde la carpeta `consola/`, ejecuta:

```bash
# Resuelve la instancia incluida
java -jar target/tsp-sat.jar --file examples/ejemplo_n4_sat.txt

# Sobrescribe el umbral K del archivo para probar el caso UNSAT
java -jar target/tsp-sat.jar --file examples/ejemplo_n4_sat.txt --k 79

# Introduce N, K y la matriz directamente como argumentos
java -jar target/tsp-sat.jar --n 3 --k 16 --matrix "0 5 4  5 0 7  4 7 0"

# Guarda la fórmula CNF generada en formato DIMACS
java -jar target/tsp-sat.jar --file examples/ejemplo_n3.txt --dimacs salida.cnf
```

Opciones adicionales: `--solver sat4j|builtin` selecciona el solver, `--timeout segundos` establece el tiempo límite y `--no-symmetry` desactiva la ruptura de simetría. Si no se indica lo contrario, el solver predeterminado es `sat4j`. Para las pruebas de la entrega, se recomienda escribir explícitamente `--solver sat4j`.

Una salida `SAT` significa que se encontró una ruta cuyo costo no supera `K`; el programa muestra la ruta, el costo y la verificación del certificado. `UNSAT` significa que no existe una ruta que cumpla el límite. Un tiempo agotado no equivale a `UNSAT`.

### 2.3 Ejecutar todos los ejemplos y la validación externa

Si la versión de consola incluye los scripts auxiliares del proyecto original, desde `consola/` puedes ejecutar:

```bash
tools/run_examples.sh sat4j
python3 tools/validate_oracle.py "java -jar target/tsp-sat.jar" --solver sat4j
```

`validate_oracle.py` compara el resultado del programa con una enumeración exhaustiva independiente para instancias pequeñas (hasta `N = 8`). Es una herramienta externa de verificación, no forma parte del flujo del programa. Si los scripts no están en la carpeta de consola que subiste a GitHub, estos dos comandos no estarán disponibles hasta que agregues esos archivos.

### 2.4 Benchmarks desde la consola

**Benchmark con K holgado:** genera instancias para los tamaños indicados y usa un umbral que permite construir una ruta SAT.

```bash
java -jar target/tsp-sat.jar --bench 3,4,10 --maxdist 20 --ratio 1.0 --seed 42 --timeout 90
```

**Benchmark de punto de quiebre:** busca mediante bisección el menor umbral `K*` para el que existe una ruta y contrasta el caso SAT con el caso UNSAT de `K* - 1`.

```bash
java -jar target/tsp-sat.jar --bench 8,10,11,12,13,14,15 --tight --maxdist 20 --seed 42 --timeout 90
```

Los tiempos dependen del equipo, la versión de Java, la carga del sistema y el solver. No compares tiempos de `sat4j` con tiempos de `builtin` como si procedieran del mismo motor.

## 3. Versión para Apache NetBeans

Las siguientes instrucciones corresponden al proyecto guardado en la carpeta `netbeans/`. Si el proyecto se encuentra en la raíz del repositorio y no dentro de una carpeta, abre directamente la carpeta que contiene `pom.xml`.

### 3.1 Preparar NetBeans

1. **Instala JDK 11 o superior.** Puedes descargar una distribución de OpenJDK desde [jdk.java.net](https://jdk.java.net/) o Eclipse Temurin desde [adoptium.net](https://adoptium.net/). Para comprobar que NetBeans reconoce el JDK, abre **Tools → Java Platforms** y confirma que aparezca la versión instalada. El uso normal del proyecto se hace desde NetBeans.
2. **Instala Apache NetBeans** desde [netbeans.apache.org](https://netbeans.apache.org/). Durante la configuración, selecciona el JDK instalado. NetBeans incluye integración con Maven, así que no hace falta instalar Maven por separado.
3. **Conéctate a Internet la primera vez que abras el proyecto.** Maven descargará Sat4j 2.3.6 y los complementos que necesita para compilar.

### 3.2 Abrir el proyecto en NetBeans

### Opción A: usar este repositorio

1. En GitHub, pulsa **Code → Download ZIP** y descomprime el archivo. También puedes clonar el repositorio desde NetBeans con **Team → Git → Clone**.
2. En NetBeans, selecciona **File → Open Project**.
3. Busca la carpeta `netbeans` (o el nombre que le hayas dado a la versión NetBeans) y ábrela. Selecciona la carpeta que contiene `pom.xml`.
4. Haz clic derecho en el proyecto y elige **Clean and Build**. Espera a que Maven termine de descargar las dependencias y compilar.

### Opción B: crear el proyecto desde cero

1. Selecciona **File → New Project → Java with Maven → Java Application** (el nombre de la categoría puede variar un poco según la versión).
2. Define el nombre del proyecto y abre el `pom.xml` generado.
3. Reemplaza su contenido por el `pom.xml` de este repositorio.
4. En **Source Packages**, crea el paquete `tspsat` y los subpaquetes `bench`, `cnf`, `decode`, `io`, `model`, `reduction` y `solver`.
5. Copia cada archivo `.java` en el paquete correspondiente. Copia también `ejemplo.txt` a la carpeta raíz del proyecto, al mismo nivel que `pom.xml`.
6. Crea las carpetas `examples` y `docs` en la raíz y copia sus archivos de ejemplo.

### 3.3 Ejecutar una instancia

1. Abre `Source Packages → tspsat → Ejecutar.java`.
2. En la sección **CONFIGURACIÓN**, revisa estos valores:
   - `MODO = "RESOLVER"` para resolver una sola instancia.
   - `ARCHIVO = "ejemplo.txt"` para usar el ejemplo de la raíz.
   - `K = null` para conservar el valor de `K` escrito en el archivo.
   - `SOLVER = "sat4j"` para usar la biblioteca exigida en el trabajo. Usa `"builtin"` solo como alternativa de prueba.
   - `DIMACS = null` para no guardar la fórmula. Puedes escribir `"salida.cnf"` para guardar el archivo en la raíz del proyecto.
3. Haz clic derecho dentro de `Ejecutar.java` y elige **Run File** (Mayús+F6), o usa el botón ▶ si NetBeans lo ofrece para el archivo.
4. Lee los resultados en la ventana **Output** de NetBeans. Si no aparece, abre **Window → Output → Output**.

Con `ejemplo.txt`, el resultado esperado es **SAT**, con una ruta de costo `80`. Para probar el caso contrario, cambia `K = 79` en la configuración. El programa debe imprimir exactamente:

`NO existe ninguna ruta con costo menor o igual a K`

El resultado SAT depende de que el solver termine. Un mensaje de tiempo agotado no significa SAT ni UNSAT.

### 3.4 Modos de ejecución

| Modo | Qué hace | Configuración principal |
|---|---|---|
| `RESOLVER` | Resuelve una instancia desde un archivo. | `ARCHIVO`, `K`, `SOLVER`, `DIMACS` |
| `BENCH_FIJO` | Genera instancias para varios valores de N y usa un umbral holgado. | `TAMANOS`, `SOLVER`, `TIMEOUT_SEG` |
| `BENCH_QUIEBRE` | Busca por bisección el menor K que permite una ruta y mide la última prueba SAT y la prueba UNSAT de K−1. | `TAMANOS_QUIEBRE`, `SOLVER`, `TIMEOUT_SEG` |

Para repetir las pruebas de rendimiento del informe:

1. Abre `Ejecutar.java`.
2. Cambia `MODO` a `"BENCH_FIJO"` para la tabla con K holgado o a `"BENCH_QUIEBRE"` para la búsqueda del punto de quiebre.
3. Ajusta `TAMANOS` o `TAMANOS_QUIEBRE`. Para comparar con el informe, conserva la semilla y los parámetros usados por `Benchmark` (semilla 42, distancias máximas 20 y límite de 90 segundos por llamada).
4. Para obtener mediciones con el solver exigido, deja `SOLVER = "sat4j"`. Espera a que cada prueba termine y copia de **Output** los tiempos, las variables, las cláusulas y el estado.
5. Repite las tablas 2 y 3 del informe con los resultados nuevos. No mezcles las mediciones de Sat4j con las obtenidas con `BuiltinCdclSolver`: identifica siempre qué solver produjo cada tabla.

Los tiempos pueden variar según el equipo, la versión de Java, la carga del sistema y las decisiones internas del solver.

### 3.5 Ejemplos incluidos

| Archivo | Entrada | Resultado esperado |
|---|---|---|
| `ejemplo.txt` | N = 4, K = 80 | SAT; existe una ruta de costo 80. |
| `examples/ejemplo_n4_sat.txt` | N = 4, K = 80 | SAT; ruta de costo 80. |
| `examples/ejemplo_n4_unsat.txt` | N = 4, K = 79 | UNSAT; se muestra el mensaje exigido. |
| `examples/ejemplo_n3.txt` | N = 3, K = 16 | SAT; ruta de costo 16. |
| `examples/ejemplo_n3_unsat.txt` | N = 3, K = 15 | UNSAT; se muestra el mensaje exigido. |

El formato de cada archivo es: primera línea `N`, segunda línea `K` y luego las `N × N` distancias de la matriz, separadas por espacios. Las distancias se leen por filas. Se permiten comentarios que comiencen con `#`.

**Nota sobre los resultados del informe:** el archivo fuente recibido no incluía las matrices originales de los casos R-05 (N = 5) y R-07 (N = 10). No se han inventado matrices para esos casos. Para reproducir exactamente sus resultados, recupera esos dos archivos de entrada del proyecto original.

### 3.6 Organización del proyecto NetBeans

- `tspsat.Ejecutar`: configuración sencilla para ejecutar desde NetBeans.
- `tspsat.Main`: coordina lectura, reducción, solver y reconstrucción.
- `tspsat.io.InstanceReader`: lee el archivo de entrada.
- `tspsat.model.TspInstance`: guarda la matriz y el umbral K.
- `tspsat.reduction.TspToSatReducer`: genera las restricciones CNF.
- `tspsat.reduction.VariableMap`: asigna identificadores a las variables.
- `tspsat.cnf.CnfFormula`: almacena las cláusulas y escribe DIMACS.
- `tspsat.solver.SatSolver`: interfaz común para los motores SAT.
- `tspsat.solver.Sat4jSolver`: conecta la fórmula con Sat4j.
- `tspsat.solver.BuiltinCdclSolver`: alternativa SAT genérica integrada.
- `tspsat.solver.SolveResult`: guarda el resultado del solver.
- `tspsat.decode.SolutionDecoder`: reconstruye la ruta y verifica su costo.
- `tspsat.bench.Benchmark`: ejecuta los experimentos de rendimiento.
- `examples/`: instancias de prueba.
- `docs/ejemplo_n3.cnf`: fórmula DIMACS de ejemplo.
- `pom.xml`: define Java 11 y la dependencia Sat4j.

### 3.7 Solución de problemas

**Sat4j aparece en rojo o sale `package org.sat4j does not exist`.**  
Comprueba que hay conexión a Internet y haz clic derecho en el proyecto → **Clean and Build**. Si necesitas revisar la reducción mientras resuelves la dependencia, cambia temporalmente `SOLVER` a `"builtin"`. Para la entrega final, prueba también `"sat4j"`.

**Las tildes aparecen mal.**  
Confirma que `project.build.sourceEncoding` está en `UTF-8` dentro de `pom.xml`.

**El proyecto usa una versión de Java distinta.**  
Abre las propiedades del proyecto y revisa **Sources** para que el nivel de código sea Java 11. También verifica que NetBeans esté usando un JDK 11 o superior.

**No encuentra `ejemplo.txt`.**  
El archivo debe estar en la carpeta raíz del proyecto, al mismo nivel que `pom.xml`. No lo coloques dentro de `src`.

**El solver supera el tiempo límite.**  
Aumenta `TIMEOUT_SEG` o prueba una instancia más pequeña. El tiempo agotado no permite concluir que no exista una ruta.

## 4. Publicar las dos versiones en GitHub

1. Crea un repositorio en GitHub o abre el repositorio que ya utilizas para el informe.
2. En la raíz del repositorio, coloca este `README.md`.
3. Crea una carpeta `consola/` y copia dentro el proyecto completo de consola: su `pom.xml`, `src/`, `examples/`, `docs/` y, si existen, `tools/` y `tools/run_examples.sh`. Conserva las rutas relativas que necesita el código.
4. Crea una carpeta `netbeans/` y copia dentro el proyecto Maven de NetBeans: `pom.xml`, `src/`, `examples/`, `docs/`, `ejemplo.txt` y `.gitignore`.
5. No subas las carpetas generadas `target/`, archivos `.class`, ni archivos temporales o privados de NetBeans.
6. Revisa en GitHub que el árbol del repositorio muestre ambas carpetas y que cada una tenga su propio `pom.xml`.

Si publicas desde NetBeans, inicializa Git desde el proyecto o desde la raíz del repositorio, agrega los archivos de ambas carpetas, confirma los cambios y usa **Team → Git → Remote → Push** (el nombre puede variar según la versión). Si ya existe un repositorio remoto, comprueba que la URL sea la correcta antes de hacer `push`.

## 5. Nota sobre las mediciones

Las mediciones incluidas en el informe inicial se hicieron con `BuiltinCdclSolver`, un solver SAT genérico desarrollado e integrado en el proyecto. **Sat4j 2.3.6 estaba declarado en Maven, pero no se ejecutó en el entorno donde se obtuvieron esas mediciones.** Por eso, los tiempos de las tablas originales no deben presentarse como resultados de Sat4j. Para cerrar la entrega, ejecuta los benchmarks con `sat4j` desde la consola o NetBeans y actualiza el informe únicamente con las mediciones que hayas obtenido realmente.

## 6. Archivos de ejemplo que faltan para reproducir algunos casos

La versión de NetBeans recibida contiene ejemplos para `N = 3` y `N = 4`. No incluye las matrices originales de los casos R-05 (`N = 5`) y R-07 (`N = 10`), por lo que no se deben inventar entradas y afirmar que reproducen exactamente esos resultados. Para repetirlos, recupera las matrices originales del proyecto fuente. La validación externa R-06 también requiere su script auxiliar `tools/validate_oracle.py` y debe identificarse como una comprobación externa, no como una ejecución ordinaria de `Ejecutar.java`.
