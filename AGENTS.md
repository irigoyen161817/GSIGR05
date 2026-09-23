# AGENTS.md — GSIGR05 · Práctica 01 (GSI, UPNA 2026/2027)

Instrucciones para agentes de código que trabajen en este repositorio. La fuente de verdad es `Práctica_1.pdf` (guion oficial); `practica1.md` es una transcripción de apoyo. Si hay conflicto, manda el PDF y, por encima de él, el anexo de dudas publicado en MiAulario.

## 1. Entorno

- Proyecto **NetBeans Java SE** (Ant). El proyecto real está en el subdirectorio `GSIGR05/`; la raíz del repo solo contiene Git, documentación y el PDF.
  - Fuentes: `GSIGR05/src` · Tests JUnit (opcional): `GSIGR05/test` · Metadatos: `GSIGR05/nbproject`.
  - Salidas generadas (no editar ni commitear): `GSIGR05/build`, `GSIGR05/dist`, `GSIGR05/dist/javadoc`, `GSIGR05/nbproject/private/`.
- Java: `javac.source`/`javac.target` = **23** (`GSIGR05/nbproject/project.properties`). No usar APIs posteriores.
- Codificación **UTF-8** en todos los fuentes (nombres y Javadoc en español: `Dueño`, `Dirección`, `Contestación`...).
- No modificar `GSIGR05/nbproject/build-impl.xml` (generado por NetBeans). Los cambios de build van en `GSIGR05/build.xml` o `project.properties`.
- `main.class` actual: `gsigr05.GSIGR05` (esqueleto generado). Para ejecutar el Tester, cambiarlo a `GSILabs.BTesting.P01.Tester` o ejecutarlo con `run-single`.

Comandos (desde la raíz del repo):

```bash
ant -f GSIGR05/build.xml clean jar        # compilar y empaquetar
ant -f GSIGR05/build.xml run              # ejecutar main.class
ant -f GSIGR05/build.xml javadoc          # generar Javadoc en GSIGR05/dist/javadoc
ant -f GSIGR05/build.xml test             # tests JUnit, si existen
```

Verificar siempre que `jar` y `javadoc` terminan sin errores (y sin warnings de Javadoc evitables) antes de dar un cambio por terminado.

## 2. Estructura obligatoria (criterio e1.1)

| Paquete | Contenido |
| :--- | :--- |
| `GSILabs.BModel` | Modelo de negocio (diccionario del sistema): entidades, reglas intrínsecas, excepciones. |
| `GSILabs.BSystem` | `LeisureOffice`, `LookupService` (proporcionadas en MiAulario) y `BusinessSystem` (almacenamiento en memoria y operaciones). |
| `GSILabs.BTesting.P01` | `Tester` ejecutable (`main`) con los sucesos S1–S10. |

- Respetar exactamente estos nombres (con mayúsculas). No poner código de la práctica en el paquete `gsigr05`.
- Nombre del proyecto: `GSIGR05` (grupo 05). No renombrar.

## 3. Requisitos de negocio (C01–C09)

Almacenamiento **no persistente** (en memoria). Operaciones: altas, bajas, modificaciones, consultas y comprobación de consistencia.

- **C01 Local**: nombre, dirección (Localidad, Provincia, Calle, Número) y descripción opcional **≤ 300 caracteres**. **No puede haber dos locales en la misma dirección.**
- **C02 Tipos de local**: `Restaurante` (precio estimado de menú, capacidad máxima total y por mesa), `Bar` (especialidades como *tags*), `Pub` (hora de apertura y de cierre).
- **C03 Usuario**: `nick` **único** y de **≥ 3 caracteres**, contraseña, fecha de nacimiento. **Edad mínima 14 años.** Perfil obligatorio: Propietario o Cliente.
- **C04 Cliente**: consulta locales y escribe reviews.
- **C05 Review**: de un Cliente sobre un Local; valoración **0–5 estrellas**, comentario **≤ 500 caracteres**, fecha de visita. Un cliente puede hacer varias reviews del mismo local, pero **no dos de la misma visita** (mismo cliente + local + fecha de visita).
- **C06 Propietario/Dueño**: un Local tiene **entre 1 y 3 propietarios**; un propietario puede tener **0..N** locales.
- **C07 Contestación**: solo la puede hacer un **propietario del local** de la review; **máximo una contestación por review**; comentario **≤ 500 caracteres**.
- **C08**: el sistema registra automáticamente la **fecha de creación** de Reviews y Contestaciones.
- **C09 Reservable**: `Restaurante` y `Bar` implementan `Reservable` (los `Pub` **no**). Una `Reserva` es de un Cliente, con fecha y hora y un porcentaje de descuento opcional. No se puede reservar en locales inexistentes.

