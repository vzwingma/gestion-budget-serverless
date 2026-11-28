# 🏗️ Architecture — gestion-budget-serverless

---

## 🎯 Vue d'ensemble

**gestion-budget-serverless** est le backend de l'application de gestion de budget personnelle. Il expose une API REST
JSON consommée par le frontend React [`gestion-budget-ihm`](https://github.com/vzwingma/gestion-budget-ihm). Les
microservices sont compiles en binaires natifs GraalVM et deployes sous forme de fonctions **AWS Lambda** via SAM.

| Propriété | Valeur |
|---|---|
| **Type** | Backend – API REST serverless |
| **Stack principale** | Java 25 (Mandrel 25 natif) + Quarkus 4.0.1 + Mutiny + MongoDB Panache |
| **Plateforme cible** | AWS Lambda (natif GraalVM/Mandrel) |
| **Version applicative** | 26.0.0-SNAPSHOT |
| **Statut** | En développement actif |

---

## 🏢 Architecture Globale

Le projet est un **monorepo Maven multi-modules**. Chaque microservice est un projet Quarkus indépendant qui se déploie en Lambda autonome.

```
gestion-budget-serverless/
├── communs/          → Bibliothèque partagée (modèles, sécurité, exceptions, providers JAX-RS, configuration commune)
├── parametrages/     → µService paramètres système   → /parametres/v2/
├── utilisateurs/     → µService auth / profils        → /utilisateurs/v2/
├── comptes/          → µService comptes bancaires     → /comptes/v2/
└── operations/       → µService budgets & opérations  → /budgets/v2/ + /budgets/v2/admin/
```

### Flux de données principal

```
Client (IHM React)
    → API Gateway AWS (HTTPS + X-Api-Key)
    → AWS Lambda (Quarkus natif)
        → Authentification quarkus-oidc (signature, iss, aud, exp) + JwtRolesAugmentor
        → Filtres de `communs` : JwtSecurityFilter (identité validée + X-Api-Key), APITraceFilters (trace + [HTTP-METRIC])
        → Ressource REST (JAX-RS)
        → Service métier (Mutiny Uni<T>)
        → Repository MongoDB Panache
        ← MongoDB Atlas
    ← JSON
```

---

## 🔧 Architecture Hexagonale (par microservice)

Chaque microservice respecte strictement l'architecture hexagonale. **Règle absolue** : chaque couche ne dépend que de la couche suivante via une **interface**.

> Note : `api/override` ne contient plus que `RootAPIResource` (chemin `/_info` lié à `tenant-paths`, [ADR-008](./adr/008-endpoint-info-public-tenant-oidc-desactive.md)). Les providers JAX-RS transverses (filtre de sécurité, mappers d'exceptions, filtres de trace) sont **mutualisés dans `communs`** ([ADR-009](./adr/009-mutualisation-communs-et-resilience-appels-inter-services.md), voir [Mutualisation](#-mutualisation-dans-communs-et-configuration)).

```
api/
  ├── XxxResource.java          ← Contrôleur JAX-RS (@Path, @GET, @RolesAllowed)
  ├── override/
  │   └── RootAPIResource.java  ← Endpoint /_info (propre à chaque module)
  ├── enums/
  │   └── XxxAPIEnum.java       ← Constantes de routes et rôles (source de vérité)
  └── spi/codecs/               ← Codecs Panache si nécessaire

business/
  ├── ports/
  │   ├── IXxxAppProvider.java  ← Interface injectée dans la ressource REST
  │   └── IXxxRepository.java   ← Interface injectée dans le service
  ├── model/                    ← Modèles métier (POJO)
  └── XxxService.java           ← Implémentation @ApplicationScoped

spi/
  ├── XxxDatabaseAdaptor.java   ← Implémentation repository MongoDB Panache
  ├── IYyyServiceProvider.java  ← Interface appel inter-µService (REST client)
  └── RequestJWTHeaderFactory.java ← Propagation JWT entre µServices

config/
  ├── OpenAPIConfig.java        ← Configuration Swagger/OpenAPI (propre à chaque module)
  └── JwtReflectionConfig.java  ← Hints GraalVM (code JWT maison mort, suppression prévue T4.0)

utils/                          ← Helpers métier
```

### Flux d'injection

```
api/XxxResource
    @Inject IXxxAppProvider           (interface)
                ↓ implémenté par
    business/XxxService
        @Inject IXxxRepository        (interface)
                    ↓ implémenté par
            spi/XxxDatabaseAdaptor    (MongoDB Panache)
```

---

## 📂 Structure détaillée des dossiers

```
gestion-budget-serverless/
├── communs/
│   ├── src/main/java/...communs/
│   │   ├── api/
│   │   │   ├── AbstractAPIResource.java          # Endpoint GET /info
│   │   │   ├── AbstractAPIInterceptors.java       # Logs requête/réponse + [HTTP-METRIC]
│   │   │   ├── AbstractAPIExceptionsHandler.java  # Mapper exceptions → HTTP (logique)
│   │   │   ├── APIExceptionsHandler.java          # @Provider concret (Jandex), mutualisé ×4
│   │   │   ├── APITraceFilters.java               # Filtres pre/post-matching de trace, mutualisés ×4
│   │   │   ├── metrics/
│   │   │   │   ├── HttpMetricsRecorder.java       # Ligne [HTTP-METRIC]
│   │   │   │   └── MetricsLog.java                # Catégorie de log des métriques (INFO)
│   │   │   ├── security/
│   │   │   │   ├── AbstractAPISecurityFilter.java # Identité validée + propagation du jeton
│   │   │   │   ├── JwtSecurityFilter.java         # @Provider @PreMatching concret, mutualisé ×4
│   │   │   │   ├── JwtRolesAugmentor.java         # Contrôle issuer, email, rôles applicatifs
│   │   │   │   ├── InfoPublicTenantResolver.java  # Tenant OIDC /_info (ADR-008)
│   │   │   │   └── IJwtSecurityContext.java
│   │   │   └── spi/codecs/
│   │   │       └── ComptePanacheCodec.java
│   │   ├── business/ports/
│   │   │   ├── IJwtSigningKeyService.java
│   │   │   ├── IJwtSigningKeyReadRepository.java
│   │   │   └── IJwtSigningKeyWriteRepository.java
│   │   ├── data/
│   │   │   ├── model/                            # Modèles partagés (CompteBancaire, etc.)
│   │   │   └── trace/
│   │   │       ├── BusinessTraceContext.java      # MDC réinitialisé après chaque réponse
│   │   │       └── BusinessTraceContextKeyEnum.java
│   │   ├── config/
│   │   │   └── EffectiveConfigLogger.java        # Log [CONFIG] au démarrage
│   │   ├── spi/
│   │   │   ├── AbstractBDDExceptionsHandler.java
│   │   │   └── SPIExceptionsHandler.java          # @Provider concret (MongoException → 503), mutualisé ×4
│   │   └── utils/
│   │       ├── security/
│   │       │   ├── JWTUtils.java                 # Parsing JWT maison (hors chemin d'authentification, suppression Phase 4)
│   │       │   └── SecurityUtils.java            # Sanitisation des inputs
│   │       ├── exceptions/                       # Exceptions typées (dont ServiceDistantIndisponibleException, ConflictException)
│   │       └── data/
│   │           └── BudgetDateTimeUtils.java
│   ├── src/main/resources/META-INF/
│   │   └── microprofile-config.properties        # Configuration COMMUNE aux 4 services (ordinal 100)
│   └── src/aws-deploy/
│       ├── sam.native.template.yaml              # Template SAM (Lambda + API Gateway)
│       └── samconfig.template.toml               # Config déploiement SAM
│
├── parametrages/src/main/java/...parametrages/
│   ├── api/ParametragesResource.java
│   ├── business/ParametragesService.java
│   └── spi/ParametragesDatabaseAdaptor.java
│
├── utilisateurs/src/main/java/...utilisateurs/
│   ├── api/UtilisateursResource.java
│   ├── business/UtilisateursService.java
│   └── spi/UtilisateursDatabaseAdaptor.java
│
├── comptes/src/main/java/...comptes/
│   ├── api/ComptesResource.java
│   ├── business/ComptesService.java
│   └── spi/ComptesDatabaseAdaptor.java
│
├── operations/src/main/java/...operations/
│   ├── api/
│   │   ├── BudgetsResource.java                  # Endpoints budget & opérations
│   │   └── AdminBudgetResource.java              # Endpoints administration
│   ├── business/
│   │   ├── BudgetService.java
│   │   ├── OperationsService.java
│   │   ├── BudgetAdminService.java
│   │   └── model/budget/
│   │       └── ProjectionBudgetSoldes.java       # Projection métier des soldes de budget
│   ├── spi/
│   │   ├── IComptesServiceProvider.java          # Client REST → µService comptes (@Retry, intercepteur de traduction)
│   │   ├── IParametragesServiceProvider.java     # Client REST → µService parametrages (+ cache des catégories)
│   │   ├── ServicesDistantsResponseExceptionMapper.java      # Réponse HTTP en erreur → exception typée
│   │   ├── TraductionErreursServiceDistant*.java             # Liaison + intercepteur CDI : échecs de transport → exception typée
│   │   ├── ServiceDistantTransitoireException.java           # Seul cas rejoué par @Retry
│   │   └── AbstractRestClientMetricsFilter.java              # Ligne [REST-METRIC] (+ 2 filtres, un par client)
│   └── utils/
│       └── BudgetDataUtils.java                  # Clonage opérations, calculs
│
└── docs/
    ├── ARCHITECTURE.md                           # Ce fichier
    └── adr/                                      # Architecture Decision Records
```

---

## 🔧 Stack Technique

### Dépendances principales

| Catégorie | Librairie | Version | Rôle |
|---|---|---|---|
| Framework | Quarkus | **4.0.1** | Runtime Lambda natif |
| Sécurité | Quarkus OIDC (`quarkus-oidc`) | (via Quarkus BOM) | Validation des `id_token` Google, mode `service` ([ADR-005](./adr/005-authentification-jwt-extension-quarkus.md)) |
| Langage | Java | **25** (Mandrel 25 en natif) | LTS, Records, Pattern Matching |
| Réactif | SmallRye Mutiny | (via Quarkus BOM) | `Uni<T>` / `Multi<T>` |
| Persistence | MongoDB Panache | (via Quarkus BOM) | Repository pattern |
| API | RESTEasy Reactive (JAX-RS) | (via Quarkus BOM) | Endpoints REST |
| OpenAPI | MicroProfile OpenAPI | (via Quarkus BOM) | Documentation Swagger |
| Métriques | Micrometer | (via Quarkus BOM) | Monitoring Lambda |
| Résilience | SmallRye Fault Tolerance (`quarkus-smallrye-fault-tolerance`, `operations`) | (via Quarkus BOM) | `@Retry` ciblé des clients REST inter-services ([ADR-009](./adr/009-mutualisation-communs-et-resilience-appels-inter-services.md)) |
| Cache | `quarkus-cache` (Caffeine, `operations`) | (via Quarkus BOM) | Cache des catégories, TTL 1 h |
| Utilitaires | Lombok | **1.18.48** | `@Getter`, `@Setter`, etc. |
| Tests | JUnit 5 + @QuarkusTest | (via Quarkus BOM) | Tests unitaires et intégration |
| Tests | Mockito | **5.x** | Mocking |
| Tests | REST Assured | (via Quarkus BOM) | Tests API |
| Couverture | JaCoCo | **0.8.16** | Rapport couverture (SonarCloud) |
| Réseau | Netty | (via Quarkus BOM) | Réseau |

> ⚠️ Maintenir ce tableau à jour à chaque montée de version majeure (vérifier dans `pom.xml`).
>
> ℹ️ Depuis Java 25 (upgrade [ADR-003](./adr/003-upgrade-java25-mandrel25.md)), Lombok est déclaré explicitement en `annotationProcessorPaths` dans la configuration `maven-compiler-plugin` du pom racine — `javac` ≥ 23 n'active plus les annotation processors présents sur le classpath par simple auto-discovery.

### Variables d'environnement

| Variable | Description | Exemple |
|---|---|---|
| `QUARKUS_MONGODB_CONNECTION_STRING` | Chaîne de connexion MongoDB | `mongodb+srv://user:pwd@cluster.mongodb.net/` |
| `QUARKUS_MONGODB_DATABASE` | Nom de la base de données | `v12-app-dev` (dev) / via SAM (prod) |
| `DATABASE_URL` | URL MongoDB (déploiement SAM) | `mongodb+srv://...` |
| `DATABASE_NAME` | Nom de la BDD (déploiement SAM) | `v12-app-prod` |
| `OIDC_JWT_ID_APPUSERCONTENT` | ID client Google OAuth2 | `xxx.apps.googleusercontent.com` |
| `APP_CONFIG_URL_IHM` | URL CORS autorisée (frontend) | `https://budget.example.com` |
| `APP_CONFIG_URL_BACKENDS` | URLs internes inter-µServices | `https://api.example.com` |
| `QUARKUS_LOG_LEVEL` | Niveau de log Quarkus | `INFO` |
| `MONGODB_LOG_LEVEL` | Niveau de log MongoDB | `WARN` |

> ⚠️ **Format des valeurs GitHub Actions (Secrets/Variables) depuis Phase 3** : saisir ces valeurs **littéralement**, sans échappement (`/` et `.` non préfixés d'un backslash). L'ancien pipeline `sed` pouvait tolérer/nécessiter des valeurs pré-échappées (`\/`, `\.`) selon le délimiteur utilisé ; ce n'est plus le cas depuis le passage aux `Parameters` SAM (`--parameter-overrides`, cf. section "Paramétrage SAM" plus bas) — la valeur GitHub est injectée telle quelle dans l'environnement Lambda, lue directement par Quarkus (ex. `quarkus.http.cors.origins`). Une valeur pré-échappée casse silencieusement la comparaison d'origine CORS (incident réel constaté sur QUA le 2026-07-07, cf. `.claude/plans/001_modernisation_stack.plan.md` Phase 3).

---

## 🌐 API REST — Endpoints

### µService `comptes` — `/comptes/v2`

| Méthode | Chemin | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/comptes/v2/tous` | Liste des comptes de l'utilisateur | `USER_COMPTES` |
| `GET` | `/comptes/v2/{idCompte}` | Détail d'un compte | `USER_COMPTES` |

### µService `operations` — `/budgets/v2`

| Méthode | Chemin | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/budgets/v2/query?idCompte&mois&annee` | Budget mensuel d'un compte | `USER_OPERATIONS` |
| `GET` | `/budgets/v2/{idBudget}/soldes` | Soldes d'un budget | `USER_OPERATIONS` |
| `GET` | `/budgets/v2/intervalles` | Intervalles de budgets disponibles | `USER_OPERATIONS` |
| `GET` | `/budgets/v2/{idBudget}/etat` | État (actif/clôturé) d'un budget | `USER_OPERATIONS` |
| `PUT` | `/budgets/v2/{idBudget}/etat` | Modifier l'état d'un budget | `USER_OPERATIONS` |
| `GET` | `/budgets/v2/{idBudget}/operations/{idOperation}` | Détail d'une opération | `USER_OPERATIONS` |
| `POST` | `/budgets/v2/{idBudget}/operations` | Créer une opération | `USER_OPERATIONS` |
| `PUT` | `/budgets/v2/{idBudget}/operations/{idOperation}` | Modifier une opération | `USER_OPERATIONS` |
| `DELETE` | `/budgets/v2/{idBudget}/operations/{idOperation}` | Supprimer une opération | `USER_OPERATIONS` |
| `POST` | `/budgets/v2/{idBudget}/operations/versCompte/{idCompte}` | Opération intercompte | `USER_OPERATIONS` |
| `GET` | `/budgets/v2/compte/{idCompte}/operations/libelles` | Libellés connus d'un compte | `USER_OPERATIONS` |
| `POST` | `/budgets/v2/compte/{idCompte}/operations/libelles/override` | Surcharger un libellé | `USER_OPERATIONS` |
| `POST` | `/budgets/v2/admin/{idCompte}/operations/libelles/override` | Consolidation en masse des libellés d'un compte (AdminBudgetResource) | `USER_OPERATIONS` |

### µService `parametrages` — `/parametres/v2`

| Méthode | Chemin | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/parametres/v2/categories` | Liste des catégories d'opérations | – |
| `GET` | `/parametres/v2/categories/{idCategorie}` | Détail d'une catégorie | – |

### µService `utilisateurs` — `/utilisateurs/v2`

| Méthode | Chemin | Description | Rôle requis |
|---|---|---|---|
| `GET` | `/utilisateurs/v2` | Profil de l'utilisateur connecté | `USER_UTILISATEURS` |
| `GET` | `/utilisateurs/v2/lastaccessdate` | Date de dernier accès de l'utilisateur | `USER_UTILISATEURS` |
| `GET` | `/utilisateurs/v2/preferences` | Préférences utilisateur | `USER_UTILISATEURS` |

> Actuellement, l'API `utilisateurs` n'expose que ces lectures.

> 📌 **Source de vérité** : toujours vérifier les chemins dans les classes `*APIEnum.java` de chaque module.

---

## 🚦 Codes HTTP et gestion des erreurs

Les exceptions sont traduites en HTTP par deux mappers concrets du module `communs` (`APIExceptionsHandler`, `SPIExceptionsHandler`), découverts par Jandex dans les 4 services : plus aucune copie par module.

| Exception | Code | Mapper |
|---|---|---|
| `BadParametersException` | **400** | `AbstractAPIExceptionsHandler` |
| `DataNotFoundException`, `BudgetNotFoundException` | **404** | `AbstractAPIExceptionsHandler` |
| `CompteClosedException` | **406** | `AbstractAPIExceptionsHandler` |
| `ConflictException` (virement : id d'opération déjà présent sur le compte destination, PR #239) | **409** | `AbstractAPIExceptionsHandler` |
| `UserNotAuthorizedException` / `UserAccessForbiddenException` | 401 / 403 | `AbstractAPIExceptionsHandler` |
| `DonneeCorrompueException` (ex. budget sans `mois`, soldes absents) | **500** | `AbstractAPIExceptionsHandler` |
| `ServiceDistantIndisponibleException` / `ServiceDistantTransitoireException` (aval en panne, lent, ou réponse 4xx/5xx inattendue) | **503** | `AbstractAPIExceptionsHandler` (repli) |
| Autre `AbstractBusinessException` (repli) | 503 | `AbstractAPIExceptionsHandler` |
| **Toute `MongoException`** (configuration, timeout, serveur indisponible, socket, commande, écriture…) | **503** | `AbstractBDDExceptionsHandler` |

Le 500 est réservé aux données corrompues : une indisponibilité de la base est toujours un 503. Le corps d'une `MongoException` est un message générique (« Erreur d'accès à la base de données »), sans détail interne.

### Appels vers un autre service (`operations` → `comptes` / `parametrages`)

Changements de code HTTP de la Phase 2 du plan 004 ([ADR-009](./adr/009-mutualisation-communs-et-resilience-appels-inter-services.md)), visibles par l'IHM :

| Situation | Avant | Maintenant |
|---|---|---|
| Compte inconnu (création / modification / suppression d'opération, `GET /budgets/v2/query`), hors virement | 500 | **404** |
| Service aval en panne, connexion refusée, timeout, ou réponse 400/401/403/406/429/5xx de l'aval | 500 | **503** |

Un 401 ou un 403 renvoyé par l'aval n'est **jamais** transmis à l'IHM (voir [Appels inter-services](#-appels-inter-services-et-résilience)). Le virement garde ses codes (404 compte destination inconnu, 406 clos, 400 gardes), complétés par 409 et 503 (tableau du virement ci-dessous).

### Lecture d'un budget : absence ≠ erreur technique (R5)

- Budget **absent** (lecture par `_id`, ou par compte/mois/année) → `BudgetNotFoundException` → **404**.
- **Erreur Mongo** (timeout, indisponibilité) → remontée telle quelle → **503**. Une erreur technique n'est jamais confondue avec une absence, et **ne déclenche jamais l'initialisation d'un budget vierge** (seule l'absence réelle l'initialise).
- Soldes (`GET /budgets/v2/soldes`) et intervalle de budgets (`GET /budgets/v2/intervalles`) : une erreur Mongo remonte → **503** (autre erreur technique → 500) ; l'absence de budget reste un **200 avec une liste vide** (contrat inchangé). Aligné en T1.6 (plan 004), auparavant toute erreur y était renvoyée en 404.

### Budget d'un compte (`GET /budgets/v2/query`)

| Cas | Comportement |
|---|---|
| **F3** — compte **inactif**, budget du mois M absent | **200** avec le **dernier budget ≤ M** (mois antérieur), `actif=false`. Le mois et l'id du budget reçu peuvent donc différer de ceux demandés. Aucun budget du tout → 404. |
| **F4** — compte actif **neuf** (aucun budget) | Budget vierge (solde précédent 0), enregistré. |
| **F4** — écart de k mois (0 ≤ k ≤ **24**) entre M-1 et le dernier budget | Les k mois intermédiaires sont créés (déjà clos, opérations prévues reportées), le dernier budget est clos s'il est actif, puis M est créé. Lectures constantes (le dernier budget, plus une relecture par id avant sa clôture). |
| **F4** — écart **> 24 mois** | Mois intermédiaires non créés, WARN, le **solde du dernier budget connu est repris** dans M ; le dernier budget n'est ni modifié ni clos. |
| Budget M existant et actif, erreur Mongo sur M-1 (**mode dégradé, réservé à l'affichage**) | **200** : M est servi avec le `soldeAtFinMoisPrecedent` stocké, log ERROR, **sans enregistrement**. |

Le mode dégradé ne s'applique qu'à l'affichage. Sur un chemin d'écriture (compte destination d'un virement), une erreur Mongo sur M-1 fait échouer l'opération (503) ; une donnée corrompue (500) ou une exception non Mongo remonte dans tous les cas.

### Virement interne (`POST /budgets/v2/{idBudget}/operations/versCompte/{idCompte}`) — ADR-007

Écritures **séquentielles avec compensation**, sans transaction MongoDB ([ADR-007](./adr/007-coherence-virement-ecriture-sequentielle-compensee.md)) : opération source, puis opération cible. Si la cible échoue, la source est compensée (`$pull` en création, `$set` positionnel `listeOperations.$[op]` en modification, `$push` si la ligne a disparu) et l'erreur d'origine est propagée. Un échec de compensation est loggé en ERROR (identifiants source, destination, opération) sans masquer l'erreur d'origine : reprise manuelle.

Gardes, avant toute écriture :

| Cas | Code |
|---|---|
| Opération qui n'est pas un virement interne, `etat` absent, ou compte source = compte destination | **400** |
| Compte destination inconnu (404 du service `comptes` traduit en `DataNotFoundException`) | **404** |
| Compte destination clos | **406** |
| Service `comptes` en panne ou en timeout (lecture du compte source ou destination) | **503**, aucune écriture |
| **Création** : l'id d'opération existe déjà sur le compte destination (`ConflictException`, PR #239) | **409**, source compensée |
| Erreur Mongo (y compris sur M-1 du compte destination) | **503**, source compensée |

Limites acceptées : soldes et totaux de la compensation calculés sur l'instantané mémoire (R2) ; clôture de M-1 par relecture puis enregistrement du document entier, sans verrou optimiste (R2 bis, verrou prévu en Phase 4 / T4.2). Voir l'ADR-007.

### Dernier accès utilisateur (`GET /utilisateurs/v2/lastaccessdate`)

- Utilisateur inconnu → **404** (`DataNotFoundException`).
- Le dernier accès est écrit par un `$set` ciblé sur `dernierAcces` (`majDernierAcces`), **chaîné avant l'émission de la réponse** (l'ancien `.subscribe()` « fire-and-forget » est supprimé). La valeur retournée est la date lue **avant** mise à jour. Un échec d'écriture est loggé en WARN et n'empêche pas la réponse 200.

---

## 🔄 Intégrations Externes

| Système | Type | Rôle |
|---|---|---|
| MongoDB Atlas | NoSQL | Persistence des données (budgets et leurs opérations, comptes, catégories, utilisateurs) |
| AWS Lambda | FaaS | Exécution des microservices (binaires natifs) |
| AWS API Gateway | HTTP | Point d'entrée, routage, X-Api-Key |
| Google OIDC (discovery + JWKS) | JWT | Authentification utilisateurs, gérée par `quarkus-oidc` |
| µService `comptes` | REST interne | Appelé par `operations` via `IComptesServiceProvider` |
| µService `parametrages` | REST interne | Appelé par `operations` via `IParametragesServiceProvider` |
| GitHub Packages | Maven registry | Distribution du module `communs` |

---

## 🧩 Mutualisation dans `communs` et configuration

Décision : [ADR-009](./adr/009-mutualisation-communs-et-resilience-appels-inter-services.md) (plan 004, Phase 2).

### Providers mutualisés

Le code transverse vivant et générique est déclaré **une seule fois** dans `communs`, sous forme de classes concrètes découvertes par **Jandex** (`jandex-maven-plugin` du pom racine) dans chacun des 4 services :

| Classe (`communs`) | Rôle |
|---|---|
| `api/APIExceptionsHandler` (`@Provider`) | Mapper des exceptions métier → HTTP |
| `spi/SPIExceptionsHandler` (`@Provider`) | Mapper de `MongoException` → 503 |
| `api/security/JwtSecurityFilter` (`@Provider @PreMatching`) | Identité validée (`quarkus-oidc`) + `X-Api-Key` |
| `api/APITraceFilters` (`@ServerRequestFilter(preMatching = true)` / `@ServerResponseFilter`) | Trace des requêtes + `[HTTP-METRIC]` |

Restent dans **chaque module** : `RootAPIResource` (chemin `/_info` lié à `tenant-paths`, ADR-008) et `OpenAPIConfig` (titre propre à chaque service ; `info.version` = version de l'application via `quarkus.smallrye-openapi.info-version`). Les ressources REST gardent `extends AbstractAPIInterceptors` pour `getAuthenticatedUser()`.

> ⚠️ Un module construit contre un SNAPSHOT `communs` obsolète perd ses mappers **sans erreur de compilation**. Toujours lancer `mvn clean install` depuis la racine ; un test REST par module (`CommunsProvidersRestTest`) vérifie les codes HTTP et le log de trace unique.

### Configuration : `communs` + un seul `application.properties` par module

| Fichier | Ordinal | Contenu |
|---|---|---|
| `communs/src/main/resources/META-INF/microprofile-config.properties` | 100 | Configuration **commune** : bloc MongoDB (connexion, timeouts, pool), OIDC (`application-type`, audience, issuer, timeouts, tenant `info-public` désactivé), CORS, logs, masquage `OidcProvider`, contournement Quarkus #55278, `info-version` OpenAPI |
| `<module>/src/main/resources/application.properties` | 250 | Clés **propres au service** : `application.name`, ports `%dev,test.`, catégorie de log du module, `tenant-paths` (ADR-008) ; `operations` : URL, timeouts et scope des clients REST, TTL du cache |
| `<module>/src/test/resources/application.properties` | — | Tests uniquement : clé publique OIDC de test, `devservices` OIDC/Keycloak désactivés |

- **Profils Quarkus** (fin des profils Maven `dev/` et `prod/` et des répertoires de ressources associés) : sans préfixe = dev + test + prod ; `%dev,prod.` = tout sauf les tests ; `%prod.` = Lambda uniquement. Les tests JVM et natifs lisent donc la **même** configuration.
- Le préfixe `%dev,prod.` est posé sur ce qui ne doit pas s'appliquer aux tests : `quarkus.mongodb.connection-string` (sans elle, Dev Services démarre un conteneur MongoDB), `quarkus.oidc.auth-server-url` (sinon appel à Google pendant les tests : la clé publique locale de test est utilisée), `quarkus.http.cors.origins`.
- ⚠️ **Piège** : SmallRye résout `%profil.clé` dans **toutes** les sources avant `clé`. Ne jamais définir la même clé avec préfixe de profil dans `communs` et sans préfixe dans un module : la version préfixée gagnerait toujours (c'est pourquoi la catégorie de log des métriques n'a volontairement aucune variante `%prod.`).
- **Log `[CONFIG]` au démarrage** (`EffectiveConfigLogger`, INFO) : profils actifs et valeurs **effectives** d'une liste blanche de clés non sensibles (timeouts et pool Mongo, timeouts OIDC, issuer, timeouts des clients REST, niveau de masquage d'`OidcProvider`). Jamais de chaîne de connexion, d'URL ni de jeton ; toute valeur qui ressemble à une URL avec identifiants est remplacée par `<masquée>`. Sert à vérifier en QUA que la configuration de `communs` est bien prise en compte en natif.

---

## 🔁 Appels inter-services et résilience

Décision : [ADR-009](./adr/009-mutualisation-communs-et-resilience-appels-inter-services.md). Concerne les clients REST `IComptesServiceProvider` et `IParametragesServiceProvider` du module `operations` (appels via l'API Gateway, JWT propagé par `RequestJWTHeaderFactory`). Contexte : budget Lambda de **15 s**, Mongo inclus ; avant la Phase 2, aucun timeout (défauts Quarkus 15 s / 30 s).

### Timeouts et retry

| Élément | Valeur |
|---|---|
| Timeout de connexion (`quarkus.rest-client."<client>".connect-timeout`) | **1500 ms** |
| Timeout de lecture (`read-timeout`) | **4000 ms** |
| `@Retry` (SmallRye Fault Tolerance) | `maxRetries = 1`, `delay = 200 ms`, `jitter = 100 ms`, sur les **lectures** seulement |
| Rejoué (`retryOn = ServiceDistantTransitoireException`) | connexion impossible (refusée, hôte inconnu, connexion coupée) et réponses **429, 502, 503, 504** |
| **Jamais rejoué** | timeout (un retry provoquerait un 2ᵉ démarrage à froid de l'aval), 404, autres 4xx, autres 5xx |
| Circuit breaker, fallback | **Aucun** (l'état d'un circuit breaker est par conteneur, inutile sous Lambda ; un fallback masquerait un échec) |

Un aval lent donne un **503 en moins de 6 s** (4 s de lecture, sans retry). Le pire cas Mongo cumulé de 16 s (timeouts du client Mongo) dépasse les 15 s de la Lambda : **accepté** par le 👤 (ADR-009, plan 004 Q8).

### Traduction des erreurs de l'aval

| Réponse ou échec de l'aval | Exception | Code renvoyé à l'IHM |
|---|---|---|
| 404 | `DataNotFoundException` | **404** |
| 429, 502, 503, 504 | `ServiceDistantTransitoireException` (rejouée 1×) | **503** |
| Tout autre code (**dont 401 et 403**, autres 4xx, autres 5xx) | `ServiceDistantIndisponibleException` | **503** |
| Timeout (connexion ou lecture) | `ServiceDistantIndisponibleException` | **503** |
| Connexion impossible | `ServiceDistantTransitoireException` (rejouée 1×) | **503** |

- **401/403 de l'aval → 503, jamais 401/403 vers l'IHM** : la requête entrante est déjà authentifiée par `operations`, donc un 401/403 de l'aval est un défaut de configuration inter-services, pas une session expirée ; le transmettre déconnecterait l'utilisateur (déconnexion IHM sur 401 et 403). Écart par rapport au texte initial de l'ADR-009 (401 propagé) : décision 👤 du Gate #2, 2026-11-22.
- Le corps de la réponse de l'aval n'est ni lu ni recopié dans les messages d'erreur.
- **Réponses HTTP** : `ServicesDistantsResponseExceptionMapper` (`@RegisterProvider` sur les 2 clients).
- **Échecs de transport** (timeout, connexion refusée) : les `ResponseExceptionMapper` et `ClientResponseFilter` ne les voient pas. Ils sont traduits par un **intercepteur CDI** (`TraductionErreursServiceDistantInterceptor`, lié par `@TraductionErreursServiceDistant(client, method)` sur chaque méthode de client). Il est *interne* à l'intercepteur de fault tolerance : le `@Retry` voit déjà l'exception typée et distingue connexion refusée (rejeu) et timeout (pas de rejeu). Client et méthode sont lus dans la liaison d'intercepteur (`ArcInvocationContext`), **sans réflexion** (sûr en natif), avec repli sur un libellé `inconnu`.

> ⚠️ **Risque** : une méthode de client ajoutée **sans** la liaison `@TraductionErreursServiceDistant` n'aurait ni traduction des timeouts / connexions refusées, ni retry ciblé, ni `[REST-METRIC]` d'échec de transport (seules les réponses HTTP resteraient traduites par le mapper). Toute nouvelle méthode des interfaces `I*ServiceProvider` doit porter la liaison (et `@Retry` si c'est une lecture rejouable).

### Lecture en parallèle du compte et du budget

`BudgetService.getBudgetAndCompteActif(idBudget)` (création, modification, suppression d'opération, réinitialisation d'un budget, virement : côté source, lu avec le compte destination) lance **en parallèle** la lecture du budget (Mongo) et celle du compte (service `comptes`, identifiant déduit de l'`idBudget` par le suffixe `_AAAA_MM`). Ainsi, le pire cas REST (~7,2 s selon l'ADR-009) reste sous le `read-timeout` Mongo de 8 s. Règles : budget introuvable → 404, **prioritaire** sur une erreur de lecture du compte ; compte stocké dans le budget différent de celui de l'id → `DonneeCorrompueException` (500) ; compte clos → 406. La lecture `GET /budgets/v2/query` (`getBudgetMensuel(idCompte, mois, annee)`) lit le compte d'abord (404 si inconnu, 503 si l'aval est en panne), puis le budget.

### Cache des catégories

- `@CacheResult(cacheName = "categories")` (`quarkus-cache`, Caffeine) sur `IParametragesServiceProvider.getCategories()` et `getSsCategorieParId(id)` ; `quarkus.cache.caffeine."categories".expire-after-write=1H`.
- Deux lectures dans le TTL = **un seul appel HTTP**. Une **erreur n'est jamais mise en cache** (le cache ne mémorise que les succès). Log DEBUG `[CACHE] miss` (nom du cache et client, sans paramètre donc sans identifiant).
- **Copie défensive** : les catégories en cache ne sont jamais modifiées par les appelants (`OperationsService` travaille sur une copie locale de la sous-catégorie de remboursement).
- Le **compte n'est pas mis en cache** : un compte clos resterait vu actif et contournerait la garde 406 (reporté, ADR-009).
- Le TTL Caffeine n'utilise pas la `Clock` de l'[ADR-004](./adr/004-clock-injection-convention.md). Les catégories étant modifiées à la main en base, elles peuvent être périmées jusqu'à 1 h par instance Lambda.

---

## 📈 Instrumentation (logs `[HTTP-METRIC]` et `[REST-METRIC]`)

Deux lignes INFO, exploitables en **CloudWatch Logs Insights**. Elles ne contiennent **jamais** d'identifiant, de chemin, de corps, d'email, de clé API ni de jeton.

| Ligne | Émise par | Exemple |
|---|---|---|
| `[HTTP-METRIC] op=<Ressource.méthode> status=<code> durationMs=<n> cold=<bool>` | `HttpMetricsRecorder` (via `APITraceFilters`), une par requête entrante | `[HTTP-METRIC] op=BudgetsResource.getBudget status=200 durationMs=42 cold=false` |
| `[REST-METRIC] client=<comptes\|parametrages> method=<GET> status=<code\|timeout\|connect-error> durationMs=<n>` | `AbstractRestClientMetricsFilter` (réponses HTTP) et `TraductionErreursServiceDistantInterceptor` (timeout, connexion) | `[REST-METRIC] client=comptes method=GET status=timeout durationMs=4003` |

- `op` est la classe et la méthode JAX-RS (`SimpleResourceInfo`, sans réflexion, sûr en natif), `unmatched` si aucune ressource n'est sélectionnée. `cold=true` uniquement pour la première requête de l'instance. `durationMs=-1` si l'heure de début est inconnue. Durées mesurées avec `System.nanoTime()` (horloge monotone).
- Catégorie de log dédiée `io.github.vzwingma.finances.budget.metrics`, **fixée à INFO** dans `communs` pour tous les profils : les métriques restent émises quel que soit `QUARKUS_LOG_LEVEL`.

Exemples de requêtes CloudWatch Logs Insights (non exécutés dans le cadre de la Phase 2 ; à adapter au groupe de logs de la Lambda) :

```
# Latence par opération, chaud / froid
fields @timestamp, @message
| filter @message like /\[HTTP-METRIC\]/
| parse @message "op=* status=* durationMs=* cold=*" as op, status, durationMs, cold
| filter durationMs >= 0
| stats count() as n, pct(durationMs, 50) as p50, pct(durationMs, 95) as p95, max(durationMs) as max by op, cold
| sort p95 desc

# Appels inter-services : volumes, erreurs et latence par client et statut
fields @timestamp, @message
| filter @message like /\[REST-METRIC\]/
| parse @message "client=* method=* status=* durationMs=*" as client, method, status, durationMs
| stats count() as n, pct(durationMs, 95) as p95 by client, status
| sort n desc

# Pannes de l'aval seulement
fields @timestamp, @message
| filter @message like /\[REST-METRIC\]/ and (@message like /status=timeout/ or @message like /status=connect-error/)
| stats count() as n by bin(5m)
```

Les lignes `REPORT` de la Lambda (`Duration`, `Init Duration`) complètent ces métriques. La comparaison avant/après du p95 prévue au plan (T2.9) a été **abandonnée** (décision 👤 du 2026-11-22) ; l'instrumentation reste en place pour un suivi ultérieur.

---

## 🔐 Sécurité

- **Authentification** : `id_token` Google OIDC passé dans le header `Authorization: Bearer <token>`, validé par l'extension `quarkus-oidc` en mode `service` ([ADR-005](./adr/005-authentification-jwt-extension-quarkus.md), plan 004 Phase 0).
- **Autorisation** : `@RolesAllowed` avec les constantes de `*APIEnum` (ex: `USER_COMPTES`, `USER_OPERATIONS`).
- **Clé d'API** : `X-Api-Key` header validé par API Gateway AWS. `ApiKeyRequired` et l'Usage Plan sont configurés **manuellement dans la console AWS** (et non dans le template SAM).

### Validation du jeton (`quarkus-oidc`)

Configuration **commune** dans `communs/src/main/resources/META-INF/microprofile-config.properties` (voir [Mutualisation](#-mutualisation-dans-communs-et-configuration)), sauf `tenant-paths` (dans l'`application.properties` de chaque module) :

| Propriété | Valeur | Rôle |
|---|---|---|
| `quarkus.oidc.application-type` | `service` | Mode API (Bearer uniquement) |
| `quarkus.oidc.auth-server-url` | `https://accounts.google.com` | Discovery OIDC activée ; clés JWKS mises en cache et rotation gérées par l'extension |
| `quarkus.oidc.client-id` / `token.audience` | `${oidc.jwt.id.appusercontent}.apps.googleusercontent.com` | Audience attendue |
| `quarkus.oidc.token.issuer` | `any` | Google émet `accounts.google.com` **ou** `https://accounts.google.com`, alors que la discovery n'annonce que la seconde |
| `quarkus.oidc.token.principal-claim` | `email` | Identité exposée |
| `quarkus.oidc.connection-timeout` | `3S` | Durée max d'une requête discovery / JWKS |
| `quarkus.oidc.connection-delay` | `4S` | Fenêtre de retry de la connexion initiale (pire cas ~7 s < budget d'init Lambda ~10 s) |

La propriété `auth-server-url` est préfixée `%dev,prod.` : le profil de test utilise une clé publique locale (`quarkus.oidc.public-key`) et n'appelle pas Google.

`JwtRolesAugmentor` (`communs/.../api/security/`) complète la validation de l'extension. Comme `issuer=any`, il est la **seule barrière sur l'issuer** :
- contrôle **strict** des 2 formes d'issuer (`accounts.google.com`, `https://accounts.google.com`) ; toute autre valeur est refusée (dont `evilaccounts.google.com`, `accounts.google.com.evil.com`) ;
- claim `email` requis (non vide) et `email_verified` = `true` (booléen ou chaîne `"true"`, les deux formes étant émises par Google) ;
- rôles applicatifs (`app.security.roles`, défaut `USER_COMPTES,USER_OPERATIONS,USER_UTILISATEURS`) attribués à **tout utilisateur authentifié** (pas de contrôle de propriété ni de rôle admin : hors périmètre, second temps).

`AbstractAPISecurityFilter` (concrétisé par `JwtSecurityFilter` dans `communs`, mutualisé) ne décode plus le jeton : il expose l'identité validée (email, jeton brut pour la propagation inter-µServices par `RequestJWTHeaderFactory`) et l'`X-Api-Key`. Les logs « Aucune identité authentifiée. Accès anonyme. » et « Clé API non trouvée » sont en **DEBUG** pour un chemin finissant par `/_info` (accès anonyme attendu), en **WARN** ailleurs.

### Retiré du chemin d'authentification

`JWTUtils`, le cache et la collection Mongo `jwks_auth_signing_keys`, le refresh des clés JWKS via `/_info` (`parametrages`) et `IJwtSigningKeyService` ne sont plus utilisés pour authentifier. Le code est **conservé jusqu'à la Phase 4** (suppression par le 👤) : inventaire dans [`PHASE_4_SUPPRESSIONS_A_FAIRE.md`](../.claude/plans/004_reports/PHASE_4_SUPPRESSIONS_A_FAIRE.md). `/_info` garde le même contrat (JSON, 200), désormais quel que soit le jeton (voir ci-dessous).

### Codes HTTP d'authentification

| Situation | Code |
|---|---|
| Jeton absent, invalide, expiré, mal signé, issuer/audience/email refusé (endpoints `@RolesAllowed`) | **401** (auparavant 403) |
| `/_info` (4 services) quel que soit le jeton : absent, invalide, expiré, `Bearer null`, valide (ignoré, requête anonyme) | **200** ([ADR-008](./adr/008-endpoint-info-public-tenant-oidc-desactive.md)) |
| `/_info/`, `/_info;x`, `/_INFO` **avec** jeton invalide (tenant par défaut, échec fermé) | **401** |
| Endpoint `@PermitAll` **sans** jeton | 200 |
| Endpoint `@PermitAll` **avec** jeton invalide, hors `/_info` (ex. GET catégories de `parametrages`, authentification proactive) | **401** (inchangé) |
| `/parametres/v2/categories/_info` (`@PermitAll`, chemin finissant par `/_info`) | **200** sans validation de jeton (figé par un test) |
| JWKS / discovery Google injoignable | **500** (limite connue, voir ci-dessous) |

Les endpoints protégés (`@RolesAllowed`) sont inchangés.

> ⚠️ Le passage 403 → 401 impacte l'IHM (déconnexion sur 401) : plan IHM `010_gestion_401_auth_oidc_backend`, **prérequis bloquant avant la PROD**. Depuis l'ADR-008, `/_info` ne renvoie plus 401 : la page Infos ne déconnecte plus ; seules les catégories `@PermitAll` gardent 401 avec un jeton invalide.

### `/_info` public : tenant `info-public` (ADR-008)

Avec l'authentification proactive de `quarkus-oidc`, tout jeton présent est validé avant l'autorisation, même sur `@PermitAll`. Pour que `/_info` (nom, version, description fixe : rien de sensible) reste lisible avec un jeton invalide, un tenant OIDC nommé `info-public` est déclaré : `tenant-enabled` dans la configuration commune de `communs`, `tenant-paths` dans l'`application.properties` de chaque service :

| Propriété | Valeur | Rôle |
|---|---|---|
| `quarkus.oidc.info-public.tenant-enabled` | `false` | Tenant désactivé : requête traitée en anonyme, `@PermitAll` s'applique |
| `quarkus.oidc.info-public.tenant-paths` | `<BASE>/_info` (`/comptes/v2`, `/budgets/v2`, `/parametres/v2`, `/utilisateurs/v2`) | Lie le tenant au seul chemin `/_info` |

`InfoPublicTenantResolver` (`communs/.../api/security/`, bean `@ApplicationScoped` découvert par les 4 services) renvoie `info-public` si `normalizedPath()` se termine par `/_info`, et `Default` (tenant par défaut) dans tous les autres cas.

- **Pourquoi pas `null`** : `null` laisse la chaîne continuer sur `tenant-paths` puis sur la résolution statique par **dernier segment** de chemin, qui sélectionne le tenant dont le nom égale ce segment. Une URL finissant par `/info-public` (ex. `/parametres/v2/categories/info-public`) partirait alors vers le tenant désactivé, jeton non validé. `Default`, non nul, court-circuite ces résolutions.
- **`tenant-enabled=false` redondant** avec le resolver : le tenant `info-public` n'a pas d'`auth-server-url` et ne valide donc aucun jeton. Conservé en défense en profondeur et comme documentation.
- **Risque** : un second bean `TenantResolver` fait échouer le démarrage (`IllegalStateException`, visible).
- **Verrouillé par des tests** : `TestInfoPublicTenantResolver`, `InfoEndpointPublicTest` ×4, `InfoPublicTenantNonRegressionTest` (`comptes`, `parametrages`), `ProtectedEndpointNonRegressionTest`.

> ⚠️ **À revérifier à chaque montée de version de `quarkus-oidc`** (ordre des resolvers, comportement « tenant désactivé → identité nulle »). Validé sur Quarkus 3.40.1.1 ; comportement du tenant `info-public` et de l'ordre des resolvers **revalidé sur Quarkus 4.0.1** par les tests CI (PR #241, 2026-11-27).

### Limites connues

- **JWKS / discovery Google injoignable → HTTP 500** et non 401 (refus appliqué, sans blocage ~2 s). L'IHM ne se déconnecte pas sur ce code. Conservé (décision 👤), à observer en QUA et réévaluer en Phase 4.
- **`kid` inconnu** : non testable avec `public-key` en test, à vérifier en QUA avec le JWKS réel. Démarrage à froid, appel réel à Google et build natif : validation QUA requise.

### Logs : masquage du jeton

`quarkus-oidc` (`io.quarkus.oidc.runtime.OidcProvider`) loggue le jeton complet en WARN sur signature invalide. Masquage par configuration : `quarkus.log.category."io.quarkus.oidc.runtime.OidcProvider".level=ERROR` (présente dans la configuration commune de `communs` ; tests de fuite dans `comptes`). Effet de bord : les WARN légitimes d'`OidcProvider` (jeton expiré…) ne sont plus visibles. **À revérifier à chaque montée de version de `quarkus-oidc`** (la protection repose sur la configuration seule). Résiduel : `OidcUtils` WARN `Failed to decrypt a token` (JWE uniquement, non utilisé par Google).

- **Sanitisation des entrées** : tous les path params sont sanitisés via `SecurityUtils.ESCAPE_INPUT_REGEX` avant tout traitement.
- **Données sensibles** : aucun secret dans le code source ; tout via variables d'environnement injectées par SAM/GitHub Secrets.
- **Réseau** : les µServices ne sont accessibles qu'au travers d'API Gateway (pas d'exposition directe).

---

## 🗄️ Base de Données

- **Type** : MongoDB (NoSQL, documents JSON)
- **Driver** : Quarkus MongoDB Panache — pattern **Repository** (pas Active Record)
- **Base de données dev** : `v12-app-dev` (localhost:27017)
- **Base de données prod** : variable d'environnement `QUARKUS_MONGODB_DATABASE`
- **Configuration** : bloc MongoDB commun dans `communs/src/main/resources/META-INF/microprofile-config.properties` ; `application.properties` unique par module (profils Quarkus `%dev` / `%prod`) ; variables d'environnement via le template SAM en prod.
  - Dev : `quarkus.mongodb.connection-string` = `QUARKUS_MONGODB_CONNECTION_STRING`, défaut `mongodb://localhost:27017`
  - Tests : pas de chaîne de connexion (Dev Services démarre un conteneur MongoDB si Docker est disponible)

### Collections principales

| Collection | µService | Description |
|---|---|---|
| `comptes` | `comptes` | Comptes bancaires par utilisateur |
| `budgets` | `operations` | Budgets mensuels ; les opérations sont **embarquées** dans le champ `listeOperations` de chaque budget |
| `categories` | `parametrages` | Catégories et sous-catégories d'opérations |
| `utilisateurs` | `utilisateurs` | Profils et préférences utilisateurs |
| `jwks_auth_signing_keys` | `communs` (tous) | Obsolète : clés JWKS Google, plus utilisée pour l'authentification (`quarkus-oidc`) ; drop par le 👤 en Phase 4 |
| `_migrations` | `communs` (tous) | Suivi d'exécution des migrations MongoDB (voir section [Migrations MongoDB](#-migrations-mongodb)) |

> ℹ️ Il n'existe **pas** de collection `operations` : une opération n'a pas de document propre, elle est une ligne de `budgets.listeOperations`. Les écritures d'opération (ajout, modification, suppression, compensation de virement) sont donc des mises à jour du document `budgets` du mois concerné.

### Identifiant et lecture des budgets

- L'`_id` d'un budget est une chaîne `<idCompte>_<AAAA>_<MM>` (ex. `inglivretA_2014_12`, année sur 4 chiffres, mois sur 2 chiffres) ; en base le champ `mois` est stocké en texte (`DECEMBER`).
- **Lecture d'un budget** (compte, mois, année) : par `_id` (index natif), sans passer par les champs `idCompteBancaire` / `annee` / `mois`. Aucun repli par champs.
- **Dernier budget ≤ M** (initialisation d'un mois absent, compte inactif) : une seule requête, égalité sur `idCompteBancaire` **plus** plage d'`_id` (`_id <= <idCompte>_<AAAA>_<MM>` et `_id >= <idCompte>_`), tri `_id` décroissant, premier résultat. L'égalité sur `idCompteBancaire` est nécessaire : deux comptes peuvent partager un préfixe d'`_id` (ex. `a` et `a_1`). Le tri lexical de l'`_id` d'un même compte est chronologique (année et mois de longueur fixe).
- Les lectures de budgets d'un compte (soldes, surcharge des libellés) sont triées par `_id` croissant.
- **Libellés connus** (`getLibellesOperations`) : agrégation `$match` → `$unwind $listeOperations` → `$group` côté MongoDB ; la normalisation des libellés reste côté Java.

### Client MongoDB : timeouts et pool

Définis dans le bloc Mongo de la configuration commune (`communs/.../META-INF/microprofile-config.properties`, identiques pour les 4 services et tous les profils) :

| Propriété | Valeur | Rôle |
|---|---|---|
| `quarkus.mongodb.server-selection-timeout` | `5S` | Attente maximale d'un serveur disponible |
| `quarkus.mongodb.connect-timeout` | `3S` | Établissement de la connexion |
| `quarkus.mongodb.read-timeout` | `8S` | Attente maximale d'une réponse sur la socket |
| `quarkus.mongodb.max-pool-size` | `10` | Connexions maximales par instance Lambda |
| `quarkus.mongodb.min-pool-size` | `0` | Aucune connexion maintenue au repos |
| `quarkus.mongodb.max-connection-idle-time` | `60S` | Fermeture des connexions inactives |

- **`wait-queue-timeout` n'est pas disponible** dans `quarkus-mongodb-client` (Quarkus 3.40.1) : une clé inconnue produit un warning de configuration, elle n'est donc pas posée. Alternative éventuelle : l'option d'URI `waitQueueTimeoutMS` dans la chaîne de connexion (variable d'environnement, hors dépôt).
- **L'agrégation des libellés n'est bornée que par le `read-timeout` client (8 s)** : aucune limite côté serveur. Un `maxTime` y avait été posé puis retiré : `io.quarkus.mongodb.AggregateOptions.maxTime` est appliqué par `quarkus-mongodb-client` 3.40.1 comme `maxAwaitTime` et n'envoie aucun `maxTimeMS` au serveur (limite inopérante).
- Le tier du cluster Atlas n'est pas précisé : le pool de 10 connexions est une valeur prudente, non dimensionnée sur un tier.

---

## 🔀 Migrations MongoDB

Mécanisme maison (module `communs`, package `migrations/`) — voir [ADR-002](./adr/002-migrations-mongodb-maison.md) pour le détail du choix (Mongock écarté : risque incompatibilité GraalVM native-image), complété par [ADR-006](./adr/006-reservation-atomique-migrations-mongodb.md) pour l'exécution concurrente (plusieurs Lambdas qui démarrent en même temps).

- **Déclenchement** : automatique au démarrage de chaque microservice, via `MongoMigrationRunner` (`@ApplicationScoped`, `@Observes StartupEvent`). Migrations découvertes par injection CDI standard (`Instance<IMongoMigration>`), triées par version croissante, exécutées séquentiellement.
- **Convention de nommage** : `V<numéro sur 3 chiffres>_<description courte>` (ex. `V001_InitMigrationsCollection`, `V002_AjoutIndexComptes`). Numéro unique, strictement croissant, jamais réutilisé ni modifié une fois publié.
- **Où ajouter une migration** : nouvelle classe `@ApplicationScoped` implémentant `IMongoMigration` dans `communs/src/main/java/.../communs/migrations/scripts/`. `V001_InitMigrationsCollection.java` sert de gabarit.
- **Idempotence** : obligatoire. Le runner garantit la non ré-exécution d'une migration déjà en statut `SUCCES` (suivi dans la collection `_migrations`), mais le code de chaque migration doit rester défensif (ex. vérifier l'existence d'un index avant de le créer).
- **Traçabilité** : chaque exécution est enregistrée dans `_migrations` (`MigrationRecord` : version, description, date, statut, `owner`). Une migration en échec est journalisée en erreur et ne bloque pas le démarrage de l'application, mais **arrête la chaîne** (voir ci-dessous).

### Réservation atomique par version (ADR-006)

Les 4 services embarquent `communs` et lancent les migrations à chaque cold start : sans verrou, plusieurs Lambdas exécutaient la même version en parallèle, et une version en `ECHEC` ne pouvait plus passer en `SUCCES` (insert en clé dupliquée, rejeu à chaque démarrage).

- **Réservation** : avant d'exécuter la version `V`, le runner la réserve par un `findOneAndUpdate` avec upsert sur `_migrations`. Le filtre ne correspond que si la version est absente, en `ECHEC`, ou en `EN_COURS` avec un bail périmé. Une version en `SUCCES`, ou `EN_COURS` avec un bail valide, produit une erreur de clé dupliquée (E11000) : elle est « déjà prise ».
- **États** (`MigrationStatutEnum`) : `EN_COURS` (réservée), `SUCCES`, `ECHEC`. Le champ `owner` (UUID du runner, un par instance) identifie le détenteur de la réservation.
- **Finalisation** : `updateOne({_id, owner})` vers `SUCCES` ou `ECHEC` ; seul le propriétaire peut clore la version. Aucun document trouvé (réservation reprise par un autre runner) → WARN, finalisation ignorée.
- **Bail** : 5 minutes, calculé avec la `Clock` injectée ([ADR-004](./adr/004-clock-injection-convention.md)) sur `dateExecution`. Une Lambda figée ou interrompue libère donc sa version après 5 minutes.
- **Arrêt de la chaîne** : les versions sont jouées dans l'ordre ; la chaîne s'arrête pour ce démarrage (les versions suivantes ne sont pas tentées, elles sont reprises au cold start suivant) dans trois cas :
  1. la migration est en `ECHEC` (la version reste rejouable) ;
  2. la finalisation `SUCCES` échoue ou ne trouve aucun document (la version reste `EN_COURS` jusqu'à expiration du bail, d'où l'exigence d'idempotence) ;
  3. la version est tenue (`EN_COURS` valide) par un autre runner.

  La chaîne **continue** si la version est déjà en `SUCCES` (déjà appliquée, ou passée en `SUCCES` par un autre runner entre la lecture initiale et la réservation).

### Migrations existantes

| Version | Contenu |
|---|---|
| `V001_InitMigrationsCollection` | No-op : valide le mécanisme et sert de gabarit |
| `V002_AjoutIndexBudgetsUtilisateurs` | Index `budgets {idCompteBancaire, annee, mois}` et `utilisateurs {login}`, ce dernier **non unique** (un index unique ne sera ajouté qu'après contrôle des doublons en base) |

V002 liste d'abord les index existants et ne crée l'index que si aucun index de même clé (champs, ordre, sens) n'existe, quel que soit son nom : un index créé à la main sur Atlas est reconnu.

### Limites connues

- **Bail non renouvelé** (pas de heartbeat) : une migration qui dure plus de 5 minutes, ou une Lambda figée plus de 5 minutes, permet à une autre instance de reprendre la version et de l'exécuter en parallèle ; le premier propriétaire voit sa finalisation ignorée (WARN). Les migrations doivent rester courtes et idempotentes. Décision 👤 : documenté, pas de renouvellement ; à réévaluer si une migration longue apparaît.
- **V002 borné par le `read-timeout` (8 s)** : la construction des index n'a pas de timeout dédié. Au-delà de 8 s, la migration passe en `ECHEC` (message explicite) et est rejouée au démarrage suivant ; la construction peut se poursuivre côté serveur, et l'index est alors détecté (création ignorée) au rejeu. Décision 👤 : accepté.

---

## 🧪 Tests

| Type | Framework | Emplacement | Commande |
|---|---|---|---|
| Unitaires (service) | JUnit 5 + Mockito | `src/test/java/` | `mvn test` |
| Intégration (REST) | @QuarkusTest + REST Assured | `src/test/java/` | `mvn test` |
| Intégration Mongo (`*MongoTest`) | @QuarkusTest + MongoDB réel (Testcontainers `mongo:7.0`) | `operations`, `utilisateurs` (`src/test/java/`) | `mvn test` (Docker requis) |
| Intégration Mongo (`communs`) | @QuarkusTest + MongoDB réel ou Dev Services | `communs/src/test/java/.../migrations/` | `mvn test` (MongoDB accessible ou Docker pour Dev Services) |
| Couverture | JaCoCo | `target/jacoco-report/` | `mvn verify -Psonar` |

**Docker et MongoDB de test (depuis la Phase 2 du plan 004)** : la chaîne de connexion MongoDB est préfixée `%dev,prod.` dans `communs` ; en profil de test, **Quarkus Dev Services démarre donc un conteneur MongoDB** pour les `@QuarkusTest` de tous les modules, dont `parametrages` et `utilisateurs` qui n'avaient auparavant pas besoin de Docker (profil Maven `dev/` avec `localhost:27017`). **Docker est requis** pour ces tests (décision 👤 du Gate #2, 2026-11-22). Sans Docker, la Phase 2 a été validée localement avec 871 tests verts (journal de bruit `Could not find a valid Docker environment` puis `[MIGRATIONS] Echec inattendu ... MongoTimeoutException`), mais sans que MongoDB soit réellement exercé. Le profil `ci-sans-mongo` désactive Dev Services (voir ci-dessous).

**Faux service aval** : les tests de résilience de `operations` s'appuient sur `test/aval/FauxAval` (`HttpServer` du JDK, sans nouvelle dépendance : statut par numéro d'appel, délai piloté, ports partagés) et sur le profil `ProfilFauxAvalTimeoutsReels`, qui ne change que les URL des clients. Ils exercent les **vrais timeouts** 1,5 s / 4 s (`ServicesDistantsTimeoutsReelsQuarkusTest`, `ResilienceAvalBudgetEndToEndTest` en bout en bout avec jeton OIDC valide, `CacheCategoriesTtlCourtQuarkusTest` avec un TTL de 1 s) : surcoût d'environ 30 s en CI. Preuve de régression de la Phase 2 : sans l'enregistrement du `ResponseExceptionMapper`, 23 échecs + 1 erreur ; sans le `@Retry` ciblé, 9 échecs + 1 erreur (rapport `PHASE_2_TESTS_REPORT.md`). Un `CommunsProvidersRestTest` par module vérifie les codes HTTP et un `ProfilsConfigurationTest` / `CommunsConfigurationTest` par module vérifie la configuration effective et sa **provenance** (clés communes lues dans `microprofile-config.properties` de `communs`).

Le profil Maven `ci-sans-mongo` exclut exactement cinq classes Surefire :
`TestMigrationRepository`, `TestMigrationRepositoryConcurrence`, `TestMigrationRepositoryPersistence`,
`UtilisateurDatabaseAdaptorMongoTest` et `OperationDatabaseAdaptorMongoTest`. Il configure aussi la propriété
système Surefire `quarkus.mongodb.devservices.enabled=false` pendant les tests. Sans ce profil, le comportement
par défaut reste inchangé. Les tests restants peuvent utiliser des mocks ; leur succès ne valide ni l'atomicité,
ni la persistance, ni les index MongoDB.

Lors d'un run Sonar, le runner CI a réellement lancé MongoDB Dev Services malgré les exclusions, puis le job
est resté bloqué lors de la fermeture du client MongoDB. La causalité exacte n'est pas établie.
La disparition du blocage doit être confirmée par un run CI complet avec Docker.

Les deux tests d'adaptateurs utilisent `MongoDbTestResource` et Testcontainers (`mongo:7.0`) : Docker
fonctionnel et accès à l'image sont requis ; Dev Services y est désactivé. Les trois tests de migrations
nécessitent un MongoDB de test accessible ou Dev Services avec Docker ; vérifier la configuration effective,
car une chaîne de connexion configurée peut empêcher le démarrage de Dev Services. Prévoir une base de test isolée.
Lors de l'upgrade, aucun Docker ou MongoDB local n'était disponible et ces cinq classes n'ont pas été validées.

```bash
# Commandes JVM CI depuis la racine
mvn clean install -B -Pci-sans-mongo --file communs/pom.xml
mvn clean install -B -Pci-sans-mongo --file pom.xml

# Exécution séparée Mongo réel, sans le profil ci-sans-mongo : commandes proposées, non exécutées lors de l'upgrade
mvn install -B -DskipTests --file communs/pom.xml
mvn test -B --file communs/pom.xml -Dtest=TestMigrationRepository,TestMigrationRepositoryConcurrence,TestMigrationRepositoryPersistence
mvn test -B --file utilisateurs/pom.xml -Dtest=UtilisateurDatabaseAdaptorMongoTest
mvn test -B --file operations/pom.xml -Dtest=OperationDatabaseAdaptorMongoTest
```

```bash
# Tous les tests du projet, Mongo réel compris : prérequis ci-dessus
mvn test

# Tests d'un seul module
mvn test -f operations/pom.xml

# Une classe de test spécifique
mvn test -Dtest=BudgetServiceTest

# Une méthode de test spécifique
mvn test -Dtest=BudgetServiceTest#testGetBudget

# Tests + rapport couverture JaCoCo (SonarCloud)
mvn verify -Psonar -f operations/pom.xml
```

Rapport de couverture : `target/jacoco-report/jacoco.xml` et `target/jacoco-aggregate/jacoco.xml`

### Pattern de test unitaire

```java
@QuarkusTest
class XxxServiceTest {
    private IXxxRepository xxxRepository;
    private XxxService xxxService;

    @BeforeEach
    void setup() {
        xxxRepository = Mockito.mock(IXxxRepository.class);
        xxxService = Mockito.spy(new XxxService(xxxRepository));
    }

    @Test
    void testNominal() {
        Mockito.when(xxxRepository.charge("id"))
               .thenReturn(Uni.createFrom().item(new XxxModel()));
        XxxModel result = xxxService.maMethode("id").await().indefinitely();
        assertNotNull(result);
    }
}
```

---

## 📐 Conventions et Patterns

### Réactivité (Mutiny)
- Toute méthode de service retourne `Uni<T>` (valeur unique) ou `Multi<T>` (flux).
- Ne **jamais** appeler `.await().indefinitely()` dans le code de production (tests uniquement).
- `.invoke()` pour les side effects (logging), `.map()` pour les transformations.

### CDI
- `@ApplicationScoped` pour les services, `@RequestScoped` si nécessaire.
- Pas de `@Singleton` Spring — CDI uniquement.

### Sanitisation
```java
idParam = idParam.replaceAll(SecurityUtils.ESCAPE_INPUT_REGEX, "_");
```

### Traçabilité
```java
BusinessTraceContext.getclear()
    .put(BusinessTraceContextKeyEnum.COMPTE, idCompte)
    .put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());
```

### Appels inter-µServices
- Jamais d'appel direct entre µServices.
- Toujours via une interface SPI : `spi/IXxxServiceProvider` → client REST Quarkus.
- Le JWT est propagé via `RequestJWTHeaderFactory`.
- Toute méthode d'un client doit porter `@TraductionErreursServiceDistant` (et `@Retry` ciblé si c'est une lecture), et le client enregistrer `ServicesDistantsResponseExceptionMapper` : voir [Appels inter-services et résilience](#-appels-inter-services-et-résilience).

### Providers et configuration transverses
- Un nouveau provider JAX-RS générique (mapper, filtre) se déclare **dans `communs`**, pas dans un module.
- Une clé de configuration commune va dans `communs/.../META-INF/microprofile-config.properties` ; jamais la même clé préfixée dans `communs` et non préfixée dans un module.

---

## 🚀 Déploiement

| Environnement | Déclencheur CI |
|---|---|
| QUA (staging) | Push / PR sur `master` |
| PROD | Push d'un tag `v*` |

### Pipeline CI/CD (GitHub Actions)

```
1. Calcul version QUA  → majeur du POM racine + date/heure Europe/Paris (yyyyMMdd-HHmm)
2. Build communs        → mvn clean install -Pci-sans-mongo + publish sur GitHub Packages
3. Génération SAM       → sed sur samconfig.template.toml (stack_name/s3_prefix uniquement)
4. Build µServices (parallèle)
   ├── parametrages    → mvn install -Pnative,ci-sans-mongo + build natif Docker
   ├── utilisateurs    → mvn install -Pnative,ci-sans-mongo + build natif Docker
   ├── comptes         → mvn install -Pnative,ci-sans-mongo + build natif Docker
   └── operations      → mvn install -Pnative,ci-sans-mongo + build natif Docker
5. Déploiement SAM      → sam deploy --parameter-overrides (Lambda + API Gateway)
6. SonarCloud           → mvn verify -Psonar,ci-sans-mongo (workflow master, après build communs)
```

Le workflow [`.github/workflows/build-on-master.yml`](../.github/workflows/build-on-master.yml) extrait le numéro majeur de la version effective du POM racine, puis forme une version d'affichage `majeur.yyyyMMdd-HHmm` avec l'horloge Europe/Paris. La valeur unique produite par `build-communs` est transmise aux builds natifs via `-Dquarkus.application.version` : l'endpoint `/_info` et `info.version` dans OpenAPI reflètent ainsi la même version. Ce paramètre de build n'altère pas la version ni les coordonnées Maven.

La même valeur est transmise au déploiement SAM comme paramètre `Version` ; le [template SAM natif](../communs/src/aws-deploy/sam.native.template.yaml) l'insère dans la description de chacune des quatre fonctions Lambda. Cette génération concerne le pipeline QUA uniquement ; le pipeline PROD par tags n'est pas modifié.

> ℹ️ Sonar et les builds natifs dépendent de `build-communs` et peuvent tourner en parallèle. Les workflows JVM de branche, natifs et Sonar activent `ci-sans-mongo` pour les tests. Les builds natifs CI utilisent Docker et l'image Mandrel JDK 25 ; SAM utilise également Docker. L'exclusion Mongo ne rend donc pas les builds natifs ou SAM indépendants de Docker.

```bash
# Déploiement manuel (depuis communs/src/aws-deploy/)
sam deploy --config-file samconfig.template.toml --parameter-overrides \
  Env=QUA Version=<version> DatabaseUrl=<url> DatabaseName=<db> \
  AppConfigUrlIhm=<url> AppConfigUrlBackends=<url> \
  OidcJwtIdAppUserContent=<id> QuarkusLogLevel=INFO MongodbLogLevel=INFO
```

### Renovate — synchronisation version Quarkus (pom.xml vs docs)

Le `pom.xml` racine fait référence pour la version Quarkus. La configuration `renovate.json` actuelle ne
déclare pas de `customManager` pour les versions documentaires ; le workflow `build-on-master.yml` consulté
ne contient pas de contrôle de synchronisation des versions. Vérifier manuellement README et architecture
lors des mises à jour.

### Paramétrage SAM (Parameters + parameter-overrides)

Depuis Phase 3 (tuning infra Lambda), `sam.native.template.yaml` utilise un vrai paramétrage SAM plutôt que des placeholders texte remplacés par `sed` :

- **`Parameters:`** en tête de template déclare les 9 valeurs injectées au déploiement (`Env`, `Version`, `DatabaseUrl`, `DatabaseName`, `AppConfigUrlIhm`, `AppConfigUrlBackends`, `OidcJwtIdAppUserContent`, `QuarkusLogLevel`, `MongodbLogLevel`), consommées via `!Ref`/`!Sub` dans `Globals.Function.Environment.Variables`.
- **`NoEcho: true`** sur les paramètres sensibles (`DatabaseUrl`, `OidcJwtIdAppUserContent`) — masqués dans la console AWS et les logs CloudFormation.
- La CI (`build-on-master.yml`, `build-on-tags.yml`) ne fait plus de `sed` sur le template YAML : `sam deploy` reçoit les 9 valeurs via `--parameter-overrides`, alimentées par les mêmes secrets/vars GitHub Actions qu'avant. Le job de déploiement déclare explicitement `environment: QUA`/`PROD` pour fiabiliser la résolution des secrets scopés environnement GitHub.
- **Exception** : `stack_name`/`s3_prefix` dans `samconfig.template.toml` gardent le placeholder texte `__ENV__`, substitué par `sed` en CI. Contrainte SAM CLI — ces valeurs pilotent la commande `sam deploy` elle-même (résolution du fichier de config avant lecture du template) et ne peuvent pas être des `Parameters` de template.
- **LogicalIds CloudFormation** simplifiés (`ParametragesNative__ENV__` → `ParametragesNative`, etc.) : sans risque de collision entre environnements (QUA et PROD déploient dans des stacks CloudFormation séparées, `budget-app-QUA`/`budget-app-PROD` — le logicalId est scopé à la stack, pas au compte AWS ; seul `Export.Name`, scope global compte+région, garde le suffixe `${Env}` via `!Sub`). **Mais risque réel sur une stack déjà déployée** : l'ancien pipeline `sed` substituait `__ENV__` *dans* le LogicalId lui-même avant déploiement (ex. LogicalId réellement déployé historiquement : `ComptesNativeQUA`), donc passer à un LogicalId statique change l'identité de la ressource aux yeux de CloudFormation → **replacement forcé** (delete+create) au lieu d'une mise à jour en place. Incident réel : premier déploiement CI sur QUA après Phase 3 a cassé (stack bloqué `UPDATE_ROLLBACK_FAILED` sur `ServerlessRestApiProdStage`), résolu par suppression + redéploiement complet de la stack (2026-07-07, cf. `.claude/plans/001_modernisation_stack.plan.md` Phase 3). **PROD nécessite le même traitement (delete-stack + redéploiement) avant son premier déploiement post-Phase 3** — pas une mise à jour in-place normale.

> ℹ️ `MemorySize` des 4 fonctions Lambda passé de 128 Mo à 256 Mo — estimation motivée (CPU proportionnel à la mémoire sur Lambda ; runtime natif GraalVM + désérialisation JSON + driver MongoDB réactif bénéficient de la marge CPU), **pas un chiffre issu d'une mesure réelle**. À confirmer via AWS Lambda Power Tuning en production dans une itération future.

### Build natif local

```bash
# Nécessite Docker et une image builder native
mvn clean package -Pnative -Dquarkus.native.container-build=true
```

---

## 🗺️ Décisions Architecturales (ADR)

> Les décisions architecturales majeures sont documentées dans `docs/adr/`.  
> Format : `docs/adr/NNN-titre-court.md`

| # | Décision | Statut |
|---|---|---|
| [001](./adr/001-strategie-modernisation-stack.md) | Stratégie de modernisation du stack backend (paliers Quarkus 3.x→4.x, tuning infra Lambda, migrations Mongo maison) | Acceptée |
| [002](./adr/002-migrations-mongodb-maison.md) | Mécanisme de migrations MongoDB maison (CDI, rejet de Mongock) | Acceptée |
| [003](./adr/003-upgrade-java25-mandrel25.md) | Upgrade Java 25 + Mandrel 25 | Acceptée |
| [004](./adr/004-clock-injection-convention.md) | Convention d'injection de `Clock` (remplace `.now()` direct) | Acceptée |
| [005](./adr/005-authentification-jwt-extension-quarkus.md) | Authentification JWT via `quarkus-oidc` (remplace la validation maison) | Acceptée |
| [006](./adr/006-reservation-atomique-migrations-mongodb.md) | Réservation atomique par version des migrations MongoDB (complète l'ADR-002) | Acceptée |
| [007](./adr/007-coherence-virement-ecriture-sequentielle-compensee.md) | Cohérence du virement interne : écriture séquentielle compensée, sans transaction MongoDB | Acceptée |
| [008](./adr/008-endpoint-info-public-tenant-oidc-desactive.md) | Endpoint `/_info` public : tenant OIDC `info-public` désactivé par chemin + `InfoPublicTenantResolver` (complète l'ADR-005) | Acceptée (amendée le 2026-11-02) |
| [009](./adr/009-mutualisation-communs-et-resilience-appels-inter-services.md) | Mutualisation partielle dans `communs`, configuration commune et résilience des appels REST inter-services (timeouts, retry ciblé, erreurs typées 503, cache catégories) | Acceptée (mise en œuvre : PR #233, #234, #235, #241) |
| [010](./adr/010-observabilite-par-logs-retrait-opentelemetry.md) | Observabilité par les logs CloudWatch, retrait d'OpenTelemetry, corrélation par `X-Amzn-Trace-Id` | Acceptée |
| [011](./adr/011-integrite-ecritures-budgets-verrou-optimiste.md) | Intégrité des écritures de budgets : verrou optimiste par champ `version`, 409 « modification concurrente », migration V003 | Acceptée |

> 💡 Toute nouvelle décision architecturale majeure (nouveau framework, changement de pattern, décision de sécurité) doit faire l'objet d'un ADR délégué à l'agent 🟣 DOCly.

---

## 🔗 Ressources

- **README** : [`README.md`](../README.md)
- **Instructions Copilot** : [`.github/copilot-instructions.md`](../.github/copilot-instructions.md)
- **ADRs** : [`docs/adr/`](./adr/)
- **Wiki du projet** : [github.com/vzwingma/gestion-budget-serverless/wiki](https://github.com/vzwingma/gestion-budget-serverless/wiki)
- **Conception globale** : [wiki/Conception-globale](https://github.com/vzwingma/gestion-budget-serverless/wiki/Conception-globale)
- **Frontend** : [`gestion-budget-ihm`](https://github.com/vzwingma/gestion-budget-ihm)
- **Quarkus** : [quarkus.io](https://quarkus.io)
- **Mutiny** : [smallrye.io/smallrye-mutiny](https://smallrye.io/smallrye-mutiny/)
