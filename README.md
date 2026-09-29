# GSIGR05 · Práctica 01

Práctica 01 de **Gestión de Sistemas de Información (GSI)**, UPNA, curso 2026/2027. Grupo 05.

Sistema en memoria (sin persistencia) para un portal de ocio: locales (bares, pubs y restaurantes), usuarios (clientes y propietarios), reviews, contestaciones y reservas. Aplica las reglas de negocio C01–C09 del guion.

## Requisitos

- JDK 23 o posterior (`javac.source`/`javac.target` = 23).
- Apache Ant o NetBeans (el proyecto es un proyecto Java SE de NetBeans con Ant).
- Solo usa la biblioteca estándar de Java.

## Estructura

```
.
├── AGENTS.md                 Instrucciones para agentes de código
├── README.md                 Este documento
├── Práctica_1.pdf            Guion oficial (no se versiona)
└── GSIGR05/                  Proyecto NetBeans
    ├── build.xml
    ├── nbproject/
    └── src/GSILabs/
        ├── BModel/           Modelo de negocio
        ├── BSystem/          Almacenamiento y operaciones
        └── BTesting/P01/     Tester de los sucesos S1–S10
```

### `GSILabs.BModel`

Entidades del dominio y reglas intrínsecas (longitudes, rangos, edad mínima):

- `Usuario` (abstracta), con los perfiles `Cliente` y `Propietario`.
- `Local` (abstracta), con los tipos `Bar`, `Pub` y `Restaurante`, y su `Direccion`.
- `Reservable`: interfaz sellada que solo implementan `Bar` y `Restaurante`, así que un `Pub` no se puede reservar.
- `Review`, `Contestacion` y `Reserva`.
- `PoliticaBorrado`: `BLOQUEAR` o `CASCADA`, para mantener la coherencia al borrar.
- `DominioException`: la única excepción propia. Se lanza cuando se incumple una regla de negocio y su mensaje explica el motivo en lenguaje natural.

Cada entidad se identifica por su clave natural: el nick del usuario, la dirección del local, la visita de la review (cliente, local y fecha) y la review que responde la contestación. Solo `Reserva` genera un identificador numérico.

### `GSILabs.BSystem`

- `LeisureOffice` y `LookupService`: interfaces proporcionadas por la asignatura (MiAulario), copiadas sin cambiar sus firmas.
- `BusinessSystem`: implementa las dos. Guarda las entidades en colecciones de `java.util` y comprueba las reglas que dependen del conjunto: nick y dirección únicos, entre 1 y 3 dueños por local, una review por visita, una contestación por review y existencia de los locales y reviews referenciados.

Operaciones principales:

| Área | Métodos (`LeisureOffice`) |
| :--- | :--- |
| Usuarios | `nuevoUsuario`, `eliminaUsuario`, `modificaUsuario`, `existeNick`, `obtenerUsuario` |
| Locales | `nuevoLocal`, `eliminarLocal`, `obtenerLocal`, `actualizarLocal`, `asociarLocal`, `desasociarLocal` |
| Reviews | `nuevaReview`, `eliminaReview`, `existeRewiew`, `verReviews` |
| Contestaciones | `nuevaContestacion`, `tieneContestacion`, `obtenerContestacion`, `eliminaContestacion` |
| Reservas | `nuevaReserva`, `obtenerReservas`, `eliminarReserva` |

`LookupService` añade las consultas: listados de locales, bares, restaurantes y pubs por ciudad y provincia, valoración media (de un local, de un propietario o por franja de edad) y locales ordenados por valoración.

Como los métodos de las interfaces no declaran excepciones, las operaciones rechazadas devuelven `false` o `null`. El motivo se consulta con `getUltimoError()`.

La política de borrado se elige en el constructor y se puede cambiar con `setPoliticaBorrado(...)`:

```java
BusinessSystem sistema = new BusinessSystem();                         // BLOQUEAR por defecto
BusinessSystem cascada = new BusinessSystem(PoliticaBorrado.CASCADA);
```

### `GSILabs.BTesting.P01`

`Tester` puebla un `BusinessSystem` y comprueba los sucesos S1–S10 del guion. Por cada comprobación imprime `OK` o `FALLO` con el valor esperado, el obtenido y, si el sistema rechaza la operación, el motivo. Al final muestra un resumen y termina con código de salida `0` si todo es correcto o `1` si algo falla.

| Suceso | Comprobación |
| :--- | :--- |
| S1 | Un usuario introducido se localiza después por su ID (nick). |
| S2 | `obtenerUsuario` de un usuario inexistente devuelve `null`. |
| S3 | No se pueden introducir dos locales en la misma dirección. |
| S4 | Tras añadir y eliminar un local, se puede introducir un bar en su dirección. |
| S5 | No se puede introducir un usuario menor de 14 años. |
| S6 | No se pueden hacer reservas para un local inexistente. |
| S7 | Tampoco si el local inexistente está en la dirección de otro que sí existe. |
| S8 | No se pueden añadir contestaciones a reviews inexistentes. |
| S9 | No se pueden añadir cuatro dueños a un bar. |
| S10 | No se pueden añadir dos reviews del mismo usuario, el mismo día y para el mismo local. |

## Compilar y ejecutar

Desde la raíz del repositorio:

```bash
ant -f GSIGR05/build.xml clean jar    # compilar y empaquetar en GSIGR05/dist/GSIGR05.jar
ant -f GSIGR05/build.xml run          # ejecutar el Tester (main.class)
ant -f GSIGR05/build.xml javadoc      # generar el Javadoc en GSIGR05/dist/javadoc
```

Una vez compilado, también se puede ejecutar con:

```bash
java -jar GSIGR05/dist/GSIGR05.jar
```

En NetBeans basta con abrir la carpeta `GSIGR05/` como proyecto y ejecutarlo (F6).

## Reglas de negocio

| Regla | Resumen |
| :--- | :--- |
| C01 | Local con nombre, dirección y descripción opcional de 300 caracteres como máximo. No puede haber dos locales en la misma dirección. |
| C02 | Tipos de local: `Restaurante` (precio de menú, capacidad total y por mesa), `Bar` (especialidades) y `Pub` (horas de apertura y cierre). |
| C03 | Usuario con nick único de al menos 3 caracteres, contraseña y fecha de nacimiento. Edad mínima de 14 años. |
| C04 | El cliente consulta locales y escribe reviews. |
| C05 | Review de 0 a 5 estrellas con comentario de 500 caracteres como máximo. Solo una por visita (mismo cliente, local y fecha). |
| C06 | Un local tiene entre 1 y 3 dueños. Un propietario puede tener cualquier número de locales. |
| C07 | Solo un dueño del local puede contestar una review, y como máximo una vez. |
| C08 | La fecha de creación de reviews y contestaciones se registra automáticamente. |
| C09 | Solo se reserva en bares y restaurantes dados de alta. La reserva tiene fecha, hora y un descuento opcional. |

## Documentación

Todo el código tiene Javadoc en español. Las decisiones de diseño y las interpretaciones de requisitos ambiguos están en el Javadoc de cada paquete (`package-info.java`) y de `BusinessSystem`.
