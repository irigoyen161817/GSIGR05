/**
 * Modelo de negocio en memoria del portal de ocio de la práctica GSILabs.
 *
 * <p>Este paquete contiene únicamente las entidades del dominio y las
 * reglas de negocio que las relacionan (C01&ndash;C09); no gestiona el
 * almacenamiento ni conoce el conjunto completo de instancias del sistema
 * (usuarios, locales, etc.), responsabilidad de
 * {@code GSILabs.BSystem.BusinessSystem}, que es quien crea y da de alta
 * las entidades apoyándose en este modelo.</p>
 *
 * <h2>Usuarios</h2>
 *
 * <p>{@link GSILabs.BModel.Usuario} es la clase base abstracta de los
 * usuarios del sistema, con nick, contraseña y fecha de nacimiento (edad
 * mínima C03); se especializa en dos perfiles disjuntos, modelados como
 * subtipos (C04, C06): {@link GSILabs.BModel.Dueño}, propietario de
 * {@link GSILabs.BModel.Local locales}, y {@link GSILabs.BModel.Cliente},
 * que publica {@link GSILabs.BModel.Review reviews} y hace
 * {@link GSILabs.BModel.Reserva reservas}. La igualdad entre usuarios se
 * basa únicamente en el nick, con independencia del perfil.</p>
 *
 * <h2>Locales</h2>
 *
 * <p>{@link GSILabs.BModel.Local} es la clase base abstracta de los
 * locales de ocio, con nombre, {@link GSILabs.BModel.Dirección} y
 * descripción opcional, y entre 1 y 3 {@link GSILabs.BModel.Dueño dueños}
 * (C06); la igualdad entre locales se basa únicamente en la dirección
 * (C01), con independencia del tipo concreto. Los tres tipos concretos
 * son {@link GSILabs.BModel.Bar}, {@link GSILabs.BModel.Pub} y
 * {@link GSILabs.BModel.Restaurante} (C02). De ellos, solo
 * {@link GSILabs.BModel.Bar} y {@link GSILabs.BModel.Restaurante}
 * implementan la interfaz sellada {@link GSILabs.BModel.Reservable}
 * ({@code permits Bar, Restaurante}), lo que garantiza en tiempo de
 * compilación que solo se pueden registrar
 * {@link GSILabs.BModel.Reserva reservas} sobre esos dos tipos, y nunca
 * sobre un {@link GSILabs.BModel.Pub} (C09).</p>
 *
 * <h2>Reviews, contestaciones y reservas</h2>
 *
 * <p>Un {@link GSILabs.BModel.Cliente} puede publicar como máximo una
 * {@link GSILabs.BModel.Review} por visita (mismo local y fecha de
 * visita, C05); el {@link GSILabs.BModel.Dueño} del local reseñado puede
 * responderla con, como mucho, una {@link GSILabs.BModel.Contestación}
 * (C07). Ambas llevan una fecha de creación asignada automáticamente
 * (C08). Un {@link GSILabs.BModel.Cliente} también puede hacer
 * {@link GSILabs.BModel.Reserva reservas} sobre cualquier local
 * {@link GSILabs.BModel.Reservable} (C09).</p>
 *
 * <h2>Ciclo de vida: vincular() y desvincular()</h2>
 *
 * <p>Las entidades que participan en relaciones bidireccionales
 * ({@link GSILabs.BModel.Local}, {@link GSILabs.BModel.Review},
 * {@link GSILabs.BModel.Contestación} y {@link GSILabs.BModel.Reserva})
 * separan su construcción de su enlace con el resto del modelo: el
 * constructor únicamente valida los datos, sin modificar ninguna otra
 * entidad, de forma que {@code this} nunca escapa del constructor antes
 * de terminar de construirse. El enlace efectivo (sincronizar las
 * colecciones de ambos lados de la relación, por ejemplo
 * {@link GSILabs.BModel.Dueño#getLocales()} y
 * {@link GSILabs.BModel.Local#getDueños()}) es una operación explícita y
 * posterior, mediante los métodos {@code vincular()} y
 * {@code desvincular()} de cada clase, que además comprueban en ese
 * momento las reglas de negocio que dependen del estado del resto del
 * sistema (por ejemplo, que el local de una review esté dado de alta).
 * Esto es responsabilidad de quien da de alta o de baja la entidad,
 * típicamente {@code GSILabs.BSystem.BusinessSystem}. Así, una entidad
 * rechazada por el sistema antes de completar su alta no deja rastro en
 * el resto del modelo.</p>
 *
 * <h2>Borrado coherente: PoliticaBorrado</h2>
 *
 * <p>Cada entidad con dependientes (un {@link GSILabs.BModel.Local}
 * depende de sus reviews y reservas; un {@link GSILabs.BModel.Cliente},
 * de sus reviews y reservas; un {@link GSILabs.BModel.Dueño}, de sus
 * contestaciones y de los locales de los que es único propietario; y una
 * {@link GSILabs.BModel.Review}, de su contestación) ofrece un método
 * público {@code eliminar(}{@link GSILabs.BModel.PoliticaBorrado}{@code )}
 * que aplica esa política de forma configurable: con
 * {@link GSILabs.BModel.PoliticaBorrado#BLOQUEAR} se rechaza el borrado
 * con una {@link GSILabs.BModel.DominioException} si hay
 * algún dependiente, sin modificar nada; con
 * {@link GSILabs.BModel.PoliticaBorrado#CASCADA} se eliminan también,
 * recursivamente, los dependientes. En ambos casos se devuelve el
 * conjunto, en orden de eliminación, de todas las entidades eliminadas,
 * para que quien gestiona el almacenamiento actualice sus colecciones.
 * Las entidades sin dependientes propios ({@link GSILabs.BModel.Reserva}
 * y {@link GSILabs.BModel.Contestación}) también exponen
 * {@code eliminar(PoliticaBorrado)} por uniformidad, aunque la política
 * no cambia su resultado.</p>
 *
 * <h2>Excepciones</h2>
 *
 * <p>La única excepción propia del modelo es
 * {@link GSILabs.BModel.DominioException} (comprobada). Se lanza solo
 * cuando se incumple una regla de negocio C01&ndash;C09, por ejemplo un
 * usuario menor de 14 años, un cuarto dueño en un local, dos reviews de
 * la misma visita o una reserva en un local que no está dado de alta. Su
 * mensaje explica en lenguaje natural qué regla se ha incumplido, con los
 * datos concretos del caso.</p>
 *
 * <p>El resto de errores, que no son de negocio sino de uso incorrecto
 * del modelo, se señalan con las excepciones estándar de Java:
 * {@link java.lang.NullPointerException} para argumentos obligatorios
 * nulos, {@link java.lang.IllegalArgumentException} para argumentos mal
 * formados (por ejemplo, un texto en blanco) e
 * {@link java.lang.IllegalStateException} para operaciones sobre un
 * objeto en un estado no válido (por ejemplo, vincular dos veces la misma
 * entidad).</p>
 *
 * <h2>Identidad</h2>
 *
 * <p>Cada entidad se identifica por su clave natural: un
 * {@link GSILabs.BModel.Usuario} por su nick, un
 * {@link GSILabs.BModel.Local} por su dirección, una
 * {@link GSILabs.BModel.Review} por su cliente, local y fecha de visita, y
 * una {@link GSILabs.BModel.Contestación} por la review que contesta. Solo
 * {@link GSILabs.BModel.Reserva}, que no tiene clave natural, genera un
 * identificador numérico propio.</p>
 */
package GSILabs.BModel;
