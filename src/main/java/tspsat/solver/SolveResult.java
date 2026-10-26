package tspsat.solver;

/** Veredicto del solver. El modelo (si SAT) es una lista de literales DIMACS, uno por variable. */
public final class SolveResult {
    public enum Status { SATISFIABLE, UNSATISFIABLE, UNKNOWN }

    public final Status status;
    public final int[] model;
    public final long millis;

    public SolveResult(Status status, int[] model, long millis) {
        this.status = status;
        this.model = model;
        this.millis = millis;
    }

    /** Valor de verdad de la variable v (1-based) en el modelo. */
    public boolean value(int v) {
        for (int l : model) if (l == v) return true; else if (l == -v) return false;
        return false;
    }
}
