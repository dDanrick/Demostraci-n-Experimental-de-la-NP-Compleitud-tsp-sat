package tspsat.model;

/**
 * Instancia del problema de decisión del viajante (TSP-D):
 * ¿existe un ciclo hamiltoniano de costo total &lt;= K?
 * La matriz puede ser asimétrica; la diagonal se ignora.
 */
public final class TspInstance {
    private final int n;
    private final int[][] dist;
    private final int k;

    public TspInstance(int[][] dist, int k) {
        if (dist == null || dist.length < 1) {
            throw new IllegalArgumentException("La matriz de distancias no puede ser vacía.");
        }
        this.n = dist.length;
        this.dist = new int[n][n];
        for (int i = 0; i < n; i++) {
            if (dist[i].length != n) {
                throw new IllegalArgumentException("La matriz de distancias debe ser cuadrada (N x N).");
            }
            for (int j = 0; j < n; j++) {
                if (i != j && dist[i][j] < 0) {
                    throw new IllegalArgumentException("Las distancias deben ser enteras no negativas.");
                }
                this.dist[i][j] = dist[i][j];
            }
        }
        if (k < 0) {
            throw new IllegalArgumentException("K debe ser no negativo.");
        }
        this.k = k;
    }

    public int size() { return n; }
    public int distance(int i, int j) { return dist[i][j]; }
    public int threshold() { return k; }

    /** Mayor distancia entre ciudades distintas (0 si N = 1). */
    public int maxDistance() {
        int m = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                if (i != j && dist[i][j] > m) m = dist[i][j];
        return m;
    }

    /** Costo real de un recorrido cíclico dado como permutación de ciudades. */
    public long tourCost(int[] route) {
        if (route.length <= 1) return 0;
        long c = 0;
        for (int t = 0; t < route.length; t++) {
            c += dist[route[t]][route[(t + 1) % route.length]];
        }
        return c;
    }

    public TspInstance withThreshold(int newK) { return new TspInstance(dist, newK); }
}
