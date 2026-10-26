package tspsat.reduction;

/**
 * Numeración de las variables booleanas de la reducción (1-based, convención DIMACS).
 * <ul>
 *   <li>X(i,t), 0 &lt;= i,t &lt; N: "la ciudad i se visita en el paso t" -> id = i*N + t + 1.</li>
 *   <li>A(s,v), 1 &lt;= s &lt;= N, 1 &lt;= v &lt;= reach(s): "el costo acumulado tras s tramos es >= v".
 *       Solo existen para v &lt;= min(K+1, s*dmax) (valores inalcanzables no se codifican).</li>
 * </ul>
 */
public final class VariableMap {
    private final int n;
    private final int cap;      // K+1 (valor de saturación); 0 si no hay restricción de costo activa
    private final int[] reach;  // reach[s]
    private final int[] offset; // offset[s]: id de A(s,0) ficticio; A(s,v) = offset[s] + v
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

    /** Variable A(s,v) con 1 &lt;= v &lt;= reach(s). */
    public int a(int s, int v) {
        if (v < 1 || v > reach[s]) throw new IllegalArgumentException("A(" + s + "," + v + ") no existe");
        return offset[s] + v;
    }

    public int numVars() { return total; }
    public int numPositionVars() { return n * n; }
}
