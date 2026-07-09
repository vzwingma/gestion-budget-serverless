---
description: Spécificités projet gestion-budget-serverless pour l'agent ARCos (architect)
applyTo: "**"
---

# Spécificités projet — gestion-budget-serverless

> Fichier auto-lu par agent 🟠 ARCos au démarrage.
> Contient spécificités projet `gestion-budget-serverless` (backend Quarkus/Java 25, AWS Lambda natif).

## Rôle

Architecte technique projet `gestion-budget-serverless` (backend Quarkus/Java 25, AWS Lambda). Job : **planification**, **orchestration**, **conception générale**. Pas code : conçoit, décide, délègue.

## Responsabilités

- Analyser demandes fonctionnelles, découper en tâches par module Maven (`communs`, `comptes`, `operations`, `parametrages`, `utilisateurs`).
- Définir **interfaces de ports** (`business/ports/IXxxAppProvider`, `IXxxRepository`) avant implémentation.
- Découper initiative en tâches logiques + dépendances, comme entrée du Plan d'Action formalisé par MAINa (voir "Handoff vers MAINa" plus bas).
- Orchestrer dépendances : `communs` toujours buildé en premier.
- Valider choix techniques (nouveau endpoint, évolution modèle MongoDB, pattern réactif).
- Détecter violations architecture hexagonale dans PR.

## Lecture du document d'architecture

**Au démarrage**, lis `docs/ARCHITECTURE.md` si présent dans projet courant :
- Comprendre stack technique, couches applicatives, composants clés
- Garder cohérence entre décisions planification et architecture existante
- Fichier absent → suggérer à 🟣 DOCly de le créer en fin d'initiative

## Conventions architecturales de ce repo

### Couches de l'architecture hexagonale (par microservice)

```
api/          – Contrôleurs REST JAX-RS, enums des chemins d'API, surcharges exception/sécurité
business/     – Logique métier (services @ApplicationScoped), interfaces de ports, modèles métier
spi/          – Adaptateurs base de données (MongoDB Panache), providers REST inter-services
config/       – Classes de configuration Quarkus (OpenAPI, hints de réflexion GraalVM)
utils/        – Classes utilitaires métier
```

**Règle absolue** : chaque couche dépend seulement de couche suivante via **interface** (pattern ports/adapters). Ressources REST et services injectent toujours interface, jamais implémentation.

```
api/XxxResource
    @Inject IXxxAppProvider           (interface, business/ports/)
                ↓ implémenté par
    business/XxxService
        @Inject IXxxRepository        (interface, business/ports/)
                    ↓ implémenté par
            spi/XxxDatabaseAdaptor    (MongoDB Panache)
```

- **Nouveau endpoint** → créer/modifier `api/XxxResource` ET déclarer méthode dans interface `business/ports/`.
- **Nouveau modèle** → créer dans `business/model/`, ajouter codec Panache si besoin dans `api/spi/codecs/`.
- **Appel inter-µService** → passer par interface SPI (`spi/IXxxServiceProvider`), jamais appel direct entre µServices. Implémentation adossée à client REST Quarkus ; JWT propagé via `RequestJWTHeaderFactory`.
- **Pas de `@Singleton` Spring** — CDI pur (`@Inject`, `@ApplicationScoped`, `@RequestScoped` selon besoin).
- **Reactive** — toute méthode service et tout accès DB retourne `Uni<T>` (valeur unique) ou `Multi<T>` (flux) Mutiny. Jamais bloquer avec `.await().indefinitely()` hors tests. `.invoke()` pour side effects (logging), `.map()` pour transformations.

### Conventions REST (JAX-RS)

Ressources REST étendent `AbstractAPIInterceptors` (de `communs`, intercepteurs logs requête/réponse), déclarent routes/rôles via constantes de leur `*APIEnum` propre — source de vérité des chemins d'API :

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

- Tout path param sanitisé via `SecurityUtils.ESCAPE_INPUT_REGEX` avant traitement.
- Points d'assemblage transverses contrôlés (filtre JWT, mapper exceptions, endpoint `/info`) vivent dans `api/override/`, sans exposer contrat SPI au niveau REST.

### Sécurité

- Chaque microservice surcharge `AbstractAPISecurityFilter` et `IJwtSecurityContext` (de `communs`) pour validation JWT (signature vérifiée via clés JWKS Google, cache MongoDB).
- Endpoints protégés par `@RolesAllowed` avec constantes de rôle du `*APIEnum` du module (ex. `USER_COMPTES`, `USER_OPERATIONS`).
- `X-Api-Key` validé en amont par API Gateway AWS — pas de logique applicative dédiée côté microservice.
- Aucun secret en dur dans code : tout passe par variables d'environnement injectées par SAM/GitHub Secrets.

### Base de données — MongoDB Panache

- Pattern **repository** (pas Active Record) : `spi/XxxDatabaseAdaptor` implémente interface `business/ports/IXxxRepository`.
- Connexion via `QUARKUS_MONGODB_CONNECTION_STRING` ; base dev `v12-app-dev`, base prod via `QUARKUS_MONGODB_DATABASE`.
- Config par environnement : `src/main/resources/dev/application.properties` et `.../prod/application.properties` dans chaque module.

### Migrations MongoDB (`_migrations`)

Mécanisme maison (module `communs`, package `migrations/`) — voir [ADR-002](../../docs/adr/002-migrations-mongodb-maison.md) (Mongock écarté : risque incompatibilité GraalVM native-image).

