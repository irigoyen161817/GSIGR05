package GSILabs.BModel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Local de tipo bar.
 *
 * <p>Además de los datos comunes de {@link Local}, un bar guarda sus
 * especialidades como un conjunto de etiquetas (C02). Un bar es
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
     * @throws DominioException si el local incumple las reglas de
     *         {@link Local} (sin dueño o descripción demasiado larga)
     */
    public Bar(String nombre, Dirección direccion, String descripcion, Dueño primerDueño)
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
     * Añade una especialidad al bar. Si ya estaba, no hace nada.
     *
     * @param especialidad etiqueta a añadir
     * @throws NullPointerException si la especialidad es {@code null}
     * @throws IllegalArgumentException si la especialidad está en blanco
     */
    public void añadirEspecialidad(String especialidad) {
        Objects.requireNonNull(especialidad, "La especialidad es obligatoria.");
        if (especialidad.isBlank()) {
            throw new IllegalArgumentException("La especialidad no puede estar en blanco.");
        }
        especialidades.add(especialidad);
    }

    /**
     * Quita una especialidad del bar. Si no estaba, no hace nada.
     *
     * @param especialidad etiqueta a quitar
     */
    public void quitarEspecialidad(String especialidad) {
        especialidades.remove(especialidad);
    }

    @Override
    public String toString() {
        return "Bar{nombre=" + getNombre() + ", direccion=" + getDireccion()
                + ", especialidades=" + especialidades + "}";
    }
}
