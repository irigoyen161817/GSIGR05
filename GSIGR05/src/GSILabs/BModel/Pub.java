package GSILabs.BModel;

import java.time.LocalTime;
import java.util.Objects;

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
     * indica que el pub cierra de madrugada, al día siguiente.</p>
     *
     * @param nombre       nombre del local
     * @param direccion    dirección física del local
     * @param descripcion  descripción opcional (puede ser {@code null})
     * @param primerDueño  dueño inicial del local
     * @param horaApertura hora de apertura
     * @param horaClausura hora de clausura
     * @throws NullPointerException si falta alguna de las horas
     * @throws DominioException si el local incumple las reglas de
     *         {@link Local} (sin dueño o descripción demasiado larga)
     */
    public Pub(String nombre, Dirección direccion, String descripcion, Dueño primerDueño,
            LocalTime horaApertura, LocalTime horaClausura)
            throws DominioException {
        super(nombre, direccion, descripcion, primerDueño);
        this.horaApertura = Objects.requireNonNull(horaApertura, "La hora de apertura es obligatoria.");
        this.horaClausura = Objects.requireNonNull(horaClausura, "La hora de clausura es obligatoria.");
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
    public String toString() {
        return "Pub{nombre=" + getNombre() + ", direccion=" + getDireccion()
                + ", horaApertura=" + horaApertura + ", horaClausura=" + horaClausura + "}";
    }
}
