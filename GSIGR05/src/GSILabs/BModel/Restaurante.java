package GSILabs.BModel;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Local de tipo restaurante.
 *
 * <p>Además de los datos comunes de {@link Local}, un restaurante tiene
 * un precio estimado de menú y una capacidad máxima, expresada como el
 * número total de comensales y el número máximo de comensales por mesa
 * (C02). Un restaurante es {@link Reservable}: se pueden registrar
 * reservas sobre él (C09).</p>
 */
public final class Restaurante extends Local implements Reservable {

    private final BigDecimal precioMenu;
    private final int capacidadTotal;
    private final int capacidadMaximaPorMesa;

    /**
     * Crea un restaurante con su primer dueño y sus datos de capacidad.
     *
     * @param nombre                 nombre del local
     * @param direccion              dirección física del local
     * @param descripcion            descripción opcional (puede ser
     *                               {@code null})
     * @param primerDueño            dueño inicial del local
     * @param precioMenu             precio estimado del menú, mayor que 0;
     *                               se guarda redondeado a 2 decimales
     * @param capacidadTotal        número total de comensales, mayor que 0
     * @param capacidadMaximaPorMesa número máximo de comensales por mesa,
     *                               mayor que 0 y no superior a
     *                               {@code capacidadTotal}
     * @throws NullPointerException si el nombre, la dirección o el precio
     *         del menú son {@code null}
     * @throws IllegalArgumentException si el nombre está en blanco
     * @throws DominioException si el local incumple las reglas de
     *         {@link Local}, si el precio del menú no es mayor que 0
     *         (una vez redondeado a 2 decimales), o
     *         si las capacidades no son positivas o la capacidad por mesa
     *         supera la total (C02)
     */
    public Restaurante(String nombre, Direccion direccion, String descripcion, Propietario primerDueño,
            BigDecimal precioMenu, int capacidadTotal, int capacidadMaximaPorMesa)
            throws DominioException {
        super(nombre, direccion, descripcion, primerDueño);

        if (precioMenu == null) {
            throw new NullPointerException("El precio del menú es obligatorio.");
        }
        BigDecimal precioRedondeado = precioMenu.setScale(2, RoundingMode.HALF_UP);
        if (precioRedondeado.signum() <= 0) {
            throw new DominioException("El precio del menú del restaurante \"" + getNombre()
                    + "\" debe ser mayor que 0 y se ha indicado " + precioMenu + ".");
        }
        if (capacidadTotal <= 0 || capacidadMaximaPorMesa <= 0) {
            throw new DominioException("El restaurante \"" + getNombre()
                    + "\" debe admitir al menos un comensal en total y por mesa, y se ha indicado "
                    + capacidadTotal + " en total y " + capacidadMaximaPorMesa + " por mesa.");
        }
        if (capacidadMaximaPorMesa > capacidadTotal) {
            throw new DominioException("En el restaurante \"" + getNombre() + "\" no puede haber mesas de "
                    + capacidadMaximaPorMesa + " comensales si solo caben " + capacidadTotal + " en total.");
        }

        this.precioMenu = precioRedondeado;
        this.capacidadTotal = capacidadTotal;
        this.capacidadMaximaPorMesa = capacidadMaximaPorMesa;
    }

    /**
     * Devuelve el precio estimado del menú.
     *
     * @return el precio del menú
     */
    public BigDecimal getPrecioMenu() {
        return precioMenu;
    }

    /**
     * Devuelve el número total de comensales que admite el restaurante.
     *
     * @return la capacidad total
     */
    public int getCapacidadTotal() {
        return capacidadTotal;
    }

    /**
     * Devuelve el número máximo de comensales por mesa.
     *
     * @return la capacidad máxima por mesa
     */
    public int getCapacidadMaximaPorMesa() {
        return capacidadMaximaPorMesa;
    }

    @Override
    protected String camposPropiosToString() {
        return ", precioMenu=" + precioMenu + ", capacidadTotal=" + capacidadTotal
                + ", capacidadMaximaPorMesa=" + capacidadMaximaPorMesa;
    }
}
