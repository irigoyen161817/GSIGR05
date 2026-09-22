package GSILabs.BModel;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reserva de un {@link Cliente} sobre un local {@link Reservable} (C09).
 *
 * <p>Al tipar el local reservado como {@link Reservable} en lugar de
 * como {@link Local}, solo se pueden crear reservas sobre los tipos de
 * local que implementan esa interfaz. Al ser {@link Reservable} una
 * interfaz sellada que solo permite como implementaciones a {@link Bar}
 * y {@link Restaurante} (ambas clases {@code final}), queda garantizado
 * en tiempo de compilación que el local de cualquier reserva es siempre
 * un {@link Bar} o un {@link Restaurante} (y por tanto un {@link Local}):
 * no existe ninguna implementación de {@link Reservable} que no sea uno
 * de esos dos tipos, por lo que un {@code Pub} nunca se puede pasar como
 * argumento y no se puede crear una reserva sobre él.</p>
 *
 * <p>Cada reserva tiene la fecha y hora de la misma y, opcionalmente, un
 * porcentaje de descuento entre 0 y 100.</p>
 *
 * <p>A diferencia del resto de entidades del modelo, una reserva no tiene
 * clave natural: nada impide que un cliente haga dos reservas con los
 * mismos datos. Por eso cada reserva recibe al crearse un identificador
 * único, generado con un contador propio de la clase.</p>
 */
public final class Reserva {

    /** Porcentaje de descuento mínimo admitido. */
    public static final int DESCUENTO_MINIMO = 0;

    /** Porcentaje de descuento máximo admitido. */
    public static final int DESCUENTO_MAXIMO = 100;

    /** Contador con el que se genera el identificador de cada reserva. */
    private static final AtomicInteger CONTADOR = new AtomicInteger();

    private final int id;
    private final Cliente cliente;
    private final Reservable reservable;
    private final LocalDateTime fechaHora;
    private final Integer descuentoPorcentaje;

    /**
     * Crea una reserva sin descuento.
     *
     * @param cliente    cliente que reserva
     * @param reservable local reservado
     * @param fechaHora  fecha y hora de la reserva
     * @throws NullPointerException si algún dato es {@code null}
     */
    public Reserva(Cliente cliente, Reservable reservable, LocalDateTime fechaHora) {
        this(null, cliente, reservable, fechaHora);
    }

    /**
     * Crea una reserva con un posible porcentaje de descuento.
     *
     * @param cliente             cliente que reserva
     * @param reservable          local reservado
     * @param fechaHora           fecha y hora de la reserva
     * @param descuentoPorcentaje porcentaje de descuento, opcional
     *                            (puede ser {@code null}), entre
     *                            {@value #DESCUENTO_MINIMO} y
     *                            {@value #DESCUENTO_MAXIMO}
     * @throws NullPointerException si el cliente, el local o la fecha son
     *         {@code null}
     * @throws DominioException si el descuento no está entre
     *         {@value #DESCUENTO_MINIMO} y {@value #DESCUENTO_MAXIMO} (C09)
     */
    public Reserva(Cliente cliente, Reservable reservable, LocalDateTime fechaHora,
            Integer descuentoPorcentaje) throws DominioException {
        this(validarDescuento(descuentoPorcentaje), cliente, reservable, fechaHora);
    }

    /**
     * Constructor común a los dos públicos: recibe el descuento ya
     * validado.
     */
    private Reserva(Integer descuentoPorcentaje, Cliente cliente, Reservable reservable,
            LocalDateTime fechaHora) {
        this.id = CONTADOR.incrementAndGet();
        this.cliente = Objects.requireNonNull(cliente, "La reserva debe tener un cliente.");
        this.reservable = Objects.requireNonNull(reservable, "La reserva debe tener un local reservable.");
        this.fechaHora = Objects.requireNonNull(fechaHora, "La reserva debe tener fecha y hora.");
        this.descuentoPorcentaje = descuentoPorcentaje;
    }

    private static Integer validarDescuento(Integer descuentoPorcentaje) throws DominioException {
        if (descuentoPorcentaje != null
                && (descuentoPorcentaje < DESCUENTO_MINIMO || descuentoPorcentaje > DESCUENTO_MAXIMO)) {
            throw new DominioException("El descuento de una reserva debe estar entre " + DESCUENTO_MINIMO
                    + " y " + DESCUENTO_MAXIMO + " % y se ha indicado un " + descuentoPorcentaje + " %.");
        }
        return descuentoPorcentaje;
    }

    /**
     * Devuelve el identificador único de la reserva.
     *
     * @return el identificador
     */
    public int getId() {
        return id;
    }

    /**
     * Devuelve el cliente que ha hecho la reserva.
     *
     * @return el cliente
     */
    public Cliente getCliente() {
        return cliente;
    }

    /**
     * Devuelve el local reservado.
     *
     * @return el local reservable
     */
    public Reservable getReservable() {
        return reservable;
    }

    /**
     * Devuelve la fecha y hora de la reserva.
     *
     * @return la fecha y hora
     */
    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    /**
     * Devuelve el porcentaje de descuento de la reserva.
     *
     * @return el porcentaje de descuento, o {@code null} si no tiene
     */
    public Integer getDescuentoPorcentaje() {
        return descuentoPorcentaje;
    }

    /**
     * Dos reservas son iguales si tienen el mismo identificador, ya que
     * no hay ninguna regla de negocio que impida a un cliente hacer
     * varias reservas iguales en datos (mismo local y hora).
     *
     * @param obj objeto con el que comparar
     * @return {@code true} si {@code obj} es una {@code Reserva} con el
     *         mismo identificador
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Reserva)) {
            return false;
        }
        Reserva otra = (Reserva) obj;
        return id == otra.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Reserva{id=" + id + ", cliente=" + cliente.getNick() + ", reservable=" + reservable
                + ", fechaHora=" + fechaHora + ", descuentoPorcentaje=" + descuentoPorcentaje + "}";
    }
}
