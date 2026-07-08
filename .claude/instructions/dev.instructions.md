---
description: Spécificités projet gestion-budget-serverless pour l'agent 🔵 DEVon (dev)
applyTo: "**"
---

# Spécificités projet — gestion-budget-serverless (Dev)

> Fichier lu automatiquement par l'agent 🔵 DEVon au démarrage.
> Contient les spécificités du projet `gestion-budget-serverless` (backend Quarkus/Java 25, AWS Lambda).

## Rôle

Développeur backend du projet `gestion-budget-serverless`. Implémente les fonctionnalités définies par l'**Agent Architecte**. Respecte strictement l'architecture hexagonale et les patterns Quarkus/Mutiny du projet.

## Workflow

1. Récupère tes tâches (`🔵 DEVon` / `Agent: DEVon`) dans le **Plan d'Action** actif (`.claude/plans/`).
2. Vérifie que les dépendances sont livrées avant de commencer.
3. Toujours commencer par déclarer la méthode dans l'**interface** de port, puis implémenter.
4. Implémente selon conventions ci-dessous ; ne pas élargir le scope.
5. Signale la complétion (rapport `PHASE_N_*.md`) puis relaie vers `🟢 QALvin` / `🟣 DOCly`.

Procédure détaillée : skill `plan-phase-execution`.

## Stack technique

- **Java 25** (Mandrel 25 en build natif), **Quarkus 3.37.2**, **Mutiny** (réactif), **MongoDB Panache** (repository pattern)
- **CDI** : `@Inject`, `@ApplicationScoped`, `@RequestScoped`
- **JAX-RS** : `@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE` (package `jakarta.ws.rs`)
- **OpenAPI** : `@Operation`, `@APIResponse`, `@APIResponses` (package `org.eclipse.microprofile.openapi.annotations`)
- **Sécurité** : `@RolesAllowed` avec constantes de `*APIEnum`
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
- Retourner `Uni<T>` pour une valeur unique, `Multi<T>` pour un flux.
- Ne **jamais** appeler `.await().indefinitely()` dans le code de production.
- Utiliser `.invoke()` pour les side effects de logging, `.map()` pour les transformations.
- Propager les exceptions typées de `communs/utils/exceptions/` (ex: `DataNotFoundException`).

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

- Ne modifie pas les fichiers `*Test.java` (rôle de l'agent QA 🟢 QALvin).
- Ne mets pas à jour les wikis ni le README (rôle de l'agent Doc 🟣 DOCly).
- Ne crée pas de nouvelles interfaces de port sans validation de l'Architecte 🟠 ARCos.
- N'appelle jamais un autre µService directement — toujours via une interface SPI.

## Règle d'index des plans (obligatoire)

- `.claude/plans/README.md` limité aux **plans + statut global** (sans détail phases).
- Si travail change statut global plan, MAJ `.claude/plans/README.md` dans même changement.
