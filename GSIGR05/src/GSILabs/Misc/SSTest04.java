package GSILabs.Misc;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Direccion;
import GSILabs.BModel.DominioException;
import GSILabs.BModel.Local;
import GSILabs.BModel.Propietario;
import GSILabs.BModel.Pub;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Usuario;
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
 * Los locales se recuperan del sistema, no de las variables de esta clase.
 * Como {@link BusinessSystem#listarBares(String, String)} y sus equivalentes
 * para restaurantes y pubs exigen una localidad y una provincia, la clase
 * recorre las localidades en las que ha dado de alta locales y pide al
 * sistema los de cada una.
 * <p>
 * Cada local ocupa una fila, empezando por la fila 1 y sin fila de cabecera,
 * como en la Figura 3 del guion. Los campos se convierten a celdas así:
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

    private static final String[][] LOCALIDADES = {
        {"Pamplona", "Navarra"},
        {"Tudela", "Navarra"},
        {"Logroño", "La Rioja"}
    };

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
     *         negocio al crear los usuarios o los locales
     * @throws IllegalStateException si el sistema rechaza el alta de algún
     *         usuario, local o dueño de prueba
     * @throws IOException si no se puede escribir el fichero
     */
    public static void main(String[] args) throws DominioException, IOException {
        BusinessSystem sistema = poblarSistema();

        List<List<Object>> bares = new ArrayList<>();
        List<List<Object>> restaurantes = new ArrayList<>();
        List<List<Object>> pubs = new ArrayList<>();
        for (String[] localidad : LOCALIDADES) {
            for (Bar bar : sistema.listarBares(localidad[0], localidad[1])) {
                List<Object> fila = filaBasica(bar);
                fila.addAll(bar.getEspecialidades());
                bares.add(fila);
            }
            for (Restaurante restaurante : sistema.listarRestaurantes(localidad[0], localidad[1])) {
                restaurantes.add(filaBasica(restaurante));
            }
            for (Pub pub : sistema.listarPubs(localidad[0], localidad[1])) {
                pubs.add(filaBasica(pub));
            }
        }

        SpreadSheet hojaDeCalculo = SpreadSheet.createEmpty(new DefaultTableModel());
        Sheet hojaBar = hojaDeCalculo.getSheet(0);
        hojaBar.setName("Bar");
        escribir(hojaBar, bares);
        escribir(hojaDeCalculo.addSheet("Restaurante"), restaurantes);
        escribir(hojaDeCalculo.addSheet("Pub"), pubs);

        File fichero = new File("test04.ods");
        hojaDeCalculo.saveAs(fichero);
        System.out.println(bares.size() + " bares, " + restaurantes.size() + " restaurantes y "
                + pubs.size() + " pubs guardados en " + fichero.getAbsolutePath());
    }

    private static BusinessSystem poblarSistema() throws DominioException {
        BusinessSystem sistema = new BusinessSystem();
        LocalDate hoy = LocalDate.now();
        Propietario ana = registrar(sistema, new Propietario("ana", "secreto", hoy.minusYears(41)));
        Propietario luis = registrar(sistema, new Propietario("luis", "secreto", hoy.minusYears(55)));
        Propietario marta = registrar(sistema, new Propietario("marta", "secreto", hoy.minusYears(33)));
        Propietario jon = registrar(sistema, new Propietario("jon", "secreto", hoy.minusYears(28)));

        Bar estafeta = new Bar("La Estafeta",
                new Direccion("Pamplona", "Navarra", "Calle Estafeta", "12"), null, ana);
        estafeta.añadirEspecialidad("Pintxos");
        estafeta.añadirEspecialidad("Vermú");
        estafeta.añadirEspecialidad("Txistorra");
        Bar laRibera = new Bar("La Ribera",
                new Direccion("Tudela", "Navarra", "Calle Herrerías", "5"), null, luis);
        laRibera.añadirEspecialidad("Verduras");
        Bar laLaurel = new Bar("Bar Laurel",
                new Direccion("Logroño", "La Rioja", "Calle Laurel", "8"), null, jon);
        laLaurel.añadirEspecialidad("Champiñones");
        laLaurel.añadirEspecialidad("Vino de Rioja");

        Restaurante asador = new Restaurante("Asador Mikel",
                new Direccion("Pamplona", "Navarra", "Calle Mayor", "3"), null, ana,
                new BigDecimal("25.50"), 60, 8);
        Restaurante remanso = new Restaurante("El Remanso",
                new Direccion("Tudela", "Navarra", "Plaza de los Fueros", "1"), null, marta,
                new BigDecimal("18"), 40, 6);

        Pub irish = new Pub("The Irish",
                new Direccion("Pamplona", "Navarra", "Calle San Nicolás", "20"), null, jon,
                LocalTime.of(20, 0), LocalTime.of(3, 0));
        Pub cubo = new Pub("El Cubo",
                new Direccion("Logroño", "La Rioja", "Calle San Juan", "14"), null, marta,
                LocalTime.of(22, 0), LocalTime.of(5, 0));

        for (Local local : new Local[] {estafeta, laRibera, laLaurel, asador, remanso, irish, cubo}) {
            if (!sistema.nuevoLocal(local)) {
                throw new IllegalStateException(sistema.getUltimoError());
            }
        }
        asociar(sistema, estafeta, luis);
        asociar(sistema, estafeta, marta);
        asociar(sistema, asador, jon);
        return sistema;
    }

    private static <T extends Usuario> T registrar(BusinessSystem sistema, T usuario) {
        if (!sistema.nuevoUsuario(usuario)) {
            throw new IllegalStateException(sistema.getUltimoError());
        }
        return usuario;
    }

    private static void asociar(BusinessSystem sistema, Local local, Propietario dueño) {
        if (!sistema.asociarLocal(local, dueño)) {
            throw new IllegalStateException(sistema.getUltimoError());
        }
    }

    private static List<Object> filaBasica(Local local) {
        Direccion direccion = local.getDireccion();
        StringJoiner dueños = new StringJoiner(", ");
        for (Propietario dueño : local.getDueños()) {
            dueños.add(dueño.getNick());
        }
        List<Object> fila = new ArrayList<>();
        fila.add(local.getNombre());
        fila.add(direccion.getCalle() + " " + direccion.getNumero());
        fila.add(direccion.getLocalidad());
        fila.add(direccion.getProvincia());
        fila.add(dueños.toString());
        return fila;
    }

    private static void escribir(Sheet hoja, List<List<Object>> filas) {
        int columnas = 0;
        for (List<Object> fila : filas) {
            columnas = Math.max(columnas, fila.size());
        }
        hoja.ensureRowCount(filas.size());
        hoja.ensureColumnCount(columnas);
        for (int fila = 0; fila < filas.size(); fila++) {
            List<Object> valores = filas.get(fila);
            for (int columna = 0; columna < valores.size(); columna++) {
                hoja.setValueAt(valores.get(columna), columna, fila);
            }
        }
    }
}
