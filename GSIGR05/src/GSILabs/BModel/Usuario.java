package GSILabs.BModel;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Clase base abstracta para los usuarios del sistema.
 *
 * <p>Un usuario tiene un {@code nick}, una contraseña y una fecha de
 * nacimiento. El {@code nick} debe tener al menos
 * {@value #LONGITUD_MINIMA_NICK} caracteres (una vez eliminados los espacios
 * en blanco de los extremos) y su unicidad en el sistema la garantiza
 * {@code GSILabs.BSystem.BusinessSystem}, no esta clase. No se permiten
 * usuarios con menos de {@value #EDAD_MINIMA} años (C03).</p>
 *
 * <p>Todo usuario tiene un perfil concreto, modelado como subtipo:
 * {@link Dueño} o {@link Cliente} (C04, C06). La igualdad entre usuarios se
 * define únicamente a partir del nick (ver {@link #equals(Object)}), con
 * independencia del perfil concreto.</p>
 */
public abstract class Usuario {

    /** Longitud mínima exigida para el nick. */
    public static final int LONGITUD_MINIMA_NICK = 3;

    /** Edad mínima, en años, para poder registrarse como usuario. */
    public static final int EDAD_MINIMA = 14;

    private final String nick;
    private final String contrasena;
    private final LocalDate fechaNacimiento;

    /**
     * Crea un usuario con los datos de cuenta indicados.
     *
     * @param nick            nick del usuario, de al menos
     *                        {@value #LONGITUD_MINIMA_NICK} caracteres tras
     *                        eliminar los espacios en blanco de los extremos
     * @param contrasena      contraseña de la cuenta, no vacía
     * @param fechaNacimiento fecha de nacimiento del usuario
     * @throws NullPointerException si el nick, la contraseña o la fecha de
     *         nacimiento son {@code null}
     * @throws IllegalArgumentException si la contraseña está vacía
     * @throws DominioException si el nick tiene menos de
     *         {@value #LONGITUD_MINIMA_NICK} caracteres (sin contar los
     *         espacios de los extremos) o si el usuario tiene menos de
     *         {@value #EDAD_MINIMA} años (C03)
     */
    protected Usuario(String nick, String contrasena, LocalDate fechaNacimiento)
            throws DominioException {
        Objects.requireNonNull(nick, "El nick es obligatorio.");
        Objects.requireNonNull(contrasena, "La contraseña es obligatoria.");
        Objects.requireNonNull(fechaNacimiento, "La fecha de nacimiento es obligatoria.");
        if (contrasena.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }
        if (nick.strip().length() < LONGITUD_MINIMA_NICK) {
            throw new DominioException("El nick \"" + nick + "\" es demasiado corto: debe tener al menos "
                    + LONGITUD_MINIMA_NICK + " caracteres.");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < EDAD_MINIMA) {
            throw new DominioException("El usuario \"" + nick + "\" tiene " + edad
                    + " años y no se permite registrar a menores de " + EDAD_MINIMA + ".");
        }

        this.nick = nick;
        this.contrasena = contrasena;
        this.fechaNacimiento = fechaNacimiento;
    }

    /**
     * Devuelve el nick del usuario.
     *
     * @return el nick, único dentro del sistema
     */
    public String getNick() {
        return nick;
    }

    /**
     * Devuelve la contraseña del usuario.
     *
     * @return la contraseña de la cuenta
     */
    public String getContrasena() {
        return contrasena;
    }

    /**
     * Devuelve la fecha de nacimiento del usuario.
     *
     * @return la fecha de nacimiento
     */
    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    /**
     * Dos usuarios son iguales si tienen el mismo nick, ya que el nick es
     * único dentro del sistema. Esta igualdad es común a todos los perfiles:
     * un {@link Dueño} y un {@link Cliente} con el mismo nick se consideran
     * iguales, puesto que ese nick no podría haberse asignado a ambos.
     *
     * @param obj objeto con el que comparar
     * @return {@code true} si {@code obj} es un {@code Usuario} (de
     *         cualquier perfil) con el mismo nick
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Usuario)) {
            return false;
        }
        Usuario otro = (Usuario) obj;
        return Objects.equals(nick, otro.nick);
    }

    /**
     * Calcula el código hash del usuario a partir de su nick, de forma
     * consistente con {@link #equals(Object)}.
     *
     * @return el código hash basado en el nick
     */
    @Override
    public final int hashCode() {
        return Objects.hash(nick);
    }
}
