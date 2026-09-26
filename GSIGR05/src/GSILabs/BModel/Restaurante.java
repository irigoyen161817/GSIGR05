package GSILabs.BModel;

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

    private final double precioMenu;
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
     * @param precioMenu             precio estimado del menú, finito y
     *                               mayor que 0
     * @param capacidadTotal         número total de comensales, mayor que 0
     * @param capacidadMaximaPorMesa número máximo de comensales por mesa,
     *                               mayor que 0 y no superior a
     *                               {@code capacidadTotal}
     * @throws DominioException si el local incumple las reglas de
     *         {@link Local}, si el precio del menú no es un número finito
     *         mayor que 0, o si las capacidades no son positivas o la
     *         capacidad por mesa supera la total (C02)
     */
    public Restaurante(String nombre, Dirección direccion, String descripcion, Dueño primerDueño,
            double precioMenu, int capacidadTotal, int capacidadMaximaPorMesa)
            throws DominioException {
        super(nombre, direccion, descripcion, primerDueño);

        if (!Double.isFinite(precioMenu) || precioMenu <= 0) {
            throw new DominioException("El precio del menú del restaurante \"" + nombre
                    + "\" debe ser mayor que 0 y se ha indicado " + precioMenu + ".");
        }
        if (capacidadTotal <= 0 || capacidadMaximaPorMesa <= 0) {
            throw new DominioException("El restaurante \"" + nombre
                    + "\" debe admitir al menos un comensal en total y por mesa, y se ha indicado "
                    + capacidadTotal + " en total y " + capacidadMaximaPorMesa + " por mesa.");
        }
        if (capacidadMaximaPorMesa > capacidadTotal) {
            throw new DominioException("En el restaurante \"" + nombre + "\" no puede haber mesas de "
                    + capacidadMaximaPorMesa + " comensales si solo caben " + capacidadTotal + " en total.");
        }

        this.precioMenu = precioMenu;
        this.capacidadTotal = capacidadTotal;
        this.capacidadMaximaPorMesa = capacidadMaximaPorMesa;
    }

    /**
     * Devuelve el precio estimado del menú.
     *
     * @return el precio del menú
     */
    public double getPrecioMenu() {
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
    public String toString() {
        return "Restaurante{nombre=" + getNombre() + ", direccion=" + getDireccion()
                + ", precioMenu=" + precioMenu + ", capacidadTotal=" + capacidadTotal
                + ", capacidadMaximaPorMesa=" + capacidadMaximaPorMesa + "}";
    }
}
