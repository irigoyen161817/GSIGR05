package GSILabs.BModel;

/**
 * Marca los locales sobre los que se pueden realizar reservas (C09).
 *
 * <p>Solo los locales que implementan {@code Reservable} pueden
 * asociarse a una {@link Reserva}. De los tres tipos de {@link Local},
 * únicamente {@link Bar} y {@link Restaurante} implementan esta interfaz;
 * {@link Pub} no lo hace, por lo que no se pueden crear reservas sobre un
 * pub. Al ser una interfaz sellada que solo permite como implementaciones
 * a {@link Bar} y {@link Restaurante}, y al ser ambas clases {@code final},
 * ninguna otra clase puede implementarla, ni directamente (ninguna
 * subclase de {@link Pub}, ninguna clase anónima ni ningún otro tipo)
 * ni indirectamente extendiendo a {@link Bar} o {@link Restaurante}: la
 * restricción queda garantizada en tiempo de compilación, de modo que
 * cualquier valor de tipo {@code Reservable} es siempre un {@link Bar} o
 * un {@link Restaurante} (y por tanto un {@link Local}).</p>
 */
public sealed interface Reservable permits Bar, Restaurante {
}
