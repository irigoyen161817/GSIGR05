package GSILabs.BModel;

import java.time.LocalTime;

/**
 * Local de tipo pub.
 *
 * <p>Además de los datos comunes de {@link Local}, un pub registra su
 * hora de apertura y su hora de clausura (C02). A diferencia de
 * {@link Bar} y {@link Restaurante}, un pub <b>no</b> implementa
 * {@link Reservable}: no se pueden registrar reservas sobre un pub
 * (C09).</p>
 */
public final class Pub extends Local {

    private final LocalTime horaApertura;
    private final LocalTime horaClausura;

    /**
     * Crea un pub con su primer dueño y su horario.
     *
     * <p>La hora de clausura puede ser anterior a la de apertura, lo que
     * indica que el pub cierra de madrugada, al día siguiente. Si ambas
     * horas coinciden, el pub abre las 24 horas.</p>
     *
     * @param nombre       nombre del local
     * @param direccion    dirección física del local
     * @param descripcion  descripción opcional (puede ser {@code null})
     * @param primerDueño  dueño inicial del local
     * @param horaApertura hora de apertura
     * @param horaClausura hora de clausura
     * @throws NullPointerException si el nombre, la dirección o alguna de
     *         las horas son {@code null}
     * @throws IllegalArgumentException si el nombre está en blanco
     * @throws DominioException si el local incumple las reglas de
     *         {@link Local} (sin dueño o descripción demasiado larga)
     */
    public Pub(String nombre, Direccion direccion, String descripcion, Propietario primerDueño,
            LocalTime horaApertura, LocalTime horaClausura)
            throws DominioException {
        super(nombre, direccion, descripcion, primerDueño);
        if (horaApertura == null) {
            throw new NullPointerException("La hora de apertura es obligatoria.");
        }
        if (horaClausura == null) {
            throw new NullPointerException("La hora de clausura es obligatoria.");
        }
        this.horaApertura = horaApertura;
        this.horaClausura = horaClausura;
    }

    /**
     * Devuelve la hora de apertura del pub.
     *
     * @return la hora de apertura
     */
    public LocalTime getHoraApertura() {
        return horaApertura;
    }

    /**
     * Devuelve la hora de clausura del pub.
     *
     * @return la hora de clausura
     */
    public LocalTime getHoraClausura() {
        return horaClausura;
    }

    @Override
    protected String camposPropiosToString() {
        return ", horaApertura=" + horaApertura + ", horaClausura=" + horaClausura;
    }
}
