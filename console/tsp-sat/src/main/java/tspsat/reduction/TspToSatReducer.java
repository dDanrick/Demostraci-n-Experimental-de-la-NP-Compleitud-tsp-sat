package tspsat.reduction;

import tspsat.cnf.CnfFormula;
import tspsat.model.TspInstance;

/**
 * Reducción polinomial TSP-D &le;p SAT.
 *
 * <h3>Variables</h3>
 * X(i,t): la ciudad i se visita en el paso t (N^2 variables) y A(s,v): el costo acumulado de los
 * primeros s tramos es &gt;= v (codificación de orden / contador secuencial ponderado).
 *
 * <h3>Restricciones</h3>
 * <ol>
 *   <li>Visita única: cada ciudad ocupa exactamente un paso (ALO + AMO por ciudad).</li>
 *   <li>Ocupación única: cada paso tiene exactamente una ciudad (ALO + AMO por paso).</li>
 *   <li>Costo: sum_t d(ruta[t], ruta[t+1 mod N]) &lt;= K mediante el contador A.</li>
 *   <li>(Opcional) Ruptura de simetría: la ciudad 0 abre el recorrido (todo ciclo admite rotación).</li>
 * </ol>
 */
public final class TspToSatReducer {

    private final boolean breakSymmetry;

    public TspToSatReducer(boolean breakSymmetry) { this.breakSymmetry = breakSymmetry; }

    /** Resultado de la reducción: fórmula + mapa de variables para decodificar el modelo. */
    public static final class Reduction {
        public final CnfFormula formula;
        public final VariableMap vars;
        Reduction(CnfFormula f, VariableMap v) { this.formula = f; this.vars = v; }
    }

    public Reduction reduce(TspInstance inst) {
        final int n = inst.size();
        final int k = inst.threshold();
        final int dmax = inst.maxDistance();

        // La restricción de costo es vacua si ningún recorrido puede costar más que K.
        final boolean costActive = n >= 2 && (long) n * dmax > k;
        final VariableMap vm = new VariableMap(n, costActive ? k + 1 : 0, dmax);
        final CnfFormula f = new CnfFormula(vm.numVars());

        // 1) Visita única: cada ciudad i aparece en exactamente un paso.
        for (int i = 0; i < n; i++) {
            int[] alo = new int[n];
            for (int t = 0; t < n; t++) alo[t] = vm.x(i, t);
            f.addClause(alo);
            for (int t1 = 0; t1 < n; t1++)
                for (int t2 = t1 + 1; t2 < n; t2++)
                    f.addClause(-vm.x(i, t1), -vm.x(i, t2));
        }

        // 2) Ocupación única: cada paso t tiene exactamente una ciudad.
        for (int t = 0; t < n; t++) {
            int[] alo = new int[n];
            for (int i = 0; i < n; i++) alo[i] = vm.x(i, t);
            f.addClause(alo);
            for (int i1 = 0; i1 < n; i1++)
                for (int i2 = i1 + 1; i2 < n; i2++)
                    f.addClause(-vm.x(i1, t), -vm.x(i2, t));
        }

        // 4) Ruptura de simetría (preserva satisfacibilidad: rotar un ciclo no cambia su costo).
        if (breakSymmetry) f.addClause(vm.x(0, 0));

        // 3) Restricción de costo.
        if (costActive) encodeCost(inst, vm, f);

        return new Reduction(f, vm);
    }

    private void encodeCost(TspInstance inst, VariableMap vm, CnfFormula f) {
        final int n = inst.size();
        final int cap = vm.cap(); // K+1

        // Monotonía del contador: A(s,v+1) -> A(s,v). Refuerza la propagación unitaria.
        for (int s = 1; s <= n; s++)
            for (int v = 1; v < vm.reach(s); v++)
                f.addClause(-vm.a(s, v + 1), vm.a(s, v));

        // Propagación del costo por tramo: el tramo t va de la ciudad en el paso t a la del paso t+1 (mod N).
        // Si el acumulado antes del tramo es >= v y se recorre (i -> j) de peso w, el acumulado
        // después es >= min(v + w, K+1).
        for (int t = 0; t < n; t++) {
            final int s = t + 1;
            final int before = t; // capa anterior (la capa 0 solo contiene v = 0, siempre verdadero)
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) continue;
                    final int w = inst.distance(i, j);
                    final int xi = vm.x(i, t);
                    final int xj = vm.x(j, (t + 1) % n);
                    final int maxV = (before == 0) ? 0 : vm.reach(before);
                    for (int v = 0; v <= maxV; v++) {
                        int target = (int) Math.min((long) v + w, cap);
                        if (v == 0) {
                            if (target > 0) f.addClause(-xi, -xj, vm.a(s, target));
                        } else {
                            f.addClause(-xi, -xj, -vm.a(before, v), vm.a(s, target));
                        }
                    }
                }
            }
        }

        // Cota: el costo total no puede alcanzar K+1.
        f.addClause(-vm.a(n, cap));
    }
}
