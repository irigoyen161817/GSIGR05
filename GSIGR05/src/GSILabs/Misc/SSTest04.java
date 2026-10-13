package GSILabs.Misc;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Direccion;
import GSILabs.BModel.DominioException;
import GSILabs.BModel.Local;
import GSILabs.BModel.Propietario;
import GSILabs.BModel.Pub;
import GSILabs.BModel.Restaurante;
import GSILabs.BSystem.BusinessSystem;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import javax.swing.table.DefaultTableModel;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

/**
 * Da de alta bares, restaurantes y pubs en un {@link BusinessSystem} y los
 * exporta a {@code test04.ods}, con una hoja por tipo de local: {@code Bar},
 * {@code Restaurante} y {@code Pub}.
 * <p>
 * Los locales se recuperan del sistema con
 * {@link BusinessSystem#obtenerLocales()}, no de las variables de esta clase,
 * y cada uno se escribe en la hoja de su tipo. Cada local ocupa una fila,
 * empezando por la fila 1 y sin fila de cabecera, como en la Figura 3 del
 * guion. Los campos se convierten a celdas así:
 * <ul>
 * <li>A: nombre del local.</li>
 * <li>B: calle y número de la dirección, separados por un espacio, en una
 * sola celda.</li>
 * <li>C: localidad.</li>
 * <li>D: provincia.</li>
 * <li>E: nicks de todos los dueños, separados por {@code ", "}. Un local
 * tiene entre 1 y 3 dueños, y juntarlos en una celda mantiene el formato de
 * la Figura 3 sin chocar con las especialidades, que también ocupan un número
 * variable de columnas.</li>
 * <li>F en adelante, solo en la hoja {@code Bar}: una especialidad por
 * celda, todas las del bar.</li>
 * </ul>
 * Los restaurantes y los pubs solo guardan el nombre, la dirección y los
 * dueños, que es lo que pide el guion.
 */
public final class SSTest04 {

    private SSTest04() {
    }

    /**
     * Puebla un {@link BusinessSystem} con propietarios y locales de los tres
     * tipos, y genera {@code test04.ods} en el directorio de trabajo. El
     * resultado se comprueba abriendo el fichero en un programa ofimático,
     * como recomienda el guion.
     *
     * @param args no se usan
     * @throws DominioException si algún dato de prueba incumple una regla de
     *         negocio, ya sea al crearlo o al darlo de alta en el sistema
     * @throws IOException si no se puede escribir el fichero
     */
    public static void main(String[] args) throws DominioException, IOException {
        BusinessSystem sistema = new BusinessSystem();
        LocalDate hoy = LocalDate.now();
        Propietario ana = new Propietario("ana", "secreto", hoy.minusYears(41));
        Propietario luis = new Propietario("luis", "secreto", hoy.minusYears(55));
        Propietario marta = new Propietario("marta", "secreto", hoy.minusYears(33));
        for (Propietario propietario : new Propietario[] {ana, luis, marta}) {
            exigir(sistema.nuevoUsuario(propietario), sistema);
        }

        Bar estafeta = new Bar("La Estafeta",
                new Direccion("Pamplona", "Navarra", "Calle Estafeta", "12"), null, ana);
        estafeta.añadirEspecialidad("Pintxos");
        estafeta.añadirEspecialidad("Vermú");
        estafeta.añadirEspecialidad("Txistorra");
        Bar laurel = new Bar("Bar Laurel",
                new Direccion("Logroño", "La Rioja", "Calle Laurel", "8"), null, luis);
        laurel.añadirEspecialidad("Champiñones");
        Restaurante asador = new Restaurante("Asador Mikel",
                new Direccion("Pamplona", "Navarra", "Calle Mayor", "3"), null, marta,
                new BigDecimal("25.50"), 60, 8);
        Restaurante remanso = new Restaurante("El Remanso",
                new Direccion("Tudela", "Navarra", "Plaza de los Fueros", "1"), null, ana,
                new BigDecimal("18"), 40, 6);
        Pub irish = new Pub("The Irish",
                new Direccion("Pamplona", "Navarra", "Calle San Nicolás", "20"), null, luis,
                LocalTime.of(20, 0), LocalTime.of(3, 0));
        Pub cubo = new Pub("El Cubo",
                new Direccion("Logroño", "La Rioja", "Calle San Juan", "14"), null, marta,
                LocalTime.of(22, 0), LocalTime.of(5, 0));
        for (Local local : new Local[] {estafeta, laurel, asador, remanso, irish, cubo}) {
            exigir(sistema.nuevoLocal(local), sistema);
        }
        exigir(sistema.asociarLocal(estafeta, luis), sistema);
        exigir(sistema.asociarLocal(estafeta, marta), sistema);

        SpreadSheet hojaDeCalculo = SpreadSheet.createEmpty(new DefaultTableModel());
        Sheet hojaBar = hojaDeCalculo.getSheet(0);
        hojaBar.setName("Bar");
        Sheet hojaRestaurante = hojaDeCalculo.addSheet("Restaurante");
        Sheet hojaPub = hojaDeCalculo.addSheet("Pub");

        int bares = 0;
        int restaurantes = 0;
        int pubs = 0;
        for (Local local : sistema.obtenerLocales()) {
            if (local instanceof Bar) {
                escribirLocal(hojaBar, bares++, local);
            } else if (local instanceof Restaurante) {
                escribirLocal(hojaRestaurante, restaurantes++, local);
            } else {
                escribirLocal(hojaPub, pubs++, local);
            }
        }

        File fichero = new File("test04.ods");
        hojaDeCalculo.saveAs(fichero);
        System.out.println(bares + " bares, " + restaurantes + " restaurantes y " + pubs
                + " pubs guardados en " + fichero.getAbsolutePath());
    }

    private static void exigir(boolean aceptado, BusinessSystem sistema) throws DominioException {
        if (!aceptado) {
            throw new DominioException(sistema.getUltimoError());
        }
    }

    private static void escribirLocal(Sheet hoja, int fila, Local local) {
        Direccion direccion = local.getDireccion();
        StringJoiner dueños = new StringJoiner(", ");
        for (Propietario dueño : local.getDueños()) {
            dueños.add(dueño.getNick());
        }

        List<String> celdas = new ArrayList<>();
        celdas.add(local.getNombre());
        celdas.add(direccion.getCalle() + " " + direccion.getNumero());
        celdas.add(direccion.getLocalidad());
        celdas.add(direccion.getProvincia());
        celdas.add(dueños.toString());
        if (local instanceof Bar bar) {
            celdas.addAll(bar.getEspecialidades());
        }

        hoja.ensureRowCount(fila + 1);
        hoja.ensureColumnCount(celdas.size());
        for (int columna = 0; columna < celdas.size(); columna++) {
            hoja.setValueAt(celdas.get(columna), columna, fila);
        }
    }
}
