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
import GSILabs.BSystem.BusinessSystem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Tester de la Práctica 01. Carga unos datos de partida en un
 * {@link BusinessSystem} y comprueba los sucesos S1&ndash;S10 del
 * Ejercicio 4, mostrando por consola si cada comprobación es correcta.
 *
 * <p>Las fechas se calculan a partir del día en que se ejecuta, así que el
 * resultado no depende de la fecha.</p>
 */
public final class Tester {

    private final BusinessSystem sistema = new BusinessSystem();
    private final LocalDate hoy = LocalDate.now();

    private int correctas = 0;
    private int fallidas = 0;

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

    private Tester() {
    }

    /**
     * Carga los datos de partida, comprueba los sucesos S1&ndash;S10 y
     * muestra el resumen. Termina con código 0 si todo es correcto y 1 si
     * alguna comprobación falla.
     *
     * @param args no se usan
     * @throws DominioException si algún dato de partida incumple una regla
     *         del modelo
     */
    public static void main(String[] args) throws DominioException {
        Tester tester = new Tester();

        tester.cargarDatos();

        tester.s1UsuarioSeLocalizaPorSuId();
        tester.s2UsuarioInexistenteDevuelveNull();
        tester.s3DosLocalesNoCompartenDireccion();
        tester.s4DireccionLiberadaAdmiteUnBar();
        tester.s5MenorDeEdadNoSeRegistra();
        tester.s6NoSeReservaEnLocalInexistente();
        tester.s7NoSeReservaEnLocalInexistenteConDireccionOcupada();
        tester.s8NoSeContestaUnaReviewInexistente();
        tester.s9BarNoAdmiteCuatroDueños();
        tester.s10NoHayDosReviewsDeLaMismaVisita();

        System.out.println();
        System.out.println("Correctas: " + tester.correctas + " | Fallidas: " + tester.fallidas);
        System.exit(tester.fallidas == 0 ? 0 : 1);
    }

    private void cargarDatos() throws DominioException {
        ana = new Propietario("ana", "secreto", hoy.minusYears(41));
        luis = new Propietario("luis", "secreto", hoy.minusYears(55));
        marta = new Propietario("marta", "secreto", hoy.minusYears(33));
        jon = new Propietario("jon", "secreto", hoy.minusYears(28));

        pepe = new Cliente("pepe", "secreto", hoy.minusYears(25));
        laura = new Cliente("laura", "secreto", hoy.minusYears(19));
        iker = new Cliente("iker", "secreto", hoy.minusYears(62));

        sistema.nuevoUsuario(ana);
        sistema.nuevoUsuario(luis);
        sistema.nuevoUsuario(marta);
        sistema.nuevoUsuario(jon);
        sistema.nuevoUsuario(pepe);
        sistema.nuevoUsuario(laura);
        sistema.nuevoUsuario(iker);

        laEstafeta = new Bar("La Estafeta", new Direccion("Pamplona", "Navarra", "Calle Estafeta", "12"),
                "Bar de pintxos.", ana);
        asadorMikel = new Restaurante("Asador Mikel", new Direccion("Pamplona", "Navarra", "Calle Mayor", "3"),
                "Asador tradicional.", luis, new BigDecimal("24.50"), 80, 8);
        theIrish = new Pub("The Irish", new Direccion("Pamplona", "Navarra", "Calle San Nicolás", "20"),
                null, jon, LocalTime.of(20, 0), LocalTime.of(3, 0));

        sistema.nuevoLocal(laEstafeta);
        sistema.nuevoLocal(asadorMikel);
        sistema.nuevoLocal(theIrish);
        sistema.asociarLocal(asadorMikel, marta);

        Review reviewPepe = new Review(pepe, laEstafeta, 4, "Pintxos muy buenos.", hoy.minusDays(10));
        Review reviewLaura = new Review(laura, asadorMikel, 5, "La mejor chuleta de Pamplona.", hoy.minusDays(5));
        sistema.nuevaReview(reviewPepe);
        sistema.nuevaReview(reviewLaura);
        sistema.nuevaContestacion(new Contestacion(ana, reviewPepe, "¡Gracias! Vuelve pronto."), reviewPepe);

        sistema.nuevaReserva(pepe, laEstafeta, hoy.plusDays(7), LocalTime.of(21, 30));
        sistema.nuevaReserva(laura, asadorMikel, hoy.plusDays(14), LocalTime.of(14, 0));

        System.out.println("Locales de partida:");
        for (Local local : sistema.listarLocales("Pamplona", "Navarra")) {
            System.out.println("  " + local);
        }
    }

