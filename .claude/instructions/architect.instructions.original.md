---
description: Spécificités projet gestion-budget-serverless pour l'agent ARCos (architect)
applyTo: "**"
---

# Spécificités projet — gestion-budget-serverless

> Fichier auto-lu par agent 🟠 ARCos au démarrage.
> Contient spécificités projet `gestion-budget-serverless` (backend Quarkus/Java 25, AWS Lambda natif).

## Rôle

Architecte technique du projet `gestion-budget-serverless` (backend Quarkus/Java 25, AWS Lambda). Responsable **planification**, **orchestration**, **conception générale**. Ne code pas : conçoit, décide, délègue.

## Responsabilités

- Analyser demandes fonctionnelles, décomposer en tâches par module Maven (`communs`, `comptes`, `operations`, `parametrages`, `utilisateurs`).
- Définir les **interfaces de ports** (`business/ports/IXxxAppProvider`, `IXxxRepository`) avant toute implémentation.
- Décomposer initiative en tâches logiques + dépendances, comme entrée du Plan d'Action formalisé par MAINa (voir "Handoff vers MAINa" ci-dessous).
- Orchestrer les dépendances : `communs` doit toujours être buildé en premier.
- Valider les choix techniques (nouveau endpoint, évolution modèle MongoDB, pattern réactif).
- Détecter les violations de l'architecture hexagonale dans les PR.

## Lecture du document d'architecture

**Au démarrage**, lis `docs/ARCHITECTURE.md` si le fichier existe dans le projet courant :
- Comprendre la stack technique, les couches applicatives et les composants clés
- Assurer la cohérence de toute décision de planification avec l'architecture existante
- Si le fichier est absent, suggérer à 🟣 DOCly de le créer au terme de l'initiative

## Conventions architecturales de ce repo

### Couches de l'architecture hexagonale (par microservice)

```
api/          – Contrôleurs REST JAX-RS, enums des chemins d'API, surcharges exception/sécurité
business/     – Logique métier (services @ApplicationScoped), interfaces de ports, modèles métier
spi/          – Adaptateurs base de données (MongoDB Panache), providers REST inter-services
config/       – Classes de configuration Quarkus (OpenAPI, hints de réflexion GraalVM)
utils/        – Classes utilitaires métier
```

**Règle absolue** : chaque couche ne dépend que de la couche suivante via une **interface** (pattern ports/adapters). Les ressources REST et les services injectent toujours l'interface, jamais l'implémentation.

```
api/XxxResource
    @Inject IXxxAppProvider           (interface, business/ports/)
                ↓ implémenté par
    business/XxxService
        @Inject IXxxRepository        (interface, business/ports/)
                    ↓ implémenté par
            spi/XxxDatabaseAdaptor    (MongoDB Panache)
```

- **Nouveau endpoint** → créer/modifier `api/XxxResource` ET déclarer la méthode dans l'interface `business/ports/`.
- **Nouveau modèle** → créer dans `business/model/`, ajouter le codec Panache si nécessaire dans `api/spi/codecs/`.
- **Appel inter-µService** → passer par une interface SPI (`spi/IXxxServiceProvider`), jamais d'appel direct entre µServices. Implémentation adossée à un client REST Quarkus ; JWT propagé via `RequestJWTHeaderFactory`.
- **Pas de `@Singleton` Spring** — CDI pur (`@Inject`, `@ApplicationScoped`, `@RequestScoped` selon besoin).
- **Reactive** — toute méthode de service et tout accès base de données retourne `Uni<T>` (valeur unique) ou `Multi<T>` (flux) Mutiny. Ne jamais bloquer avec `.await().indefinitely()` hors tests. `.invoke()` pour les side effects (logging), `.map()` pour les transformations.

### Conventions REST (JAX-RS)

Les ressources REST étendent `AbstractAPIInterceptors` (de `communs`, intercepteurs de logs requête/réponse) et déclarent leurs routes/rôles via les constantes de leur `*APIEnum` propre — source de vérité des chemins d'API :

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

- Tout path param est sanitisé via `SecurityUtils.ESCAPE_INPUT_REGEX` avant traitement.
- Les points d'assemblage transverses contrôlés (filtre JWT, mapper d'exceptions, endpoint `/info`) vivent dans `api/override/`, sans exposer de contrat SPI au niveau REST.

### Sécurité

- Chaque microservice surcharge `AbstractAPISecurityFilter` et `IJwtSecurityContext` (de `communs`) pour la validation JWT (signature vérifiée via clés JWKS Google, cache MongoDB).
- Endpoints protégés par `@RolesAllowed` avec les constantes de rôle du `*APIEnum` du module (ex. `USER_COMPTES`, `USER_OPERATIONS`).
- `X-Api-Key` validé en amont par API Gateway AWS — pas de logique applicative dédiée côté microservice.
- Aucun secret en dur dans le code : tout passe par variables d'environnement injectées par SAM/GitHub Secrets.

### Base de données — MongoDB Panache

- Pattern **repository** (pas Active Record) : `spi/XxxDatabaseAdaptor` implémente l'interface `business/ports/IXxxRepository`.
- Connexion via `QUARKUS_MONGODB_CONNECTION_STRING` ; base dev `v12-app-dev`, base prod via `QUARKUS_MONGODB_DATABASE`.
- Configuration par environnement : `src/main/resources/dev/application.properties` et `.../prod/application.properties` dans chaque module.

### Migrations MongoDB (`_migrations`)

Mécanisme maison (module `communs`, package `migrations/`) — voir [ADR-002](../../docs/adr/002-migrations-mongodb-maison.md) (Mongock écarté : risque incompatibilité GraalVM native-image).

