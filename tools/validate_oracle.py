#!/usr/bin/env python3
"""
Validación EXTERNA de la reducción (no forma parte del programa Java ni de la entrega funcional).
Compara el veredicto SAT/UNSAT del programa con una enumeración exhaustiva independiente sobre
instancias pequeñas (N <= 8, distancias en [0,15], incluyendo tramos de costo 0), usando K = óptimo-1, óptimo y óptimo+3. Uso:
    python3 tools/validate_oracle.py "java -cp out tspsat.Main" [--solver builtin]
"""
import itertools, random, subprocess, sys, tempfile, os, re

def optimum(d):
    n = len(d); best = None
    for p in itertools.permutations(range(1, n)):
        r = (0,) + p
        c = sum(d[r[i]][r[(i + 1) % n]] for i in range(n))
        best = c if best is None or c < best else best
    return best

def run(cmd, d, k, extra):
    n = len(d)
    with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False) as f:
        f.write(f"{n}\n{k}\n" + "\n".join(" ".join(map(str, r)) for r in d) + "\n")
        name = f.name
    out = subprocess.run(cmd.split() + ["--file", name] + extra, capture_output=True, text=True).stdout
    os.unlink(name)
    return out

def main():
    cmd = sys.argv[1]; extra = sys.argv[2:]
    random.seed(7); checked = bad = 0
    for trial in range(60):
        n = random.randint(3, 8)
        sym = random.random() < 0.5
        d = [[0 if i == j else random.randint(0, 15) for j in range(n)] for i in range(n)]
        if sym:
            for i in range(n):
                for j in range(i):
                    d[i][j] = d[j][i]
        opt = optimum(d)
        for k, expect_sat in ((opt - 1, False), (opt, True), (opt + 3, True)):
            for flags in ([], ["--no-symmetry"]):
                out = run(cmd, d, k, extra + flags)
                got_sat = "Verificación del certificado: OK" in out
                got_unsat = "NO existe ninguna ruta con costo menor o igual a K" in out
                ok = (expect_sat and got_sat) or (not expect_sat and got_unsat)
                if expect_sat:
                    m = re.search(r"Costo real: (\d+)", out)
                    ok = ok and m and int(m.group(1)) <= k
                checked += 1
                if not ok:
                    bad += 1; print("DISCREPANCIA", n, k, opt, flags, out)
    print(f"Casos verificados: {checked}  discrepancias: {bad}")
    sys.exit(1 if bad else 0)

main()
