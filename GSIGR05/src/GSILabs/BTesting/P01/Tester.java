package GSILabs.BTesting.P01;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Cliente;
import GSILabs.BModel.Contestacion;
import GSILabs.BModel.Direccion;
import GSILabs.BModel.DominioException;
import GSILabs.BModel.Local;
import GSILabs.BModel.Propietario;
import GSILabs.BModel.Pub;
import GSILabs.BModel.Restaurante;
import GSILabs.BModel.Review;
import GSILabs.BModel.Usuario;
import GSILabs.BSystem.BusinessSystem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Objects;

/**
 * Programa de prueba de la Práctica 01: crea un {@link BusinessSystem}, lo
 * puebla con usuarios, locales, reviews, contestaciones y reservas, y
 * comprueba los sucesos S1&ndash;S10 del Ejercicio 4 mostrando el
 * resultado por consola.
 *
 * <p>Cada comprobación imprime una línea con su resultado ({@code OK} o
 * {@code FALLO}), seguida del valor esperado, el obtenido y, cuando el
 * sistema rechaza una operación, el motivo que devuelve
 * {@link BusinessSystem#getUltimoError()} o el mensaje de la
 * {@link DominioException} del modelo. El Tester no reimplementa ninguna
 * regla de negocio: se limita a invocar la API del sistema y del modelo y
 * a comparar lo que devuelven con lo esperado.</p>
 *
 * <p>Las fechas se calculan a partir del día de ejecución (visitas en el
 * pasado, reservas en el futuro y edades en años cumplidos), de modo que
 * el resultado es el mismo cualquier día que se ejecute. Al terminar se
 * muestra un resumen y el proceso acaba con código de salida 0 si todas
 * las comprobaciones son correctas y 1 si alguna ha fallado.</p>
 */
public final class Tester {

    /** Localidad en la que están los locales de prueba. */
    private static final String LOCALIDAD = "Pamplona";

    /** Provincia en la que están los locales de prueba. */
    private static final String PROVINCIA = "Navarra";

    /** Contraseña común de los usuarios de prueba. */
    private static final String CONTRASENA = "secreto";

    /** Hora a la que se hacen las reservas de prueba. */
    private static final LocalTime HORA_RESERVA = LocalTime.of(21, 30);

    /** Sistema que se pone a prueba, con la política de borrado por defecto. */
    private final BusinessSystem sistema = new BusinessSystem();

    /** Día de ejecución, base de todas las fechas de prueba. */
    private final LocalDate hoy = LocalDate.now();

    /** Número de comprobaciones con el resultado esperado. */
    private int correctas;

    /** Número de comprobaciones con un resultado distinto del esperado. */
    private int fallidas;

    private Propietario ana;
    private Propietario luis;
    private Propietario marta;
    private Propietario jon;
    private Cliente pepe;
    private Cliente laura;
    private Cliente iker;
    private Bar laEstafeta;
    private Restaurante asadorMikel;
    private Pub theIrish;

    /**
     * Crea un Tester con un {@link BusinessSystem} vacío.
     */
    private Tester() {
    }

    /**
     * Ejecuta la población del sistema y las comprobaciones S1&ndash;S10, y
     * termina con código 0 si todas son correctas o 1 si alguna falla.
     *
     * @param args argumentos de la línea de comandos (no se usan)
     * @throws DominioException si algún dato de prueba de la población
     *         inicial incumple una regla del modelo, lo que indicaría un
     *         error en el propio Tester
     */
    public static void main(String[] args) throws DominioException {
        Tester tester = new Tester();
        tester.poblar();
        tester.s1();
        tester.s2();
        tester.s3();
        tester.s4();
        tester.s5();
        tester.s6();
        tester.s7();
        tester.s8();
        tester.s9();
        tester.s10();
        tester.resumen();
        System.exit(tester.fallidas == 0 ? 0 : 1);
    }

