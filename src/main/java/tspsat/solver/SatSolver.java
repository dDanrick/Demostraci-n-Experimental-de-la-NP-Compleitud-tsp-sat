package tspsat.solver;

import tspsat.cnf.CnfFormula;

/** Interfaz mínima con un SAT-Solver: recibe una CNF y devuelve veredicto + modelo. */
public interface SatSolver {
    SolveResult solve(CnfFormula formula, long timeoutMillis) throws Exception;
    String name();
}
