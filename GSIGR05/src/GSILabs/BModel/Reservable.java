package GSILabs.BModel;

/**
 * Marca los locales sobre los que se pueden realizar reservas (C09).
 *
 * <p>Solo los locales que implementan {@code Reservable} pueden
 * asociarse a una {@code Reserva}. De los tres tipos de {@link Local},
 * únicamente {@link Bar} y {@link Restaurante} implementan esta interfaz;
 * {@link Pub} no lo hace, por lo que no se pueden crear reservas sobre un
 * pub. Al ser una interfaz sellada que solo permite a {@link Bar} y
 * {@link Restaurante} como implementaciones, ninguna otra clase (ni
 * siquiera una subclase de {@link Pub} o una clase anónima) puede
 * implementarla: la restricción queda garantizada en tiempo de
 * compilación.</p>
 */
public sealed interface Reservable permits Bar, Restaurante {
}
