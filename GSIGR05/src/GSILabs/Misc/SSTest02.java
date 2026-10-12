package GSILabs.Misc;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import javax.swing.table.DefaultTableModel;
import org.jopendocument.dom.spreadsheet.MutableCell;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

/**
 * Guarda una matriz de 4&times;6 enteros en las celdas D6:I9 de la primera
 * hoja de {@code test02.ods}, con fondo azul si el valor es mayor o igual que
 * 10 y rojo si es menor.
 */
public final class SSTest02 {

    private SSTest02() {
    }

    /**
     * Genera {@code test02.ods} en el directorio de trabajo.
     *
     * @param args no se usan
     * @throws IOException si no se puede escribir el fichero
     */
    public static void main(String[] args) throws IOException {
        int[][] matriz = {
            {7, 4, 7, 5, 19, 5},
            {4, 7, 6, 18, 1, 6},
            {3, 11, 6, 15, 8, 9},
            {9, 8, 7, 22, 7, 13}
        };
        int filas = matriz.length;
        int columnas = matriz[0].length;
        int filasVacias = 5;
        int columnasVacias = 3;

        SpreadSheet hojaDeCalculo = SpreadSheet.createEmpty(new DefaultTableModel());
        Sheet hoja = hojaDeCalculo.getSheet(0);
        hoja.ensureRowCount(filasVacias + filas);
        hoja.ensureColumnCount(columnasVacias + columnas);

        for (int fila = 0; fila < filas; fila++) {
            for (int columna = 0; columna < columnas; columna++) {
                int valor = matriz[fila][columna];
                MutableCell<SpreadSheet> celda = hoja.getCellAt(columnasVacias + columna, filasVacias + fila);
                celda.setValue(valor);
                celda.setBackgroundColor(valor >= 10 ? Color.CYAN : Color.RED);
            }
        }

        File fichero = new File("test02.ods");
        hojaDeCalculo.saveAs(fichero);
        System.out.println("Matriz guardada en " + fichero.getAbsolutePath());
    }
}
