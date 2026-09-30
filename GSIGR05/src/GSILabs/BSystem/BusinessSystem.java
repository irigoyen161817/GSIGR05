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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntFunction;

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
 * hecha por un dueño del local (C07) y, en las reservas, que el local esté
 * dado de alta, que la fecha y hora sean futuras y que el cliente no
 * reserve dos veces en el mismo local el mismo día (C09).</p>
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
 * <p><b>Consultas de {@link LookupService}.</b> Resuelven así las
 * ambigüedades del interfaz:</p>
 * <ul>
 *   <li>La valoración media es la media aritmética de las estrellas de las
 *       reviews registradas; si no hay ninguna, vale 0.</li>
 *   <li>Cuando el local o el propietario no existen se devuelve -1, aunque
 *       el Javadoc de
 *       {@link LookupService#obtenerValoracionMedia(Local, int, int)}
 *       mencione también {@code null}, porque el tipo devuelto es
 *       {@code float}.</li>
 *   <li>La edad del autor "en el momento de la valoración" se calcula en la
 *       fecha de creación de la review (C08), que es cuando el cliente
 *       valoró el local, y no en la fecha de la visita.</li>
 *   <li>Los listados ordenados van de mayor a menor valoración media; los
 *       locales empatados, incluidos los que no tienen reviews (que valen
 *       0), conservan el orden en que se dieron de alta.</li>
 * </ul>
 */
public class BusinessSystem implements LeisureOffice, LookupService {

    private final Map<String, Usuario> usuarios = new LinkedHashMap<>();

    private final Map<Direccion, Local> locales = new LinkedHashMap<>();

    private final Set<Review> reviews = new LinkedHashSet<>();

    private final Map<Review, Contestacion> contestaciones = new LinkedHashMap<>();

    private final Set<Reserva> reservas = new LinkedHashSet<>();

    private PoliticaBorrado politicaBorrado;

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

    private void operacionCorrecta() {
        ultimoError = null;
    }

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

    private void comprobarNickLibre(String nick) throws DominioException {
        if (existeNick(nick)) {
            throw new DominioException("Ya hay un usuario registrado con el nick \"" + nick
                    + "\" y no puede haber dos usuarios con el mismo nick.");
        }
    }

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
     * <p>Los datos propios de la review (valoración de 0 a 5 estrellas,
     * comentario de 500 caracteres como máximo y visita ya ocurrida) y su
     * fecha de creación (C08) ya los garantiza {@link Review}; aquí se
     * comprueba que su autor sea el cliente registrado con ese nick, que su
     * local sea el registrado en su dirección y que ese cliente no haya
     * valorado ya la misma visita (C05). Tras el alta, la review aparece en
     * {@link Cliente#getReviews()} y en {@link Local#getReviews()}.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si el cliente o el local no están registrados o si ya hay una review
     * del mismo cliente sobre el mismo local con la misma fecha de
     * visita.</p>
     *
     * @throws NullPointerException si {@code r} es {@code null}
     */
    @Override
    public boolean nuevaReview(Review r) {
        if (r == null) {
            throw new NullPointerException("Hay que indicar la review que se quiere publicar.");
        }
        try {
            comprobarClienteRegistrado(r.getCliente());
            comprobarLocalRegistrado(r.getLocal());
            comprobarVisitaSinValorar(r);
            r.vincular();
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        reviews.add(r);
        operacionCorrecta();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Solo se elimina la review registrada: otra review de la misma
     * visita que no se ha dado de alta se considera inexistente. La baja
     * aplica la {@link #getPoliticaBorrado() política de borrado} a su
     * contestación. Con {@link PoliticaBorrado#BLOQUEAR}, si la tiene, la
     * baja se rechaza, que es el comportamiento que describe
     * {@link LeisureOffice}. Con {@link PoliticaBorrado#CASCADA}, se
     * elimina también la contestación.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si la review no está registrada o si la política de borrado impide la
     * baja.</p>
     *
     * @throws NullPointerException si {@code r} es {@code null}
     */
    @Override
    public boolean eliminaReview(Review r) {
        if (r == null) {
            throw new NullPointerException("Hay que indicar la review que se quiere eliminar.");
        }
        Set<Object> eliminados;
        try {
            comprobarReviewRegistrada(r);
            eliminados = r.eliminar(politicaBorrado);
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
     * <p>Se consideran incorrectos o inexistentes, y el resultado es
     * {@code false}, un argumento {@code null}, un usuario que no es el
     * {@link Cliente} registrado con ese nick y un local que no es el
     * registrado en su dirección.</p>
     */
    @Override
    public boolean existeRewiew(Usuario u, Local l, LocalDate ld) {
        if (!(u instanceof Cliente cliente) || !esClienteRegistrado(cliente)
                || l == null || !esLocalRegistrado(l) || ld == null) {
            return false;
        }
        for (Review review : cliente.getReviews()) {
            if (review.getLocal().equals(l) && review.getFechaVisita().equals(ld)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Las reviews se devuelven en el orden en que se publicaron. Un
     * {@code l} nulo o que no es el local registrado en su dirección se
     * considera inexistente.</p>
     */
    @Override
    public Review[] verReviews(Local l) {
        if (l == null || !esLocalRegistrado(l)) {
            return null;
        }
        return l.getReviews().toArray(new Review[0]);
    }

    private void comprobarClienteRegistrado(Cliente c) throws DominioException {
        if (usuarioRegistrado(c) != c) {
            throw new DominioException("El cliente \"" + c.getNick()
                    + "\" no es el cliente registrado en el sistema con ese nick.");
        }
    }

    private boolean esClienteRegistrado(Cliente c) {
        return obtenerUsuario(c.getNick()) == c;
    }

    private void comprobarVisitaSinValorar(Review r) throws DominioException {
        Cliente cliente = r.getCliente();
        Local local = r.getLocal();
        LocalDate fechaVisita = r.getFechaVisita();
        if (existeRewiew(cliente, local, fechaVisita)) {
            throw new DominioException("\"" + cliente.getNick() + "\" ya ha valorado su visita a \""
                    + local.getNombre() + "\" del " + fechaVisita
                    + " y no se puede valorar dos veces la misma visita.");
        }
    }

    private void comprobarReviewRegistrada(Review r) throws DominioException {
        comprobarClienteRegistrado(r.getCliente());
        comprobarLocalRegistrado(r.getLocal());
        if (!r.isVinculada() || !reviews.contains(r)) {
            throw new DominioException("La review de \"" + r.getCliente().getNick() + "\" sobre \""
                    + r.getLocal().getNombre() + "\" del " + r.getFechaVisita()
                    + " no está registrada en el sistema.");
        }
    }

    private boolean esReviewRegistrada(Review r) {
        return esClienteRegistrado(r.getCliente())
                && esLocalRegistrado(r.getLocal())
                && r.isVinculada() && reviews.contains(r);
    }

    /**
     * {@inheritDoc}
     *
     * <p>El comentario (no vacío y de 500 caracteres como máximo) y la fecha
     * de creación (C08) ya los garantiza {@link Contestacion}; aquí se
     * comprueba que {@code r} sea la review registrada en el sistema (S8),
     * que el autor sea el {@link Propietario} registrado con ese nick y
     * dueño del local de la review, y que la review no tenga ya una
     * contestación (C07). Tras el alta, la contestación aparece en
     * {@link Review#getContestacion()} y en
     * {@link Propietario#getContestaciones()}.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si la review no está registrada, si el autor no está registrado o ya
     * no es dueño del local, o si la review ya tiene contestación.</p>
     *
     * @throws NullPointerException     si {@code c} o {@code r} son
     *                                  {@code null}
     * @throws IllegalArgumentException si {@code c} no contesta a la review
     *                                  {@code r}, sino a otro objeto
     */
    @Override
    public boolean nuevaContestacion(Contestacion c, Review r) {
        if (c == null) {
            throw new NullPointerException("Hay que indicar la contestación que se quiere publicar.");
        }
        if (r == null) {
            throw new NullPointerException("Hay que indicar la review que se quiere contestar.");
        }
        if (c.getReview() != r) {
            throw new IllegalArgumentException("La contestación de \"" + c.getAutor().getNick()
                    + "\" responde a otra review, no a la de \"" + r.getCliente().getNick()
                    + "\" sobre \"" + r.getLocal().getNombre() + "\" del " + r.getFechaVisita() + ".");
        }
        try {
            comprobarReviewRegistrada(r);
            comprobarPropietarioRegistrado(c.getAutor());
            if (contestaciones.containsKey(r)) {
                throw new DominioException("La review de \"" + r.getCliente().getNick() + "\" sobre \""
                        + r.getLocal().getNombre() + "\" del " + r.getFechaVisita()
                        + " ya tiene una contestación y solo se permite una.");
            }
            c.vincular();
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        contestaciones.put(r, c);
        operacionCorrecta();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Un {@code r} nulo o que no es la review registrada en el sistema se
     * considera inexistente y el resultado es {@code false}.</p>
     */
    @Override
    public boolean tieneContestacion(Review r) {
        return obtenerContestacion(r) != null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Un {@code r} nulo o que no es la review registrada en el sistema se
     * considera inexistente y el resultado es {@code null}.</p>
     */
    @Override
    public Contestacion obtenerContestacion(Review r) {
        if (r == null || !esReviewRegistrada(r)) {
            return null;
        }
        return contestaciones.get(r);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Solo se elimina la contestación registrada: otra contestación a la
     * misma review que no se ha dado de alta se considera inexistente. Una
     * contestación no tiene dependientes, así que la
     * {@link #getPoliticaBorrado() política de borrado} no impide la baja.
     * Tras la baja, la review queda sin contestación y puede volver a
     * contestarse.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si la contestación no está registrada.</p>
     *
     * @throws NullPointerException si {@code c} es {@code null}
     */
    @Override
    public boolean eliminaContestacion(Contestacion c) {
        if (c == null) {
            throw new NullPointerException("Hay que indicar la contestación que se quiere eliminar.");
        }
        Review review = c.getReview();
        try {
            if (!esReviewRegistrada(review) || contestaciones.get(review) != c) {
                throw new DominioException("La contestación de \"" + c.getAutor().getNick()
                        + "\" a la review de \"" + review.getCliente().getNick() + "\" sobre \""
                        + review.getLocal().getNombre() + "\" no está registrada en el sistema.");
            }
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        olvidar(c.eliminar(politicaBorrado));
        operacionCorrecta();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Se comporta como {@link #eliminaContestacion(Contestacion)} con la
     * contestación registrada de {@code r}.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si la review no está registrada o si no tiene contestación.</p>
     *
     * @throws NullPointerException si {@code r} es {@code null}
     */
    @Override
    public boolean eliminaContestacion(Review r) {
        if (r == null) {
            throw new NullPointerException("Hay que indicar la review cuya contestación se quiere eliminar.");
        }
        Contestacion contestacion;
        try {
            comprobarReviewRegistrada(r);
            contestacion = contestaciones.get(r);
            if (contestacion == null) {
                throw new DominioException("La review de \"" + r.getCliente().getNick() + "\" sobre \""
                        + r.getLocal().getNombre() + "\" del " + r.getFechaVisita()
                        + " no tiene ninguna contestación que eliminar.");
            }
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        olvidar(contestacion.eliminar(politicaBorrado));
        operacionCorrecta();
        return true;
    }

    private void comprobarPropietarioRegistrado(Propietario p) throws DominioException {
        if (usuarioRegistrado(p) != p) {
            throw new DominioException("El propietario \"" + p.getNick()
                    + "\" no es el propietario registrado en el sistema con ese nick.");
        }
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
            comprobarLocalRegistrado(l);
            eliminados = l.eliminar(politicaBorrado);
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
            comprobarLocalRegistrado(l);
            Propietario propietario = (Propietario) usuarioRegistrado(p);
            if (l.getDueños().contains(propietario)) {
                throw new DominioException("El propietario \"" + propietario.getNick()
                        + "\" ya es dueño del local \"" + l.getNombre() + "\".");
            }
            l.añadirDueño(propietario);
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
            comprobarLocalRegistrado(l);
            Propietario propietario = (Propietario) usuarioRegistrado(p);
            if (!l.getDueños().contains(propietario)) {
                throw new DominioException("El propietario \"" + propietario.getNick()
                        + "\" no es dueño del local \"" + l.getNombre() + "\".");
            }
            l.quitarDueño(propietario);
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
     * termina con exactamente los mismos dueños: los dueños con los que se
     * creó {@code nuevoL} se descartan, así que no hace falta que estén
     * registrados. El local pasa a estar registrado en la dirección de
     * {@code nuevoL}. Si esa dirección es distinta, la de {@code viejoL}
     * queda libre.</p>
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
        Direccion direccionVieja = viejoL.getDireccion();
        Direccion direccionNueva = nuevoL.getDireccion();
        try {
            comprobarLocalRegistrado(viejoL);
            if (viejoL == nuevoL) {
                operacionCorrecta();
                return true;
            }
            if (nuevoL.isVinculado()) {
                throw new IllegalStateException("El local \"" + nuevoL.getNombre()
                        + "\" ya está vinculado a sus dueños y no puede sustituir a otro.");
            }
            if (viejoL.getClass() != nuevoL.getClass()) {
                throw new DominioException("No se puede cambiar el local \"" + viejoL.getNombre()
                        + "\" de " + viejoL.getClass().getSimpleName() + " a "
                        + nuevoL.getClass().getSimpleName() + ".");
            }
            int numReviews = viejoL.getReviews().size();
            int numReservas = viejoL.getReservas().size();
            if (numReviews > 0 || numReservas > 0) {
                throw new DominioException("No se puede actualizar el local \"" + viejoL.getNombre()
                        + "\" porque tiene " + numReviews + " review(s) y " + numReservas
                        + " reserva(s) que seguirían apuntando a sus datos antiguos.");
            }
            if (!direccionVieja.equals(direccionNueva)) {
                comprobarDireccionLibre(nuevoL);
            }
            traspasarDueños(viejoL, nuevoL);
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        viejoL.desvincular();
        locales.remove(direccionVieja);
        nuevoL.vincular();
        locales.put(direccionNueva, nuevoL);
        operacionCorrecta();
        return true;
    }

    private void comprobarDireccionLibre(Local l) throws DominioException {
        Direccion direccion = l.getDireccion();
        Local ocupante = locales.get(direccion);
        if (ocupante != null) {
            throw new DominioException("No se puede registrar el local \"" + l.getNombre()
                    + "\" porque la dirección " + direccion + " ya está ocupada por el local \""
                    + ocupante.getNombre() + "\".");
        }
    }

    private void comprobarLocalRegistrado(Local l) throws DominioException {
        Direccion direccion = l.getDireccion();
        Local registrado = locales.get(direccion);
        if (registrado == null) {
            throw new DominioException("El local \"" + l.getNombre() + "\" no está registrado en el sistema.");
        }
        if (registrado != l) {
            throw new DominioException("El local \"" + l.getNombre() + "\" no está registrado en el sistema: en "
                    + direccion + " está registrado otro local, \"" + registrado.getNombre() + "\".");
        }
    }

    private boolean esLocalRegistrado(Local l) {
        return obtenerLocal(l.getDireccion()) == l;
    }

    private void comprobarDueñosRegistrados(Local l) throws DominioException {
        for (Propietario dueño : l.getDueños()) {
            if (usuarioRegistrado(dueño) != dueño) {
                throw new DominioException("El dueño \"" + dueño.getNick() + "\" del local \""
                        + l.getNombre() + "\" no es el propietario registrado en el sistema con ese nick.");
            }
        }
    }

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
     * <p>Que el local no sea un {@link Pub} lo garantiza el tipo
     * {@link Reservable}, que solo implementan {@link Bar} y
     * {@link Restaurante} (C09). Aquí se comprueba que el cliente sea el
     * registrado con ese nick, que el local sea el registrado en su
     * dirección, que la fecha y hora sean posteriores al momento actual y
     * que el cliente no tenga ya una reserva en ese local ese mismo día. La
     * reserva se crea sin descuento. Tras el alta, aparece en
     * {@link Cliente#getReservas()} y en {@link Local#getReservas()}.</p>
     *
     * <p>El local se compara con el objeto registrado, no solo con su
     * dirección: un local que no se ha dado de alta se considera
     * inexistente aunque haya otro registrado en su misma dirección.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si el cliente o el local no están registrados, si la fecha y hora no
     * son futuras o si el cliente ya tiene una reserva en ese local ese
     * día.</p>
     *
     * @throws NullPointerException si algún argumento es {@code null}
     */
    @Override
    public boolean nuevaReserva(Cliente c, Reservable r, LocalDate ld, LocalTime lt) {
        if (c == null) {
            throw new NullPointerException("Hay que indicar el cliente que hace la reserva.");
        }
        if (r == null) {
            throw new NullPointerException("Hay que indicar el local que se quiere reservar.");
        }
        if (ld == null) {
            throw new NullPointerException("Hay que indicar la fecha de la reserva.");
        }
        if (lt == null) {
            throw new NullPointerException("Hay que indicar la hora de la reserva.");
        }
        Local local = (Local) r;
        LocalDateTime fechaHora = LocalDateTime.of(ld, lt);
        Reserva reserva;
        try {
            comprobarClienteRegistrado(c);
            comprobarLocalRegistrado(local);
            comprobarFechaFutura(local, fechaHora);
            comprobarDiaSinReserva(c, local, ld);
            reserva = new Reserva(c, r, fechaHora);
            reserva.vincular();
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        reservas.add(reserva);
        operacionCorrecta();
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Las reservas se devuelven en el orden en que se hicieron. Un
     * {@code c} nulo o que no es el cliente registrado con su nick se
     * considera inexistente.</p>
     */
    @Override
    public Reserva[] obtenerReservas(Cliente c) {
        if (c == null || !esClienteRegistrado(c)) {
            return null;
        }
        return c.getReservas().toArray(new Reserva[0]);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Las reservas se devuelven en el orden en que se hicieron. Un
     * {@code r} nulo o que no es el local registrado en su dirección se
     * considera inexistente.</p>
     */
    @Override
    public Reserva[] obtenerReservas(Reservable r) {
        if (r == null) {
            return null;
        }
        Local local = (Local) r;
        if (!esLocalRegistrado(local)) {
            return null;
        }
        return local.getReservas().toArray(new Reserva[0]);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Las reservas se devuelven en el orden en que se hicieron. El
     * resultado nunca es {@code null}: si no hay reservas ese día, tiene
     * longitud 0.</p>
     *
     * @throws NullPointerException si {@code ld} es {@code null}
     */
    @Override
    public Reserva[] obtenerReservas(LocalDate ld) {
        if (ld == null) {
            throw new NullPointerException("Hay que indicar la fecha de las reservas que se buscan.");
        }
        List<Reserva> delDia = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if (esDelDia(reserva, ld)) {
                delDia.add(reserva);
            }
        }
        return delDia.toArray(new Reserva[0]);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Solo se elimina la reserva registrada. Una reserva no tiene
     * dependientes, así que la {@link #getPoliticaBorrado() política de
     * borrado} no impide la baja. Tras la baja, la reserva deja de aparecer
     * en {@link Cliente#getReservas()} y en {@link Local#getReservas()}.</p>
     *
     * <p>Devuelve {@code false}, con el motivo en {@link #getUltimoError()},
     * si la reserva no está registrada.</p>
     *
     * @throws NullPointerException si {@code r} es {@code null}
     */
    @Override
    public boolean eliminarReserva(Reserva r) {
        if (r == null) {
            throw new NullPointerException("Hay que indicar la reserva que se quiere eliminar.");
        }
        try {
            comprobarReservaRegistrada(r);
        } catch (DominioException e) {
            operacionRechazada(e);
            return false;
        }
        olvidar(r.eliminar(politicaBorrado));
        operacionCorrecta();
        return true;
    }

    private static void comprobarFechaFutura(Local local, LocalDateTime fechaHora) throws DominioException {
        if (!fechaHora.isAfter(LocalDateTime.now())) {
            throw new DominioException("No se puede reservar en \"" + local.getNombre() + "\" para el "
                    + fechaHora.toLocalDate() + " a las " + fechaHora.toLocalTime().truncatedTo(ChronoUnit.MINUTES)
                    + " porque las reservas tienen que ser para una fecha y hora futuras.");
        }
    }

    private static void comprobarDiaSinReserva(Cliente c, Local local, LocalDate dia) throws DominioException {
        for (Reserva reserva : c.getReservas()) {
            if (reserva.getReservable() == local && esDelDia(reserva, dia)) {
                throw new DominioException("\"" + c.getNick() + "\" ya tiene una reserva en \""
                        + local.getNombre() + "\" el " + dia
                        + " y no puede tener dos reservas en el mismo local el mismo día.");
            }
        }
    }

    private static boolean esDelDia(Reserva reserva, LocalDate dia) {
        return reserva.getFechaHora().toLocalDate().equals(dia);
    }

    private void comprobarReservaRegistrada(Reserva r) throws DominioException {
        if (!reservas.contains(r)) {
            throw new DominioException("La reserva " + r.getId() + " de \"" + r.getCliente().getNick()
                    + "\" para el " + r.getFechaHora().toLocalDate() + " no está registrada en el sistema.");
        }
    }

    /**
     * Lista los locales registrados de cualquier tipo ({@link Bar},
     * {@link Pub} o {@link Restaurante}) de una localidad y provincia.
     *
     * <p>La localidad y la provincia se comparan con
     * {@link Direccion#estaEn(String, String)}: sin distinguir mayúsculas y
     * minúsculas e ignorando espacios sobrantes, igual que al comprobar si
     * dos locales comparten dirección (C01). Los locales aparecen en el orden
     * en que se dieron de alta.</p>
     *
     * @param ciudad    localidad de interés
     * @param provincia provincia en la que se encuentra la localidad
     * @return los locales encontrados, nunca {@code null} y potencialmente de
     *         longitud 0
     * @throws NullPointerException si {@code ciudad} o {@code provincia} son
     *                              {@code null}
     */
    @Override
    public Local[] listarLocales(String ciudad, String provincia) {
        return listar(Local.class, ciudad, provincia, Local[]::new);
    }

    /**
     * Lista los bares registrados de una localidad y provincia, con el mismo
     * criterio de comparación y orden que
     * {@link #listarLocales(String, String)}.
     *
     * @param ciudad    localidad de interés
     * @param provincia provincia en la que se encuentra la localidad
     * @return los bares encontrados, nunca {@code null} y potencialmente de
     *         longitud 0
     * @throws NullPointerException si {@code ciudad} o {@code provincia} son
     *                              {@code null}
     */
    @Override
    public Bar[] listarBares(String ciudad, String provincia) {
        return listar(Bar.class, ciudad, provincia, Bar[]::new);
    }

    /**
     * Lista los restaurantes registrados de una localidad y provincia, con
     * el mismo criterio de comparación y orden que
     * {@link #listarLocales(String, String)}.
     *
     * @param ciudad    localidad de interés
     * @param provincia provincia en la que se encuentra la localidad
     * @return los restaurantes encontrados, nunca {@code null} y
     *         potencialmente de longitud 0
     * @throws NullPointerException si {@code ciudad} o {@code provincia} son
     *                              {@code null}
     */
    @Override
    public Restaurante[] listarRestaurantes(String ciudad, String provincia) {
        return listar(Restaurante.class, ciudad, provincia, Restaurante[]::new);
    }

    /**
     * Lista los pubs registrados de una localidad y provincia, con el mismo
     * criterio de comparación y orden que
     * {@link #listarLocales(String, String)}.
     *
     * @param ciudad    localidad de interés
     * @param provincia provincia en la que se encuentra la localidad
     * @return los pubs encontrados, nunca {@code null} y potencialmente de
     *         longitud 0
     * @throws NullPointerException si {@code ciudad} o {@code provincia} son
     *                              {@code null}
     */
    @Override
    public Pub[] listarPubs(String ciudad, String provincia) {
        return listar(Pub.class, ciudad, provincia, Pub[]::new);
    }

    private <T extends Local> T[] listar(Class<T> tipo, String ciudad, String provincia,
            IntFunction<T[]> nuevoArray) {
        if (ciudad == null) {
            throw new NullPointerException("Hay que indicar la localidad de los locales que se buscan.");
        }
        if (provincia == null) {
            throw new NullPointerException("Hay que indicar la provincia de los locales que se buscan.");
        }
        List<T> encontrados = new ArrayList<>();
        for (Local local : locales.values()) {
            if (tipo.isInstance(local) && local.getDireccion().estaEn(ciudad, provincia)) {
                encontrados.add(tipo.cast(local));
            }
        }
        return encontrados.toArray(nuevoArray);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Es la media aritmética de las estrellas de todas las reviews
     * publicadas sobre el local. Un {@code l} nulo o que no es el local
     * registrado en su dirección se considera inexistente y el resultado es
     * -1.</p>
     */
    @Override
    public float obtenerValoracionMedia(Local l) {
        if (l == null || !esLocalRegistrado(l)) {
            return -1;
        }
        return media(l.getReviews());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Es la media aritmética de las estrellas de todas las reviews de
     * todos los locales de los que es dueño actualmente, de modo que cada
     * review pesa lo mismo, sea cual sea el número de reviews de su local.
     * Un {@code p} nulo o que no es el {@link Propietario} registrado con su
     * nick se considera inexistente y el resultado es -1.</p>
     */
    @Override
    public float obtenerValoracionMedia(Propietario p) {
        if (p == null || obtenerUsuario(p.getNick()) != p) {
            return -1;
        }
        List<Review> deSusLocales = new ArrayList<>();
        for (Local local : p.getLocales()) {
            deSusLocales.addAll(local.getReviews());
        }
        return media(deSusLocales);
    }

    /**
     * Obtiene la valoración media de un local contando solo las reviews de
     * clientes cuya edad, en el momento de la valoración, estaba en el rango
     * indicado.
     *
     * <p>La edad del autor se calcula en años cumplidos en la fecha de
     * creación de la review (C08), que es cuando valoró el local; no en la
     * fecha de la visita ni en la fecha actual. Aunque el Javadoc del
     * interfaz menciona {@code null} para un local inexistente, el tipo
     * devuelto es {@code float}, así que se devuelve -1. Un {@code l} nulo o
     * que no es el local registrado en su dirección se considera
     * inexistente.</p>
     *
     * @param l         local de interés
     * @param edadEntre edad mínima del rango (incluida)
     * @param edadHasta edad máxima del rango (incluida)
     * @return la media aritmética de las estrellas de esas reviews, 0 si no
     *         hay ninguna o -1 si el local no existe
     * @throws IllegalArgumentException si {@code edadEntre} es mayor que
     *                                  {@code edadHasta}
     */
    @Override
    public float obtenerValoracionMedia(Local l, int edadEntre, int edadHasta) {
        if (edadEntre > edadHasta) {
            throw new IllegalArgumentException("El rango de edad de " + edadEntre + " a " + edadHasta
                    + " años está vacío: la edad mínima no puede ser mayor que la máxima.");
        }
        if (l == null || !esLocalRegistrado(l)) {
            return -1;
        }
        List<Review> delRango = new ArrayList<>();
        for (Review review : l.getReviews()) {
            long edad = ChronoUnit.YEARS.between(review.getCliente().getFechaNacimiento(),
                    review.getFechaCreacion());
            if (edad >= edadEntre && edad <= edadHasta) {
                delRango.add(review);
            }
        }
        return media(delRango);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Selecciona los mismos locales que
     * {@link #listarLocales(String, String)}. Los locales con la misma
     * valoración media, incluidos los que no tienen reviews, conservan el
     * orden en que se dieron de alta. El resultado nunca es {@code null}.</p>
     *
     * @throws NullPointerException si {@code ciudad} o {@code provincia} son
     *                              {@code null}
     */
    @Override
    public Local[] obtenerLocalesOrdenados(String ciudad, String provincia) {
        return ordenarPorValoracion(listarLocales(ciudad, provincia));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Incluye los locales de cualquier localidad de la provincia, que se
     * compara con {@link Direccion#estaEnProvincia(String)}: sin distinguir
     * mayúsculas y minúsculas e ignorando espacios sobrantes. Los locales con
     * la misma valoración media, incluidos los que no tienen reviews,
     * conservan el orden en que se dieron de alta. El resultado nunca es
     * {@code null}.</p>
     *
     * @throws NullPointerException si {@code provincia} es {@code null}
     */
    @Override
    public Local[] obtenerLocalesOrdenados(String provincia) {
        if (provincia == null) {
            throw new NullPointerException("Hay que indicar la provincia de los locales que se buscan.");
        }
        List<Local> encontrados = new ArrayList<>();
        for (Local local : locales.values()) {
            if (local.getDireccion().estaEnProvincia(provincia)) {
                encontrados.add(local);
            }
        }
        return ordenarPorValoracion(encontrados.toArray(new Local[0]));
    }

    /**
     * Obtiene los bares de una localidad y provincia ordenados por su
     * valoración media, de mayor a menor. Los bares sin reviews valen 0.
     *
     * <p>Selecciona los mismos bares que
     * {@link #listarBares(String, String)}. Los bares con la misma
     * valoración media conservan el orden en que se dieron de alta.</p>
     *
     * @param ciudad    localidad de interés
     * @param provincia provincia en la que se encuentra la localidad
     * @return los bares ordenados por nota descendente, nunca {@code null} y
     *         potencialmente de longitud 0
     * @throws NullPointerException si {@code ciudad} o {@code provincia} son
     *                              {@code null}
     */
    @Override
    public Bar[] obtenerBaresOrdenados(String ciudad, String provincia) {
        return ordenarPorValoracion(listarBares(ciudad, provincia));
    }

    /**
     * Obtiene los restaurantes de una localidad y provincia ordenados por su
     * valoración media, de mayor a menor. Los restaurantes sin reviews valen
     * 0.
     *
     * <p>Selecciona los mismos restaurantes que
     * {@link #listarRestaurantes(String, String)}. Los restaurantes con la
     * misma valoración media conservan el orden en que se dieron de
     * alta.</p>
     *
     * @param ciudad    localidad de interés
     * @param provincia provincia en la que se encuentra la localidad
     * @return los restaurantes ordenados por nota descendente, nunca
     *         {@code null} y potencialmente de longitud 0
     * @throws NullPointerException si {@code ciudad} o {@code provincia} son
     *                              {@code null}
     */
    @Override
    public Restaurante[] obtenerRestaurantesOrdenados(String ciudad, String provincia) {
        return ordenarPorValoracion(listarRestaurantes(ciudad, provincia));
    }

    /**
     * Obtiene los pubs de una localidad y provincia ordenados por su
     * valoración media, de mayor a menor. Los pubs sin reviews valen 0.
     *
     * <p>Selecciona los mismos pubs que {@link #listarPubs(String, String)}.
     * Los pubs con la misma valoración media conservan el orden en que se
     * dieron de alta.</p>
     *
     * @param ciudad    localidad de interés
     * @param provincia provincia en la que se encuentra la localidad
     * @return los pubs ordenados por nota descendente, nunca {@code null} y
     *         potencialmente de longitud 0
     * @throws NullPointerException si {@code ciudad} o {@code provincia} son
     *                              {@code null}
     */
    @Override
    public Pub[] obtenerPubOrdenados(String ciudad, String provincia) {
        return ordenarPorValoracion(listarPubs(ciudad, provincia));
    }

    private static float media(Collection<Review> reviews) {
        if (reviews.isEmpty()) {
            return 0;
        }
        int estrellas = 0;
        for (Review review : reviews) {
            estrellas += review.getValoracion();
        }
        return (float) estrellas / reviews.size();
    }

    private static <T extends Local> T[] ordenarPorValoracion(T[] locales) {
        Map<Local, Float> medias = new HashMap<>();
        for (Local local : locales) {
            medias.put(local, media(local.getReviews()));
        }
        Arrays.sort(locales, Comparator.comparing((T local) -> medias.get(local)).reversed());
        return locales;
    }
}
