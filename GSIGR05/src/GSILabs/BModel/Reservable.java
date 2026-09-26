package GSILabs.BModel;

/**
 * Marca los locales sobre los que se pueden realizar reservas (C09).
 *
 * <p>Solo los locales que implementan {@code Reservable} pueden
 * asociarse a una {@code Reserva}. De los tres tipos de {@link Local},
 * únicamente {@code Bar} y {@code Restaurante} implementan esta interfaz;
 * {@code Pub} no lo hace, por lo que no se pueden crear reservas sobre un
 * pub (la restricción queda garantizada en tiempo de compilación).</p>
 */
public interface Reservable {
}