    /**
     * Da de alta los datos de partida: cuatro propietarios, tres clientes,
     * un bar, un restaurante con dos dueños y un pub, tres reviews, una
     * contestación y dos reservas. Después muestra lo registrado.
     *
     * @throws DominioException si algún dato de prueba incumple una regla
     *         del modelo
     * @throws IllegalStateException si el sistema rechaza alguna de las
     *         altas de partida
     */
    private void poblar() throws DominioException {
        titulo("Población inicial del sistema");

        ana = new Propietario("ana", CONTRASENA, hoy.minusYears(41));
        luis = new Propietario("luis", CONTRASENA, hoy.minusYears(55));
        marta = new Propietario("marta", CONTRASENA, hoy.minusYears(33));
        jon = new Propietario("jon", CONTRASENA, hoy.minusYears(28));
        pepe = new Cliente("pepe", CONTRASENA, hoy.minusYears(25));
        laura = new Cliente("laura", CONTRASENA, hoy.minusYears(19));
        iker = new Cliente("iker", CONTRASENA, hoy.minusYears(62));
        for (Usuario u : new Usuario[] {ana, luis, marta, jon, pepe, laura, iker}) {
            alta(sistema.nuevoUsuario(u), "el usuario " + u);
        }

        laEstafeta = new Bar("La Estafeta", direccion("Calle Estafeta", "12"),
                "Bar de pintxos en pleno recorrido del encierro.", ana);
        laEstafeta.añadirEspecialidad("pintxos");
        laEstafeta.añadirEspecialidad("vermú");
        asadorMikel = new Restaurante("Asador Mikel", direccion("Calle Mayor", "3"),
                "Asador tradicional navarro.", luis, new BigDecimal("24.50"), 80, 8);
        theIrish = new Pub("The Irish", direccion("Calle San Nicolás", "20"), null, jon,
                LocalTime.of(20, 0), LocalTime.of(3, 0));
        alta(sistema.nuevoLocal(laEstafeta), "el bar " + laEstafeta.getNombre());
        alta(sistema.nuevoLocal(asadorMikel), "el restaurante " + asadorMikel.getNombre());
        alta(sistema.nuevoLocal(theIrish), "el pub " + theIrish.getNombre());
        alta(sistema.asociarLocal(asadorMikel, marta), "marta como segunda dueña de " + asadorMikel.getNombre());

        Review reviewPepe = new Review(pepe, laEstafeta, 4, "Pintxos muy buenos y rápidos.", hoy.minusDays(10));
        Review reviewLaura = new Review(laura, asadorMikel, 5, "La mejor chuleta de Pamplona.", hoy.minusDays(5));
        Review reviewIker = new Review(iker, theIrish, 3, "Buen ambiente, algo ruidoso.", hoy.minusDays(2));
        alta(sistema.nuevaReview(reviewPepe), reviewPepe.toString());
        alta(sistema.nuevaReview(reviewLaura), reviewLaura.toString());
        alta(sistema.nuevaReview(reviewIker), reviewIker.toString());
        Contestacion respuestaAna = new Contestacion(ana, reviewPepe, "¡Gracias, pepe! Vuelve pronto.");
        alta(sistema.nuevaContestacion(respuestaAna, reviewPepe), respuestaAna.toString());

        alta(sistema.nuevaReserva(pepe, laEstafeta, hoy.plusDays(7), HORA_RESERVA),
                "la reserva de pepe en " + laEstafeta.getNombre());
        alta(sistema.nuevaReserva(laura, asadorMikel, hoy.plusDays(14), LocalTime.of(14, 0)),
                "la reserva de laura en " + asadorMikel.getNombre());

        System.out.println("Locales en " + LOCALIDAD + " (" + PROVINCIA + "):");
        for (Local local : sistema.listarLocales(LOCALIDAD, PROVINCIA)) {
            System.out.println("  " + local + " · valoración media: "
                    + sistema.obtenerValoracionMedia(local));
        }
        System.out.println("Reviews de " + laEstafeta.getNombre() + ": "
                + Arrays.toString(sistema.verReviews(laEstafeta)));
        System.out.println("Contestación a la review de pepe: " + sistema.obtenerContestacion(reviewPepe));
        System.out.println("Reservas de pepe: " + Arrays.toString(sistema.obtenerReservas(pepe)));
        System.out.println("Reservas de laura: " + Arrays.toString(sistema.obtenerReservas(laura)));
    }

