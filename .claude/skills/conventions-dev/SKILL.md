---
name: conventions-dev
description: "Conventions de développement du projet gestion-budget-serverless pour 🔵 DEVon : stack et versions, structure, conventions de code, accès réseau, modèles, commandes de vérification. Prioritaire sur le générique."
user-invocable: false
---

# Conventions projet — gestion-budget-serverless (développement)

> Projet : `gestion-budget-serverless` — backend Quarkus/Java 25 multi-modules Maven, architecture hexagonale,
> Mutiny + MongoDB Panache, déployé en images natives sur AWS Lambda (SAM).

## Stack technique

- **Java 25** (`maven.compiler.release`, `pom.xml`) — build natif **Mandrel 25** (ADR-003)
- **Quarkus** — version = `quarkus.platform.version` du `pom.xml` racine (pilotée par Renovate : **ne pas la
  recopier** ailleurs, voir AP 002) ; **Mutiny** réactif, **MongoDB Panache** (pattern repository)
- **CDI** uniquement (`@Inject`, `@ApplicationScoped`, `@RequestScoped`) — aucune annotation Spring
- **JAX-RS** (`jakarta.ws.rs`) + **OpenAPI** (`org.eclipse.microprofile.openapi.annotations`)
- **Lombok** (`lombok.version`, pom racine) — déclaré en `annotationProcessorPaths` du `maven-compiler-plugin` (ADR-003)
- **Sécurité** : JWT (Google OIDC) validé via `AbstractAPISecurityFilter` / `IJwtSecurityContext` de `communs` ;
  ne jamais parser un jeton hors `JWTUtils`

## Commandes de vérification (preuves avant handoff)

```bash
mvn clean install -f communs/pom.xml   # communs d'abord (dépendance de tous les µServices)
mvn clean package                      # build tous modules (JVM)
mvn clean test -f <module>/pom.xml     # tests existants (non-régression) du module touché
```

Pas de lint/format configuré (ni Checkstyle ni Spotless) : qualité contrôlée par SonarCloud (`mvn verify -Psonar`).

## Structure et nommage

- Code source : `<module>/src/main/java/io/github/vzwingma/finances/budget/services/<module>/` ;
  organisation **par couche** : `api/` → `business/` (+ `business/ports/`, `business/model/`) → `spi/` ; `config/`, `utils/`
- Classes : `XxxResource` (REST), `XxxService` (métier), `IXxxAppProvider` / `IXxxRepository` (ports),
  `XxxDatabaseAdaptor` (Mongo), `IXxxServiceProvider` (client REST inter-µService), `XxxAPIEnum` (chemins + rôles)
- Identifiants : camelCase, libellés métier en français (`chargeCompte`, `idCompte`)

## Conventions de code

- Déclarer la méthode dans l'**interface de port** (`business/ports/`) d'abord, puis l'implémenter ; injecter
  toujours l'interface, jamais l'implémentation.
- Réactif : `Uni<T>` / `Multi<T>` partout ; jamais `.await().indefinitely()` hors tests ; `.invoke()` pour les
  effets de bord (logs), `.map()` pour les transformations.
- Gestion d'erreurs : exceptions typées de `communs/utils/exceptions/` (`DataNotFoundException`,
  `BadParametersException`, `CompteClosedException`, `UserNotAuthorizedException`…) propagées dans le `Uni`.
- Entrées : path params sanitisés `idParam.replaceAll(SecurityUtils.ESCAPE_INPUT_REGEX, "_")` ; traçage via
  `BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.XXX, valeur)`.
- Accès données : via `IXxxRepository` (`spi/XxxDatabaseAdaptor`) ; autre µService : via `IXxxServiceProvider`
  (`spi/`, client REST Quarkus), jamais d'appel direct.
- Modèles : `business/model/` (partagés : `communs/.../data/model/`) ; codecs Panache dans `spi/codecs/` (ex. `communs/.../spi/codecs/ComptePanacheCodec`).
- Constantes : chemins + rôles dans `api/enums/XxxAPIEnum` ; clés de trace dans `BusinessTraceContextKeyEnum`.
- Dates : injecter `Clock` (ADR-004), jamais `.now()` direct.
- Ajout de champ dans `LigneOperation.Categorie` / `SsCategorie` → l'ajouter aussi dans
  `BudgetDataUtils.cloneOperationToMoisSuivant()`.
- Migration Mongo : classe `V<NNN>_<Description>` implémentant `IMongoMigration` dans
  `communs/.../migrations/scripts/`, idempotente (ADR-002).
- Jamais d'URL, clé ou secret en dur : variables d'environnement (`application.properties` dev/prod, paramètres SAM).

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

## Ce que DEVon ne fait PAS

- Modifier les fichiers de test `<module>/src/test/**` (`Test*.java`, `*Test.java`) — rôle 🟢 QALvin.
- Mettre à jour `README.md`, `docs/` ou les instructions d'agents (rôle 🟣 DOCly).
- Introduire une dépendance, une interface de port structurante ou un nouveau pattern sans tâche 🟠 ARCos dans le Plan d'Action.
