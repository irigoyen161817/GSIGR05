package GSILabs.BModel;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Reseña de un {@link Cliente} sobre un {@link Local}.
 *
 * <p>Tiene una valoración numérica entre {@value #VALORACION_MINIMA} y
 * {@value #VALORACION_MAXIMA} estrellas, un comentario opcional de hasta
 * {@value #MAX_CARACTERES_COMENTARIO} caracteres, la fecha de la visita y
 * la fecha de creación de la reseña, asignada automáticamente (C05,
 * C08).</p>
 *
 * <p>Un cliente puede publicar varias reviews, incluso varias del mismo
 * local, pero no puede valorar dos veces la misma visita. Para permitir
 * detectar esa duplicidad, {@link #equals(Object)} se basa únicamente en
 * el cliente, el local y la fecha de visita (no en la valoración ni en el
 * comentario): esa terna es la clave natural de la review, así que dos
 * reviews son iguales exactamente cuando representan la misma visita,
 * que es la situación que debe rechazarse.</p>
 *
 * <p>La fecha de visita no puede ser posterior a la fecha actual (C05,
 * C08): no se puede valorar una visita que todavía no ha ocurrido, ya
 * que debe ser anterior o igual a la fecha de creación de la review.</p>
 */
public final class Review {

    /** Valoración mínima, en estrellas. */
    public static final int VALORACION_MINIMA = 0;

    /** Valoración máxima, en estrellas. */
    public static final int VALORACION_MAXIMA = 5;

    /** Longitud máxima permitida para el comentario. */
    public static final int MAX_CARACTERES_COMENTARIO = 500;

    private final Cliente cliente;
    private final Local local;
    private final int valoracion;
    private final String comentario;
    private final LocalDate fechaVisita;
    private final LocalDate fechaCreacion;

    /**
     * Crea una review. La fecha de creación se asigna automáticamente a
     * la fecha actual.
     *
     * @param cliente     autor de la review
     * @param local       local valorado
     * @param valoracion  valoración en estrellas, entre
     *                    {@value #VALORACION_MINIMA} y
     *                    {@value #VALORACION_MAXIMA}
     * @param comentario  comentario opcional (puede ser {@code null}),
     *                    de hasta {@value #MAX_CARACTERES_COMENTARIO}
     *                    caracteres
     * @param fechaVisita fecha en la que se realizó la visita al local
     * @throws NullPointerException si el cliente, el local o la fecha de
     *         visita son {@code null}
     * @throws DominioException si la valoración está fuera de rango, si el
     *         comentario supera {@value #MAX_CARACTERES_COMENTARIO}
     *         caracteres o si la fecha de visita es posterior a hoy (C05)
     */
    public Review(Cliente cliente, Local local, int valoracion, String comentario,
            LocalDate fechaVisita) throws DominioException {
        Objects.requireNonNull(cliente, "La review debe tener un cliente autor.");
        Objects.requireNonNull(local, "La review debe tener un local valorado.");
        Objects.requireNonNull(fechaVisita, "La fecha de visita es obligatoria.");
        if (valoracion < VALORACION_MINIMA || valoracion > VALORACION_MAXIMA) {
            throw new DominioException("La review de " + cliente.getNick() + " sobre \"" + local.getNombre()
                    + "\" tiene " + valoracion + " estrellas, pero la valoración debe estar entre "
                    + VALORACION_MINIMA + " y " + VALORACION_MAXIMA + ".");
        }
        if (comentario != null && comentario.length() > MAX_CARACTERES_COMENTARIO) {
            throw new DominioException("El comentario de la review de " + cliente.getNick() + " tiene "
                    + comentario.length() + " caracteres y el máximo permitido es "
                    + MAX_CARACTERES_COMENTARIO + ".");
        }
        if (fechaVisita.isAfter(LocalDate.now())) {
            throw new DominioException(cliente.getNick() + " no puede valorar una visita a \""
                    + local.getNombre() + "\" del " + fechaVisita + " porque esa fecha todavía no ha llegado.");
        }

        this.cliente = cliente;
        this.local = local;
        this.valoracion = valoracion;
        this.comentario = comentario;
        this.fechaVisita = fechaVisita;
        this.fechaCreacion = LocalDate.now();
    }

    /**
     * Devuelve el cliente autor de la review.
     *
     * @return el cliente
     */
    public Cliente getCliente() {
        return cliente;
    }

    /**
     * Devuelve el local valorado.
     *
     * @return el local
     */
    public Local getLocal() {
        return local;
    }

    /**
     * Devuelve la valoración en estrellas.
     *
     * @return la valoración, entre {@value #VALORACION_MINIMA} y
     *         {@value #VALORACION_MAXIMA}
     */
    public int getValoracion() {
        return valoracion;
    }

    /**
     * Devuelve el comentario de la review.
     *
     * @return el comentario, o {@code null} si no tiene
     */
    public String getComentario() {
        return comentario;
    }

    /**
     * Devuelve la fecha en la que se realizó la visita.
     *
     * @return la fecha de visita
     */
    public LocalDate getFechaVisita() {
        return fechaVisita;
    }

    /**
     * Devuelve la fecha en la que se creó la review.
     *
     * @return la fecha de creación
     */
    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Dos reviews son iguales si representan la misma visita: mismo
     * cliente, mismo local y misma fecha de visita. No se tiene en
     * cuenta la valoración ni el comentario, para que esta igualdad sirva
     * para detectar duplicados (C05).
     *
     * @param obj objeto con el que comparar
     * @return {@code true} si {@code obj} es una {@code Review} de la
     *         misma visita (mismo cliente, local y fecha)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Review)) {
            return false;
        }
        Review otra = (Review) obj;
        return cliente.equals(otra.cliente)
                && local.equals(otra.local)
                && fechaVisita.equals(otra.fechaVisita);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cliente, local, fechaVisita);
    }

    @Override
    public String toString() {
        return "Review{cliente=" + cliente.getNick() + ", local=" + local.getNombre()
                + ", valoracion=" + valoracion + ", fechaVisita=" + fechaVisita
                + ", fechaCreacion=" + fechaCreacion + "}";
    }
}
