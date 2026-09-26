package GSILabs.BModel;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Perfil de usuario que puede consultar locales y redactar reseñas.
 *
 * <p>Un {@code Cliente} puede publicar {@link Review reviews} sobre
 * {@link Local locales} y realizar reservas sobre los que sean
 * {@link Reservable} (C04). Esas operaciones se ofrecen desde
 * {@code GSILabs.BSystem.BusinessSystem}, que coordina la creación de las
 * entidades relacionadas.</p>
 *
 * <p>El cliente mantiene, a efectos de coherencia al eliminar (véase
 * {@link PoliticaBorrado}), sus propias reviews y reservas vinculadas,
 * sincronizadas por {@link Review#vincular()}/{@link Review#desvincular()}
 * y {@link Reserva#vincular()}/{@link Reserva#desvincular()}.</p>
 */
public class Cliente extends Usuario {

    private final Set<Review> reviews = new LinkedHashSet<>();
    private final Set<Reserva> reservas = new LinkedHashSet<>();

    /**
     * Crea un cliente con los datos de cuenta indicados.
     *
     * @param nick            nick único de al menos
     *                        {@value Usuario#LONGITUD_MINIMA_NICK} caracteres
     * @param contrasena      contraseña de la cuenta
     * @param fechaNacimiento fecha de nacimiento del cliente
     * @throws DominioException si el nick o la edad incumplen las reglas
     *         de {@link Usuario} (C03)
     */
    public Cliente(String nick, String contrasena, LocalDate fechaNacimiento)
            throws DominioException {
        super(nick, contrasena, fechaNacimiento);
    }

    @Override
    public String toString() {
        return "Cliente{nick=" + getNick() + "}";
    }

    /**
     * Devuelve las reviews publicadas por este cliente.
     *
     * @return vista de solo lectura de las reviews del cliente
     */
    public Set<Review> getReviews() {
        return Collections.unmodifiableSet(reviews);
    }

    /**
     * Registra una review de este cliente. Solo debe invocarse desde
     * {@link Review}, en cuyo constructor se crea la asociación.
     *
     * @param review review publicada por este cliente
     */
    void añadirReviewInterna(Review review) {
        reviews.add(review);
    }

    /**
     * Quita una review de este cliente. Solo debe invocarse desde
     * {@link Review#desvincular()}.
     *
     * @param review review a desasociar de este cliente
     */
    void quitarReviewInterna(Review review) {
        reviews.remove(review);
    }

    /**
     * Devuelve las reservas hechas por este cliente.
     *
     * @return vista de solo lectura de las reservas del cliente
     */
    public Set<Reserva> getReservas() {
        return Collections.unmodifiableSet(reservas);
    }

    /**
     * Registra una reserva de este cliente. Solo debe invocarse desde
     * {@link Reserva}, en cuyo constructor se crea la asociación.
     *
     * @param reserva reserva hecha por este cliente
     */
    void añadirReservaInterna(Reserva reserva) {
        reservas.add(reserva);
    }

    /**
     * Quita una reserva de este cliente. Solo debe invocarse desde
     * {@link Reserva#desvincular()}.
     *
     * @param reserva reserva a desasociar de este cliente
     */
    void quitarReservaInterna(Reserva reserva) {
        reservas.remove(reserva);
    }

    /**
     * Aplica la política de borrado a este cliente y lo desvincula de sus
     * reviews y reservas.
     *
     * <p>Los dependientes de un cliente son sus {@link #getReviews()
     * reviews} y sus {@link #getReservas() reservas} vinculadas. Con
     * {@link PoliticaBorrado#BLOQUEAR}, si tiene alguna se lanza una
     * excepción sin modificar nada. Con {@link PoliticaBorrado#CASCADA},
     * se elimina cada review (con su propio
     * {@link Review#eliminar(PoliticaBorrado)}, que arrastra su
     * contestación si la tuviera) y cada reserva (con
     * {@link Reserva#eliminar(PoliticaBorrado)}).</p>
     *
     * @param politica política a aplicar si el cliente tiene reviews o
     *                 reservas
     * @return conjunto de solo lectura, en orden de eliminación, con las
     *         entidades eliminadas (contestaciones, reviews y reservas) y,
     *         al final, este cliente
     * @throws NullPointerException si {@code politica} es {@code null}
     * @throws DominioException si {@code politica} es
     *         {@link PoliticaBorrado#BLOQUEAR} y el cliente tiene alguna
     *         review o reserva
     */
    public Set<Object> eliminar(PoliticaBorrado politica) throws DominioException {
        Objects.requireNonNull(politica, "La política de borrado es obligatoria.");
        if (politica == PoliticaBorrado.BLOQUEAR && (!reviews.isEmpty() || !reservas.isEmpty())) {
            throw new DominioException("No se puede eliminar al cliente " + getNick() + " porque tiene "
                    + reviews.size() + " review(s) y " + reservas.size() + " reserva(s); "
                    + "bórralas antes o elimínalo en cascada.");
        }

        Set<Object> eliminados = new LinkedHashSet<>();
        for (Review review : new LinkedHashSet<>(reviews)) {
            eliminados.addAll(review.eliminar(politica));
        }
        for (Reserva reserva : new LinkedHashSet<>(reservas)) {
            eliminados.addAll(reserva.eliminar(politica));
        }
        eliminados.add(this);
        return Collections.unmodifiableSet(eliminados);
    }
}
