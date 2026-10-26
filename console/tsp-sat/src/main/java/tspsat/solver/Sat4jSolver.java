package tspsat.solver;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import tspsat.cnf.CnfFormula;

/** Adaptador a Sat4j (org.ow2.sat4j.core). Es el solver exigido por el enunciado. */
public final class Sat4jSolver implements SatSolver {

    @Override
    public SolveResult solve(CnfFormula formula, long timeoutMillis) throws Exception {
        long t0 = System.nanoTime();
        ISolver solver = SolverFactory.newDefault();
        if (timeoutMillis > 0) solver.setTimeoutMs(timeoutMillis);
        solver.newVar(formula.numVars());
        solver.setExpectedNumberOfClauses(formula.numClauses());
        try {
            for (int[] c : formula.clauses()) solver.addClause(new VecInt(c));
        } catch (ContradictionException e) {
            // Sat4j detecta la contradicción ya al añadir cláusulas (propagación unitaria en nivel 0).
            return new SolveResult(SolveResult.Status.UNSATISFIABLE, null, (System.nanoTime() - t0) / 1_000_000);
        }
        try {
            if (solver.isSatisfiable()) {
                return new SolveResult(SolveResult.Status.SATISFIABLE, solver.model(), (System.nanoTime() - t0) / 1_000_000);
            }
            return new SolveResult(SolveResult.Status.UNSATISFIABLE, null, (System.nanoTime() - t0) / 1_000_000);
        } catch (TimeoutException e) {
            return new SolveResult(SolveResult.Status.UNKNOWN, null, (System.nanoTime() - t0) / 1_000_000);
        }
    }

    @Override
    public String name() { return "Sat4j (SolverFactory.newDefault)"; }
}
