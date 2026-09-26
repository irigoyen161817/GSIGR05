package GSILabs.BModel;

import java.time.LocalDate;

/**
 * Perfil de usuario propietario de locales.
 *
 * <p>Un {@code Dueño} puede poseer cualquier número de locales
 * ({@code 0..N}); cada {@code Local}, a su vez, debe tener entre 1 y 3
 * dueños (C06). La colección de locales de cada dueño y la relación
 * bidireccional con {@code Local} se completan en la clase {@code Local}
 * una vez definida, para mantener la coherencia en ambos sentidos.</p>
 */
public class Dueño extends Usuario {

    /**
     * Crea un dueño con los datos de cuenta indicados.
     *
     * @param nick            nick único de al menos
     *                        {@value Usuario#LONGITUD_MINIMA_NICK} caracteres
     * @param contrasena      contraseña de la cuenta
     * @param fechaNacimiento fecha de nacimiento del dueño
     * @throws DominioException si el nick o la edad incumplen las reglas
     *         de {@link Usuario} (C03)
     */
    public Dueño(String nick, String contrasena, LocalDate fechaNacimiento)
            throws DominioException {
        super(nick, contrasena, fechaNacimiento);
    }

    @Override
    public String toString() {
        return "Dueño{nick=" + getNick() + "}";
    }
}
