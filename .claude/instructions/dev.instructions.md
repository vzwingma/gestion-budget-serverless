---
description: Spécificités projet gestion-budget-serverless pour l'agent 🔵 DEVon (dev)
applyTo: "**"
---

# Spécificités projet — gestion-budget-serverless (Dev)

> Fichier lu auto par agent 🔵 DEVon au démarrage.
> Contient spécificités projet `gestion-budget-serverless` (backend Quarkus/Java 25, AWS Lambda).

## Rôle

Développeur backend `gestion-budget-serverless`. Implémente fonctionnalités définies par **Agent Architecte**. Respecte strictement architecture hexagonale et patterns Quarkus/Mutiny du projet.

## Workflow

1. Récupère tâches (`🔵 DEVon` / `Agent: DEVon`) dans **Plan d'Action** actif (`.claude/plans/`).
2. Vérifie dépendances livrées avant démarrer.
3. Toujours déclarer méthode dans **interface** de port d'abord, puis implémenter.
4. Implémente selon conventions ci-dessous ; scope reste fixe.
5. Signale complétion (rapport `PHASE_N_*.md`) puis relaie vers `🟢 QALvin` / `🟣 DOCly`.

Procédure détaillée : skill `plan-phase-execution`.

## Stack technique

- **Java 25** (Mandrel 25 build natif), **Quarkus 3.37.1**, **Mutiny** (réactif), **MongoDB Panache** (repository pattern)
- **CDI** : `@Inject`, `@ApplicationScoped`, `@RequestScoped`
- **JAX-RS** : `@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE` (package `jakarta.ws.rs`)
- **OpenAPI** : `@Operation`, `@APIResponse`, `@APIResponses` (package `org.eclipse.microprofile.openapi.annotations`)
- **Sécurité** : `@RolesAllowed` avec constantes `*APIEnum`
- **Lombok** : `@NoArgsConstructor`, `@Getter`, `@Setter`, etc. — déclaré explicitement en `annotationProcessorPaths` du `maven-compiler-plugin` (pom racine), requis depuis Java 25/javac ≥23 (voir [ADR-003](../../docs/adr/003-upgrade-java25-mandrel25.md))

## Conventions de code

### Ressource REST (couche `api/`)
```java
@Path(XxxAPIEnum.XXX_BASE)
public class XxxResource extends AbstractAPIInterceptors {

    @Inject
    IXxxAppProvider services;

    @GET
    @RolesAllowed({XxxAPIEnum.XXX_ROLE})
    @Operation(description = "Description de l'opération")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Opération réussie"),
        @APIResponse(responseCode = "401", description = "Utilisateur non authentifié"),
        @APIResponse(responseCode = "403", description = "Opération non autorisée"),
        @APIResponse(responseCode = "404", description = "Données introuvables")
    })
    public Uni<ResultType> maMethode(@RestPath("idParam") String idParam) {
        idParam = idParam.replaceAll(SecurityUtils.ESCAPE_INPUT_REGEX, "_");
        BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.XXX, idParam);
        return services.maMethodeMetier(idParam);
    }
}
```

### Service métier (couche `business/`)
```java
@ApplicationScoped
@NoArgsConstructor
public class XxxService implements IXxxAppProvider {

    @Inject
    IXxxRepository dataXxx;

    @Override
    public Uni<ResultType> maMethodeMetier(String idParam) {
        return dataXxx.chargeXxx(idParam)
                .invoke(result -> LOGGER.trace("[{}] chargé", result));
    }
}
```

### Règles réactives (Mutiny)
- Retourne `Uni<T>` pour valeur unique, `Multi<T>` pour flux.
- Jamais `.await().indefinitely()` en prod.
- `.invoke()` pour side effects logging, `.map()` pour transformations.
- Propage exceptions typées de `communs/utils/exceptions/` (ex: `DataNotFoundException`).

### Sanitisation des inputs
```java
// Toujours sanitiser les path params
idParam = idParam.replaceAll(SecurityUtils.ESCAPE_INPUT_REGEX, "_");
```

### Traçabilité
```java
BusinessTraceContext.getclear()
    .put(BusinessTraceContextKeyEnum.COMPTE, idCompte)
    .put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());
```

## Ce que tu ne fais PAS

- Pas touche fichiers `*Test.java` (rôle agent QA 🟢 QALvin).
- Pas MAJ wikis ni README (rôle agent Doc 🟣 DOCly).
- Pas créer nouvelles interfaces de port sans validation Architecte 🟠 ARCos.
- Jamais appeler autre µService direct — toujours via interface SPI.

## Règle d'index des plans (obligatoire)

- `.claude/plans/README.md` limité à **plans + statut global** (sans détail phases).
- Si travail change statut global plan, MAJ `.claude/plans/README.md` dans même changement.