    // Se registra un Cliente en BusinessSystem y obtenerUsuario, buscando por su nick,
    // devuelve ese mismo Cliente.
    private void s1UsuarioSeLocalizaPorSuId() throws DominioException {
        System.out.println("\nS1 - Un usuario introducido se localiza por su ID");
        Cliente mikel = new Cliente("mikel", "secreto", hoy.minusYears(30));

        comprobar("Se registra a mikel", true, sistema.nuevoUsuario(mikel));
        comprobar("obtenerUsuario(\"mikel\") devuelve a mikel", mikel, sistema.obtenerUsuario("mikel"));
    }

    // obtenerUsuario con un nick que no está registrado en BusinessSystem devuelve null.
    private void s2UsuarioInexistenteDevuelveNull() {
        System.out.println("\nS2 - obtenerUsuario de un usuario que no existe devuelve null");

        comprobar("obtenerUsuario(\"fantasma\") devuelve null", null, sistema.obtenerUsuario("fantasma"));
    }

    // BusinessSystem rechaza un Pub con la misma Direccion que un Bar ya registrado
    // (aunque esté escrita en minúsculas), y el Bar sigue ocupando esa Direccion.
    private void s3DosLocalesNoCompartenDireccion() throws DominioException {
        System.out.println("\nS3 - No se pueden introducir dos locales en la misma dirección");
        Direccion mismaDireccion = new Direccion("pamplona", "navarra", "calle estafeta", "12");
        Pub pub = new Pub("Pub Intruso", mismaDireccion, null, jon, LocalTime.of(22, 0), LocalTime.of(4, 0));

        comprobar("Se rechaza el pub en la dirección de La Estafeta", false, sistema.nuevoLocal(pub));
        System.out.println("    motivo: " + sistema.getUltimoError());
        comprobar("La Estafeta sigue en esa dirección", laEstafeta, sistema.obtenerLocal(mismaDireccion));
    }

    // Se registra un Restaurante y se elimina; su Direccion queda libre y BusinessSystem
    // acepta un Bar en ella.
    private void s4DireccionLiberadaAdmiteUnBar() throws DominioException {
        System.out.println("\nS4 - Tras añadir y eliminar un local, se puede introducir un bar en su dirección");
        Direccion chapitela = new Direccion("Pamplona", "Navarra", "Calle Chapitela", "5");
        Restaurante restaurante = new Restaurante("Restaurante Temporal", chapitela, null, luis,
                new BigDecimal("15"), 40, 4);
        Bar bar = new Bar("Bar Chapitela", chapitela, null, marta);

        comprobar("Se registra el restaurante", true, sistema.nuevoLocal(restaurante));
        comprobar("Se elimina el restaurante", true, sistema.eliminarLocal(restaurante));
        comprobar("Se registra el bar en la misma dirección", true, sistema.nuevoLocal(bar));
    }

    // El constructor de Cliente lanza DominioException si el Usuario tiene menos de 14 años,
    // así que nunca llega a registrarse en BusinessSystem.
    private void s5MenorDeEdadNoSeRegistra() {
        System.out.println("\nS5 - No se puede introducir un usuario menor de edad");
        boolean registrado = false;
        String motivo = null;

        try {
            registrado = sistema.nuevoUsuario(new Cliente("menor", "secreto", hoy.minusYears(13)));
        } catch (DominioException e) {
            motivo = e.getMessage();
        }

        comprobar("No se registra un cliente de 13 años", false, registrado);
        System.out.println("    motivo: " + motivo);
    }

