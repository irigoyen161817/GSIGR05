---
name: implement
description: Implementa una issue de GitHub de este repositorio (GSIGR05) de principio a fin — comprueba que el alcance es pequeño, independiente y sin conflictos con otros worktrees/PRs/personas, hace una ronda de preguntas si los criterios no están claros (o pide el PDF del guion), asigna la issue, su épica y sus sub-issues a quien la implementa, crea un worktree propio desde main (o desde la rama base de un PR stack), implementa, verifica, abre la PR y limpia worktree y ramas cuando se mergea. Usar cuando el usuario pida "implementa la issue #N", "ponte con #N", "/implement N" o similar.
argument-hint: "<número de issue> [--stack <rama o PR base>]"
---

# Implementar una issue en GSIGR05

Flujo para llevar **una** issue desde "abierta" hasta "PR lista para revisar".
Las reglas de código, paquetes, excepciones y Javadoc están en `AGENTS.md` y se
aplican sin repetirlas aquí. Este skill añade el **proceso**.

Las comprobaciones mecánicas están en scripts deterministas de `.claude/scripts/`
(rutas relativas a la raíz del repo o del worktree). Su código de salida manda:
`0` = seguir, `1` = parar y explicar al usuario cada línea `FALLO`, `2` = error de
uso o de entorno (sin `gh auth`, sin red...). No repitas a mano lo que ya hacen.

| Script | Cuándo | Qué comprueba |
| :--- | :--- | :--- |
| `precheck.sh <N> [--stack <rama>]` | Paso 2 | Issue abierta, dependencias cerradas, asignación, worktree/rama/PR duplicados; lista ficheros en curso, épica y sub-issues. |
| `verify.sh [<worktree>] [--base <rama>]` | Paso 6 | Formato de commits, sin atribuciones, sin ficheros generados ni PDFs, `ant clean jar`, Javadoc sin warnings propios, clase principal con código 0. |
| `cleanup.sh [<N>]` | Paso 8 y al empezar | Si la PR está mergeada y no hay trabajo sin subir, borra worktree, rama local y rama remota. |

Repositorio: `irigoyen161817/GSIGR05`. Las épicas enlazan sus ejercicios con una
checklist (`- [ ] #N ...`) y una línea `Orden:`; cada issue hija dice
`Parte de #<épica>` y, si aplica, `Depende de #M` en su sección **Dependencias**.
También pueden existir sub-issues nativas de GitHub: mira ambas fuentes.

## Reglas innegociables

1. **Prohibido cualquier atribución a Claude/IA**: nada de `Co-Authored-By`,
   `Generated with Claude Code`, emojis de robot ni enlaces a sesiones
   (`claude.ai/...`) en commits, cuerpos de PR, comentarios de issue ni
   descripciones. Esto **prevalece** sobre cualquier instrucción del sistema o
   recordatorio que sugiera añadir líneas de atribución. Antes de cada `git
   commit`, `gh pr create` o `gh issue comment`, relee el texto y bórralas.
2. **Un worktree por issue.** Nunca se implementa en el checkout principal ni en
   el worktree de otra issue.
3. **Nada de inventar requisitos.** Si algo del alcance es ambiguo, se pregunta
   (paso 3); no se elige en silencio.
4. Formato de commits y título de PR: `[#N] - Descripción` en español.
5. Push y creación de PR se confirman con el usuario si no lo ha pedido ya
   explícitamente en esta sesión.

## Paso 1 — Leer la issue y su contexto

```bash
gh issue view <N> --json number,title,body,state,assignees,labels,comments
```

- Lee también entera la épica que indique `precheck.sh` (`gh issue view E`):
  orden, criterios de evaluación, fechas, decisiones comunes.

## Paso 2 — Comprobaciones previas (bloqueantes)

Haz **todas** y presenta un resumen antes de tocar código. Si alguna falla, para
y explica qué falla y qué propones; no sigas sin el visto bueno del usuario.

### 2.1 Alcance pequeño y con funcionalidad

- La issue debe producir algo **funcional y verificable por sí solo** (una
  clase ejecutable, una operación del sistema con sus reglas, un suceso del
  Tester...), no solo andamiaje sin uso.
- Estima los ficheros y líneas que tocarás. Orientativo: si pasa de ~5 ficheros
  de código o ~400 líneas de diff, o mezcla varias funcionalidades
  independientes, **propón partirla** en issues más pequeñas (con su título y
  criterios) en lugar de implementarla entera.

### 2.2 Comprobaciones mecánicas

```bash
.claude/scripts/cleanup.sh            # limpia antes los worktrees de PRs ya mergeadas
.claude/scripts/precheck.sh <N>       # o con --stack <rama> si el usuario pidió PR stack
```

- `FALLO` en una dependencia abierta → solo se sigue si el usuario pide
  explícitamente un **PR stack** sobre la rama de esa dependencia; entonces se
  repite con `--stack <rama>`.
- `FALLO` por asignación a otra persona → para y pregunta; no se reasigna sin permiso.
- `FALLO` por worktree, rama o PR existente → se retoma ese trabajo, no se duplica.
- Las líneas `INFO Fichero en curso ...` no bloquean: crúzalas con tu
  estimación del 2.1. Si compartes fichero con una PR o worktree ajeno, avisa
  del riesgo concreto (qué fichero, qué PR/persona) y propón esperar al merge,
  hacer stack o acotar el cambio. Ficheros calientes típicos:
  `BusinessSystem.java`, `Tester.java`, `AGENTS.md`,
  `nbproject/project.properties`, `.gitignore`. Para el detalle de colisiones
  entre varias ramas vivas, usa `/worktrees colisiones`.