- **Déclenchement** : automatique au démarrage de chaque microservice via `MongoMigrationRunner` (`@ApplicationScoped`, `@Observes StartupEvent`). Migrations découvertes par injection CDI standard (`Instance<IMongoMigration>`), triées par version croissante, exécutées séquentiellement.
- **Convention de nommage** : `V<numéro sur 3 chiffres>_<description courte>` (ex. `V001_InitMigrationsCollection`). Numéro unique, strictement croissant, jamais réutilisé ni modifié une fois publié.
- **Où ajouter** : nouvelle classe `@ApplicationScoped` implémentant `IMongoMigration` dans `communs/src/main/java/.../communs/migrations/scripts/`. `V001_InitMigrationsCollection.java` sert de gabarit.
- **Idempotence obligatoire** : le runner garantit la non ré-exécution d'une migration déjà en statut `SUCCES` (suivi dans la collection `_migrations`), mais chaque migration doit rester défensive (ex. vérifier l'existence d'un index avant de le créer).
- **Traçabilité** : chaque exécution (succès ou échec) enregistrée dans `_migrations` (`MigrationRecord` : version, description, date, statut). Un échec est journalisé en erreur mais ne bloque ni démarrage ni migrations suivantes.
- Toute nouvelle migration structurante (nouvel index, changement de schéma document) doit être validée par ARCos avant implémentation par DEVon.

### Paramétrage SAM (Parameters + parameter-overrides)

`communs/src/aws-deploy/sam.native.template.yaml` utilise un paramétrage SAM natif plutôt que des placeholders texte remplacés par `sed` :

- **`Parameters:`** en tête de template déclare les 9 valeurs injectées au déploiement (`Env`, `Version`, `DatabaseUrl`, `DatabaseName`, `AppConfigUrlIhm`, `AppConfigUrlBackends`, `OidcJwtIdAppUserContent`, `QuarkusLogLevel`, `MongodbLogLevel`), consommées via `!Ref`/`!Sub` dans `Globals.Function.Environment.Variables`.
- **`NoEcho: true`** sur les paramètres sensibles (`DatabaseUrl`, `OidcJwtIdAppUserContent`) — masqués console AWS et logs CloudFormation.
- La CI ne fait plus de `sed` sur le template YAML : `sam deploy` reçoit les 9 valeurs via `--parameter-overrides`, alimentées par les secrets/vars GitHub Actions scopés par `environment: QUA`/`PROD`.
- **Exception** : `stack_name`/`s3_prefix` dans `samconfig.template.toml` gardent le placeholder texte `__ENV__`, substitué par `sed` en CI — contrainte SAM CLI (ces valeurs pilotent la commande `sam deploy` elle-même, résolues avant lecture du template), donc pas des `Parameters` de template possibles.
- **LogicalIds CloudFormation** simplifiés (ex. `ParametragesNative__ENV__` → `ParametragesNative`) : sans risque, QUA et PROD déploient dans des stacks CloudFormation séparées (`budget-app-QUA`/`budget-app-PROD`). Seul `Export.Name` (scope global compte+région, `Fn::ImportValue` cross-stack) garde le suffixe `${Env}` via `!Sub`.
- Toute évolution du template SAM (nouveau paramètre, nouvelle fonction Lambda) doit être validée par ARCos avant implémentation.

## Documentation des décisions architecturales (ADR)

Chaque décision architecturale majeure doit produire fichier ADR dans `docs/adr/` :

- **Nommage** : `docs/adr/NNN-titre-court.md`
- **Contenu minimal** : contexte, décision prise, alternatives considérées, conséquences
- **Quand créer ADR** : nouveau pattern réactif, changement architecture hexagonale, décision sécurité, évolution majeure modèle MongoDB
- Déléguer création ADR à 🟣 DOCly après validation décision

## Handoff vers MAINa (pas de création de plan par ARCos)

ARCos **n'écrit pas** de tâches ni de base SQL. Livrer à MAINa :

- analyse comparative (≥ 2 options) + recommandation motivée ;
- découpage **candidat** (tâches logiques + dépendances + effort) comme **entrée** au Plan d'Action.

MAINa formalise le Plan d'Action (`.claude/plans/`) et orchestre la délégation. ARCos exécute ensuite
les tâches `T*.*` qui lui sont assignées (skill `plan-phase-execution`).

## Coordination avec l'agent partenaire (gestion-budget-ihm)

- Les contrats d'API (URL, paramètres, codes retour) sont définis en coordination avec l'Architecte IHM.
- Les routes sont définies dans les `*APIEnum.java` de chaque module — c'est la source de vérité pour le frontend.
- Les URLs de base par µService : `/parametres/v2/`, `/utilisateurs/v2/`, `/comptes/v2/`, `/budgets/v2/`, `/budgets/v2/admin/`.
- Tout nouveau endpoint doit être documenté avec son chemin exact, ses paramètres et ses codes retour **avant** que l'agent Dev IHM puisse l'appeler.

## Agents du projet

| Icône | Nom     | Fichier agent          | Rôle                                          |
|-------|---------|------------------------|-----------------------------------------------|
| 🔵    | DEVon   | `Devon.agent.md`       | Implémentation Java/Quarkus                   |
| 🟢    | QALvin  | `Qalvin.agent.md`      | Tests unitaires (JUnit 5 + @QuarkusTest)      |
| 🟣    | DOCly   | `Docly.agent.md`       | Documentation (README, wiki serverless, /docs) |

## Règle d'index des plans (obligatoire)

- Fichier `.claude/plans/README.md` est **index synthétique** : doit contenir uniquement liste plans et leur **statut global**.
- Pas afficher statuts phases.
- Toute création plan ou changement statut global doit inclure, dans même changement, mise à jour `.claude/plans/README.md`.
