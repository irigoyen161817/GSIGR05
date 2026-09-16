# Copilot instructions for GSIGR05

## Project shape

GSIGR05 is a NetBeans Java SE project. The actual NetBeans project is in the nested `GSIGR05/` directory; the repository root contains the Git metadata and `.gitignore`.

- Java sources are under `GSIGR05/src`.
- Tests, when added, belong under `GSIGR05/test`.
- NetBeans build metadata is under `GSIGR05/nbproject`.
- Generated output goes to `GSIGR05/build`, `GSIGR05/dist`, and `GSIGR05/dist/javadoc`; these are build products, not source.
- The configured source encoding is UTF-8.
- The project currently targets Java 24 (`javac.source` and `javac.target` in `GSIGR05/nbproject/project.properties`).
- The configured main class is `gsigr05.GSIGR05`.

The current source is only the generated `gsigr05.GSIGR05` application shell. The coursework implementation is expected to grow around the packages named by the assignment, especially `GSILabs.BModel`, `GSILabs.BSystem`, and `GSILabs.BTesting.P01`.

## Practice 1 domain requirements

Implement the rules from `Práctica_1.md`; these are the source of truth for the coursework.

### Business model (`GSILabs.BModel`)

The model must represent the following concepts and relationships:

- `Local` has a name, a `Dirección` consisting of locality, province, street, and number, and an optional description of at most 300 characters. Two locals cannot share an address.
- The venue types are `Restaurante`, `Bar`, and `Pub`. Restaurants store estimated menu price, total capacity, and maximum diners per table. Bars store specialties as tags. Pubs store opening and closing times.
- `Usuario` has a unique nickname of at least three characters, a password, and a birth date. Users must be at least 14 years old and have either the `Dueño` or `Cliente` profile.
- `Cliente` can access venue information and create reviews. A `Review` belongs to a client and a local, contains a rating from 0 to 5 stars, a comment of at most 500 characters, and the visit date. The same client cannot review the same local twice for the same visit date.
- `Dueño` can own any number of locals, while each local must have between one and three owners.
- A user can create at most one `Contestación` for a review, and only when that user owns the review's local. Replies contain a comment of at most 500 characters. Store creation dates for both reviews and replies.
- Restaurants and bars are reservable. `Reserva` stores a client, the reservation date and time, and an optional discount percentage. Reservations must not target nonexistent venues and must not target pubs.

Use the exact names and structure required by the assignment diagram: `Usuario`, `Dueño`, `Cliente`, `Contestación`, `Review`, `Local`, `Dirección`, `Reserva`, `Bar`, `Pub`, `Restaurante`, and `Reservable`. `Dueño` and `Cliente` derive from `Usuario`; venue types derive from `Local`; reservable venue types implement `Reservable`.

Model validation errors with documented exceptions. Where object attributes do not provide a stable identity, generate a unique identifier transparently (for example, with `AtomicInteger`). Instantiable classes should override `toString()` and `equals()` consistently. Keep relationships coherent when objects are removed; use the assignment's configurable instance-locking or cascade-removal requirements rather than leaving dangling references.

### Business system (`GSILabs.BSystem`)

`BusinessSystem` is the in-memory system layer, separate from the business model. It must implement the provided `GSILabs.BSystem.LeisureOffice` and `LookupService` interfaces and use the model classes for storage and validation. Keep the model as the domain dictionary and put lookup, registration, modification, deletion, and consistency checks in this layer.

### Required executable verification (`GSILabs.BTesting.P01`)

Create `GSILabs.BTesting.P01.Tester`. It must instantiate `BusinessSystem`, load representative users, locals, reviews, replies, and reservations, and print clear results for every scenario below. The source must comment which operation verifies each scenario:

- S1: a registered user can be found by ID.
- S2: `ObtenerUsuario` returns `null` for an unknown user.
- S3: two locals cannot be registered at the same address.
- S4: after deleting a local, a bar can be registered at the released address.
- S5: a user younger than 14 cannot be registered.
- S6: a reservation cannot be created for a nonexistent local.
- S7: matching an existing address does not make a nonexistent local reservable.
- S8: a reply/comment cannot be added for a nonexistent review.
- S9: a bar cannot have four owners.
- S10: the same user cannot submit two reviews for the same local on the same day.

## Build and run

Run these commands from the repository root, which is one directory above the NetBeans project:

```bash
ant -f GSIGR05/build.xml clean
ant -f GSIGR05/build.xml jar
ant -f GSIGR05/build.xml run
```

The JAR is written to `GSIGR05/dist/GSIGR05.jar`. NetBeans can run the same Ant targets through its Clean and Build, Run, Test, and Generate Javadoc actions.

The project uses the standard NetBeans-generated Ant script. Do not replace `GSIGR05/nbproject/build-impl.xml`; customize `GSIGR05/build.xml` or project properties if a build change is needed.

## Tests

The project is configured with `GSIGR05/test` as its test source root, but no test classes currently exist. When tests are added, run the complete test target with:

```bash
ant -f GSIGR05/build.xml test
```

To run one test class using the NetBeans-generated target:

```bash
ant -f GSIGR05/build.xml -Dtest.includes=**/MyTest.java test-single
```

Use the test class's source-relative path in `test.includes`; for example, `**/BusinessSystemTest.java`.

## Javadoc

Generate the configured project documentation with:

```bash
ant -f GSIGR05/build.xml javadoc
```

Output is placed under `GSIGR05/dist/javadoc`. Public classes and interfaces in the coursework packages should have complete Javadoc, including domain constraints, parameters, return values, and exceptions. Keep source files in UTF-8 so Spanish names and documentation such as `Dueño`, `Dirección`, and `Contestación` remain intact.

## Implementation conventions

- Preserve the package names required by the assignment rather than placing coursework classes in the generated `gsigr05` package.
- Keep the business model (`GSILabs.BModel`) separate from the in-memory system/service layer (`GSILabs.BSystem`) and from executable checks (`GSILabs.BTesting.P01`).
- Enforce domain invariants at the model or system boundary where the invalid state is introduced; do not make the tester duplicate business rules.
- Use the exact class and interface names required by the coursework diagram, including `Usuario`, `Dueño`, `Cliente`, `Contestación`, `Review`, `Local`, `Dirección`, `Reserva`, `Bar`, `Pub`, `Restaurante`, and `Reservable`.
- Preserve the model relationships: `Dueño` and `Cliente` derive from `Usuario`; the venue types derive from `Local`; reservable venue types implement `Reservable`; and reviews, replies, reservations, owners, clients, and addresses retain their domain associations.
- Keep all public classes, interfaces, constructors, methods, and domain constraints documented with complete Javadoc. The generated documentation must be meaningful and include `@param`, `@return`, and `@throws` where applicable.
- Keep the tester focused on demonstrating the system behavior; do not duplicate business rules in the test code.
- Keep generated/private NetBeans metadata out of feature changes. In particular, `GSIGR05/nbproject/private/` is machine-specific.
