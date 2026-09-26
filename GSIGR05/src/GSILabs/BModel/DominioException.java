package GSILabs.BModel;

/**
 * Excepción que se lanza cuando una operación incumple alguna de las
 * reglas de negocio de la práctica (C01 a C09).
 *
 * <p>Algunos ejemplos: registrar dos locales en la misma dirección,
 * registrar un usuario menor de 14 años, añadir un cuarto dueño a un
 * local o contestar dos veces a la misma review.</p>
 *
 * <p>Los errores que no son de negocio (por ejemplo, pasar {@code null}
 * donde no se admite) se notifican con las excepciones estándar de Java,
 * como {@link NullPointerException} o {@link IllegalArgumentException}.</p>
 */
public class DominioException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción indicando qué regla de negocio se ha incumplido.
     *
     * @param mensaje explicación en lenguaje natural de la regla incumplida,
     *                con los datos concretos del caso (por ejemplo: "El local
     *                \"Casa Pepe\" ya tiene 3 dueños, que es el máximo
     *                permitido.")
     */
    public DominioException(String mensaje) {
        super(mensaje);
    }
}
