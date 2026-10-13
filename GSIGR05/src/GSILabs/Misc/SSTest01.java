package GSILabs.Misc;

import java.io.File;
import java.io.IOException;
import javax.swing.table.DefaultTableModel;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

/**
 * Guarda una matriz de 4&times;6 enteros en las celdas A1:F4 de la primera
 * hoja de {@code test01.ods}.
 */
public final class SSTest01 {

    private SSTest01() {
    }

    /**
     * Genera {@code test01.ods} en el directorio de trabajo.
     *
     * @param args no se usan
     * @throws IOException si no se puede escribir el fichero
     */
    public static void main(String[] args) throws IOException {
        int[][] matriz = {
            {7, 4, 7, 5, 9, 5},
            {4, 7, 6, 8, 1, 6},
            {3, 1, 6, 5, 8, 9},
            {9, 8, 7, 2, 7, 3}
        };
        int filas = matriz.length;
        int columnas = matriz[0].length;

        SpreadSheet hojaDeCalculo = SpreadSheet.createEmpty(new DefaultTableModel(filas, columnas));
        Sheet hoja = hojaDeCalculo.getSheet(0);

        for (int fila = 0; fila < filas; fila++) {
            for (int columna = 0; columna < columnas; columna++) {
                hoja.setValueAt(matriz[fila][columna], columna, fila);
            }
        }

        File fichero = new File("test01.ods");
        hojaDeCalculo.saveAs(fichero);
        System.out.println("Matriz guardada en " + fichero.getAbsolutePath());
    }
}
