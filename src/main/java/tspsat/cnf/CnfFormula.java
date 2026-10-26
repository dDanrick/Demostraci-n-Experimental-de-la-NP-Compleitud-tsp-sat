package tspsat.cnf;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Fórmula en Forma Normal Conjuntiva. Literales en convención DIMACS: +v / -v, con v >= 1. */
public final class CnfFormula {
    private final int numVars;
    private final List<int[]> clauses = new ArrayList<>();

    public CnfFormula(int numVars) { this.numVars = numVars; }

    public void addClause(int... lits) {
        for (int l : lits) {
            if (l == 0 || Math.abs(l) > numVars) throw new IllegalArgumentException("Literal fuera de rango: " + l);
        }
        clauses.add(lits);
    }

    public int numVars() { return numVars; }
    public int numClauses() { return clauses.size(); }
    public List<int[]> clauses() { return clauses; }

    /** Escribe el archivo en formato DIMACS CNF. */
    public void writeDimacs(Path out, String... comments) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            for (String c : comments) { w.write("c " + c); w.newLine(); }
            w.write("p cnf " + numVars + " " + clauses.size());
            w.newLine();
            StringBuilder sb = new StringBuilder();
            for (int[] c : clauses) {
                sb.setLength(0);
                for (int l : c) sb.append(l).append(' ');
                sb.append('0');
                w.write(sb.toString());
                w.newLine();
            }
        }
    }
}
