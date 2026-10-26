#!/bin/sh
# Ejecuta todos los ejemplos con el solver indicado (por defecto sat4j). Uso: tools/run_examples.sh [sat4j|builtin]
SOLVER=${1:-sat4j}
JAR=target/tsp-sat.jar
for f in examples/*.txt; do
  echo "=== $f"
  java -jar "$JAR" --file "$f" --solver "$SOLVER"
done
