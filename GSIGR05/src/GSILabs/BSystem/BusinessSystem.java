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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
        if (usuarios.containsKey(nick)) {
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
        Usuario registrado = usuarios.get(u.getNick());
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
     * @throws UnsupportedOperationException pendiente de implementar en #25
     */
    @Override
    public boolean nuevoLocal(Local l) {
        throw new UnsupportedOperationException("Pendiente: #25");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #25
     */
    @Override
    public boolean eliminarLocal(Local l) {
        throw new UnsupportedOperationException("Pendiente: #25");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #25
     */
    @Override
    public Local obtenerLocal(Direccion d) {
        throw new UnsupportedOperationException("Pendiente: #25");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #25
     */
    @Override
    public boolean asociarLocal(Local l, Propietario p) {
        throw new UnsupportedOperationException("Pendiente: #25");
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException pendiente de implementar en #25
     */
    @Override
    public boolean desasociarLocal(Local l, Propietario p) {
        throw new UnsupportedOperationException("Pendiente: #25");
    }

    /**
     * Reemplaza en el sistema un local registrado por otro.
     *
     * @param viejoL local registrado que se quiere reemplazar
     * @param nuevoL local que lo sustituye
     * @return {@code true} si y solo si la operación se completó
     * @throws UnsupportedOperationException pendiente de implementar en #25
     */
    @Override
    public boolean actualizarLocal(Local viejoL, Local nuevoL) {
        throw new UnsupportedOperationException("Pendiente: #25");
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