Clases sugeridas por la Figura 1 (diagrama raster del PDF; no se puede leer como texto — confirmar con el PDF ante dudas de nombres): `Usuario`, `Cliente`, `Dueño`/`Propietario`, `Local`, `Direccion`, `Bar`, `Pub`, `Restaurante`, `Review`, `Contestacion`, `Reserva`, interfaz `Reservable`. Relaciones: `Cliente` y `Dueño` heredan de `Usuario`; `Bar`, `Pub`, `Restaurante` heredan de `Local`; `Bar` y `Restaurante` implementan `Reservable`; `Local` compone una `Direccion`. Se permiten clases/interfaces auxiliares adicionales (p. ej. excepciones, enums de perfil).

Ambigüedades en los requisitos: consultar el anexo de preguntas de MiAulario. Si no está disponible, elegir la interpretación más restrictiva/coherente, documentarla en el Javadoc y avisar al usuario.

## 4. Ejercicios

1. **Proyecto** `GSIGR05` con paquete `GSILabs.BModel` — hecho.
2. **Modelo** (`GSILabs.BModel`):
   - Identidad por **clave natural** siempre que exista: `Usuario` → nick; `Local` → dirección; `Review` → cliente + local + fecha de visita; `Contestacion` → review contestada. Solo si una clase no tiene clave natural (previsiblemente `Reserva`) o `LeisureOffice`/`LookupService` exigen IDs numéricos, generar el ID en la propia clase con un contador estático (`private static final AtomicInteger CONTADOR = new AtomicInteger();` e `id = CONTADOR.incrementAndGet();` en el constructor). No crear clases utilitarias de generación de IDs.
   - Errores mediante excepciones documentadas (`@throws`), no `System.out` ni valores mágicos. Única excepción propia: `DominioException` (ver sección 6.1).
   - Sobrescribir `toString()`, `equals()` **y** `hashCode()` en **todas** las clases instanciables, de forma coherente con la identidad de cada entidad (la clave natural o, si no la hay, el id generado).
   - Encapsulación: atributos `private`, colecciones devueltas como copias o vistas inmodificables.
3. **Sistema** (`GSILabs.BSystem`):
   - `LeisureOffice` y `LookupService` se **descargan de MiAulario**; deben copiarse sin alterar sus firmas. **No inventar sus métodos**: si no están en el repo, pedir al usuario que los añada antes de implementar `BusinessSystem`.
   - `BusinessSystem implements LeisureOffice, LookupService` y gestiona el almacenamiento en memoria (colecciones de `java.util`) y las reglas que dependen del conjunto (unicidad de dirección y nick, 1–3 dueños, review duplicada, contestación única, existencia del local/review).
   - **Coherencia referencial** al borrar: mecanismo **configurable** de bloqueo (impedir borrar lo referenciado) o **borrado en cascada** (p. ej. borrar un Local elimina sus reviews, contestaciones y reservas). Hacerlo explícito (flag/política) y documentado.
4. **Tester** (`GSILabs.BTesting.P01.Tester`, con `main`): crea un `BusinessSystem`, lo puebla con locales, usuarios, reviews, reservas, etc., y comprueba mostrando por consola:

| Suceso | Comprobación |
| :--- | :--- |
| S1 | Un usuario introducido se localiza luego por su ID. |
| S2 | `ObtenerUsuario` de un usuario inexistente devuelve `null`. |
| S3 | No se pueden introducir dos locales en la misma dirección. |
| S4 | Tras añadir y eliminar un local, se puede introducir un Bar en esa dirección. |
| S5 | No se puede introducir un usuario menor de 14 años. |
| S6 | No se pueden hacer reservas para un local inexistente. |
| S7 | Ídem, aunque el local inexistente esté en la misma dirección que otro existente. |
| S8 | No se pueden añadir comentarios/contestaciones a Reviews inexistentes. |
| S9 | No se pueden añadir cuatro dueños a un bar. |
| S10 | No se pueden añadir dos Reviews del mismo usuario, el mismo día, para el mismo local. |

   - Cada suceso debe estar **comentado en el código** explicando cómo se comprueba.
   - Salida clara por línea: esperado vs. obtenido y `OK`/`FALLO`. El Tester no reimplementa reglas de negocio; solo ejercita el sistema.

