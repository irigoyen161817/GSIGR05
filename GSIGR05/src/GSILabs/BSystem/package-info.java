/**
 * Sistema de gestión en memoria del portal de ocio de la práctica GSILabs.
 *
 * <p>Contiene los interfaces proporcionados por la asignatura,
 * {@link GSILabs.BSystem.LeisureOffice} (altas, bajas, modificaciones y
 * consultas) y {@link GSILabs.BSystem.LookupService} (valoraciones medias y
 * listados ordenados), copiados sin alterar sus firmas, y la clase
 * {@link GSILabs.BSystem.BusinessSystem}, que implementa ambos.</p>
 *
 * <p>{@link GSILabs.BSystem.BusinessSystem} guarda las entidades del
 * modelo ({@code GSILabs.BModel}) en colecciones de {@code java.util}, sin
 * persistencia, y comprueba las reglas de negocio que dependen del
 * conjunto de entidades registradas (unicidad de nick y dirección,
 * cardinalidad de dueños, una review por visita, una contestación por
 * review, existencia de los locales y reviews referenciados). El motivo de
 * cada operación rechazada se consulta con
 * {@link GSILabs.BSystem.BusinessSystem#getUltimoError()}, y la coherencia
 * al borrar se configura con {@link GSILabs.BModel.PoliticaBorrado}.</p>
 */
package GSILabs.BSystem;
