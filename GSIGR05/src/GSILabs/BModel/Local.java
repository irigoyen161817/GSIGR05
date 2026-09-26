package GSILabs.BModel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Clase base abstracta para los locales de ocio del sistema.
 *
 * <p>Un local tiene un nombre, una {@link Dirección} y una descripción
 * opcional de hasta {@value #MAX_CARACTERES_DESCRIPCION} caracteres
 * (C01). La unicidad de la dirección entre distintos locales no la
 * comprueba esta clase, sino {@code GSILabs.BSystem.BusinessSystem}, que
 * conoce a todos los locales del sistema.</p>
 *
 * <p>Un local debe tener entre {@value #MIN_DUENOS} y
 * {@value #MAX_DUENOS} dueños (C06). Esta clase es la responsable de
 * mantener esa cardinalidad, pero la sincronización con la colección de
 * locales de {@link Dueño} es explícita y no ocurre en el constructor.</p>
 *
 * <p><b>Ciclo de vida.</b> Un local se crea sin estar enlazado con sus
 * dueños: el constructor solo valida los datos y guarda el conjunto de
 * dueños del propio local, sin tocar la colección de locales de ningún
 * {@link Dueño} (evita que {@code this} escape del constructor antes de
 * terminar de construirse). Es responsabilidad de quien da de alta el
 * local, típicamente {@code GSILabs.BSystem.BusinessSystem} tras
 * comprobar que la dirección es única en el sistema, invocar
 * {@link #vincular()} para reflejar el local en la colección de locales
 * de cada uno de sus dueños. Al dar de baja el local hay que invocar
 * {@link #desvincular()} para deshacer ese enlace. La política de borrado
 * configurable (bloquear la baja o propagarla en cascada a las reviews y
 * reservas dependientes del local) se aplica con
 * {@link #eliminar(PoliticaBorrado)}.</p>
 *
 * <p>Los tipos concretos ({@link Restaurante}, {@link Bar} y
 * {@link Pub}) heredan de {@code Local}.</p>
 *
 * <p>Un local también mantiene, a efectos de coherencia al eliminar
 * (véase {@link PoliticaBorrado}), las reviews y reservas vinculadas que
 * lo referencian, sincronizadas por
 * {@link Review#vincular()}/{@link Review#desvincular()} y
 * {@link Reserva#vincular()}/{@link Reserva#desvincular()}.</p>
 */
public abstract class Local {

    /** Longitud máxima permitida para la descripción. */
    public static final int MAX_CARACTERES_DESCRIPCION = 300;

    /** Número mínimo de dueños que debe tener un local. */
    public static final int MIN_DUENOS = 1;

    /** Número máximo de dueños que puede tener un local. */
    public static final int MAX_DUENOS = 3;

    private final String nombre;
    private final Dirección direccion;
    private String descripcion;
    private final Set<Dueño> dueños = new LinkedHashSet<>();
    private boolean vinculado;
    private final Set<Review> reviews = new LinkedHashSet<>();
    private final Set<Reserva> reservas = new LinkedHashSet<>();

    /**
     * Crea un local con su primer dueño, sin enlazarlo todavía con él.
     *
     * <p>El constructor solo valida los datos y registra el primer dueño
     * en el conjunto de dueños de este local; no modifica la colección de
     * locales de {@code primerDueño}. Para reflejar el local en esa
     * colección hay que invocar {@link #vincular()} una vez dado de alta
     * en el sistema.</p>
     *
     * @param nombre      nombre del local
     * @param direccion   dirección física del local
     * @param descripcion descripción del local, opcional (puede ser
     *                    {@code null}), de hasta
     *                    {@value #MAX_CARACTERES_DESCRIPCION} caracteres
     * @param primerDueño dueño inicial del local; todo local necesita al
     *                    menos uno en el momento de su creación
     * @throws NullPointerException si el nombre o la dirección son
     *         {@code null}
     * @throws IllegalArgumentException si el nombre está en blanco
     * @throws DominioException si no se indica un primer dueño (C06) o si
     *         la descripción supera {@value #MAX_CARACTERES_DESCRIPCION}
     *         caracteres (C01)
     */
    protected Local(String nombre, Dirección direccion, String descripcion, Dueño primerDueño)
            throws DominioException {
        Objects.requireNonNull(nombre, "El nombre del local es obligatorio.");
        Objects.requireNonNull(direccion, "La dirección del local es obligatoria.");
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del local no puede estar en blanco.");
        }
        if (primerDueño == null) {
            throw new DominioException("El local \"" + nombre
                    + "\" no se puede crear sin dueño: todo local debe tener al menos uno.");
        }
        validarDescripcion(nombre, descripcion);

        this.nombre = nombre;
        this.direccion = direccion;
        this.descripcion = descripcion;
        this.dueños.add(primerDueño);
    }

    private static void validarDescripcion(String nombre, String descripcion) throws DominioException {
        if (descripcion != null && descripcion.length() > MAX_CARACTERES_DESCRIPCION) {
            throw new DominioException("La descripción del local \"" + nombre + "\" tiene "
                    + descripcion.length() + " caracteres y el máximo permitido es "
                    + MAX_CARACTERES_DESCRIPCION + ".");
        }
    }

    /**
     * Devuelve el nombre del local.
     *
     * @return el nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Devuelve la dirección física del local.
     *
     * @return la dirección
     */
    public Dirección getDireccion() {
        return direccion;
    }

    /**
     * Devuelve la descripción del local.
     *
     * @return la descripción, o {@code null} si no tiene
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Cambia la descripción del local.
     *
     * @param descripcion nueva descripción, opcional (puede ser
     *                    {@code null}), de hasta
     *                    {@value #MAX_CARACTERES_DESCRIPCION} caracteres
     * @throws DominioException si la descripción supera
     *         {@value #MAX_CARACTERES_DESCRIPCION} caracteres (C01)
     */
    public void setDescripcion(String descripcion) throws DominioException {
        validarDescripcion(nombre, descripcion);
        this.descripcion = descripcion;
    }

    /**
     * Devuelve los dueños actuales del local.
     *
     * @return vista de solo lectura de los dueños
     */
    public Set<Dueño> getDueños() {
        return Collections.unmodifiableSet(dueños);
    }

    /**
     * Indica si el local está enlazado con la colección de locales de sus
     * dueños.
     *
     * @return {@code true} si se ha invocado {@link #vincular()} sin una
     *         {@link #desvincular()} posterior
     */
    public boolean isVinculado() {
        return vinculado;
    }

    /**
     * Enlaza este local con sus dueños: se añade a la colección de
     * locales de cada uno de ellos (véase {@link Dueño#getLocales()}).
     *
     * <p>Antes de modificar nada, comprueba que el local no esté ya
     * vinculado y que ninguno de sus dueños tenga ya, en su colección de
     * locales, otra instancia distinta con la misma dirección; si alguna
     * comprobación falla no se modifica el estado de nadie.</p>
     *
     * @throws IllegalStateException si el local ya estaba vinculado
     * @throws DominioException si algún dueño ya tiene otro local (una
     *         instancia distinta de esta) en la misma dirección (C01)
     */
    public void vincular() throws DominioException {
        if (vinculado) {
            throw new IllegalStateException("El local \"" + nombre + "\" ya está vinculado a sus dueños.");
        }
        for (Dueño dueño : dueños) {
            for (Local otro : dueño.getLocales()) {
                if (otro != this && otro.equals(this)) {
                    throw new DominioException("No se puede dar de alta el local \"" + nombre
                            + "\" porque su dueño " + dueño.getNick() + " ya tiene el local \""
                            + otro.getNombre() + "\" en la misma dirección (" + direccion + ").");
                }
            }
        }
        for (Dueño dueño : dueños) {
            dueño.añadirLocalInterno(this);
        }
        vinculado = true;
    }

    /**
     * Desenlaza este local de sus dueños: se quita de la colección de
     * locales de cada uno de ellos. El local conserva su propio conjunto
     * de dueños; solo se deshace la sincronización con {@link Dueño}.
     *
     * <p>Operación idempotente: si el local no estaba vinculado, no hace
     * nada.</p>
     */
    public void desvincular() {
        if (!vinculado) {
            return;
        }
        for (Dueño dueño : dueños) {
            dueño.quitarLocalInterno(this);
        }
        vinculado = false;
    }

    /**
     * Añade un dueño al local. Si el local está vinculado, también se
     * añade a la colección de locales del nuevo dueño. Si el dueño ya lo
     * era, no hace nada.
     *
     * @param dueño nuevo dueño del local
     * @throws NullPointerException si {@code dueño} es {@code null}
     * @throws DominioException si el local ya tiene {@value #MAX_DUENOS}
     *         dueños (C06)
     */
    public void añadirDueño(Dueño dueño) throws DominioException {
        Objects.requireNonNull(dueño, "El dueño es obligatorio.");
        if (dueños.contains(dueño)) {
            return;
        }
        if (dueños.size() >= MAX_DUENOS) {
            throw new DominioException("No se puede añadir a " + dueño.getNick() + " como dueño del local \""
                    + nombre + "\" porque ya tiene " + MAX_DUENOS + " dueños, que es el máximo permitido.");
        }
        dueños.add(dueño);
        if (vinculado) {
            dueño.añadirLocalInterno(this);
        }
    }

    /**
     * Quita un dueño del local. Si el local está vinculado, también se
     * quita de la colección de locales de ese dueño. Si el dueño no lo
     * era, no hace nada.
     *
     * @param dueño dueño a retirar
     * @throws DominioException si {@code dueño} es el único dueño del
     *         local, ya que un local no puede quedarse sin dueños (C06)
     */
    public void quitarDueño(Dueño dueño) throws DominioException {
        if (dueño == null || !dueños.contains(dueño)) {
            return;
        }
        if (dueños.size() <= MIN_DUENOS) {
            throw new DominioException("No se puede quitar a " + dueño.getNick()
                    + " porque es el único dueño del local \"" + nombre
                    + "\" y un local no puede quedarse sin dueños.");
        }
        dueños.remove(dueño);
        if (vinculado) {
            dueño.quitarLocalInterno(this);
        }
    }

    /**
     * Dos locales son iguales si tienen la misma {@link Dirección},
     * independientemente de su tipo concreto (C01): dos instancias de
     * subclases distintas con la misma dirección se consideran el mismo
     * local.
     *
     * @param obj objeto con el que comparar
     * @return {@code true} si {@code obj} es un {@code Local} con la
     *         misma dirección
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Local)) {
            return false;
        }
        Local otro = (Local) obj;
        return direccion.equals(otro.direccion);
    }

    @Override
    public final int hashCode() {
        return direccion.hashCode();
    }

    /**
     * Devuelve las reviews que se han publicado sobre este local.
     *
     * @return vista de solo lectura de las reviews del local
     */
    public Set<Review> getReviews() {
        return Collections.unmodifiableSet(reviews);
    }

    /**
     * Registra una review sobre este local. Solo debe invocarse desde
     * {@link Review}, en cuyo constructor se crea la asociación.
     *
     * @param review review que referencia a este local
     */
    void añadirReviewInterna(Review review) {
        reviews.add(review);
    }

    /**
     * Quita una review de este local. Solo debe invocarse desde
     * {@link Review#desvincular()}.
     *
     * @param review review a desasociar de este local
     */
    void quitarReviewInterna(Review review) {
        reviews.remove(review);
    }

    /**
     * Devuelve las reservas hechas sobre este local.
     *
     * @return vista de solo lectura de las reservas del local
     */
    public Set<Reserva> getReservas() {
        return Collections.unmodifiableSet(reservas);
    }

    /**
     * Registra una reserva sobre este local. Solo debe invocarse desde
     * {@link Reserva}, en cuyo constructor se crea la asociación.
     *
     * @param reserva reserva que referencia a este local
     */
    void añadirReservaInterna(Reserva reserva) {
        reservas.add(reserva);
    }

    /**
     * Quita una reserva de este local. Solo debe invocarse desde
     * {@link Reserva#desvincular()}.
     *
     * @param reserva reserva a desasociar de este local
     */
    void quitarReservaInterna(Reserva reserva) {
        reservas.remove(reserva);
    }

    /**
     * Aplica la política de borrado a este local y lo desvincula de sus
     * dueños.
     *
     * <p>Los dependientes de un local son sus {@link #getReviews()
     * reviews} y sus {@link #getReservas() reservas} vinculadas. Con
     * {@link PoliticaBorrado#BLOQUEAR}, si tiene alguna se lanza una
     * excepción sin modificar nada. Con {@link PoliticaBorrado#CASCADA},
     * se elimina cada review (con su propio
     * {@link Review#eliminar(PoliticaBorrado)}, que arrastra su
     * contestación si la tuviera) y cada reserva (con
     * {@link Reserva#eliminar(PoliticaBorrado)}), y después se desvincula
     * este local de todos sus dueños.</p>
     *
     * @param politica política a aplicar si el local tiene reviews o
     *                 reservas
     * @return conjunto de solo lectura, en orden de eliminación, con las
     *         entidades eliminadas (contestaciones, reviews y reservas) y,
     *         al final, este local
     * @throws NullPointerException si {@code politica} es {@code null}
     * @throws DominioException si {@code politica} es
     *         {@link PoliticaBorrado#BLOQUEAR} y el local tiene alguna
     *         review o reserva
     */
    public Set<Object> eliminar(PoliticaBorrado politica) throws DominioException {
        Objects.requireNonNull(politica, "La política de borrado es obligatoria.");
        if (politica == PoliticaBorrado.BLOQUEAR && (!reviews.isEmpty() || !reservas.isEmpty())) {
            throw new DominioException("No se puede eliminar el local \"" + nombre + "\" porque tiene "
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
        desvincular();
        eliminados.add(this);
        return Collections.unmodifiableSet(eliminados);
    }
}
