package tspsat;

import tspsat.bench.Benchmark;
import tspsat.decode.SolutionDecoder;
import tspsat.io.InstanceReader;
import tspsat.model.TspInstance;
import tspsat.reduction.TspToSatReducer;
import tspsat.solver.BuiltinCdclSolver;
import tspsat.solver.SatSolver;
import tspsat.solver.SolveResult;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Punto de entrada. Flujo automatizado: lectura -> reducción a CNF (DIMACS) -> SAT-Solver -> reconstrucción.
 * El programa NO resuelve el TSP: toda la búsqueda la realiza el SAT-Solver.
 */
public final class Main {

    private static final String USAGE = String.join("\n",
        "Uso:",
        "  java -jar tsp-sat.jar --file instancia.txt [--k K] [--solver sat4j|builtin] [--dimacs salida.cnf] [--timeout seg]",
        "  java -jar tsp-sat.jar --n N --k K --matrix \"d00 d01 ... d(N-1)(N-1)\" [opciones]",
        "  java -jar tsp-sat.jar --bench 3,4,5,6,7,8,9,10 [--maxdist D] [--ratio R] [--seed S] [--timeout seg] [--solver ...]",
        "  java -jar tsp-sat.jar --bench 8,10,12 --tight   (busca K mínimo por bisección y mide el último SAT/UNSAT)",
        "Opciones: --no-symmetry (desactiva la ruptura de simetría)");

    public static void main(String[] argv) throws Exception {
        System.setOut(new PrintStream(System.out, true, "UTF-8"));
        Map<String, String> a = parse(argv);
        if (a.containsKey("help") || argv.length == 0) { System.out.println(USAGE); return; }

        SatSolver solver = chooseSolver(a.getOrDefault("solver", "sat4j"));
        long timeoutMs = (long) (Double.parseDouble(a.getOrDefault("timeout", "0")) * 1000);
        boolean sym = !a.containsKey("no-symmetry");

        if (a.containsKey("bench")) {
            String[] ns = a.get("bench").split(",");
            int[] sizes = new int[ns.length];
            for (int i = 0; i < ns.length; i++) sizes[i] = Integer.parseInt(ns[i].trim());
            if (a.containsKey("tight")) {
                Benchmark.runTight(sizes, Integer.parseInt(a.getOrDefault("maxdist", "20")),
                    Long.parseLong(a.getOrDefault("seed", "42")), timeoutMs, solver, sym);
                return;
            }
            Benchmark.run(sizes,
                Integer.parseInt(a.getOrDefault("maxdist", "20")),
                Double.parseDouble(a.getOrDefault("ratio", "1.0")),
                Long.parseLong(a.getOrDefault("seed", "42")),
                timeoutMs, solver, sym);
            return;
        }

        // Fase 0: lectura de la instancia
        TspInstance inst;
        if (a.containsKey("file")) {
            inst = InstanceReader.fromFile(Paths.get(a.get("file")));
            if (a.containsKey("k")) inst = inst.withThreshold(Integer.parseInt(a.get("k")));
        } else if (a.containsKey("n") && a.containsKey("k") && a.containsKey("matrix")) {
            inst = InstanceReader.fromArgs(Integer.parseInt(a.get("n")), Integer.parseInt(a.get("k")), a.get("matrix"));
        } else {
            System.err.println("Faltan datos de entrada.\n" + USAGE);
            System.exit(2);
            return;
        }

        // Fase 1: reducción TSP -> SAT
        long t0 = System.nanoTime();
        TspToSatReducer.Reduction red = new TspToSatReducer(sym).reduce(inst);
        long genMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("Instancia: N = %d, K = %d%n", inst.size(), inst.threshold());
        System.out.printf("CNF generada: %d variables, %d cláusulas (%d ms)%n",
            red.formula.numVars(), red.formula.numClauses(), genMs);
        if (a.containsKey("dimacs")) {
            Path out = Paths.get(a.get("dimacs"));
            red.formula.writeDimacs(out, "TSP decision -> SAT", "N=" + inst.size() + " K=" + inst.threshold());
            System.out.println("Archivo DIMACS escrito en " + out);
        }

        // Fase 2: SAT-Solver
        System.out.println("Solver: " + solver.name());
        SolveResult res = solver.solve(red.formula, timeoutMs);
        System.out.printf("Veredicto: %s (%d ms)%n", res.status, res.millis);

        // Fase 3: reconstrucción
        switch (res.status) {
            case UNSATISFIABLE:
                System.out.println("NO existe ninguna ruta con costo menor o igual a K");
                break;
            case SATISFIABLE:
                SolutionDecoder.Tour tour = SolutionDecoder.decode(inst, red.vars, res);
                StringBuilder sb = new StringBuilder();
                for (int c : tour.route) sb.append(c).append(" -> ");
                sb.append(tour.route[0]);
                System.out.println("Ruta encontrada: " + sb);
                System.out.printf("Costo real: %d (K = %d)%n", tour.cost, inst.threshold());
                System.out.println("Verificación del certificado: " + (tour.valid ? "OK" : "FALLÓ"));
                if (!tour.valid) System.exit(3);
                break;
            default:
                System.out.println("El solver no terminó dentro del tiempo límite (resultado indeterminado).");
        }
    }

    private static SatSolver chooseSolver(String name) throws Exception {
        if (name.equalsIgnoreCase("builtin")) return new BuiltinCdclSolver();
        return (SatSolver) Class.forName("tspsat.solver.Sat4jSolver").getDeclaredConstructor().newInstance();
    }

    private static Map<String, String> parse(String[] argv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < argv.length; i++) {
            if (!argv[i].startsWith("--")) continue;
            String key = argv[i].substring(2);
            if (i + 1 < argv.length && !argv[i + 1].startsWith("--")) m.put(key, argv[++i]);
            else m.put(key, "true");
        }
        return m;
    }
}
