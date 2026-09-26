package GSILabs.BModel;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Perfil de usuario propietario de locales.
 *
 * <p>Un {@code Dueño} puede poseer cualquier número de locales
 * ({@code 0..N}); cada {@link Local}, a su vez, debe tener entre 1 y 3
 * dueños (C06). La relación es bidireccional y la gestiona por completo
 * {@link Local}: esta colección solo contiene los locales que están
 * <em>vinculados</em> (véase {@link Local#vincular()} y
 * {@link Local#desvincular()}), y los métodos de paquete de esta clase
 * existen únicamente para que {@code Local} la mantenga sincronizada; un
 * local recién creado, o ya desvinculado, no aparece aquí aunque este
 * usuario figure en su conjunto de dueños.</p>
 */
public class Dueño extends Usuario {

    private final Set<Local> locales = new LinkedHashSet<>();

    /**
     * Crea un dueño con los datos de cuenta indicados.
     *
     * @param nick            nick único de al menos
     *                        {@value Usuario#LONGITUD_MINIMA_NICK} caracteres
     * @param contrasena      contraseña de la cuenta
     * @param fechaNacimiento fecha de nacimiento del dueño
     * @throws DominioException si el nick o la edad incumplen las reglas
     *         de {@link Usuario} (C03)
     */
    public Dueño(String nick, String contrasena, LocalDate fechaNacimiento)
            throws DominioException {
        super(nick, contrasena, fechaNacimiento);
    }

    /**
     * Devuelve los locales de los que este usuario es dueño.
     *
     * @return vista de solo lectura de los locales en propiedad
     */
    public Set<Local> getLocales() {
        return Collections.unmodifiableSet(locales);
    }

    /**
     * Añade un local a la colección de este dueño. Solo debe invocarse
     * desde {@link Local#vincular()}, que es quien decide cuándo un local
     * pasa a estar enlazado con sus dueños.
     *
     * @param local local que se vincula a este dueño
     */
    void añadirLocalInterno(Local local) {
        locales.add(local);
    }

    /**
     * Quita un local de la colección de este dueño. Solo debe invocarse
     * desde {@link Local#desvincular()}, que es quien decide cuándo un
     * local deja de estar enlazado con sus dueños.
     *
     * @param local local que se desvincula de este dueño
     */
    void quitarLocalInterno(Local local) {
        locales.remove(local);
    }

    @Override
    public String toString() {
        return "Dueño{nick=" + getNick() + "}";
    }
}
