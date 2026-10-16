GSIGR05 · Gestión de Sistemas de Información

Repositorio del Grupo 05 para las prácticas de la asignatura Gestión de Sistemas de Información (GSI), Universidad Pública de Navarra (UPNA), curso 2026/2027.

Este proyecto engloba la construcción progresiva de un sistema de información para un portal de ocio (bares, pubs, restaurantes, reservas y reseñas) y su posterior integración con herramientas ofimáticas.

🛠️ Requisitos del Entorno

Java: JDK 23 o posterior (javac.source/javac.target = 23).

IDE / Build: Apache Ant o NetBeans (el proyecto es un proyecto Java SE de NetBeans con Ant).

Librerías:

Biblioteca estándar de Java.

jOpenDocument (añadida mediante ruta local al proyecto para la Práctica 02).

📂 Estructura del Proyecto

.
├── AGENTS.md                 Instrucciones para agentes de código
├── README.md                 Este documento
├── Práctica_1.pdf            Guion oficial P01 (no se versiona)
├── Práctica_2.pdf            Guion oficial P02 (no se versiona)
└── GSIGR05/                  Proyecto NetBeans
    ├── build.xml
    ├── nbproject/
    ├── lib/                  Librerías externas (jOpenDocument)
    └── src/GSILabs/
        ├── BModel/           Modelo de negocio (Entidades y reglas)
        ├── BSystem/          Almacenamiento, operaciones y lógica central
        ├── BTesting/P01/     Tester de los sucesos S1–S10 de la P01
        └── Misc/             Integración ofimática y pruebas ODS (P02)


📘 Práctica 01: Lógica de Negocio y Sistema en Memoria

Sistema en memoria (sin persistencia) que aplica las reglas de negocio C01–C09 del guion para locales, usuarios, reviews, contestaciones y reservas.

GSILabs.BModel (Entidades)

Entidades del dominio y reglas intrínsecas:

Usuarios: Usuario (abstracta), perfiles Cliente y Propietario. (Edad mínima 14 años, nick único).

Locales: Local (abstracta), tipos Bar, Pub y Restaurante, con su Direccion (única por local).

Interfaces: Reservable (solo Bar y Restaurante).

Interacciones: Review (una por visita), Contestacion (solo por un dueño, una vez) y Reserva.

Excepciones: DominioException (lanzada al incumplir reglas de negocio).

Políticas: PoliticaBorrado (BLOQUEAR o CASCADA).

GSILabs.BSystem (Gestión)

Implementa las interfaces LeisureOffice y LookupService.

BusinessSystem guarda las entidades en colecciones y comprueba las restricciones de negocio cruzadas (límites de dueños, nicks/direcciones únicas, unicidad de reviews, etc.).

Las operaciones rechazadas devuelven false o null, y el motivo se consulta con getUltimoError().

GSILabs.BTesting.P01 (Pruebas)

Tester puebla un BusinessSystem y comprueba los sucesos S1–S10 del guion (búsquedas, duplicados, validaciones de edad, referencias huérfanas, restricciones de reservas y reviews).

📊 Práctica 02: Integración Ofimática

Extensión del sistema para permitir la lectura y escritura automatizada de información utilizando hojas de cálculo (Open Document Format - .ods). Todo el código se encuentra bajo el paquete GSILabs.Misc y extensiones en GSILabs.BSystem.

Ejercicios Desarrollados:

Ejercicio 1: Configuración de jOpenDocument con rutas relativas/locales.

Ejercicio 2 (SSTest01.java): Generación de test01.ods con una matriz bidimensional de $4 \times t$ números enteros.

Ejercicio 3 (SSTest02.java): Creación de test02.ods con matriz de $4 \times 6$ (desplazada a la 4ª fila, 6ª columna) y formateo condicional de celdas (azul si $\ge 10$, rojo si $< 10$).

Ejercicio 4 (SSTest03.java): Lectura de los datos numéricos introducidos en el fichero test02.ods.

Ejercicio 5 (SSTest04.java): Exportación de la vista lógica a vista física (ODS). Almacena Bares, Restaurantes y Pubs desde un BusinessSystem en tres hojas separadas, estructurando propiedades y colecciones (tags) en columnas.

Ejercicio 6 (SSTest05.java & BusinessSystem.java): Método importaBares(File f): int añadido al sistema para cargar masivamente listados de Bares desde un archivo externo estandarizado.

🚀 Compilar y Ejecutar

Desde la raíz del repositorio:

ant -f GSIGR05/build.xml clean jar    # compilar y empaquetar en GSIGR05/dist/GSIGR05.jar
ant -f GSIGR05/build.xml run          # ejecutar el Tester principal
ant -f GSIGR05/build.xml javadoc      # generar el Javadoc en GSIGR05/dist/javadoc


También se puede ejecutar directamente el .jar (asegurándose de tener el .jar de jOpenDocument en el classpath si es necesario):

java -jar GSIGR05/dist/GSIGR05.jar


En NetBeans, basta con abrir la carpeta GSIGR05/ como proyecto y ejecutarlo (F6).

📦 Instrucciones de Entrega (Interno del Grupo)

Comprimir el proyecto (NetBeans/Eclipse) en un archivo Gr05.zip o Gr05.7z.

CRÍTICO: El proyecto debe ser totalmente autocontenido. No usar rutas absolutas ni dependencias a archivos en el C:\ local. La librería jOpenDocument debe ir dentro de la carpeta del proyecto.

📖 Documentación

Todo el código tiene Javadoc en español. Las decisiones de diseño y las interpretaciones de requisitos ambiguos están en el Javadoc de cada paquete (package-info.java) y de la clase principal BusinessSystem.
