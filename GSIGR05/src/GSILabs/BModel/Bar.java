package GSILabs.BModel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Local de tipo bar.
 *
 * <p>Además de los datos comunes de {@link Local}, un bar guarda sus
 * especialidades como un conjunto de etiquetas (C02), guardadas sin
 * espacios en los extremos y en minúsculas para que {@code "Tapas"} y
 * {@code "tapas "} cuenten como la misma. Un bar es
 * {@link Reservable}: se pueden registrar reservas sobre él (C09).</p>
 */
public final class Bar extends Local implements Reservable {

    private final Set<String> especialidades = new LinkedHashSet<>();

    /**
     * Crea un bar con su primer dueño, sin especialidades iniciales.
     *
     * @param nombre      nombre del local
     * @param direccion   dirección física del local
     * @param descripcion descripción opcional (puede ser {@code null})
     * @param primerDueño dueño inicial del local
     * @throws NullPointerException si el nombre o la dirección son
     *         {@code null}
     * @throws IllegalArgumentException si el nombre está en blanco
     * @throws DominioException si el local incumple las reglas de
     *         {@link Local} (sin dueño o descripción demasiado larga)
     */
    public Bar(String nombre, Direccion direccion, String descripcion, Propietario primerDueño)
            throws DominioException {
        super(nombre, direccion, descripcion, primerDueño);
    }

    /**
     * Devuelve las especialidades del bar.
     *
     * @return vista de solo lectura del conjunto de especialidades
     */
    public Set<String> getEspecialidades() {
        return Collections.unmodifiableSet(especialidades);
    }

    /**
     * Añade una especialidad al bar, sin espacios en los extremos y en
     * minúsculas. Si ya estaba, no hace nada.
     *
     * @param especialidad etiqueta a añadir
     * @throws NullPointerException si la especialidad es {@code null}
     * @throws IllegalArgumentException si la especialidad está en blanco
     */
    public void añadirEspecialidad(String especialidad) {
        if (especialidad == null) {
            throw new NullPointerException("La especialidad es obligatoria.");
        }
        if (especialidad.isBlank()) {
            throw new IllegalArgumentException("La especialidad no puede estar en blanco.");
        }
        especialidades.add(normalizar(especialidad));
    }

    /**
     * Quita una especialidad del bar, sin distinguir mayúsculas ni espacios
     * en los extremos. Si no estaba, no hace nada.
     *
     * @param especialidad etiqueta a quitar
     */
    public void quitarEspecialidad(String especialidad) {
        if (especialidad != null) {
            especialidades.remove(normalizar(especialidad));
        }
    }

    private static String normalizar(String especialidad) {
        return especialidad.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    protected String camposPropiosToString() {
        return ", especialidades=" + especialidades;
    }
}
