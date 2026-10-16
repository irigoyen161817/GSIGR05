package GSILabs.Misc;

import GSILabs.BModel.Bar;
import GSILabs.BModel.Propietario;
import GSILabs.BSystem.BusinessSystem;
import java.io.File;
import java.util.Set;

/**
 * Prueba {@link BusinessSystem#importaBares(File)} con el fichero de ejemplo
 * del guion, {@code datos/P02Ej05.ods}, incluido en el proyecto.
 * <p>
 * Importa el fichero en un sistema vacío, muestra cuántos bares se han
 * incorporado y el motivo del último descartado, y comprueba que el bar de
 * la primera fila ("Rincon las palmeras", en Travesia Cervantes 174, Mataro,
 * Barcelona, de Carlos Perez y especializado en calamares) está en el
 * sistema con sus datos. El fichero tiene 3500 bares, de los que 10
 * repiten la dirección de otro anterior y se descartan.
 */
public final class SSTest05 {

    private static final int BARES_ESPERADOS = 3490;

    private SSTest05() {
    }

    /**
     * Importa {@code datos/P02Ej05.ods}, relativo al directorio de trabajo,
     * y muestra por consola cada comprobación con el valor esperado, el
     * obtenido y {@code OK} o {@code FALLO}. Termina con código 1 si alguna
     * comprobación falla.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        BusinessSystem sistema = new BusinessSystem();
        int importados = sistema.importaBares(new File("datos", "P02Ej05.ods"));
        System.out.println("Último bar descartado: " + sistema.getUltimoError());

        boolean correcto = comprobar("Bares importados", BARES_ESPERADOS, importados);

        Bar encontrado = null;
        for (Bar bar : sistema.listarBares("Mataro", "Barcelona")) {
            if (bar.getNombre().equals("Rincon las palmeras")) {
                encontrado = bar;
            }
        }
        correcto &= comprobar("Rincon las palmeras está en Mataro", true, encontrado != null);
        if (encontrado != null) {
            correcto &= comprobar("Calle", "Travesia Cervantes", encontrado.getDireccion().getCalle());
            correcto &= comprobar("Número", "174", encontrado.getDireccion().getNumero());
            Set<Propietario> dueños = encontrado.getDueños();
            correcto &= comprobar("Dueño", "Carlos Perez",
                    dueños.isEmpty() ? null : dueños.iterator().next().getNick());
            correcto &= comprobar("Especialidades", Set.of("calamares"), encontrado.getEspecialidades());
        }

        if (!correcto) {
            System.exit(1);
        }
    }

    private static boolean comprobar(String descripcion, Object esperado, Object obtenido) {
        boolean ok = esperado.equals(obtenido);
        System.out.println((ok ? "OK    " : "FALLO ") + descripcion + ": esperado " + esperado
                + ", obtenido " + obtenido);
        return ok;
    }
}
