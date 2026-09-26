package GSILabs.BModel;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Dirección física de un {@link Local}: localidad, provincia, calle y
 * número.
 *
 * <p>Es un objeto de valor inmutable: dos direcciones son iguales si
 * coinciden en sus cuatro campos. Esa igualdad por valor es la que usa
 * {@code GSILabs.BSystem.BusinessSystem} para impedir que dos locales
 * compartan la misma dirección física (C01).</p>
 *
 * <p>Para que la igualdad resista diferencias triviales de formato, cada
 * campo se normaliza al construir la dirección: se eliminan los espacios
 * en blanco de los extremos ({@code strip()}) y las secuencias de varios
 * espacios internos se colapsan en uno solo. Además, la comparación con
 * {@link #equals(Object)} y {@link #hashCode()} no distingue mayúsculas
 * de minúsculas. No se eliminan tildes ni otros signos diacríticos.</p>
 */
public final class Dirección {

    private static final Pattern ESPACIOS_MULTIPLES = Pattern.compile("\\s+");

    private final String localidad;
    private final String provincia;
    private final String calle;
    private final String numero;

    /**
     * Crea una dirección con los datos indicados.
     *
     * <p>Cada campo se normaliza antes de guardarse: se eliminan los
     * espacios en blanco de los extremos y las secuencias de varios
     * espacios internos se colapsan en uno solo.</p>
     *
     * @param localidad localidad de la dirección
     * @param provincia provincia de la dirección
     * @param calle     nombre de la calle
     * @param numero    número de la calle (se admite texto para portales
     *                  como {@code "12B"})
     * @throws NullPointerException si algún campo es {@code null}
     * @throws IllegalArgumentException si algún campo está en blanco
     */
    public Dirección(String localidad, String provincia, String calle, String numero) {
        this.localidad = normalizar(localidad, "La localidad");
        this.provincia = normalizar(provincia, "La provincia");
        this.calle = normalizar(calle, "La calle");
        this.numero = normalizar(numero, "El número");
    }

    private static String normalizar(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " de la dirección es obligatorio.");
        if (valor.isBlank()) {
            throw new IllegalArgumentException(campo + " de la dirección no puede estar en blanco.");
        }
        return ESPACIOS_MULTIPLES.matcher(valor.strip()).replaceAll(" ");
    }

    /**
     * Devuelve la localidad de la dirección.
     *
     * @return la localidad
     */
    public String getLocalidad() {
        return localidad;
    }

    /**
     * Devuelve la provincia de la dirección.
     *
     * @return la provincia
     */
    public String getProvincia() {
        return provincia;
    }

    /**
     * Devuelve la calle de la dirección.
     *
     * @return la calle
     */
    public String getCalle() {
        return calle;
    }

    /**
     * Devuelve el número de la dirección.
     *
     * @return el número de calle
     */
    public String getNumero() {
        return numero;
    }

    /**
     * Dos direcciones son iguales si coinciden en localidad, provincia,
     * calle y número.
     *
     * @param obj objeto con el que comparar
     * @return {@code true} si {@code obj} es una {@code Dirección} con los
     *         mismos cuatro campos
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Dirección)) {
            return false;
        }
        Dirección otra = (Dirección) obj;
        return localidad.equalsIgnoreCase(otra.localidad)
                && provincia.equalsIgnoreCase(otra.provincia)
                && calle.equalsIgnoreCase(otra.calle)
                && numero.equalsIgnoreCase(otra.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localidad.toLowerCase(Locale.ROOT), provincia.toLowerCase(Locale.ROOT),
                calle.toLowerCase(Locale.ROOT), numero.toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return calle + " " + numero + ", " + localidad + " (" + provincia + ")";
    }
}
