package GSILabs.BModel;

import java.time.LocalDate;

/**
 * Perfil de usuario que puede consultar locales y redactar reseñas.
 *
 * <p>Un {@code Cliente} puede publicar {@code Review reviews} sobre
 * {@link Local locales} y realizar reservas sobre los que sean
 * {@link Reservable} (C04). Esas operaciones se ofrecen desde
 * {@code GSILabs.BSystem.BusinessSystem}, que coordina la creación de las
 * entidades relacionadas.</p>
 */
public class Cliente extends Usuario {

    /**
     * Crea un cliente con los datos de cuenta indicados.
     *
     * @param nick            nick único de al menos
     *                        {@value Usuario#LONGITUD_MINIMA_NICK} caracteres
     * @param contrasena      contraseña de la cuenta
     * @param fechaNacimiento fecha de nacimiento del cliente
     * @throws DominioException si el nick o la edad incumplen las reglas
     *         de {@link Usuario} (C03)
     */
    public Cliente(String nick, String contrasena, LocalDate fechaNacimiento)
            throws DominioException {
        super(nick, contrasena, fechaNacimiento);
    }

    @Override
    public String toString() {
        return "Cliente{nick=" + getNick() + "}";
    }
}
