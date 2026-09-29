package GSILabs.BModel;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Reseña de un {@link Cliente} sobre un {@link Local}.
 *
 * <p>Tiene una valoración numérica entre {@value #VALORACION_MINIMA} y
 * {@value #VALORACION_MAXIMA} estrellas, un comentario obligatorio de hasta
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
 * <p>Decisiones de interpretación (no están explícitas en C05): el
 * comentario es obligatorio, por ser la opción más restrictiva, y la fecha
 * de visita no puede ser posterior a la fecha actual, ya que no se puede
 * valorar una visita que todavía no ha ocurrido. Esta segunda no es una
 * regla de negocio, sino una fecha mal formada, por lo que se rechaza con
 * {@link IllegalArgumentException} y no con {@link DominioException}.</p>
 *
 * <p>Una review puede tener como máximo una {@link Contestacion}, del
 * dueño del local reseñado.</p>
 *
 * <p><b>Ciclo de vida.</b> Una review se crea sin estar enlazada con el
 * cliente autor ni con el local valorado: el constructor solo valida los
 * datos, sin tocar ninguna otra entidad (evita que {@code this} escape
 * del constructor antes de terminar de construirse). Es responsabilidad
 * de quien da de alta la review, típicamente
 * {@code GSILabs.BSystem.BusinessSystem}, invocar {@link #vincular()},
 * que comprueba que el local esté dado de alta y que el cliente no tenga
 * ya una review de la misma visita, y la refleja en
 * {@link Cliente#getReviews()} y {@link Local#getReviews()}. Al dar de
 * baja la review hay que invocar {@link #desvincular()} para deshacer ese
 * enlace. La política de borrado configurable (bloquear la baja o
 * propagarla en cascada a la contestación dependiente) se aplica con
 * {@link #eliminar(PoliticaBorrado)}.</p>
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
    private Contestacion contestacion;
    private boolean vinculada;

    /**
     * Crea una review. La fecha de creación se asigna automáticamente a
     * la fecha actual.
     *
     * @param cliente     autor de la review
     * @param local       local valorado
     * @param valoracion  valoración en estrellas, entre
     *                    {@value #VALORACION_MINIMA} y
     *                    {@value #VALORACION_MAXIMA}
     * @param comentario  comentario obligatorio, de hasta
     *                    {@value #MAX_CARACTERES_COMENTARIO} caracteres
     * @param fechaVisita fecha en la que se realizó la visita al local
     * @throws NullPointerException si el cliente, el local, el comentario o
     *         la fecha de visita son {@code null}
     * @throws IllegalArgumentException si la fecha de visita es posterior a
     *         hoy, porque no se puede valorar una visita que todavía no ha
     *         ocurrido
     * @throws DominioException si la valoración no está entre
     *         {@value #VALORACION_MINIMA} y {@value #VALORACION_MAXIMA}
     *         estrellas o si el comentario supera
     *         {@value #MAX_CARACTERES_COMENTARIO} caracteres (C05)
     */
    public Review(Cliente cliente, Local local, int valoracion, String comentario,
            LocalDate fechaVisita) throws DominioException {
        if (cliente == null) {
            throw new NullPointerException("La review debe tener un cliente autor.");
        }
        if (local == null) {
            throw new NullPointerException("La review debe tener un local valorado.");
        }
        if (comentario == null) {
            throw new NullPointerException("La review de \"" + cliente.getNick() + "\" sobre \""
                    + local.getNombre() + "\" debe llevar un comentario.");
        }
        if (fechaVisita == null) {
            throw new NullPointerException("La review debe tener una fecha de visita.");
        }
        LocalDate hoy = LocalDate.now();
        if (valoracion < VALORACION_MINIMA || valoracion > VALORACION_MAXIMA) {
            throw new DominioException("La review de \"" + cliente.getNick() + "\" sobre \"" + local.getNombre()
                    + "\" tiene " + valoracion + " estrellas, pero la valoración debe estar entre "
                    + VALORACION_MINIMA + " y " + VALORACION_MAXIMA + ".");
        }
        if (comentario.length() > MAX_CARACTERES_COMENTARIO) {
            throw new DominioException("El comentario de la review de \"" + cliente.getNick() + "\" sobre \""
                    + local.getNombre() + "\" tiene " + comentario.length()
                    + " caracteres y el máximo permitido es " + MAX_CARACTERES_COMENTARIO + ".");
        }
        if (fechaVisita.isAfter(hoy)) {
            throw new IllegalArgumentException("La visita a \"" + local.getNombre() + "\" del " + fechaVisita
                    + " todavía no ha ocurrido, así que \"" + cliente.getNick() + "\" no puede valorarla.");
        }

        this.cliente = cliente;
        this.local = local;
        this.valoracion = valoracion;
        this.comentario = comentario;
        this.fechaVisita = fechaVisita;
        this.fechaCreacion = hoy;
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
     * @return el comentario, nunca {@code null}
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
     * Devuelve la contestación del dueño a esta review, si existe.
     *
     * @return la contestación, o {@code null} si aún no se ha respondido
     * @see Contestacion#vincular()
     */
    public Contestacion getContestacion() {
        return contestacion;
    }

    /**
     * Asigna la contestación de esta review. Solo debe invocarse desde
     * {@link Contestacion#vincular()}, que es quien valida que no exista
     * ya una contestación previa y que el autor sea dueño del local
     * reseñado.
     *
     * @param contestacion contestación a asociar con esta review
     */
    void asignarContestacionInterna(Contestacion contestacion) {
        this.contestacion = contestacion;
    }

    /**
     * Quita la contestación de esta review, si es {@code contestacion}.
     * Solo debe invocarse desde {@link Contestacion#desvincular()}.
     *
     * @param contestacion contestación a desasociar de esta review; si no
     *                      coincide con la contestación actual, no se
     *                      hace nada
     */
    void quitarContestacionInterna(Contestacion contestacion) {
        if (this.contestacion == contestacion) {
            this.contestacion = null;
        }
    }

    /**
     * Indica si la review está enlazada con el cliente y el local.
     *
     * @return {@code true} si se ha invocado {@link #vincular()} sin una
     *         {@link #desvincular()} posterior
     */
    public boolean isVinculada() {
        return vinculada;
    }

    /**
     * Enlaza esta review con el cliente autor y el local valorado: se
     * añade a {@link Cliente#getReviews()} y a {@link Local#getReviews()}.
     *
     * <p>Antes de modificar nada, comprueba que la review no esté ya
     * vinculada, que el local esté vinculado (dado de alta en el sistema,
     * véase {@link Local#isVinculado()}) y que el cliente no tenga ya
     * vinculada otra review de la misma visita, es decir, del mismo local
     * y con la misma fecha de visita (C05, ver {@link #equals(Object)});
     * si alguna comprobación falla no se modifica el estado de nadie.</p>
     *
     * @throws IllegalStateException si la review ya estaba vinculada
     * @throws DominioException si el local no está dado de alta o si el
     *         cliente ya tiene una review de la misma visita (C05)
     */
    public void vincular() throws DominioException {
        if (vinculada) {
            throw new IllegalStateException("La review ya está vinculada.");
        }
        if (!local.isVinculado()) {
            throw new DominioException("No se puede publicar la review de \"" + cliente.getNick()
                    + "\" porque el local \"" + local.getNombre() + "\" no está dado de alta.");
        }
        if (cliente.getReviews().contains(this)) {
            throw new DominioException("\"" + cliente.getNick() + "\" ya ha valorado su visita a \""
                    + local.getNombre() + "\" del " + fechaVisita
                    + " y no se puede valorar dos veces la misma visita.");
        }
        cliente.añadirReviewInterna(this);
        local.añadirReviewInterna(this);
        vinculada = true;
    }

    /**
     * Desenlaza esta review del cliente y del local: se quita de
     * {@link Cliente#getReviews()} y de {@link Local#getReviews()}. No
     * afecta a la contestación asociada, si la hubiera.
     *
     * <p>Operación idempotente: si la review no estaba vinculada, no hace
     * nada.</p>
     */
    public void desvincular() {
        if (!vinculada) {
            return;
        }
        cliente.quitarReviewInterna(this);
        local.quitarReviewInterna(this);
        vinculada = false;
    }

    /**
     * Aplica la política de borrado a esta review y la desenlaza.
     *
     * <p>El único dependiente de una review es su {@link #getContestacion()
     * contestación}, si tiene una. Con {@link PoliticaBorrado#BLOQUEAR}, si
     * existe esa contestación se lanza una excepción sin modificar nada.
     * Con {@link PoliticaBorrado#CASCADA}, primero se elimina la
     * contestación (con su propio {@link Contestacion#eliminar(PoliticaBorrado)})
     * y después se desvincula esta review.</p>
     *
     * @param politica política a aplicar si la review tiene una
     *                 contestación
     * @return conjunto de solo lectura, en orden de eliminación, con esta
     *         review y, si se eliminó en cascada, su contestación
     * @throws NullPointerException si {@code politica} es {@code null}
     * @throws DominioException si {@code politica} es
     *         {@link PoliticaBorrado#BLOQUEAR} y la review tiene una
     *         contestación
     */
    public Set<Object> eliminar(PoliticaBorrado politica) throws DominioException {
        if (politica == null) {
            throw new NullPointerException("La política de borrado es obligatoria.");
        }
        if (politica == PoliticaBorrado.BLOQUEAR && contestacion != null) {
            throw new DominioException("No se puede eliminar la review de \"" + cliente.getNick()
                    + "\" sobre \"" + local.getNombre() + "\" porque ya tiene una contestación del dueño; "
                    + "bórrala antes o elimina la review en cascada.");
        }

        Set<Object> eliminados = new LinkedHashSet<>();
        if (contestacion != null) {
            eliminados.addAll(contestacion.eliminar(politica));
        }
        desvincular();
        eliminados.add(this);
        return Collections.unmodifiableSet(eliminados);
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

    /**
     * Devuelve el código hash de la review, derivado del cliente, el local
     * y la fecha de visita para ser coherente con {@link #equals(Object)}.
     *
     * @return el código hash de la terna cliente, local y fecha de visita
     */
    @Override
    public int hashCode() {
        return Objects.hash(cliente, local, fechaVisita);
    }

    /**
     * Devuelve una representación en texto de la review con el nick del
     * cliente, el nombre del local, la valoración y las fechas de visita y
     * de creación.
     *
     * @return la descripción de la review
     */
    @Override
    public String toString() {
        return "Review{cliente=" + cliente.getNick() + ", local=" + local.getNombre()
                + ", valoracion=" + valoracion + ", fechaVisita=" + fechaVisita
                + ", fechaCreacion=" + fechaCreacion + "}";
    }
}