    /**
     * S1: si se introduce un usuario, después se localiza por su
     * identificador, que es su nick (clave natural, ya que
     * {@code obtenerUsuario} busca por nick).
     *
     * @throws DominioException si el usuario de prueba incumple una regla
     *         del modelo
     */
    private void s1() throws DominioException {
        titulo("S1 · Un usuario introducido se localiza después por su ID (nick)");
        // Se da de alta un cliente nuevo, "mikel", comprobando que nuevoUsuario
        // devuelve true. Después se busca con obtenerUsuario("mikel") y se
        // comprueba que el sistema devuelve exactamente el mismo objeto que se
        // introdujo (misma referencia) y que existeNick también lo encuentra.
        Cliente mikel = new Cliente("mikel", CONTRASENA, hoy.minusYears(30));
        comprobar("Alta del cliente mikel", true, sistema.nuevoUsuario(mikel));
        Usuario encontrado = sistema.obtenerUsuario("mikel");
        comprobar("obtenerUsuario(\"mikel\") devuelve el usuario introducido", mikel, encontrado);
        comprobar("Es el mismo objeto que se introdujo", true, encontrado == mikel);
        comprobar("existeNick(\"mikel\")", true, sistema.existeNick("mikel"));
    }

    /**
     * S2: {@code obtenerUsuario} devuelve {@code null} para un usuario que
     * no existe.
     */
    private void s2() {
        titulo("S2 · obtenerUsuario de un usuario inexistente devuelve null");
        // Se busca un nick que nunca se ha dado de alta ("fantasma") y se
        // comprueba que obtenerUsuario devuelve null y existeNick devuelve false.
        comprobar("obtenerUsuario(\"fantasma\")", null, sistema.obtenerUsuario("fantasma"));
        comprobar("existeNick(\"fantasma\")", false, sistema.existeNick("fantasma"));
    }

    /**
     * S3: no se pueden introducir dos locales en la misma dirección.
     *
     * @throws DominioException si el local de prueba incumple una regla del
     *         modelo
     */
    private void s3() throws DominioException {
        titulo("S3 · No se pueden introducir dos locales en la misma dirección");
        // Se crea un pub en la dirección de "La Estafeta" (escrita en minúsculas,
        // porque las direcciones no distinguen mayúsculas) y se intenta darlo de
        // alta: nuevoLocal debe devolver false y obtenerLocal debe seguir
        // devolviendo el bar que ya estaba registrado en esa dirección.
        Direccion mismaDireccion = new Direccion("pamplona", "navarra", "calle estafeta", "12");
        Pub intruso = new Pub("Pub Intruso", mismaDireccion, null, jon, LocalTime.of(22, 0), LocalTime.of(4, 0));
        comprobarRechazo("Alta de un pub en " + mismaDireccion, sistema.nuevoLocal(intruso));
        comprobar("El local registrado en esa dirección sigue siendo La Estafeta",
                laEstafeta, sistema.obtenerLocal(mismaDireccion));
    }