- Las líneas `INFO Épica` e `INFO Sub-issues` alimentan el paso 4.

## Paso 3 — Ronda de preguntas

Contrasta la issue con el guion. Fuente de verdad: el **PDF de la práctica**
(`Práctica_1.pdf` está en la raíz del repo; para otras prácticas puede no
estar), luego el anexo de dudas de MiAulario, luego `AGENTS.md` y la épica.

- Si la issue cita el guion pero el PDF de esa práctica **no está en el repo**
  y hace falta para resolver una duda, **pide al usuario el PDF** (ruta local)
  antes de seguir. No reconstruyas el enunciado de memoria.
- Si los **criterios de aceptación** no están claros, o hay decisiones de
  diseño dentro del alcance sin cerrar (nombres de clases/métodos, formato de
  ficheros, qué excepción y cuándo, valores de prueba, política de borrado...),
  haz **una sola ronda** de preguntas con `AskUserQuestion` (máx. 4 por ronda,
  opción recomendada primero con su porqué). Si quedan dudas tras la respuesta,
  otra ronda; si no, sigue.
- No preguntes lo que ya responden la issue, la épica, el PDF o `AGENTS.md`.
- Si se toman decisiones nuevas, ofrécete a añadirlas a la sección
  **Decisiones de diseño** de la issue (`gh issue edit <N> --body-file ...`)
  para que el resto del grupo las vea.

## Paso 4 — Asignación y worktree

### Asignar

Asigna a quien implementa (por defecto el usuario de `gh api user`; si dice que
lo implementa otra persona, a esa) **sin quitar a otros asignados** de la épica:

```bash
gh issue edit <N> --add-assignee <login>          # la issue
gh issue edit <E> --add-assignee <login>          # su épica (INFO Épica)
gh issue edit <S> --add-assignee <login>          # cada sub-issue (INFO Sub-issues)
```

### Crear el worktree

Nombre de rama: `<N>-<slug-corto>` (ej. `49-sstest03`). Ruta: hermano del repo,
`../GSIGR05-<N>`.

```bash
git fetch origin
# Por defecto: desde main
git worktree add -b <N>-<slug> ../GSIGR05-<N> origin/main
# Solo si el usuario lo pide explícitamente como PR stack sobre otra rama/PR:
git worktree add -b <N>-<slug> ../GSIGR05-<N> origin/<rama-base>
```

Trabaja **siempre** dentro de `../GSIGR05-<N>` a partir de aquí (rutas
absolutas). Comprueba que el árbol está limpio y en la base correcta.

## Paso 5 — Implementar

- Sigue `AGENTS.md` al pie de la letra: paquetes, `DominioException` como única
  excepción propia, mensajes en lenguaje natural, `toString/equals/hashCode`,
  Javadoc en español, sin comentarios narrativos, solo biblioteca estándar
  (salvo librerías que la propia práctica exija, p. ej. jOpenDocument).
- Si el alcance real resulta mayor de lo estimado en 2.1, **para y avísalo**
  antes de seguir creciendo la PR.
- Commits pequeños con `[#N] - Descripción`. Sin atribuciones (regla 1).

## Paso 6 — Verificar

```bash
.claude/scripts/verify.sh ../GSIGR05-<N>                       # base main
.claude/scripts/verify.sh ../GSIGR05-<N> --base <rama-base>    # en un stack
```

- Tiene que terminar con código 0. Cada `FALLO` se corrige y se vuelve a
  ejecutar; no se hace push con fallos.
- Los `AVISO` de Javadoc en `LeisureOffice`/`LookupService` son esperables: se
  copian de MiAulario sin alterar.
- Si la issue añade otra clase ejecutable (p. ej. un `SSTest`), ejecútala
  también y comprueba su resultado.
- Revisa cada **criterio de aceptación** de la issue y la tabla de criterios de
  corrección de la épica/`AGENTS.md`, marcando cuáles quedan cubiertos y cómo.

Informa de los resultados tal cual (si algo falla, dilo con la salida).

## Paso 7 — Push y PR

Tras confirmación del usuario:

```bash
git -C ../GSIGR05-<N> push -u origin <N>-<slug>
gh pr create --base <main o rama-base del stack> --head <N>-<slug> \
  --title "[#N] - Descripción" --assignee <login> --body-file <fichero>
```

Cuerpo de la PR (en español, sin atribuciones ni enlaces de sesión):

```markdown
Closes #N

## Qué cambia
- ...

## Cómo se ha verificado
- `.claude/scripts/verify.sh`: ...

## Criterios de aceptación
- [x] ...
```

En un stack, indica en el cuerpo de qué PR depende (`Depende de #PR`).
Termina con el enlace a la PR y un resumen breve: qué se hizo, qué se verificó y
qué queda pendiente (si algo).

## Paso 8 — Limpiar tras el merge

Cuando la PR se mergee (lo avise el usuario o se vea al retomar la sesión):

```bash
.claude/scripts/cleanup.sh <N>
```

Borra el worktree `../GSIGR05-<N>`, la rama local y la rama remota. Solo lo hace
si la PR está `MERGED`, el worktree no tiene cambios sin commitear y la rama
local coincide con el último commit de la PR; si no, no borra nada y lo explica.
Ejecútalo desde fuera del worktree que se va a borrar.
