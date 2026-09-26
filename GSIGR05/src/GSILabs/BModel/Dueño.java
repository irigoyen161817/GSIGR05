package GSILabs.BModel;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
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
 *
 * <p>Un dueño también mantiene, a efectos de coherencia al eliminar
 * (véase {@link PoliticaBorrado}), sus contestaciones vinculadas,
 * sincronizadas por
 * {@link Contestación#vincular()}/{@link Contestación#desvincular()}.</p>
 */
public class Dueño extends Usuario {

    private final Set<Local> locales = new LinkedHashSet<>();
    private final Set<Contestación> contestaciones = new LinkedHashSet<>();

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

    /**
     * Devuelve las contestaciones vinculadas de este dueño.
     *
     * @return vista de solo lectura de las contestaciones del dueño
     */
    public Set<Contestación> getContestaciones() {
        return Collections.unmodifiableSet(contestaciones);
    }

    /**
     * Añade una contestación a la colección de este dueño. Solo debe
     * invocarse desde {@link Contestación#vincular()}.
     *
     * @param contestacion contestación que se vincula a este dueño
     */
    void añadirContestacionInterna(Contestación contestacion) {
        contestaciones.add(contestacion);
    }

    /**
     * Quita una contestación de la colección de este dueño. Solo debe
     * invocarse desde {@link Contestación#desvincular()}.
     *
     * @param contestacion contestación que se desvincula de este dueño
     */
    void quitarContestacionInterna(Contestación contestacion) {
        contestaciones.remove(contestacion);
    }

    /**
     * Aplica la política de borrado a este dueño.
     *
     * <p>Los dependientes de un dueño son sus {@link #getContestaciones()
     * contestaciones} vinculadas y los locales vinculados de los que es
     * el único dueño (los que comparte con otros dueños no se consideran
     * dependientes, porque el local sigue siendo válido sin él). Con
     * {@link PoliticaBorrado#BLOQUEAR}, si tiene alguno se lanza una
     * excepción sin modificar nada. Con {@link PoliticaBorrado#CASCADA},
     * se elimina cada contestación (con
     * {@link Contestación#eliminar(PoliticaBorrado)}) y cada local del
     * que es único dueño (con su propio
     * {@link Local#eliminar(PoliticaBorrado)}, que arrastra sus reviews,
     * contestaciones y reservas). En ambos modos, si no se lanza
     * excepción, este dueño se retira ({@link Local#quitarDueño(Dueño)})
     * de los locales que comparte con otros dueños, de forma que nunca
     * queda un local sin ningún dueño.</p>
     *
     * @param politica política a aplicar si el dueño tiene contestaciones
     *                 o locales en propiedad exclusiva
     * @return conjunto de solo lectura, en orden de eliminación, con las
     *         entidades eliminadas (contestaciones, reviews, reservas y
     *         locales) y, al final, este dueño
     * @throws NullPointerException si {@code politica} es {@code null}
     * @throws DominioException si {@code politica} es
     *         {@link PoliticaBorrado#BLOQUEAR} y el dueño tiene alguna
     *         contestación o algún local en propiedad exclusiva
     */
    public Set<Object> eliminar(PoliticaBorrado politica) throws DominioException {
        Objects.requireNonNull(politica, "La política de borrado es obligatoria.");

        Set<Local> localesExclusivos = new LinkedHashSet<>();
        Set<Local> localesCompartidos = new LinkedHashSet<>();
        for (Local local : locales) {
            if (local.getDueños().size() == 1) {
                localesExclusivos.add(local);
            } else {
                localesCompartidos.add(local);
            }
        }

        if (politica == PoliticaBorrado.BLOQUEAR
                && (!contestaciones.isEmpty() || !localesExclusivos.isEmpty())) {
            throw new DominioException("No se puede eliminar al dueño " + getNick() + " porque tiene "
                    + contestaciones.size() + " contestación(es) y es el único dueño de "
                    + localesExclusivos.size() + " local(es); bórralos antes o elimínalo en cascada.");
        }

        Set<Object> eliminados = new LinkedHashSet<>();
        for (Contestación contestacion : new LinkedHashSet<>(contestaciones)) {
            eliminados.addAll(contestacion.eliminar(politica));
        }
        for (Local local : localesExclusivos) {
            eliminados.addAll(local.eliminar(politica));
        }
        for (Local local : localesCompartidos) {
            local.quitarDueño(this);
        }
        eliminados.add(this);
        return Collections.unmodifiableSet(eliminados);
    }

    @Override
    public String toString() {
        return "Dueño{nick=" + getNick() + "}";
    }
}
