# Copilot Instructions – gestion-budget-serverless

This is the **Quarkus/Java 21 backend** of the Budget Management application, deployed as native AWS Lambda functions. The frontend is the companion repo [`gestion-budget-ihm`](../gestion-budget-ihm) (React/TypeScript).

## Build, Test & Lint

```bash
# Build all modules (JVM mode)
mvn clean package

# Build a single module
mvn clean package -f comptes/pom.xml

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=ComptesServiceTest

# Run a specific test method
mvn test -Dtest=ComptesServiceTest#testGetComptes

# Run tests in a single module
mvn test -f operations/pom.xml

# Build native Linux executable for Lambda (requires GraalVM/Mandrel)
mvn clean package -Pnative -Dquarkus.native.container-build=true

# Run SonarCloud analysis (needs sonar.token)
mvn verify -Psonar
```

## Architecture

### Multi-module Maven project
```
gestion-budget-serverless/
├── communs/          # Shared library: base classes, models, security, exceptions
├── parametrages/     # Microservice: system parameters  → /parametres/v2/
├── utilisateurs/     # Microservice: user auth/profiles → /utilisateurs/v2/
├── comptes/          # Microservice: bank accounts      → /comptes/v2/
└── operations/       # Microservice: budgets & ops      → /budgets/v2/
```

All microservices (`comptes`, `operations`, `parametrages`, `utilisateurs`) depend on `communs` and follow the same internal layering.

### Hexagonal architecture layers (per microservice)
```
api/          – JAX-RS REST controllers, enums for API paths, exception/security overrides
business/     – Domain logic (@ApplicationScoped services), port interfaces, domain models
spi/          – Database adapters (MongoDB Panache), inter-service REST client providers
config/       – Quarkus config classes (OpenAPI, GraalVM reflection hints)
utils/        – Business utility classes
```

### Key framework patterns

**REST resources** extend `AbstractAPIInterceptors` (from `communs`) and use standard JAX-RS annotations:
```java
@Path(ComptesAPIEnum.COMPTES_BASE)
public class ComptesResource extends AbstractAPIInterceptors {
    @Inject IComptesAppProvider services;

    @GET
    @RolesAllowed({ComptesAPIEnum.COMPTES_ROLE})
    @Operation(description = "...")
    public Uni<List<CompteBancaire>> getComptes() { ... }
}
```

**Reactive programming** – All service methods and database calls return `Uni<T>` (single value) or `Multi<T>` (stream) from Mutiny. Never block with `.await().indefinitely()` except in tests.

**Dependency injection** – CDI only (`@Inject`, `@ApplicationScoped`). No Spring annotations.

**Port interfaces** – Business logic is always hidden behind an interface in `business/ports/` (e.g. `IBudgetAppProvider`, `IComptesRepository`). REST resources inject the interface, not the implementation.

**Security** – Every microservice overrides `AbstractAPISecurityFilter` and `IJwtSecurityContext` from `communs`. Endpoints declare `@RolesAllowed` with role constants from their own `*APIEnum`.

**Inter-service calls** – Services that need data from other microservices inject a provider interface in `spi/` (e.g. `IComptesServiceProvider`, `IParametragesServiceProvider`) backed by a Quarkus REST client.

### Database
- **MongoDB** via Quarkus MongoDB Panache (repository pattern, not Active Record).
- Connection string: `QUARKUS_MONGODB_CONNECTION_STRING` env var (defaults to `localhost:27017` in dev).
- Dev database: `v12-app-dev`. Prod database: `QUARKUS_MONGODB_DATABASE` env var.
- Config lives in `src/main/resources/dev/application.properties` and `src/main/resources/prod/application.properties` per module.

### `communs` module
Shared across all microservices:
- `api/AbstractAPIResource` – `/info` endpoint base
- `api/AbstractAPIInterceptors` – request/response logging interceptors
- `api/security/AbstractAPISecurityFilter` – JWT validation
- `utils/security/JWTUtils`, `SecurityUtils` – JWT parsing, input sanitization
- `utils/exceptions/` – typed exceptions (`DataNotFoundException`, `UserNotAuthorizedException`, etc.)
- `data/trace/BusinessTraceContext` – MDC-style tracing context cleared after each response
- `aws-deploy/` – AWS SAM templates and API Gateway configuration

### Testing conventions
- Use `@QuarkusTest` on test classes.
- Mock dependencies with `Mockito.mock()` / `Mockito.spy()` in `@BeforeEach`.
- Resolve reactive results in tests with `.await().indefinitely()`.
- `communs` is published to GitHub Packages; microservice POMs reference it as a dependency.

## Deployment
- CI builds `communs` first, publishes to GitHub Packages, then builds each microservice in parallel as a native image.
- Native images are deployed to AWS Lambda via SAM. API routes are defined in `communs/src/aws-deploy/`.
- SonarCloud runs on master after all builds complete.
