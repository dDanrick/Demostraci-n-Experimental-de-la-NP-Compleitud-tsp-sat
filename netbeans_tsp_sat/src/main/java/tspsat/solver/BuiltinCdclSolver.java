package tspsat.solver;

/**
 *
 * @author edani
 */

import tspsat.cnf.CnfFormula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** SAT-Solver CDCL genérico (no sabe nada del TSP). Alternativa sin dependencias. */
public final class BuiltinCdclSolver implements SatSolver {

    private int n;
    private byte[] litVal;
    private int[] level;
    private int[][] reason;
    private int[] trail;
    private int trailSize, qhead;
    private int[] trailLim;
    private int numLevels;
    private List<int[]>[] watches;
    private final List<int[]> originals = new ArrayList<>();
    private final List<int[]> learnts = new ArrayList<>();
    private double[] activity;
    private double inc = 1.0;
    private int[] heap, heapPos;
    private int heapSize;
    private boolean[] phase, seen;

    public String name() { return "CDCL integrado (VSIDS + 1-UIP + Luby)"; }

    @SuppressWarnings("unchecked")
    private void init(int vars) {
        n = vars;
        originals.clear();
        learnts.clear();
        trailSize = 0; qhead = 0; numLevels = 0; heapSize = 0; inc = 1.0;
        litVal = new byte[2 * n + 2];
        level = new int[n + 1];
        reason = new int[n + 1][];
        trail = new int[n + 1];
        trailLim = new int[n + 2];
        watches = new List[2 * n + 2];
        for (int i = 0; i < watches.length; i++) watches[i] = new ArrayList<>();
        activity = new double[n + 1];
        heap = new int[n + 1];
        heapPos = new int[n + 1];
        Arrays.fill(heapPos, -1);
        phase = new boolean[n + 1];
        seen = new boolean[n + 1];
        for (int v = 1; v <= n; v++) heapInsert(v);
    }

    public SolveResult solve(CnfFormula f, long timeoutMillis) {
        long t0 = System.nanoTime();
        long deadline = timeoutMillis > 0 ? t0 + timeoutMillis * 1_000_000L : Long.MAX_VALUE;
        init(f.numVars());
        for (int[] c : f.clauses()) {
            if (!addOriginal(c)) return done(SolveResult.Status.UNSATISFIABLE, t0);
        }
        if (propagate() != null) return done(SolveResult.Status.UNSATISFIABLE, t0);

        long conflicts = 0;
        int restartIdx = 1;
        long untilRestart = 100L * luby(restartIdx);
        int maxLearnts = Math.max(20000, originals.size() / 2);

        while (true) {
            int[] confl = propagate();
            if (confl != null) {
                conflicts++;
                if (numLevels == 0) return done(SolveResult.Status.UNSATISFIABLE, t0);
                int[] learnt = analyze(confl);
                int bt = 0;
                if (learnt.length > 1) {
                    int mi = 1;
                    for (int i = 2; i < learnt.length; i++)
                        if (level[learnt[i] >> 1] > level[learnt[mi] >> 1]) mi = i;
                    int tmp = learnt[1]; learnt[1] = learnt[mi]; learnt[mi] = tmp;
                    bt = level[learnt[1] >> 1];
                }
                cancelUntil(bt);
                if (learnt.length == 1) {
                    enqueue(learnt[0], null);
                } else {
                    learnts.add(learnt);
                    watches[learnt[0]].add(learnt);
                    watches[learnt[1]].add(learnt);
                    enqueue(learnt[0], learnt);
                }
                inc /= 0.95;
                untilRestart--;
                if ((conflicts & 255) == 0 && System.nanoTime() > deadline) return done(SolveResult.Status.UNKNOWN, t0);
            } else {
                if (untilRestart <= 0) {
                    cancelUntil(0);
                    restartIdx++;
                    untilRestart = 100L * luby(restartIdx);
                    if (learnts.size() > maxLearnts) { reduceDb(); maxLearnts += maxLearnts / 10; }
                    if (System.nanoTime() > deadline) return done(SolveResult.Status.UNKNOWN, t0);
                    continue;
                }
                int v = pickBranch();
                if (v == 0) {
                    int[] model = new int[n];
                    for (int i = 1; i <= n; i++) model[i - 1] = litVal[2 * i] == 1 ? i : -i;
                    return new SolveResult(SolveResult.Status.SATISFIABLE, model, (System.nanoTime() - t0) / 1_000_000);
                }
                trailLim[numLevels++] = trailSize;
                enqueue(2 * v + (phase[v] ? 0 : 1), null);
            }
        }
    }

    private SolveResult done(SolveResult.Status s, long t0) {
        return new SolveResult(s, null, (System.nanoTime() - t0) / 1_000_000);
    }

    private boolean addOriginal(int[] dimacs) {
        int[] tmp = new int[dimacs.length];
        int m = 0;
        for (int d : dimacs) {
            int lit = d > 0 ? 2 * d : 2 * (-d) + 1;
            boolean dup = false, taut = false;
            for (int i = 0; i < m; i++) { if (tmp[i] == lit) dup = true; if (tmp[i] == (lit ^ 1)) taut = true; }
            if (taut) return true;
            if (!dup) tmp[m++] = lit;
        }
        if (m == 0) return false;
        int[] c = Arrays.copyOf(tmp, m);
        if (m == 1) {
            if (litVal[c[0]] == -1) return false;
            if (litVal[c[0]] == 0) enqueue(c[0], null);
            return true;
        }
        originals.add(c);
        watches[c[0]].add(c);
        watches[c[1]].add(c);
        return true;
    }

