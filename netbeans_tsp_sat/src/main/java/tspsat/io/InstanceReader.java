package tspsat.io;

/**
 *
 * @author edani
 */

import tspsat.model.TspInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class InstanceReader {
    private InstanceReader() { }

    public static TspInstance fromFile(Path path) throws IOException {
        List<String> tokens = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            for (String tk : s.split("[\\s,;]+")) if (!tk.isEmpty()) tokens.add(tk);
        }
        if (tokens.size() < 2) throw new IOException("Archivo incompleto: se esperaba N y K.");
        int n = Integer.parseInt(tokens.get(0));
        int k = Integer.parseInt(tokens.get(1));
        if (tokens.size() != 2 + n * n) {
            throw new IOException("Se esperaban " + (n * n) + " distancias y se encontraron " + (tokens.size() - 2) + ".");
        }
        int[][] d = new int[n][n];
        int p = 2;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) d[i][j] = Integer.parseInt(tokens.get(p++));
        return new TspInstance(d, k);
    }

    public static TspInstance fromArgs(int n, int k, String flatMatrix) {
        String[] tk = flatMatrix.trim().split("[\\s,;]+");
        if (tk.length != n * n) throw new IllegalArgumentException("La matriz debe tener N*N = " + (n * n) + " valores.");
        int[][] d = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) d[i][j] = Integer.parseInt(tk[i * n + j]);
        return new TspInstance(d, k);
    }

    public static int[][] randomSymmetric(int n, int maxDist, long seed) {
        Random r = new Random(seed);
        int[][] d = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) d[i][j] = d[j][i] = 1 + r.nextInt(maxDist);
        return d;
    }
}
