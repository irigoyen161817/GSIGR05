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
public final class Direccion {

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
    public Direccion(String localidad, String provincia, String calle, String numero) {
        this.localidad = normalizar(localidad, "La localidad");
        this.provincia = normalizar(provincia, "La provincia");
        this.calle = normalizar(calle, "La calle");
        this.numero = normalizar(numero, "El número");
    }

    private static String normalizar(String valor, String campo) {
        if (valor == null) {
            throw new NullPointerException(campo + " de la dirección es obligatorio.");
        }
        if (valor.isBlank()) {
            throw new IllegalArgumentException(campo + " de la dirección no puede estar en blanco.");
        }
        return compactar(valor);
    }

    /**
     * Elimina los espacios en blanco de los extremos y colapsa las
     * secuencias de varios espacios internos en uno solo.
     */
    private static String compactar(String valor) {
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
     * Comprueba si la dirección está en la localidad y provincia indicadas.
     *
     * <p>Se comparan con el mismo criterio que {@link #equals(Object)}: se
     * ignoran los espacios de los extremos y los espacios internos repetidos,
     * y no se distinguen mayúsculas de minúsculas. Las tildes sí cuentan. Una
     * localidad o provincia en blanco no coincide con ninguna dirección.</p>
     *
     * @param localidad localidad que se busca
     * @param provincia provincia en la que está la localidad
     * @return {@code true} si la dirección está en esa localidad y provincia
     * @throws NullPointerException si {@code localidad} o {@code provincia}
     *                              son {@code null}
     */
    public boolean estaEn(String localidad, String provincia) {
        if (localidad == null) {
            throw new NullPointerException("Hay que indicar la localidad.");
        }
        if (provincia == null) {
            throw new NullPointerException("Hay que indicar la provincia.");
        }
        return clave(this.localidad).equals(clave(compactar(localidad)))
                && estaEnProvincia(provincia);
    }

    /**
     * Comprueba si la dirección está en la provincia indicada, sea cual sea
     * su localidad, con el mismo criterio de comparación que
     * {@link #estaEn(String, String)}.
     *
     * @param provincia provincia que se busca
     * @return {@code true} si la dirección está en esa provincia
     * @throws NullPointerException si {@code provincia} es {@code null}
     */
    public boolean estaEnProvincia(String provincia) {
        if (provincia == null) {
            throw new NullPointerException("Hay que indicar la provincia.");
        }
        return clave(this.provincia).equals(clave(compactar(provincia)));
    }

    /**
     * Dos direcciones son iguales si coinciden en localidad, provincia,
     * calle y número.
     *
     * @param obj objeto con el que comparar
     * @return {@code true} si {@code obj} es una {@code Direccion} con los
     *         mismos cuatro campos
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Direccion)) {
            return false;
        }
        Direccion otra = (Direccion) obj;
        return clave(localidad).equals(clave(otra.localidad))
                && clave(provincia).equals(clave(otra.provincia))
                && clave(calle).equals(clave(otra.calle))
                && clave(numero).equals(clave(otra.numero));
    }

    /**
     * Devuelve el código hash de la dirección, calculado con la misma
     * normalización (minúsculas) que usa {@link #equals(Object)}, de modo
     * que dos direcciones iguales tienen siempre el mismo código.
     *
     * @return el código hash de la dirección
     */
    @Override
    public int hashCode() {
        return Objects.hash(clave(localidad), clave(provincia), clave(calle), clave(numero));
    }

    /**
     * Forma de un campo que se usa para comparar: en minúsculas, sin
     * distinguir mayúsculas de minúsculas. La comparten
     * {@link #equals(Object)} y {@link #hashCode()} para que sean coherentes.
     */
    private static String clave(String campo) {
        return campo.toLowerCase(Locale.ROOT);
    }

    /**
     * Devuelve la dirección en forma legible, por ejemplo
     * {@code Calle Mayor 12, Pamplona (Navarra)}.
     *
     * @return la calle, el número, la localidad y la provincia
     */
    @Override
    public String toString() {
        return calle + " " + numero + ", " + localidad + " (" + provincia + ")";
    }
}