    // BusinessSystem rechaza una Reserva para un Bar que no está registrado.
    private void s6NoSeReservaEnLocalInexistente() throws DominioException {
        System.out.println("\nS6 - No se pueden hacer reservas para un local inexistente");
        Bar barNoRegistrado = new Bar("Bar Fantasma", new Direccion("Pamplona", "Navarra", "Calle Inexistente", "99"),
                null, ana);

        comprobar("Se rechaza la reserva", false,
                sistema.nuevaReserva(pepe, barNoRegistrado, hoy.plusDays(3), LocalTime.of(21, 30)));
        System.out.println("    motivo: " + sistema.getUltimoError());
    }

    // Un Bar no registrado con la misma Direccion que otro registrado es equals a él, pero
    // BusinessSystem rechaza la Reserva porque no es el Local registrado.
    private void s7NoSeReservaEnLocalInexistenteConDireccionOcupada() throws DominioException {
        System.out.println("\nS7 - Tampoco si el local inexistente está en la dirección de otro existente");
        Bar barNoRegistrado = new Bar("Bar Impostor", laEstafeta.getDireccion(), null, ana);

        comprobar("Se rechaza la reserva", false,
                sistema.nuevaReserva(laura, barNoRegistrado, hoy.plusDays(3), LocalTime.of(21, 30)));
        System.out.println("    motivo: " + sistema.getUltimoError());
    }

    // BusinessSystem rechaza la Contestacion de un Propietario a una Review que no está registrada.
    private void s8NoSeContestaUnaReviewInexistente() throws DominioException {
        System.out.println("\nS8 - No se pueden añadir contestaciones a reviews que no existen");
        Review reviewNoRegistrada = new Review(laura, laEstafeta, 2, "Tardaron mucho.", hoy.minusDays(1));
        Contestacion contestacion = new Contestacion(ana, reviewNoRegistrada, "Sentimos la espera.");

        comprobar("Se rechaza la contestación", false, sistema.nuevaContestacion(contestacion, reviewNoRegistrada));
        System.out.println("    motivo: " + sistema.getUltimoError());
    }

    // BusinessSystem acepta hasta tres Propietario en un Bar y rechaza el cuarto.
    private void s9BarNoAdmiteCuatroDueños() throws DominioException {
        System.out.println("\nS9 - No se pueden añadir cuatro dueños a un bar");
        Bar bar = new Bar("Bar Compartido", new Direccion("Pamplona", "Navarra", "Calle Zapatería", "8"), null, ana);
        sistema.nuevoLocal(bar);

        comprobar("Se añade el 2.º dueño", true, sistema.asociarLocal(bar, luis));
        comprobar("Se añade el 3.er dueño", true, sistema.asociarLocal(bar, marta));
        comprobar("Se rechaza el 4.º dueño", false, sistema.asociarLocal(bar, jon));
        System.out.println("    motivo: " + sistema.getUltimoError());
    }

    // BusinessSystem rechaza una segunda Review del mismo Cliente, Local y fecha de visita;
    // con otra fecha de visita sí la acepta.
    private void s10NoHayDosReviewsDeLaMismaVisita() throws DominioException {
        System.out.println("\nS10 - No se pueden añadir dos reviews del mismo usuario, el mismo día y local");
        LocalDate visita = hoy.minusDays(3);
        Review primera = new Review(iker, laEstafeta, 5, "Espectacular.", visita);
        Review mismaVisita = new Review(iker, laEstafeta, 1, "Pensándolo mejor, no me gustó.", visita);
        Review otraVisita = new Review(iker, laEstafeta, 4, "Volví y sigue bien.", hoy.minusDays(1));

        comprobar("Se registra la primera review", true, sistema.nuevaReview(primera));
        comprobar("Se rechaza otra review del mismo día", false, sistema.nuevaReview(mismaVisita));
        System.out.println("    motivo: " + sistema.getUltimoError());
        comprobar("Se registra una review de otro día", true, sistema.nuevaReview(otraVisita));
    }

    private void comprobar(String descripcion, Object esperado, Object obtenido) {
        if (Objects.equals(esperado, obtenido)) {
            correctas++;
            System.out.println("  [OK]    " + descripcion);
        } else {
            fallidas++;
            System.out.println("  [FALLO] " + descripcion
                    + " (esperado: " + esperado + ", obtenido: " + obtenido + ")");
        }
    }
}