    private void enqueue(int lit, int[] why) {
        litVal[lit] = 1;
        litVal[lit ^ 1] = -1;
        int v = lit >> 1;
        level[v] = numLevels;
        reason[v] = why;
        trail[trailSize++] = lit;
    }

    private int[] propagate() {
        while (qhead < trailSize) {
            int p = trail[qhead++];
            int fl = p ^ 1;
            List<int[]> ws = watches[fl];
            int i = 0, j = 0, sz = ws.size();
            while (i < sz) {
                int[] c = ws.get(i++);
                if (c[0] == fl) { c[0] = c[1]; c[1] = fl; }
                if (litVal[c[0]] == 1) { ws.set(j++, c); continue; }
                boolean moved = false;
                for (int k = 2; k < c.length; k++) {
                    if (litVal[c[k]] != -1) {
                        c[1] = c[k]; c[k] = fl;
                        watches[c[1]].add(c);
                        moved = true;
                        break;
                    }
                }
                if (moved) continue;
                ws.set(j++, c);
                if (litVal[c[0]] == -1) {
                    while (i < sz) ws.set(j++, ws.get(i++));
                    ws.subList(j, sz).clear();
                    qhead = trailSize;
                    return c;
                }
                enqueue(c[0], c);
            }
            ws.subList(j, sz).clear();
        }
        return null;
    }

    private int[] analyze(int[] confl) {
        List<Integer> out = new ArrayList<>();
        out.add(0);
        int pathC = 0, p = -1, idx = trailSize - 1;
        int[] c = confl;
        do {
            for (int k = (p == -1 ? 0 : 1); k < c.length; k++) {
                int q = c[k], v = q >> 1;
                if (!seen[v] && level[v] > 0) {
                    bump(v);
                    seen[v] = true;
                    if (level[v] >= numLevels) pathC++; else out.add(q);
                }
            }
            while (!seen[trail[idx--] >> 1]) { }
            p = trail[idx + 1];
            c = reason[p >> 1];
            seen[p >> 1] = false;
            pathC--;
        } while (pathC > 0);
        out.set(0, p ^ 1);
        int[] res = new int[out.size()];
        for (int i = 0; i < res.length; i++) res[i] = out.get(i);
        for (int i = 1; i < res.length; i++) seen[res[i] >> 1] = false;
        return res;
    }

    private void cancelUntil(int lvl) {
        if (numLevels <= lvl) return;
        for (int i = trailSize - 1; i >= trailLim[lvl]; i--) {
            int lit = trail[i], v = lit >> 1;
            phase[v] = (lit & 1) == 0;
            litVal[lit] = 0;
            litVal[lit ^ 1] = 0;
            reason[v] = null;
            if (heapPos[v] < 0) heapInsert(v);
        }
        trailSize = trailLim[lvl];
        qhead = trailSize;
        numLevels = lvl;
    }

    private void reduceDb() {
        List<int[]> keep = new ArrayList<>();
        int half = learnts.size() / 2;
        for (int i = 0; i < learnts.size(); i++) {
            int[] c = learnts.get(i);
            if (i >= half || c.length <= 3) keep.add(c);
        }
        learnts.clear();
        learnts.addAll(keep);
        for (List<int[]> w : watches) w.clear();
        for (int[] c : originals) { watches[c[0]].add(c); watches[c[1]].add(c); }
        for (int[] c : learnts) { watches[c[0]].add(c); watches[c[1]].add(c); }
    }

    private void bump(int v) {
        if ((activity[v] += inc) > 1e100) {
            for (int i = 1; i <= n; i++) activity[i] *= 1e-100;
            inc *= 1e-100;
        }
        if (heapPos[v] >= 0) siftUp(heapPos[v]);
    }

    private int pickBranch() {
        while (heapSize > 0) {
            int v = heapPop();
            if (litVal[2 * v] == 0) return v;
        }
        return 0;
    }

    private void heapInsert(int v) { heap[heapSize] = v; heapPos[v] = heapSize; siftUp(heapSize++); }

    private int heapPop() {
        int top = heap[0];
        heapPos[top] = -1;
        heapSize--;
        if (heapSize > 0) { heap[0] = heap[heapSize]; heapPos[heap[0]] = 0; siftDown(0); }
        return top;
    }

    private void siftUp(int i) {
        int v = heap[i];
        while (i > 0) {
            int par = (i - 1) >> 1;
            if (activity[heap[par]] >= activity[v]) break;
            heap[i] = heap[par]; heapPos[heap[i]] = i; i = par;
        }
        heap[i] = v; heapPos[v] = i;
    }

    private void siftDown(int i) {
        int v = heap[i];
        while (true) {
            int c = 2 * i + 1;
            if (c >= heapSize) break;
            if (c + 1 < heapSize && activity[heap[c + 1]] > activity[heap[c]]) c++;
            if (activity[heap[c]] <= activity[v]) break;
            heap[i] = heap[c]; heapPos[heap[i]] = i; i = c;
        }
        heap[i] = v; heapPos[v] = i;
    }

    private static long luby(int i) {
        long x = i - 1L;
        long size = 1; int seq = 0;
        while (size < x + 1) { seq++; size = 2 * size + 1; }
        while (size - 1 != x) { size = (size - 1) >> 1; seq--; x = x % size; }
        return 1L << seq;
    }
}