## 5. Criterios de corrección (12 puntos)

Cada criterio: fallido (0 %), incompleto (25/50/75 %) o completo (100 %).

| Criterio | Pts | Qué se evalúa |
| :--- | :---: | :--- |
| e1.1 | 1 | Nombrado correcto de proyecto y paquetes. |
| e1.2 | 3 | `BModel` completo: (a) control de errores con excepciones documentadas y controladas; (b) `toString()`, `equals()`... sobrescritos en todas las clases; (c) medidas configurables de bloqueo o eliminación en cascada para la coherencia. |
| e1.3 | 4 | `BSystem` funcional según C01–C09 y `LeisureOffice`; (a) `BusinessSystem` implementa también `LookupService`. |
| e1.4 | 1 | Javadoc completo y significativo; (a) código comentado apropiadamente. |
| e1.5 | 3 | `Tester` comprueba S1–S10 mostrando resultados por consola. |

Antes de cerrar cualquier tarea, revisar el cambio contra esta tabla.

## 6. Convenciones de código

- Javadoc en español en **todas** las clases, interfaces, constructores y métodos públicos/protegidos: descripción, restricciones de dominio, `@param`, `@return`, `@throws`. Mantener `package-info.java` en cada paquete.
- Separación estricta de capas: el modelo no conoce `BusinessSystem`; el Tester solo usa la API del sistema.
- Invariantes intrínsecas (longitudes, rangos, edad) en el modelo; invariantes de conjunto (unicidad, cardinalidades, existencia) en `BusinessSystem`.
- Solo biblioteca estándar de Java; sin dependencias externas.
- Fechas con `java.time` (`LocalDate`, `LocalTime`, `LocalDateTime`).

### 6.1 Excepciones

- La **única excepción propia** del proyecto es `GSILabs.BModel.DominioException` (comprobada, extiende `Exception`). No crear más excepciones personalizadas ni jerarquías de excepciones.
- Se lanza **solo cuando se incumple una regla de negocio** C01–C09: dirección repetida, nick repetido o de menos de 3 caracteres, usuario menor de 14 años, descripción de más de 300 caracteres, comentario de más de 500, valoración fuera de 0–5, local sin dueños o con más de 3, review duplicada de la misma visita, segunda contestación, contestación de alguien que no es dueño del local, reserva en un Pub o en un local inexistente, etc.
- El resto de errores usa las **excepciones estándar de Java**, importándolas si hace falta (`java.util.NoSuchElementException`, etc.):
  - `NullPointerException` para argumentos nulos (`Objects.requireNonNull(x, "...")`).
  - `IllegalArgumentException` para argumentos mal formados que no son una regla de negocio.
  - `IllegalStateException` para usar un objeto en un estado no válido.
- **El mensaje explica la regla incumplida en lenguaje natural**, como se lo diría una persona al usuario: qué ha pasado y por qué no se permite, con los datos concretos del caso. Nada de códigos, nombres de variables ni mensajes genéricos tipo "Error de validación".

```java
// Bien
throw new DominioException("No se puede registrar el local \"" + nombre
        + "\" porque ya existe otro local en " + direccion + ".");
throw new DominioException("El usuario \"" + nick + "\" tiene " + edad
        + " años y no se permite registrar usuarios menores de 14.");
throw new DominioException("El local \"" + local.getNombre()
        + "\" ya tiene 3 dueños, que es el máximo permitido.");

// Mal
throw new DominioException("C06 violated");
throw new DominioException("Error: edad inválida");
```

- Documentar cada `DominioException` con `@throws DominioException si ...`, describiendo la regla en lenguaje natural.
- Quien captura la excepción (p. ej. el `Tester`) muestra `e.getMessage()` tal cual, así que el mensaje debe entenderse por sí solo.

## 7. Entrega

- 30 de septiembre (entrega y discusión). Sesiones de laboratorio: 9, 16 y 23 de septiembre.
- Proyecto NetBeans completo comprimido en `.zip` o `.7z`, subido a la Tarea de MiAulario (lo hace el alumno, no el agente).
- Penalización: **−3 puntos por cada día o fracción** de retraso.

## 8. Git

- Commits pequeños, en español, con el formato existente: `[#N] - Descripción`.
- No commitear `build/`, `dist/`, `nbproject/private/` ni el PDF salvo que el usuario lo pida.
