# Copilot Instructions for Pax JPA

## Project Overview

Pax JPA is an OSGi JPA Service extender implementation.
It watches for bundles containing `META-INF/persistence.xml`, parses their persistence descriptors, and registers `EntityManagerFactoryBuilder` / `EntityManagerFactory` services on their behalf.
The project implements the OSGi JPA Service Specification 1.1.

## Build Commands

Requires Java 17+ and Maven 3+.

```sh
# Build (main module only)
mvn clean install

# Build including TCK (OSGi JPA compliance tests)
mvn clean install -Ptck

# Build ignoring TCK failures (as CI does)
mvn -B verify -Ptck -Dbnd.testing.failure.ignore=true

# Build a single module
mvn clean install -pl pax-jpa
```

There are no unit tests in `pax-jpa`.
All testing is done via the OSGi JPA TCK (`pax-jpa-tck`), which runs inside an OSGi container using `bnd-testing-maven-plugin`.

## Architecture

### Extender Pattern

`PaxJPA` (the `BundleActivator`) starts three trackers:

1. **PersistenceProvider service tracker** — watches for `PersistenceProvider` services already registered in the OSGi registry (highest priority).
2. **PersistenceProvider bundle tracker** — discovers providers statically via `META-INF/services/javax.persistence.spi.PersistenceProvider`.
3. **Persistence bundle tracker** — finds bundles with `Meta-Persistence` header or `META-INF/persistence.xml`, parses their descriptors, and coordinates service registration.

When a persistence bundle is found, `PersistenceBundleTrackerCustomizer` parses its `persistence.xml` using JAXB, creates an `EntityManagerFactoryBuilderImpl` per persistence unit, and tracks datasource + provider availability before registering `EntityManagerFactory` services.

### Key Subsystems

| Package | Responsibility |
|---|---|
| `org.ops4j.pax.jpa` | Public API and constants (`JpaConstants`) |
| `o.o.p.j.impl` | Core: activator, EMF builder, weaving hook, datasource tracking |
| `o.o.p.j.impl.tracker` | OSGi `BundleTrackerCustomizer` / `ServiceTrackerCustomizer` implementations |
| `o.o.p.j.impl.descriptor` | JAXB-based `persistence.xml` parsing and `PersistenceUnitInfo` adapter |
| `o.o.p.j.impl.cm` | OSGi ConfigAdmin integration (`ManagedServiceFactory` / `ManagedService`) |

### JAXB Code Generation

The `persistence-2.1.xsd` schema lives in `pax-jpa/src/main/xsd/`.
`jaxb2-maven-plugin` generates model classes into `target/generated-sources/jaxb/` (package `org.jcp.xmlns.xml.ns.persistence`).
Do not edit generated classes — modify the XSD or binding configuration instead.

### OSGi Bundle Packaging

The `bnd-maven-plugin` generates the OSGi manifest.
BND instructions are inline in `pax-jpa/pom.xml` (not a separate `bnd.bnd` file).
The bundle embeds `org.osgi.service.jpa.jar` and re-exports it due to [osgi/osgi#731](https://github.com/osgi/osgi/issues/731).

### TCK

`pax-jpa-tck` runs the official `org.osgi.test.cases.jpa` (v8.1.0) inside an Eclipse OSGi container with EclipseLink and H2.
The `tck.bndrun` file defines the OSGi runtime composition.
BND resolves dependencies at `pre-integration-test` and executes tests at `integration-test` phase.

## Code Conventions

- **Tabs** for indentation (not spaces)
- Opening brace on same line
- Apache License 2.0 header on every Java file
- Explicit imports (no wildcards)
- Thread safety via `AtomicBoolean`, `ConcurrentHashMap`, `CopyOnWriteArraySet`, and `synchronized` where needed
- Logging via SLF4J
- Prefer public or package-private top-level types over inner classes/interfaces/records
- Checkstyle rules inherited from the `org.ops4j:master` parent POM
