package tspsat.solver;

/**
 *
 * @author edani
 */

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

    public boolean value(int v) {
        for (int l : model) if (l == v) return true; else if (l == -v) return false;
        return false;
    }
}
