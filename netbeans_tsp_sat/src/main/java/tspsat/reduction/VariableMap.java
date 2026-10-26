package tspsat.reduction;

/**
 *
 * @author edani
 */

/**
 * X(i,t): la ciudad i se visita en el paso t  -> id = i*N + t + 1
 * A(s,v): el costo acumulado tras s tramos es >= v (solo v <= min(K+1, s*dmax)).
 */
public final class VariableMap {
    private final int n;
    private final int cap;
    private final int[] reach;
    private final int[] offset;
    private final int total;

    VariableMap(int n, int cap, int dmax) {
        this.n = n;
        this.cap = cap;
        this.reach = new int[n + 1];
        this.offset = new int[n + 1];
        int next = n * n;
        if (cap > 0) {
            for (int s = 1; s <= n; s++) {
                long r = Math.min((long) cap, (long) s * dmax);
                reach[s] = (int) r;
                offset[s] = next;
                next += reach[s];
            }
        }
        this.total = next;
    }

    public int x(int city, int step) { return city * n + step + 1; }

    public boolean hasCost() { return cap > 0; }
    public int cap() { return cap; }
    public int reach(int s) { return reach[s]; }

    public int a(int s, int v) {
        if (v < 1 || v > reach[s]) throw new IllegalArgumentException("A(" + s + "," + v + ") no existe");
        return offset[s] + v;
    }

    public int numVars() { return total; }
    public int numPositionVars() { return n * n; }
}
