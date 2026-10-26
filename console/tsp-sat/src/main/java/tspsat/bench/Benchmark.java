package tspsat.bench;

import tspsat.cnf.CnfFormula;
import tspsat.decode.SolutionDecoder;
import tspsat.io.InstanceReader;
import tspsat.model.TspInstance;
import tspsat.reduction.TspToSatReducer;
import tspsat.solver.SatSolver;
import tspsat.solver.SolveResult;

/**
 * Pruebas de rendimiento. Para cada N genera una matriz simétrica aleatoria y fija
 * K = floor(ratio * costo(0,1,...,N-1,0)). Con ratio = 1.0 la instancia es SAT por construcción
 * (el recorrido identidad es un certificado); con ratio &lt; 1 el resultado es desconocido a priori.
 * El benchmark solo mide: nunca resuelve el TSP por su cuenta.
 */
public final class Benchmark {
    private Benchmark() { }

    public static void run(int[] sizes, int maxDist, double ratio, long seed, long timeoutMs,
                           SatSolver solver, boolean breakSymmetry) {
        System.out.println("# Solver: " + solver.name() + " | maxDist=" + maxDist + " ratio=" + ratio + " seed=" + seed);
        System.out.println("N;K;variables;clausulas;t_reduccion_ms;t_solver_ms;veredicto;costo_ruta");
        for (int n : sizes) {
            int[][] d = InstanceReader.randomSymmetric(n, maxDist, seed + n);
            int[] ident = new int[n];
            for (int i = 0; i < n; i++) ident[i] = i;
            int k = (int) Math.floor(ratio * new TspInstance(d, 0).tourCost(ident));
            TspInstance inst = new TspInstance(d, k);

            long t0 = System.nanoTime();
            TspToSatReducer.Reduction red = new TspToSatReducer(breakSymmetry).reduce(inst);
            double genMs = (System.nanoTime() - t0) / 1e6;
            CnfFormula f = red.formula;
            String verdict, cost = "-";
            long solveMs;
            try {
                SolveResult r = solver.solve(f, timeoutMs);
                solveMs = r.millis;
                verdict = r.status == SolveResult.Status.SATISFIABLE ? "SAT"
                        : r.status == SolveResult.Status.UNSATISFIABLE ? "UNSAT" : "TIMEOUT";
                if (r.status == SolveResult.Status.SATISFIABLE) {
                    SolutionDecoder.Tour t = SolutionDecoder.decode(inst, red.vars, r);
                    cost = t.cost + (t.valid ? "" : "(INVALIDA)");
                }
            } catch (Exception e) {
                solveMs = -1; verdict = "ERROR:" + e.getMessage();
            }
            System.out.printf("%d;%d;%d;%d;%.1f;%d;%s;%s%n", n, k, f.numVars(), f.numClauses(), genMs, solveMs, verdict, cost);
        }
    }

    /**
     * Modo "punto de quiebre": localiza el K mínimo con SAT mediante bisección sobre K, es decir,
     * convierte el problema de decisión en optimización llamando repetidamente al SAT-Solver.
     * Reporta el tiempo del último SAT (K = K*) y del último UNSAT (K = K*-1), que es la prueba
     * de optimalidad y el caso más difícil. Ninguna decisión sobre rutas la toma este código.
     */
    public static void runTight(int[] sizes, int maxDist, long seed, long timeoutMs,
                                SatSolver solver, boolean breakSymmetry) {
        System.out.println("# Modo tight (bisección sobre K) | Solver: " + solver.name() + " | maxDist=" + maxDist + " seed=" + seed);
        System.out.println("N;K*;variables;clausulas_K*;t_SAT_K*_ms;t_UNSAT_K*-1_ms;t_total_bisec_ms;estado");
        for (int n : sizes) {
            int[][] d = InstanceReader.randomSymmetric(n, maxDist, seed + n);
            int[] ident = new int[n];
            for (int i = 0; i < n; i++) ident[i] = i;
            int hi = (int) new TspInstance(d, 0).tourCost(ident); // SAT por construcción
            int lo = -1;                                          // K = -1 sería UNSAT (costos >= 0)
            long tSat = -1, tUnsat = -1, total = 0;
            int vars = 0, clauses = 0;
            String status = "OK";
            try {
                while (hi - lo > 1) {
                    int mid = (lo + hi) >>> 1;
                    TspInstance inst = new TspInstance(d, mid);
                    TspToSatReducer.Reduction red = new TspToSatReducer(breakSymmetry).reduce(inst);
                    SolveResult r = solver.solve(red.formula, timeoutMs);
                    total += r.millis;
                    if (r.status == SolveResult.Status.UNKNOWN) { status = "TIMEOUT(K=" + mid + ")"; break; }
                    if (r.status == SolveResult.Status.SATISFIABLE) {
                        SolutionDecoder.Tour t = SolutionDecoder.decode(inst, red.vars, r);
                        hi = (int) Math.min(mid, t.cost);
                        tSat = r.millis; vars = red.formula.numVars(); clauses = red.formula.numClauses();
                    } else {
                        lo = mid; tUnsat = r.millis;
                    }
                }
            } catch (Exception e) { status = "ERROR:" + e.getMessage(); }
            System.out.printf("%d;%d;%d;%d;%d;%d;%d;%s%n", n, hi, vars, clauses, tSat, tUnsat, total, status);
        }
    }
}
