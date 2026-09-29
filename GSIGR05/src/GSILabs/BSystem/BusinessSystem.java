package GSILabs.BSystem;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Cliente;
import GSILabs.BModel.Contestacion;
import GSILabs.BModel.Direccion;
import GSILabs.BModel.DominioException;
import GSILabs.BModel.Local;
import GSILabs.BModel.PoliticaBorrado;
import GSILabs.BModel.Propietario;
import GSILabs.BModel.Pub;
import GSILabs.BModel.Reserva;
import GSILabs.BModel.Reservable;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Review;
import GSILabs.BModel.Usuario;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Sistema de gestión del portal de ocio: almacena en memoria (sin
 * persistencia) los usuarios, locales, reviews, contestaciones y reservas,
 * y ofrece las operaciones de {@link LeisureOffice} y
 * {@link LookupService}.
 *
 * <p><b>Reparto de reglas.</b> Las reglas intrínsecas de cada entidad
 * (longitud del nick, edad mínima, longitud de descripciones y
 * comentarios, rango de la valoración...) las comprueba el propio modelo
 * ({@code GSILabs.BModel}) al construir los objetos. Esta clase comprueba
 * las reglas que dependen del conjunto de entidades registradas: nick
 * único (C03), una sola dirección por local (C01), entre 1 y 3 dueños por
 * local (C06), una review por visita (C05), una contestación por review
 * hecha por un dueño del local (C07) y que no se reserve en locales que no
 * están dados de alta (C09).</p>
 *
 * <p><b>Control de errores.</b> Los métodos de {@link LeisureOffice} y
 * {@link LookupService} devuelven {@code boolean}, {@code null} o un valor
 * especial y no declaran excepciones, así que esta clase no puede
 * propagar la {@link DominioException} (comprobada) del modelo. Todas las
 * operaciones siguen el mismo patrón:</p>
 * <ol>
 *   <li>la validación interna lanza una {@link DominioException} cuyo
 *       mensaje explica en lenguaje natural qué regla de negocio se
 *       incumple;</li>
 *   <li>el método público la captura, guarda su mensaje como último error
 *       y devuelve {@code false} o {@code null}, sin modificar nada;</li>
 *   <li>si la operación tiene éxito, el último error vuelve a
 *       {@code null}.</li>
 * </ol>
 * <p>Solo las altas, bajas y modificaciones actualizan el último error;
 * las consultas no lo modifican.</p>
 * <p>El motivo del último rechazo se consulta con {@link #getUltimoError()}.
 * Los errores de uso que no son reglas de negocio (por ejemplo, un
 * argumento {@code null}) no se capturan y se señalan con las excepciones
 * estándar de Java, salvo que el interfaz indique un resultado concreto
 * ({@code false} o {@code null}) para ese caso.</p>
 *
 * <p><b>Coherencia al borrar.</b> Las bajas de entidades con dependientes
 * (los que define cada {@code eliminar(PoliticaBorrado)} del modelo; ver
 * {@link PoliticaBorrado}) aplican una {@link PoliticaBorrado} configurable:</p>
 * <ul>
 *   <li>{@link PoliticaBorrado#BLOQUEAR} (por defecto): la baja se
 *       rechaza, devuelve {@code false}, no se modifica nada y
 *       {@link #getUltimoError()} explica qué la impide. Coincide con el
 *       comportamiento que describe {@link LeisureOffice#eliminaReview(Review)}.</li>
 *   <li>{@link PoliticaBorrado#CASCADA}: se eliminan también, de forma
 *       recursiva, los dependientes, de modo que no queda ninguna entidad
 *       apuntando a otra eliminada ni ningún local sin dueños.</li>
 * </ul>
 * <p>La política se elige al crear el sistema
 * ({@link #BusinessSystem(PoliticaBorrado)}) y se puede cambiar después con
 * {@link #setPoliticaBorrado(PoliticaBorrado)}.</p>
 *
 * <p><b>Estado de la implementación.</b> Los métodos que aún no están
 * implementados lanzan {@link UnsupportedOperationException} indicando la
 * issue del repositorio que los implementará.</p>
 */
public class BusinessSystem implements LeisureOffice, LookupService {

    /** Usuarios registrados, indexados por su nick (clave natural, C03). */
    private final Map<String, Usuario> usuarios = new LinkedHashMap<>();

    /** Locales registrados, indexados por su dirección (clave natural, C01). */
    private final Map<Direccion, Local> locales = new LinkedHashMap<>();

    /** Reviews registradas; su identidad es cliente, local y fecha de visita (C05). */
    private final Set<Review> reviews = new LinkedHashSet<>();

    /** Contestaciones registradas, indexadas por la review que contestan (C07). */
    private final Map<Review, Contestacion> contestaciones = new LinkedHashMap<>();

    /** Reservas registradas; su identidad es el id que genera {@link Reserva}. */
    private final Set<Reserva> reservas = new LinkedHashSet<>();

    /** Política aplicada en las bajas de entidades con dependientes. */
    private PoliticaBorrado politicaBorrado;

    /** Motivo del último rechazo, o {@code null} si la última operación tuvo éxito. */
    private String ultimoError;

    /**
     * Crea un sistema vacío que bloquea las bajas de entidades con
     * dependientes ({@link PoliticaBorrado#BLOQUEAR}).
     */
    public BusinessSystem() {
        this(PoliticaBorrado.BLOQUEAR);
    }

    /**
     * Crea un sistema vacío con la política de borrado indicada.
     *
     * @param politicaBorrado política que se aplicará en las bajas de
     *                        entidades con dependientes
     * @throws NullPointerException si {@code politicaBorrado} es {@code null}
     */
    public BusinessSystem(PoliticaBorrado politicaBorrado) {
        setPoliticaBorrado(politicaBorrado);
    }

    /**
     * Devuelve la política que se aplica en las bajas de entidades con
     * dependientes.
     *
     * @return la política de borrado actual
     */
    public PoliticaBorrado getPoliticaBorrado() {
        return politicaBorrado;
    }

    /**
     * Cambia la política que se aplica en las bajas de entidades con
     * dependientes. Solo afecta a las bajas posteriores.
     *
     * @param politicaBorrado nueva política de borrado
     * @throws NullPointerException si {@code politicaBorrado} es {@code null}
     */
    public final void setPoliticaBorrado(PoliticaBorrado politicaBorrado) {
        if (politicaBorrado == null) {
            throw new NullPointerException("Hay que indicar una política de borrado.");
        }
        this.politicaBorrado = politicaBorrado;
    }

    /**
     * Devuelve el motivo por el que se rechazó la última operación, en
     * lenguaje natural y con los datos concretos del caso.
     *
     * @return el mensaje del último rechazo, o {@code null} si la última
     *         operación terminó con éxito o todavía no se ha hecho ninguna
     */
    public String getUltimoError() {
        return ultimoError;
    }

    /**
     * Marca la operación en curso como correcta, borrando el último error.
     */
    private void operacionCorrecta() {
        ultimoError = null;
    }

    /**
     * Marca la operación en curso como rechazada, guardando el motivo.
     *
     * @param e excepción con el motivo del rechazo en lenguaje natural
     */
    private void operacionRechazada(DominioException e) {
        ultimoError = e.getMessage();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Las reglas del nick (al menos 3 caracteres) y de la edad (al menos
     * 14 años) ya las garantiza el constructor de {@link Usuario}; aquí se
     * comprueba que el nick no esté en uso (C03). Si lo está, devuelve
     * {@code false} y el motivo queda en {@link #getUltimoError()}.</p>
     *
     * @throws NullPointerException si {@code u} es {@code null}
     */
    @Override
    public boolean nuevoUsuario(Usuario u) {
        if (u == null) {
            throw new NullPointerException("Hay que indicar el usuario que se quiere dar de alta.");
        }
        try {
            comprobarNickLibre(u.getNick());
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        usuarios.put(u.getNick(), u);
        operacionCorrecta();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>La baja aplica la {@link #getPoliticaBorrado() política de borrado}
     * a los dependientes del usuario: las reviews y reservas de un
     * {@link Cliente}, y las contestaciones y los locales de los que es
     * único dueño un {@link Propietario}. Con
     * {@link PoliticaBorrado#BLOQUEAR}, si tiene alguno, la baja se rechaza.
     * Con {@link PoliticaBorrado#CASCADA}, se eliminan también esos
     * dependientes, y el propietario se retira de los locales que comparte
     * con otros dueños.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si no hay ningún usuario registrado con ese nick y ese perfil o si la
     * política de borrado impide la baja.</p>
     *
     * @throws NullPointerException si {@code u} es {@code null}
     */
    @Override
    public boolean eliminaUsuario(Usuario u) {
        if (u == null) {
            throw new NullPointerException("Hay que indicar el usuario que se quiere eliminar.");
        }
        Set<Object> eliminados;
        try {
            Usuario registrado = usuarioRegistrado(u);
            if (registrado instanceof Cliente cliente) {
                eliminados = cliente.eliminar(politicaBorrado);
            } else {
                eliminados = ((Propietario) registrado).eliminar(politicaBorrado);
            }
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        olvidar(eliminados);
        operacionCorrecta();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si {@code u} no está registrado con ese nick y ese perfil, si
     * {@code nuevoU} tiene otro perfil (un {@link Cliente} solo se sustituye
     * por otro {@code Cliente} y un {@link Propietario} por otro
     * {@code Propietario}), si {@code nuevoU} cambia a un nick que ya usa
     * otro usuario o si {@code u} tiene dependientes.</p>
     *
     * <p>Se consideran dependientes las reviews y reservas de un
     * {@code Cliente} y los locales y contestaciones de un
     * {@code Propietario}. Esta restricción no aparece en
     * {@link LeisureOffice} y se aplica con cualquier política de borrado:
     * las entidades del modelo son inmutables y guardan una referencia al
     * usuario, así que tras la sustitución seguirían apuntando al usuario
     * antiguo. Rehacerlas haría perder su fecha de creación (C08) o su
     * identificador.</p>
     *
     * @throws NullPointerException si {@code u} o {@code nuevoU} son
     *                              {@code null}
     */
    @Override
    public boolean modificaUsuario(Usuario u, Usuario nuevoU) {
        if (u == null) {
            throw new NullPointerException("Hay que indicar el usuario que se quiere modificar.");
        }
        if (nuevoU == null) {
            throw new NullPointerException("Hay que indicar los nuevos datos del usuario.");
        }
        try {
            Usuario registrado = usuarioRegistrado(u);
            if (registrado.getClass() != nuevoU.getClass()) {
                throw new DominioException("No se puede cambiar el perfil del usuario \"" + u.getNick()
                        + "\" de " + registrado.getClass().getSimpleName() + " a "
                        + nuevoU.getClass().getSimpleName() + ".");
            }
            if (registrado instanceof Cliente cliente
                    && (!cliente.getReviews().isEmpty() || !cliente.getReservas().isEmpty())) {
                throw new DominioException("No se puede modificar al cliente \"" + cliente.getNick()
                        + "\" porque tiene " + cliente.getReviews().size() + " review(s) y "
                        + cliente.getReservas().size() + " reserva(s) que seguirían apuntando a sus datos antiguos.");
            }
            if (registrado instanceof Propietario propietario
                    && (!propietario.getLocales().isEmpty() || !propietario.getContestaciones().isEmpty())) {
                throw new DominioException("No se puede modificar al propietario \"" + propietario.getNick()
                        + "\" porque tiene " + propietario.getLocales().size() + " local(es) y "
                        + propietario.getContestaciones().size()
                        + " contestación(es) que seguirían apuntando a sus datos antiguos.");
            }
            if (!registrado.equals(nuevoU)) {
                comprobarNickLibre(nuevoU.getNick());
            }
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        usuarios.remove(u.getNick());
        usuarios.put(nuevoU.getNick(), nuevoU);
        operacionCorrecta();
        return true;
    }

    /**
     * Comprueba si existe algún usuario registrado con ese nick. Los
     * espacios al principio y al final se ignoran, igual que al crear el
     * {@link Usuario}, y se distinguen mayúsculas y minúsculas.
     *
     * @param nick nick que se busca
     * @return {@code true} si existe un usuario con ese nick
     * @throws NullPointerException si {@code nick} es {@code null}
     */
    @Override
    public boolean existeNick(String nick) {
        return obtenerUsuario(nick) != null;
    }

    /**
     * Recupera el usuario asociado a un nick, en caso de que exista. Los
     * espacios al principio y al final se ignoran, igual que al crear el
     * {@link Usuario}, y se distinguen mayúsculas y minúsculas.
     *
     * @param nick nick del usuario que se busca
     * @return el usuario con ese nick, o {@code null} si
     *         {@link #existeNick(String)} es falso
     * @throws NullPointerException si {@code nick} es {@code null}
     */
    @Override
    public Usuario obtenerUsuario(String nick) {
        if (nick == null) {
            throw new NullPointerException("Hay que indicar el nick del usuario que se busca.");
        }
        return usuarios.get(nick.strip());
    }

    /**
     * Comprueba que ningún usuario registrado use ya el nick indicado (C03).
     *
     * @param nick nick que se quiere usar
     * @throws DominioException si ya hay un usuario registrado con ese nick
     */
    private void comprobarNickLibre(String nick) throws DominioException {
        if (existeNick(nick)) {
            throw new DominioException("Ya hay un usuario registrado con el nick \"" + nick
                    + "\" y no puede haber dos usuarios con el mismo nick.");
        }
    }

    /**
     * Devuelve el usuario registrado con el mismo nick y el mismo perfil
     * que {@code u}.
     *
     * @param u usuario que se busca
     * @return el usuario registrado con ese nick
     * @throws DominioException si no hay ningún usuario registrado con ese
     *         nick o si está registrado con otro perfil
     */
    private Usuario usuarioRegistrado(Usuario u) throws DominioException {
        Usuario registrado = obtenerUsuario(u.getNick());
        if (registrado == null) {
            throw new DominioException("El usuario \"" + u.getNick() + "\" no está registrado en el sistema.");
        }
        if (registrado.getClass() != u.getClass()) {
            throw new DominioException("El usuario \"" + u.getNick() + "\" está registrado como "
                    + registrado.getClass().getSimpleName() + ", no como "
                    + u.getClass().getSimpleName() + ".");
        }
        return registrado;
    }

    /**
     * Quita de las colecciones del sistema las entidades que ha eliminado el
     * modelo al aplicar la política de borrado.
     *
     * @param eliminados entidades devueltas por un
     *                   {@code eliminar(PoliticaBorrado)} del modelo
     */
    private void olvidar(Set<Object> eliminados) {
        for (Object eliminado : eliminados) {
            if (eliminado instanceof Usuario usuario) {
                usuarios.remove(usuario.getNick());
            } else if (eliminado instanceof Local local) {
                locales.remove(local.getDireccion());
            } else if (eliminado instanceof Review review) {
                reviews.remove(review);
            } else if (eliminado instanceof Contestacion contestacion) {
                contestaciones.remove(contestacion.getReview());
            } else if (eliminado instanceof Reserva reserva) {
                reservas.remove(reserva);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #27
     */
    @Override
    public boolean nuevaReview(Review r) {
        throw new UnsupportedOperationException("Pendiente: #27");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #27
     */
    @Override
    public boolean eliminaReview(Review r) {
        throw new UnsupportedOperationException("Pendiente: #27");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #27
     */
    @Override
    public boolean existeRewiew(Usuario u, Local l, LocalDate ld) {
        throw new UnsupportedOperationException("Pendiente: #27");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #27
     */
    @Override
    public Review[] verReviews(Local l) {
        throw new UnsupportedOperationException("Pendiente: #27");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #28
     */
    @Override
    public boolean nuevaContestacion(Contestacion c, Review r) {
        throw new UnsupportedOperationException("Pendiente: #28");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #28
     */
    @Override
    public boolean tieneContestacion(Review r) {
        throw new UnsupportedOperationException("Pendiente: #28");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #28
     */
    @Override
    public Contestacion obtenerContestacion(Review r) {
        throw new UnsupportedOperationException("Pendiente: #28");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #28
     */
    @Override
    public boolean eliminaContestacion(Contestacion c) {
        throw new UnsupportedOperationException("Pendiente: #28");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #28
     */
    @Override
    public boolean eliminaContestacion(Review r) {
        throw new UnsupportedOperationException("Pendiente: #28");
    }

    /**
     * {@inheritDoc}
     *
     * <p>La longitud de la descripción y que el local tenga entre 1 y 3
     * dueños ya los garantiza {@link Local}; aquí se comprueba que no haya
     * otro local registrado en la misma {@link Direccion} (C01) y que todos
     * los dueños del local sean propietarios registrados en el sistema
     * (C06). Tras el alta, el local aparece en
     * {@link Propietario#getLocales()} de cada uno de sus dueños.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si la dirección está ocupada o si algún dueño no es el propietario
     * registrado con ese nick.</p>
     *
     * @throws NullPointerException  si {@code l} es {@code null}
     * @throws IllegalStateException si {@code l} ya está vinculado a sus
     *                               dueños sin estar registrado en el sistema
     */
    @Override
    public boolean nuevoLocal(Local l) {
        if (l == null) {
            throw new NullPointerException("Hay que indicar el local que se quiere dar de alta.");
        }
        try {
            comprobarDireccionLibre(l);
            comprobarDueñosRegistrados(l);
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        l.vincular();
        locales.put(l.getDireccion(), l);
        operacionCorrecta();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Solo se elimina el objeto registrado: un local distinto con la
     * misma dirección se considera inexistente. La baja aplica la
     * {@link #getPoliticaBorrado() política de borrado} a las reviews (con
     * sus contestaciones) y reservas del local. Con
     * {@link PoliticaBorrado#BLOQUEAR}, si tiene alguna, la baja se
     * rechaza. Con {@link PoliticaBorrado#CASCADA}, se eliminan también.
     * En ambos casos el local se retira de {@link Propietario#getLocales()}
     * de sus dueños y su dirección queda libre para otro local.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si el local no está registrado o si la política de borrado impide la
     * baja.</p>
     *
     * @throws NullPointerException si {@code l} es {@code null}
     */
    @Override
    public boolean eliminarLocal(Local l) {
        if (l == null) {
            throw new NullPointerException("Hay que indicar el local que se quiere eliminar.");
        }
        Set<Object> eliminados;
        try {
            eliminados = localRegistrado(l).eliminar(politicaBorrado);
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        olvidar(eliminados);
        operacionCorrecta();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Las direcciones se comparan con {@link Direccion#equals(Object)},
     * que no distingue mayúsculas y minúsculas.</p>
     *
     * @throws NullPointerException si {@code d} es {@code null}
     */
    @Override
    public Local obtenerLocal(Direccion d) {
        if (d == null) {
            throw new NullPointerException("Hay que indicar la dirección del local que se busca.");
        }
        return locales.get(d);
    }

    /**
     * {@inheritDoc}
     *
     * <p>El local debe ser el objeto registrado y el propietario debe estar
     * registrado con ese nick como {@link Propietario}; se asocia el
     * propietario registrado. La relación queda reflejada en los dos
     * sentidos: en {@link Local#getDueños()} y en
     * {@link Propietario#getLocales()}.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si el local o el propietario no están registrados, si el propietario
     * ya es dueño del local o si el local ya tiene
     * {@value Local#MAX_DUENOS} dueños (C06).</p>
     *
     * @throws NullPointerException si {@code l} o {@code p} son {@code null}
     */
    @Override
    public boolean asociarLocal(Local l, Propietario p) {
        if (l == null) {
            throw new NullPointerException("Hay que indicar el local al que se quiere añadir un dueño.");
        }
        if (p == null) {
            throw new NullPointerException("Hay que indicar el propietario que se quiere añadir como dueño.");
        }
        try {
            Local local = localRegistrado(l);
            Propietario propietario = (Propietario) usuarioRegistrado(p);
            if (local.getDueños().contains(propietario)) {
                throw new DominioException("El propietario \"" + propietario.getNick()
                        + "\" ya es dueño del local \"" + local.getNombre() + "\".");
            }
            local.añadirDueño(propietario);
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        operacionCorrecta();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>El local debe ser el objeto registrado y el propietario debe estar
     * registrado con ese nick como {@link Propietario}. La relación se
     * deshace en los dos sentidos. Las contestaciones que el propietario ya
     * hubiera escrito en reviews del local se conservan, porque la regla de
     * que solo contesta un dueño (C07) se comprueba al contestar.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si el local o el propietario no están registrados, si el propietario
     * no es dueño del local o si es su único dueño, ya que un local no puede
     * quedarse sin dueños (C06).</p>
     *
     * @throws NullPointerException si {@code l} o {@code p} son {@code null}
     */
    @Override
    public boolean desasociarLocal(Local l, Propietario p) {
        if (l == null) {
            throw new NullPointerException("Hay que indicar el local del que se quiere quitar un dueño.");
        }
        if (p == null) {
            throw new NullPointerException("Hay que indicar el propietario que se quiere quitar como dueño.");
        }
        try {
            Local local = localRegistrado(l);
            Propietario propietario = (Propietario) usuarioRegistrado(p);
            if (!local.getDueños().contains(propietario)) {
                throw new DominioException("El propietario \"" + propietario.getNick()
                        + "\" no es dueño del local \"" + local.getNombre() + "\".");
            }
            local.quitarDueño(propietario);
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        operacionCorrecta();
        return true;
    }

    /**
     * Reemplaza en el sistema un local registrado por otro del mismo tipo.
     * El interfaz {@link LeisureOffice} no documenta este método, así que
     * su comportamiento se define aquí.
     *
     * <p>Los dueños de {@code viejoL} se traspasan a {@code nuevoL}, que
     * termina con exactamente los mismos dueños, y el local pasa a estar
     * registrado en la dirección de {@code nuevoL}. Si esa dirección es
     * distinta, la de {@code viejoL} queda libre.</p>
     *
     * <p>Los cambios que no afectan a la identidad del local (por ejemplo, la
     * descripción o las especialidades de un {@link Bar}) no necesitan este
     * método: se hacen con los métodos del modelo sobre el local ya
     * registrado, que se obtiene con {@link #obtenerLocal(Direccion)}.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * y sin modificar nada, si:</p>
     * <ul>
     *   <li>{@code viejoL} no es el local registrado en su dirección;</li>
     *   <li>{@code nuevoL} es de otro tipo concreto ({@link Bar},
     *       {@link Pub} o {@link Restaurante});</li>
     *   <li>{@code nuevoL} está en otra dirección que ya ocupa otro
     *       local (C01);</li>
     *   <li>algún dueño de {@code nuevoL} no es el propietario registrado
     *       con ese nick;</li>
     *   <li>{@code viejoL} tiene reviews o reservas. Esta restricción se
     *       aplica con cualquier política de borrado: las reviews y reservas
     *       guardan una referencia al local y seguirían apuntando al antiguo;
     *       rehacerlas haría perder su fecha de creación (C08) o su
     *       identificador.</li>
     * </ul>
     *
     * <p>Si {@code nuevoL} es el mismo objeto que {@code viejoL}, no hay
     * nada que cambiar y devuelve {@code true}.</p>
     *
     * @param viejoL local registrado que se quiere reemplazar
     * @param nuevoL local que lo sustituye
     * @return {@code true} si y solo si la operación se completó
     * @throws NullPointerException  si {@code viejoL} o {@code nuevoL} son
     *                               {@code null}
     * @throws IllegalStateException si {@code nuevoL} es otro objeto que ya
     *                               está vinculado a sus dueños
     */
    @Override
    public boolean actualizarLocal(Local viejoL, Local nuevoL) {
        if (viejoL == null) {
            throw new NullPointerException("Hay que indicar el local que se quiere actualizar.");
        }
        if (nuevoL == null) {
            throw new NullPointerException("Hay que indicar los nuevos datos del local.");
        }
        Local registrado;
        try {
            registrado = localRegistrado(viejoL);
            if (registrado == nuevoL) {
                operacionCorrecta();
                return true;
            }
            if (nuevoL.isVinculado()) {
                throw new IllegalStateException("El local \"" + nuevoL.getNombre()
                        + "\" ya está vinculado a sus dueños y no puede sustituir a otro.");
            }
            if (registrado.getClass() != nuevoL.getClass()) {
                throw new DominioException("No se puede cambiar el local \"" + registrado.getNombre()
                        + "\" de " + registrado.getClass().getSimpleName() + " a "
                        + nuevoL.getClass().getSimpleName() + ".");
            }
            if (!registrado.getReviews().isEmpty() || !registrado.getReservas().isEmpty()) {
                throw new DominioException("No se puede actualizar el local \"" + registrado.getNombre()
                        + "\" porque tiene " + registrado.getReviews().size() + " review(s) y "
                        + registrado.getReservas().size()
                        + " reserva(s) que seguirían apuntando a sus datos antiguos.");
            }
            if (!registrado.getDireccion().equals(nuevoL.getDireccion())) {
                comprobarDireccionLibre(nuevoL);
            }
            comprobarDueñosRegistrados(nuevoL);
            traspasarDueños(registrado, nuevoL);
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        registrado.desvincular();
        locales.remove(registrado.getDireccion());
        nuevoL.vincular();
        locales.put(nuevoL.getDireccion(), nuevoL);
        operacionCorrecta();
        return true;
    }

    /**
     * Comprueba que ningún local registrado ocupe ya la dirección del local
     * indicado (C01).
     *
     * @param l local que se quiere registrar
     * @throws DominioException si ya hay un local registrado en esa dirección
     */
    private void comprobarDireccionLibre(Local l) throws DominioException {
        Local ocupante = locales.get(l.getDireccion());
        if (ocupante != null) {
            throw new DominioException("No se puede registrar el local \"" + l.getNombre()
                    + "\" porque ya existe otro local, \"" + ocupante.getNombre() + "\", en "
                    + l.getDireccion() + ".");
        }
    }

    /**
     * Comprueba que todos los dueños del local sean los propietarios
     * registrados en el sistema con su nick, y no otros objetos con el mismo
     * nick, para que el local quede enlazado con los usuarios del sistema
     * (C06).
     *
     * @param l local cuyos dueños se comprueban
     * @throws DominioException si algún dueño no está registrado, está
     *         registrado con otro perfil o no es el objeto registrado
     */
    private void comprobarDueñosRegistrados(Local l) throws DominioException {
        for (Propietario dueño : l.getDueños()) {
            if (usuarioRegistrado(dueño) != dueño) {
                throw new DominioException("El dueño \"" + dueño.getNick() + "\" del local \""
                        + l.getNombre() + "\" no es el propietario registrado en el sistema con ese nick.");
            }
        }
    }

    /**
     * Devuelve el local registrado, exigiendo que sea el mismo objeto que
     * {@code l}: otro local en la misma dirección se considera inexistente.
     *
     * @param l local que se busca
     * @return el local registrado, que es el propio {@code l}
     * @throws DominioException si {@code l} no es el local registrado en su
     *         dirección
     */
    private Local localRegistrado(Local l) throws DominioException {
        Local registrado = locales.get(l.getDireccion());
        if (registrado == null) {
            throw new DominioException("El local \"" + l.getNombre() + "\" no está registrado en el sistema.");
        }
        if (registrado != l) {
            throw new DominioException("El local \"" + l.getNombre() + "\" no está registrado en el sistema: en "
                    + l.getDireccion() + " está registrado otro local, \"" + registrado.getNombre() + "\".");
        }
        return registrado;
    }

    /**
     * Deja a {@code destino} con exactamente los mismos dueños que
     * {@code origen}. Si {@code destino} ya tiene el máximo de dueños, se
     * quita uno que no esté en {@code origen} antes de añadir el siguiente,
     * de modo que nunca se supera el máximo ni se queda sin dueños.
     *
     * @param origen  local cuyos dueños se traspasan
     * @param destino local sin vincular que recibe los dueños
     * @throws DominioException no se produce, porque el orden de las
     *         operaciones respeta los límites de dueños de {@link Local}
     */
    private static void traspasarDueños(Local origen, Local destino) throws DominioException {
        for (Propietario dueño : origen.getDueños()) {
            if (!destino.getDueños().contains(dueño)) {
                if (destino.getDueños().size() == Local.MAX_DUENOS) {
                    destino.quitarDueño(dueñoAjeno(origen, destino));
                }
                destino.añadirDueño(dueño);
            }
        }
        for (Propietario dueño : new ArrayList<>(destino.getDueños())) {
            if (!origen.getDueños().contains(dueño)) {
                destino.quitarDueño(dueño);
            }
        }
    }

    /**
     * Devuelve un dueño de {@code destino} que no lo es de {@code origen}.
     *
     * @param origen  local de referencia
     * @param destino local en el que se busca el dueño
     * @return un dueño de {@code destino} que no está en {@code origen}
     * @throws NoSuchElementException si todos los dueños de {@code destino}
     *         lo son también de {@code origen}
     */
    private static Propietario dueñoAjeno(Local origen, Local destino) {
        for (Propietario dueño : destino.getDueños()) {
            if (!origen.getDueños().contains(dueño)) {
                return dueño;
            }
        }
        throw new NoSuchElementException("Todos los dueños del local \"" + destino.getNombre()
                + "\" lo son también del local \"" + origen.getNombre() + "\".");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #29
     */
    @Override
    public boolean nuevaReserva(Cliente c, Reservable r, LocalDate ld, LocalTime lt) {
        throw new UnsupportedOperationException("Pendiente: #29");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #29
     */
    @Override
    public Reserva[] obtenerReservas(Cliente c) {
        throw new UnsupportedOperationException("Pendiente: #29");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #29
     */
    @Override
    public Reserva[] obtenerReservas(Reservable r) {
        throw new UnsupportedOperationException("Pendiente: #29");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #29
     */
    @Override
    public Reserva[] obtenerReservas(LocalDate ld) {
        throw new UnsupportedOperationException("Pendiente: #29");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #29
     */
    @Override
    public boolean eliminarReserva(Reserva r) {
        throw new UnsupportedOperationException("Pendiente: #29");
    }

    /**
     * Lista los locales de cualquier tipo de una localidad y provincia.
     *
     * @param ciudad    localidad de interés
     * @param provincia provincia en la que se encuentra la localidad
     * @return los locales encontrados, potencialmente de longitud 0
     * @throws UnsupportedOperationException pendiente de implementar en #26
     */
    @Override
    public Local[] listarLocales(String ciudad, String provincia) {
        throw new UnsupportedOperationException("Pendiente: #26");
    }

    /**
     * Lista los bares de una localidad y provincia.
     *
     * @param ciudad    localidad de interés
     * @param provincia provincia en la que se encuentra la localidad
     * @return los bares encontrados, potencialmente de longitud 0
     * @throws UnsupportedOperationException pendiente de implementar en #26
     */
    @Override
    public Bar[] listarBares(String ciudad, String provincia) {
        throw new UnsupportedOperationException("Pendiente: #26");
    }

    /**
     * Lista los restaurantes de una localidad y provincia.
     *
     * @param ciudad    localidad de interés
     * @param provincia provincia en la que se encuentra la localidad
     * @return los restaurantes encontrados, potencialmente de longitud 0
     * @throws UnsupportedOperationException pendiente de implementar en #26
     */
    @Override
    public Restaurante[] listarRestaurantes(String ciudad, String provincia) {
        throw new UnsupportedOperationException("Pendiente: #26");
    }

    /**
     * Lista los pubs de una localidad y provincia.
     *
     * @param ciudad    localidad de interés
     * @param provincia provincia en la que se encuentra la localidad
     * @return los pubs encontrados, potencialmente de longitud 0
     * @throws UnsupportedOperationException pendiente de implementar en #26
     */
    @Override
    public Pub[] listarPubs(String ciudad, String provincia) {
        throw new UnsupportedOperationException("Pendiente: #26");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #30
     */
    @Override
    public float obtenerValoracionMedia(Local l) {
        throw new UnsupportedOperationException("Pendiente: #30");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #30
     */
    @Override
    public float obtenerValoracionMedia(Propietario p) {
        throw new UnsupportedOperationException("Pendiente: #30");
    }

    /**
     * Obtiene la valoración media de un local contando solo las reviews
     * de clientes cuya edad estaba en el rango indicado.
     *
     * @param l         local de interés
     * @param edadEntre edad mínima del rango (incluida)
     * @param edadHasta edad máxima del rango (incluida)
     * @return la valoración media de esas reviews, 0 si no hay ninguna o
     *         -1 si el local no existe
     * @throws UnsupportedOperationException pendiente de implementar en #30
     */
    @Override
    public float obtenerValoracionMedia(Local l, int edadEntre, int edadHasta) {
        throw new UnsupportedOperationException("Pendiente: #30");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #30
     */
    @Override
    public Local[] obtenerLocalesOrdenados(String ciudad, String provincia) {
        throw new UnsupportedOperationException("Pendiente: #30");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #30
     */
    @Override
    public Local[] obtenerLocalesOrdenados(String provincia) {
        throw new UnsupportedOperationException("Pendiente: #30");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #30
     */
    @Override
    public Bar[] obtenerBaresOrdenados(String ciudad, String provincia) {
        throw new UnsupportedOperationException("Pendiente: #30");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #30
     */
    @Override
    public Restaurante[] obtenerRestaurantesOrdenados(String ciudad, String provincia) {
        throw new UnsupportedOperationException("Pendiente: #30");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #30
     */
    @Override
    public Pub[] obtenerPubOrdenados(String ciudad, String provincia) {
        throw new UnsupportedOperationException("Pendiente: #30");
    }
}
