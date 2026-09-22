package GSILabs.BModel;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Respuesta de un {@link Dueño} a una {@link Review} sobre un local de su
 * propiedad.
 *
 * <p>Tiene un comentario breve de hasta
 * {@value #MAX_CARACTERES_COMENTARIO} caracteres y una fecha de creación
 * asignada automáticamente (C07, C08). Solo puede crearla un usuario que
 * sea dueño del local asociado a la review, y una review no puede tener
 * más de una contestación: ambas restricciones las comprueba el
 * constructor.</p>
 *
 * <p><b>Ciclo de vida.</b> Una contestación se crea sin estar enlazada
 * con la review que contesta: el constructor solo valida los datos (y
 * comprueba que el autor sea, en ese momento, dueño del local reseñado),
 * sin tocar la review (evita que {@code this} escape del constructor
 * antes de terminar de construirse). Es responsabilidad de quien da de
 * alta la contestación, típicamente {@code GSILabs.BSystem.BusinessSystem}
 * tras comprobar que la review existe en el sistema, invocar
 * {@link #vincular()} para asociarla con la review. Al dar de baja la
 * contestación hay que invocar {@link #desvincular()} para deshacer ese
 * enlace. Así, una contestación rechazada por el sistema no deja rastro
 * en la review.</p>
 */
public final class Contestación {

    /** Longitud máxima permitida para el comentario. */
    public static final int MAX_CARACTERES_COMENTARIO = 500;

    private final Dueño autor;
    private final Review review;
    private final String comentario;
    private final LocalDate fechaCreacion;
    private boolean vinculada;

    /**
     * Crea la contestación de {@code autor} a {@code review}, sin
     * enlazarla todavía con esa review.
     *
     * <p>El constructor solo valida los datos y comprueba que
     * {@code autor} sea, en este momento, dueño del local de
     * {@code review}; no modifica la review. Para reflejar la
     * contestación en ella hay que invocar {@link #vincular()} una vez
     * dada de alta en el sistema.</p>
     *
     * @param autor      dueño que responde; debe ser dueño del local de
     *                   {@code review}
     * @param review     review que se contesta
     * @param comentario texto de la contestación, no vacío y de hasta
     *                   {@value #MAX_CARACTERES_COMENTARIO} caracteres
     * @throws NullPointerException si el autor, la review o el comentario
     *         son {@code null}
     * @throws IllegalArgumentException si el comentario está en blanco
     * @throws DominioException si el comentario supera
     *         {@value #MAX_CARACTERES_COMENTARIO} caracteres o si
     *         {@code autor} no es dueño del local de {@code review} (C07)
     */
    public Contestación(Dueño autor, Review review, String comentario) throws DominioException {
        Objects.requireNonNull(autor, "La contestación debe tener un autor.");
        Objects.requireNonNull(review, "La contestación debe estar asociada a una review.");
        Objects.requireNonNull(comentario, "El comentario de la contestación es obligatorio.");
        if (comentario.isBlank()) {
            throw new IllegalArgumentException("El comentario de la contestación no puede estar en blanco.");
        }
        if (comentario.length() > MAX_CARACTERES_COMENTARIO) {
            throw new DominioException("La contestación de " + autor.getNick() + " tiene "
                    + comentario.length() + " caracteres y el máximo permitido es "
                    + MAX_CARACTERES_COMENTARIO + ".");
        }
        comprobarQueEsDueño(autor, review);

        this.autor = autor;
        this.review = review;
        this.comentario = comentario;
        this.fechaCreacion = LocalDate.now();
    }

    private static void comprobarQueEsDueño(Dueño autor, Review review) throws DominioException {
        Local local = review.getLocal();
        if (!local.getDueños().contains(autor)) {
            throw new DominioException(autor.getNick() + " no puede contestar la review de "
                    + review.getCliente().getNick() + " porque no es dueño del local \""
                    + local.getNombre() + "\".");
        }
    }

    /**
     * Devuelve el dueño autor de la contestación.
     *
     * @return el autor
     */
    public Dueño getAutor() {
        return autor;
    }

    /**
     * Devuelve la review contestada.
     *
     * @return la review
     */
    public Review getReview() {
        return review;
    }

    /**
     * Devuelve el comentario de la contestación.
     *
     * @return el comentario
     */
    public String getComentario() {
        return comentario;
    }

    /**
     * Devuelve la fecha en la que se creó la contestación.
     *
     * @return la fecha de creación
     */
    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Indica si la contestación está enlazada con su review.
     *
     * @return {@code true} si se ha invocado {@link #vincular()} sin una
     *         {@link #desvincular()} posterior
     */
    public boolean isVinculada() {
        return vinculada;
    }

    /**
     * Enlaza esta contestación con su review: se asigna como la
     * contestación de {@link #getReview()} (véase
     * {@link Review#getContestacion()}).
     *
     * <p>Antes de modificar nada, comprueba que la contestación no esté
     * ya vinculada, que la review no tenga ya asignada otra contestación
     * (C07) y que el autor siga siendo dueño del local reseñado; si
     * alguna comprobación falla no se modifica el estado de nadie.</p>
     *
     * @throws IllegalStateException si la contestación ya estaba vinculada
     * @throws DominioException si la review ya tiene otra contestación o
     *         si el autor ya no es dueño del local reseñado (C07)
     */
    public void vincular() throws DominioException {
        if (vinculada) {
            throw new IllegalStateException("La contestación ya está vinculada a la review.");
        }
        if (review.getContestacion() != null) {
            throw new DominioException("La review de " + review.getCliente().getNick() + " sobre \""
                    + review.getLocal().getNombre() + "\" ya tiene una contestación y solo se permite una.");
        }
        comprobarQueEsDueño(autor, review);
        review.asignarContestacionInterna(this);
        vinculada = true;
    }

    /**
     * Desenlaza esta contestación de su review: si la review tiene
     * asignada esta contestación, se quita. La contestación conserva sus
     * propios datos; solo se deshace la sincronización con {@link Review}.
     *
     * <p>Operación idempotente: si la contestación no estaba vinculada,
     * no hace nada.</p>
     */
    public void desvincular() {
        if (!vinculada) {
            return;
        }
        review.quitarContestacionInterna(this);
        vinculada = false;
    }

    /**
     * Dos contestaciones son iguales si responden a la misma review, ya
     * que una review no puede tener más de una contestación.
     *
     * @param obj objeto con el que comparar
     * @return {@code true} si {@code obj} es una {@code Contestación}
     *         que responde a la misma review
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Contestación)) {
            return false;
        }
        Contestación otra = (Contestación) obj;
        return review.equals(otra.review);
    }

    @Override
    public int hashCode() {
        return Objects.hash(review);
    }

    @Override
    public String toString() {
        return "Contestación{autor=" + autor.getNick() + ", review=" + review.getCliente().getNick()
                + "@" + review.getLocal().getNombre() + " (" + review.getFechaVisita() + ")"
                + ", fechaCreacion=" + fechaCreacion + "}";
    }
}