    /**
     * S4: tras añadir un local y eliminarlo, se puede introducir un bar en
     * la misma dirección.
     *
     * @throws DominioException si los locales de prueba incumplen una regla
     *         del modelo
     */
    private void s4() throws DominioException {
        titulo("S4 · Tras añadir y eliminar un local, se puede introducir un bar en su dirección");
        // Se da de alta un restaurante en Calle Chapitela 5 y se elimina con
        // eliminarLocal (no tiene reviews ni reservas, así que la política
        // BLOQUEAR no lo impide). Después se da de alta un bar en esa misma
        // dirección: nuevoLocal debe devolver true y obtenerLocal debe devolver
        // el bar nuevo.
        Direccion chapitela = direccion("Calle Chapitela", "5");
        Restaurante temporal = new Restaurante("Restaurante Temporal", chapitela, null, luis,
                new BigDecimal("15"), 40, 4);
        comprobar("Alta del restaurante en " + chapitela, true, sistema.nuevoLocal(temporal));
        comprobar("Baja del restaurante", true, sistema.eliminarLocal(temporal));
        comprobar("La dirección queda libre", null, sistema.obtenerLocal(chapitela));
        Bar nuevoBar = new Bar("Bar Chapitela", chapitela, null, marta);
        comprobar("Alta de un bar en la misma dirección", true, sistema.nuevoLocal(nuevoBar));
        comprobar("El local registrado en esa dirección es el bar nuevo", nuevoBar, sistema.obtenerLocal(chapitela));
    }

    /**
     * S5: no se puede introducir un usuario menor de 14 años.
     */
    private void s5() {
        titulo("S5 · No se puede introducir un usuario menor de 14 años");
        // La edad mínima es una regla intrínseca del usuario, así que la
        // comprueba el constructor de Cliente lanzando DominioException. Se
        // intenta crear y dar de alta a "menor", que cumplió 13 años hoy: si el
        // constructor lanza la excepción, el alta no llega a producirse. Además
        // se comprueba que el sistema no conoce ese nick.
        boolean registrado;
        String motivo = null;
        try {
            Cliente menor = new Cliente("menor", CONTRASENA, hoy.minusYears(13));
            registrado = sistema.nuevoUsuario(menor);
        } catch (DominioException e) {
            registrado = false;
            motivo = e.getMessage();
        }
        comprobar("Alta de un cliente de 13 años", false, registrado, motivo);
        comprobar("existeNick(\"menor\")", false, sistema.existeNick("menor"));
    }

    /**
     * S6: no se pueden hacer reservas para un local inexistente.
     *
     * @throws DominioException si el local de prueba incumple una regla del
     *         modelo
     */
    private void s6() throws DominioException {
        titulo("S6 · No se pueden hacer reservas para un local inexistente");
        // Se crea un bar en una dirección libre pero no se da de alta en el
        // sistema. nuevaReserva de pepe para ese bar debe devolver false, y
        // obtenerReservas del bar debe devolver null (local inexistente). Las
        // reservas de pepe no deben cambiar.
        Bar fantasma = new Bar("Bar Fantasma", direccion("Calle Inexistente", "99"), null, ana);
        int reservasPrevias = sistema.obtenerReservas(pepe).length;
        comprobarRechazo("Reserva de pepe en el bar no registrado",
                sistema.nuevaReserva(pepe, fantasma, hoy.plusDays(3), HORA_RESERVA));
        comprobar("obtenerReservas del bar no registrado", null, sistema.obtenerReservas(fantasma));
        comprobar("Número de reservas de pepe", reservasPrevias, sistema.obtenerReservas(pepe).length);
    }

    /**
     * S7: no se pueden hacer reservas para un local inexistente aunque esté
     * en la misma dirección que otro que sí existe.
     *
     * @throws DominioException si el local de prueba incumple una regla del
     *         modelo
     */
    private void s7() throws DominioException {
        titulo("S7 · Tampoco si el local inexistente está en la dirección de otro existente");
        // Se crea otro bar, "Bar Impostor", en la misma dirección que "La
        // Estafeta" (registrado), pero no se da de alta. Aunque ambos locales
        // son iguales según equals (misma dirección), el sistema compara con el
        // objeto registrado, así que nuevaReserva debe devolver false y las
        // reservas de La Estafeta no deben cambiar.
        Bar impostor = new Bar("Bar Impostor", laEstafeta.getDireccion(), null, ana);
        int reservasPrevias = sistema.obtenerReservas(laEstafeta).length;
        comprobarRechazo("Reserva de laura en el bar no registrado de " + laEstafeta.getDireccion(),
                sistema.nuevaReserva(laura, impostor, hoy.plusDays(3), HORA_RESERVA));
        comprobar("Número de reservas de La Estafeta", reservasPrevias,
                sistema.obtenerReservas(laEstafeta).length);
    }

