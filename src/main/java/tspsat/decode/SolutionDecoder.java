package tspsat.decode;

import tspsat.model.TspInstance;
import tspsat.reduction.VariableMap;
import tspsat.solver.SolveResult;

/** Reconstruye la ruta a partir del certificado booleano (modelo) devuelto por el SAT-Solver. */
public final class SolutionDecoder {
    private SolutionDecoder() { }

    public static final class Tour {
        public final int[] route;   // route[t] = ciudad visitada en el paso t
        public final long cost;     // costo real calculado con la matriz original
        public final boolean valid; // permutación válida y costo <= K (auto-verificación del certificado)
        Tour(int[] r, long c, boolean v) { route = r; cost = c; valid = v; }
    }

    public static Tour decode(TspInstance inst, VariableMap vm, SolveResult res) {
        int n = inst.size();
        int[] route = new int[n];
        boolean[] used = new boolean[n];
        boolean ok = true;
        for (int t = 0; t < n; t++) {
            int found = -1, count = 0;
            for (int i = 0; i < n; i++) {
                if (res.value(vm.x(i, t))) { found = i; count++; }
            }
            if (count != 1 || used[found]) { ok = false; found = Math.max(found, 0); }
            route[t] = found;
            used[found] = true;
        }
        long cost = n <= 1 ? 0 : inst.tourCost(route);
        return new Tour(route, cost, ok && cost <= inst.threshold());
    }
}