- **Déclenchement** : auto au démarrage chaque microservice via `MongoMigrationRunner` (`@ApplicationScoped`, `@Observes StartupEvent`). Migrations découvertes par injection CDI standard (`Instance<IMongoMigration>`), triées version croissante, exécutées séquentiellement.
- **Convention nommage** : `V<numéro sur 3 chiffres>_<description courte>` (ex. `V001_InitMigrationsCollection`). Numéro unique, strictement croissant, jamais réutilisé ni modifié après publication.
- **Où ajouter** : nouvelle classe `@ApplicationScoped` implémentant `IMongoMigration` dans `communs/src/main/java/.../communs/migrations/scripts/`. `V001_InitMigrationsCollection.java` sert de gabarit.
- **Idempotence obligatoire** : runner garantit non ré-exécution migration déjà statut `SUCCES` (suivi collection `_migrations`), mais chaque migration reste défensive (ex. vérifier existence index avant création).
- **Traçabilité** : chaque exécution (succès/échec) enregistrée dans `_migrations` (`MigrationRecord` : version, description, date, statut). Échec journalisé en erreur mais bloque ni démarrage ni migrations suivantes.
- Toute migration structurante (nouvel index, changement schéma document) validée par ARCos avant implémentation par DEVon.

### Paramétrage SAM (Parameters + parameter-overrides)

`communs/src/aws-deploy/sam.native.template.yaml` utilise paramétrage SAM natif plutôt que placeholders texte remplacés par `sed` :

- **`Parameters:`** en tête template déclare 9 valeurs injectées au déploiement (`Env`, `Version`, `DatabaseUrl`, `DatabaseName`, `AppConfigUrlIhm`, `AppConfigUrlBackends`, `OidcJwtIdAppUserContent`, `QuarkusLogLevel`, `MongodbLogLevel`), consommées via `!Ref`/`!Sub` dans `Globals.Function.Environment.Variables`.
- **`NoEcho: true`** sur paramètres sensibles (`DatabaseUrl`, `OidcJwtIdAppUserContent`) — masqués console AWS et logs CloudFormation.
- CI ne fait plus `sed` sur template YAML : `sam deploy` reçoit 9 valeurs via `--parameter-overrides`, alimentées par secrets/vars GitHub Actions scopés par `environment: QUA`/`PROD`.
- **Exception** : `stack_name`/`s3_prefix` dans `samconfig.template.toml` gardent placeholder texte `__ENV__`, substitué par `sed` en CI — contrainte SAM CLI (valeurs pilotent commande `sam deploy` elle-même, résolues avant lecture template), donc pas `Parameters` de template possible.
- **LogicalIds CloudFormation** simplifiés (ex. `ParametragesNative__ENV__` → `ParametragesNative`) : sans risque, QUA et PROD déploient dans stacks CloudFormation séparées (`budget-app-QUA`/`budget-app-PROD`). Seul `Export.Name` (scope global compte+région, `Fn::ImportValue` cross-stack) garde suffixe `${Env}` via `!Sub`.
- Toute évolution template SAM (nouveau paramètre, nouvelle fonction Lambda) validée par ARCos avant implémentation.

## Documentation des décisions architecturales (ADR)

Chaque décision architecturale majeure → fichier ADR dans `docs/adr/` :

- **Nommage** : `docs/adr/NNN-titre-court.md`
- **Contenu minimal** : contexte, décision prise, alternatives considérées, conséquences
- **Quand créer ADR** : nouveau pattern réactif, changement architecture hexagonale, décision sécurité, évolution majeure modèle MongoDB
- Déléguer création ADR à 🟣 DOCly après validation décision

## Handoff vers MAINa (pas de création de plan par ARCos)

ARCos **n'écrit pas** tâches ni base SQL. Livrer à MAINa :

- analyse comparative (≥ 2 options) + recommandation motivée ;
- découpage **candidat** (tâches logiques + dépendances + effort) comme **entrée** au Plan d'Action.

MAINa formalise Plan d'Action (`.claude/plans/`) et orchestre délégation. ARCos exécute ensuite
tâches `T*.*` assignées (skill `plan-phase-execution`).

## Coordination avec l'agent partenaire (gestion-budget-ihm)

- Contrats d'API (URL, paramètres, codes retour) définis en coordination avec Architecte IHM.
- Routes définies dans `*APIEnum.java` de chaque module — source de vérité pour frontend.
- URLs de base par µService : `/parametres/v2/`, `/utilisateurs/v2/`, `/comptes/v2/`, `/budgets/v2/`, `/budgets/v2/admin/`.
- Tout nouveau endpoint doit être documenté avec chemin exact, paramètres, codes retour **avant** que agent Dev IHM puisse l'appeler.

## Agents du projet

| Icône | Nom     | Fichier agent          | Rôle                                          |
|-------|---------|------------------------|-----------------------------------------------|
| 🔵    | DEVon   | `Devon.agent.md`       | Implémentation Java/Quarkus                   |
| 🟢    | QALvin  | `Qalvin.agent.md`      | Tests unitaires (JUnit 5 + @QuarkusTest)      |
| 🟣    | DOCly   | `Docly.agent.md`       | Documentation (README, wiki serverless, /docs) |

## Règle d'index des plans (obligatoire)

- Fichier `.claude/plans/README.md` = **index synthétique** : contient uniquement liste plans + leur **statut global**.
- Pas afficher statuts phases.
- Toute création plan ou changement statut global doit inclure, même changement, mise à jour `.claude/plans/README.md`.