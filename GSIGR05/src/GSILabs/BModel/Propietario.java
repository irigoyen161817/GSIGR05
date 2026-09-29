package GSILabs.BModel;

import java.time.LocalDate;

/**
 * Perfil de usuario propietario de locales.
 *
 * <p>Un {@code Propietario} puede poseer cualquier número de locales
 * ({@code 0..N}); cada {@code Local}, a su vez, debe tener entre 1 y 3
 * propietarios (C06). La colección de locales de cada propietario y la relación
 * bidireccional con {@code Local} se completan en la clase {@code Local}
 * una vez definida, para mantener la coherencia en ambos sentidos.</p>
 *
 * <p>La igualdad ({@link Usuario#equals(Object)} y {@link Usuario#hashCode()})
 * se hereda de {@link Usuario}, donde es {@code final} y se basa solo en el
 * nick: así un {@code Cliente} y un {@code Propietario} con el mismo nick se
 * consideran el mismo usuario, que es lo que permite garantizar que el nick
 * es único (C03).</p>
 */
public class Propietario extends Usuario {

    /**
     * Crea un propietario con los datos de cuenta indicados.
     *
     * @param nick            nick único de al menos
     *                        {@value Usuario#LONGITUD_MINIMA_NICK} caracteres
     * @param contrasena      contraseña de la cuenta
     * @param fechaNacimiento fecha de nacimiento del propietario
     * @throws DominioException si el nick o la edad incumplen las reglas
     *         de {@link Usuario} (C03)
     */
    public Propietario(String nick, String contrasena, LocalDate fechaNacimiento)
            throws DominioException {
        super(nick, contrasena, fechaNacimiento);
    }

    /**
     * Devuelve una representación legible del propietario: su perfil
     * seguido de su nick, por ejemplo {@code Propietario ana}. Nunca incluye
     * la contraseña.
     *
     * @return el perfil y el nick del propietario
     */
    @Override
    public String toString() {
        return "Propietario " + getNick();
    }
}
