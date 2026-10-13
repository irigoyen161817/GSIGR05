package GSILabs.Misc;

import java.io.File;
import java.io.IOException;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

/**
 * Lee la matriz de 4&times;6 enteros que {@link SSTest02} guarda en las celdas
 * D6:I9 de la primera hoja de {@code test02.ods} y la muestra por consola.
 * <p>
 * La posición de la matriz está fijada en la clase, como permite el guion, y
 * solo se recuperan los valores numéricos, no el color de fondo. El fichero
 * debe existir en el directorio de trabajo, así que hay que ejecutar antes
 * {@link SSTest02}.
 */
public final class SSTest03 {

    private SSTest03() {
    }

    /**
     * Lee {@code test02.ods} del directorio de trabajo y muestra la matriz,
     * una fila por línea. Si el fichero no existe, avisa de que hay que
     * ejecutar antes {@link SSTest02} y termina con código 1.
     *
     * @param args no se usan
     * @throws IOException si no se puede leer el fichero
     */
    public static void main(String[] args) throws IOException {
        int filas = 4;
        int columnas = 6;
        int filasVacias = 5;
        int columnasVacias = 3;

        File fichero = new File("test02.ods");
        if (!fichero.exists()) {
            System.err.println("No se encuentra " + fichero.getAbsolutePath()
                    + ". Ejecuta antes SSTest02 para generarlo.");
            System.exit(1);
        }

        Sheet hoja = SpreadSheet.createFromFile(fichero).getSheet(0);
        int[][] matriz = new int[filas][columnas];
        for (int fila = 0; fila < filas; fila++) {
            for (int columna = 0; columna < columnas; columna++) {
                int filaHoja = filasVacias + fila;
                int columnaHoja = columnasVacias + columna;
                Number valor = (Number) hoja.getValueAt(columnaHoja, filaHoja);
                matriz[fila][columna] = valor.intValue();
            }
        }

        System.out.println("Matriz leída de " + fichero.getAbsolutePath() + ":");
        for (int[] fila : matriz) {
            StringBuilder linea = new StringBuilder();
            for (int valor : fila) {
                linea.append(String.format("%4d", valor));
            }
            System.out.println(linea);
        }
    }
}