    /**
     * S8: no se pueden añadir comentarios (contestaciones) a reviews que no
     * existen.
     *
     * @throws DominioException si la review o la contestación de prueba
     *         incumplen una regla del modelo
     */
    private void s8() throws DominioException {
        titulo("S8 · No se pueden añadir contestaciones a reviews inexistentes");
        // Se crea una review de laura sobre La Estafeta sin darla de alta en el
        // sistema, y una contestación de ana (dueña del bar) a esa review. Como
        // la review no está registrada, nuevaContestacion debe devolver false y
        // tieneContestacion debe devolver false.
        Review inexistente = new Review(laura, laEstafeta, 2, "Tardaron mucho en servir.", hoy.minusDays(1));
        Contestacion respuesta = new Contestacion(ana, inexistente, "Sentimos la espera.");
        comprobarRechazo("Contestación de ana a una review no registrada",
                sistema.nuevaContestacion(respuesta, inexistente));
        comprobar("tieneContestacion de la review no registrada", false, sistema.tieneContestacion(inexistente));
    }

    /**
     * S9: no se pueden añadir cuatro dueños a un bar.
     *
     * @throws DominioException si el bar de prueba incumple una regla del
     *         modelo
     */
    private void s9() throws DominioException {
        titulo("S9 · No se pueden añadir cuatro dueños a un bar");
        // Se da de alta un bar con ana como primera dueña y se asocian con
        // asociarLocal luis y marta (segundo y tercer dueño, deben aceptarse).
        // Al asociar a jon como cuarto dueño, asociarLocal debe devolver false y
        // el bar debe seguir teniendo 3 dueños.
        Bar compartido = new Bar("Bar Compartido", direccion("Calle Zapatería", "8"), null, ana);
        comprobar("Alta del bar con ana como dueña", true, sistema.nuevoLocal(compartido));
        comprobar("Asociar a luis (2.º dueño)", true, sistema.asociarLocal(compartido, luis));
        comprobar("Asociar a marta (3.er dueño)", true, sistema.asociarLocal(compartido, marta));
        comprobarRechazo("Asociar a jon (4.º dueño)", sistema.asociarLocal(compartido, jon));
        comprobar("Número de dueños del bar", 3, compartido.getDueños().size());
    }

    /**
     * S10: no se pueden añadir dos reviews del mismo usuario, el mismo día
     * y para el mismo local.
     *
     * @throws DominioException si las reviews de prueba incumplen una regla
     *         del modelo
     */
    private void s10() throws DominioException {
        titulo("S10 · No se pueden añadir dos reviews del mismo usuario, el mismo día y local");
        // iker publica una review de La Estafeta con fecha de visita de hace 3
        // días (debe aceptarse). Luego intenta publicar otra review distinta
        // (otra valoración y comentario) del mismo local con la misma fecha de
        // visita: nuevaReview debe devolver false. Como control, una review del
        // mismo local con otra fecha de visita sí se acepta, porque C05 permite
        // varias reviews del mismo local. Al final La Estafeta tiene 3 reviews.
        LocalDate visita = hoy.minusDays(3);
        Review primera = new Review(iker, laEstafeta, 5, "Espectacular el vermú.", visita);
        Review repetida = new Review(iker, laEstafeta, 1, "Pensándolo mejor, no me gustó.", visita);
        Review otraVisita = new Review(iker, laEstafeta, 4, "Volví y sigue bien.", hoy.minusDays(1));
        comprobar("Primera review de iker del " + visita, true, sistema.nuevaReview(primera));
        comprobarRechazo("Segunda review de iker del mismo día y local", sistema.nuevaReview(repetida));
        comprobar("Review de iker de otro día (control)", true, sistema.nuevaReview(otraVisita));
        comprobar("Número de reviews de La Estafeta", 3, sistema.verReviews(laEstafeta).length);
    }

