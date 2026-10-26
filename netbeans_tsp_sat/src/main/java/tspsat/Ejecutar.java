package tspsat;

/**
 *
 * @author edani
 */
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase para ejecutar el proyecto con el botón Run de NetBeans.
 * Edita solo la sección "CONFIGURACIÓN".
 */
public class Ejecutar {

    // ================== CONFIGURACIÓN ==================

    // Qué hacer: "RESOLVER" (una instancia), "BENCH_FIJO" (tabla con K holgado)
    //            o "BENCH_QUIEBRE" (busca el K mínimo y mide el punto de quiebre)
    static final String MODO = "RESOLVER";

    // Archivo de la instancia (relativo a la carpeta del proyecto, donde está pom.xml)
    static final String ARCHIVO = "ejemplo_n10.txt"; //Aquí se pone el archivo txt del ejemplo que quiera revisar

    // K para sobrescribir el del archivo; null = usar el del archivo
    static final Integer K = null;

    // "sat4j" (el exigido) o "builtin" (solver incluido, sin dependencias)
    static final String SOLVER = "sat4j";

    // Archivo DIMACS a generar (por ejemplo "salida.cnf"); null = no guardar
    static final String DIMACS = null;

    // Tiempo límite del solver en segundos (0 = sin límite)
    static final double TIMEOUT_SEG = 60;

    // Solo para los modos BENCH_*: tamaños N a probar
    static final String TAMANOS = "3,4,10";          // para BENCH_FIJO
    static final String TAMANOS_QUIEBRE = "8,10,11,12,13"; // para BENCH_QUIEBRE

    // ===================================================

    public static void main(String[] args) throws Exception {
        List<String> a = new ArrayList<>();

        switch (MODO) {
            case "RESOLVER":
                File f = new File(ARCHIVO);
                if (!f.exists()) {
                    System.out.println("No se encontró el archivo: " + f.getAbsolutePath());
                    System.out.println("Crea '" + ARCHIVO + "' en la carpeta del proyecto (junto a pom.xml).");
                    return;
                }
                a.add("--file"); a.add(ARCHIVO);
                if (K != null) { a.add("--k"); a.add(String.valueOf(K)); }
                if (DIMACS != null) { a.add("--dimacs"); a.add(DIMACS); }
                break;
            case "BENCH_FIJO":
                a.add("--bench"); a.add(TAMANOS);
                a.add("--ratio"); a.add("1.0");
                break;
            case "BENCH_QUIEBRE":
                a.add("--bench"); a.add(TAMANOS_QUIEBRE);
                a.add("--tight");
                break;
            default:
                System.out.println("MODO no válido: " + MODO);
                return;
        }
        a.add("--solver"); a.add(SOLVER);
        a.add("--timeout"); a.add(String.valueOf(TIMEOUT_SEG));

        Main.main(a.toArray(new String[0]));
    }
}