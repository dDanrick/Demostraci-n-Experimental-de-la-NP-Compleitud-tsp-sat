package tspsat.solver;

/**
 *
 * @author edani
 */

import tspsat.cnf.CnfFormula;

public interface SatSolver {
    SolveResult solve(CnfFormula formula, long timeoutMillis) throws Exception;
    String name();
}