    /**
     * Muestra el número de comprobaciones correctas y fallidas.
     */
    private void resumen() {
        titulo("Resumen");
        System.out.println("Comprobaciones correctas: " + correctas);
        System.out.println("Comprobaciones fallidas:  " + fallidas);
        System.out.println(fallidas == 0 ? "RESULTADO: OK" : "RESULTADO: FALLO");
    }

    /**
     * Crea una dirección en la localidad y provincia de prueba.
     *
     * @param calle  nombre de la calle
     * @param numero número de la calle
     * @return la dirección en {@value #LOCALIDAD} ({@value #PROVINCIA})
     */
    private static Direccion direccion(String calle, String numero) {
        return new Direccion(LOCALIDAD, PROVINCIA, calle, numero);
    }

    /**
     * Comprueba que una alta de la población inicial se haya completado y
     * la muestra.
     *
     * @param hecho       resultado de la operación del sistema
     * @param descripcion qué se ha dado de alta
     * @throws IllegalStateException si el sistema ha rechazado la alta, con
     *         el motivo que da {@link BusinessSystem#getUltimoError()}
     */
    private void alta(boolean hecho, String descripcion) {
        if (!hecho) {
            throw new IllegalStateException("No se ha podido dar de alta " + descripcion
                    + " en la población inicial: " + sistema.getUltimoError());
        }
        System.out.println("  + " + descripcion);
    }

    /**
     * Comprueba que el sistema haya rechazado una operación
     * ({@code false} esperado) y muestra el motivo del rechazo.
     *
     * @param descripcion operación que se intenta
     * @param obtenido    resultado devuelto por el sistema
     */
    private void comprobarRechazo(String descripcion, boolean obtenido) {
        comprobar(descripcion, false, obtenido, obtenido ? null : sistema.getUltimoError());
    }

    /**
     * Compara el resultado obtenido con el esperado y muestra la
     * comprobación.
     *
     * @param descripcion qué se comprueba
     * @param esperado    valor esperado
     * @param obtenido    valor devuelto por el sistema
     */
    private void comprobar(String descripcion, Object esperado, Object obtenido) {
        comprobar(descripcion, esperado, obtenido, null);
    }

    /**
     * Compara el resultado obtenido con el esperado, actualiza el recuento
     * y muestra una línea con {@code OK} o {@code FALLO}, el valor esperado,
     * el obtenido y, si lo hay, el motivo del rechazo.
     *
     * @param descripcion qué se comprueba
     * @param esperado    valor esperado
     * @param obtenido    valor devuelto por el sistema
     * @param motivo      mensaje del sistema que explica un rechazo, o
     *                    {@code null} si no hay
     */
    private void comprobar(String descripcion, Object esperado, Object obtenido, String motivo) {
        boolean correcta = Objects.equals(esperado, obtenido);
        if (correcta) {
            correctas++;
        } else {
            fallidas++;
        }
        System.out.println("  [" + (correcta ? "OK" : "FALLO") + "] " + descripcion
                + " -> esperado: " + esperado + " | obtenido: " + obtenido);
        if (motivo != null) {
            System.out.println("         motivo: " + motivo);
        }
    }

    /**
     * Muestra el título de una sección de la salida.
     *
     * @param texto título de la sección
     */
    private static void titulo(String texto) {
        System.out.println();
        System.out.println("== " + texto + " ==");
    }
}
