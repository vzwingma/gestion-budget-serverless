# 🏗️ Architecture — gestion-budget-serverless

---

## 🎯 Vue d'ensemble

**gestion-budget-serverless** est le backend de l'application de gestion de budget personnelle. Il expose une API REST
JSON consommée par le frontend React [`gestion-budget-ihm`](https://github.com/vzwingma/gestion-budget-ihm). Les
microservices sont compiles en binaires natifs GraalVM et deployes sous forme de fonctions **AWS Lambda** via SAM.

| Propriété | Valeur |
|---|---|
| **Type** | Backend – API REST serverless |
| **Stack principale** | Java 25 (Mandrel 25 natif) + Quarkus 3.40.1.1 + Mutiny + MongoDB Panache |
| **Plateforme cible** | AWS Lambda (natif GraalVM/Mandrel) |
| **Version applicative** | 26.0.0-SNAPSHOT |
| **Statut** | En développement actif |

---

## 🏢 Architecture Globale

Le projet est un **monorepo Maven multi-modules**. Chaque microservice est un projet Quarkus indépendant qui se déploie en Lambda autonome.

```
gestion-budget-serverless/
├── communs/          → Bibliothèque partagée (modèles, sécurité, exceptions)
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
        → Filtre AbstractAPISecurityFilter (identité validée + X-Api-Key)
        → Ressource REST (JAX-RS)
        → Service métier (Mutiny Uni<T>)
        → Repository MongoDB Panache
        ← MongoDB Atlas
    ← JSON
```

---

## 🔧 Architecture Hexagonale (par microservice)

Chaque microservice respecte strictement l'architecture hexagonale. **Règle absolue** : chaque couche ne dépend que de la couche suivante via une **interface**.

> Note : quelques points d'assemblage contrôlés existent dans `api/override` pour des préoccupations transverses, sans exposer de contrat SPI au niveau REST.

```
api/
  ├── XxxResource.java          ← Contrôleur JAX-RS (@Path, @GET, @RolesAllowed)
  ├── override/
  │   ├── RootAPIResource.java  ← Endpoint /_info
  │   ├── JwtSecurityFilter.java← Filtre sécurité (surcharge AbstractAPISecurityFilter)
  │   └── APIExceptionsHandler.java ← Mapper d'exceptions HTTP
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
  ├── OpenAPIConfig.java        ← Configuration Swagger/OpenAPI
  └── JwtReflectionConfig.java  ← Hints GraalVM pour la sérialisation native

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
│   │   │   ├── AbstractAPIInterceptors.java       # Logs requête/réponse
│   │   │   ├── AbstractAPIExceptionsHandler.java  # Mapper exceptions → HTTP
│   │   │   ├── security/
│   │   │   │   ├── AbstractAPISecurityFilter.java # Identité validée + propagation du jeton
│   │   │   │   ├── JwtRolesAugmentor.java         # Contrôle issuer, email, rôles applicatifs
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
│   │   ├── spi/
│   │   │   └── AbstractBDDExceptionsHandler.java
│   │   └── utils/
│   │       ├── security/
│   │       │   ├── JWTUtils.java                 # Parsing JWT maison (hors chemin d'authentification, suppression Phase 4)
│   │       │   └── SecurityUtils.java            # Sanitisation des inputs
│   │       ├── exceptions/                       # Exceptions typées
│   │       └── data/
│   │           └── BudgetDateTimeUtils.java
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
│   │   ├── IComptesServiceProvider.java          # Client REST → µService comptes
│   │   └── IParametragesServiceProvider.java     # Client REST → µService parametrages
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
| Framework | Quarkus | **3.40.1.1** | Runtime Lambda natif |
| Sécurité | Quarkus OIDC (`quarkus-oidc`) | (via Quarkus BOM) | Validation des `id_token` Google, mode `service` ([ADR-005](./adr/005-authentification-jwt-extension-quarkus.md)) |
| Langage | Java | **25** (Mandrel 25 en natif) | LTS, Records, Pattern Matching |
| Réactif | SmallRye Mutiny | (via Quarkus BOM) | `Uni<T>` / `Multi<T>` |
| Persistence | MongoDB Panache | (via Quarkus BOM) | Repository pattern |
| API | RESTEasy Reactive (JAX-RS) | (via Quarkus BOM) | Endpoints REST |
| OpenAPI | MicroProfile OpenAPI | (via Quarkus BOM) | Documentation Swagger |
| Métriques | Micrometer | (via Quarkus BOM) | Monitoring Lambda |
| Utilitaires | Lombok | **1.18.46** | `@Getter`, `@Setter`, etc. |
| Tests | JUnit 5 + @QuarkusTest | (via Quarkus BOM) | Tests unitaires et intégration |
| Tests | Mockito | **5.x** | Mocking |
| Tests | REST Assured | (via Quarkus BOM) | Tests API |
| Couverture | JaCoCo | **0.8.14** | Rapport couverture (SonarCloud) |
| Réseau | Netty | **4.1.135.Final** (via Quarkus BOM) | Fix CVE-2026-33870/33871 |

> ⚠️ Maintenir ce tableau à jour à chaque montée de version majeure (vérifier dans `pom.xml`).
>
> ℹ️ Depuis Quarkus 3.37.1, l'override Netty explicite dans les POM a été retiré : le BOM Quarkus embarque nativement Netty 4.1.135.Final, qui couvre déjà les CVE-2026-33870/33871.
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

Les exceptions sont traduites en HTTP par deux mappers du module `communs`, repris dans chaque service (`APIExceptionsHandler`, `SPIExceptionsHandler`).

| Exception | Code | Mapper |
|---|---|---|
| `BadParametersException` | **400** | `AbstractAPIExceptionsHandler` |
| `DataNotFoundException`, `BudgetNotFoundException` | **404** | `AbstractAPIExceptionsHandler` |
| `CompteClosedException` | **406** | `AbstractAPIExceptionsHandler` |
| `UserNotAuthorizedException` / `UserAccessForbiddenException` | 401 / 403 | `AbstractAPIExceptionsHandler` |
| `DonneeCorrompueException` (ex. budget sans `mois`, soldes absents) | **500** | `AbstractAPIExceptionsHandler` |
| Autre `AbstractBusinessException` (repli) | 503 | `AbstractAPIExceptionsHandler` |
| **Toute `MongoException`** (configuration, timeout, serveur indisponible, socket, commande, écriture…) | **503** | `AbstractBDDExceptionsHandler` |

Le 500 est réservé aux données corrompues : une indisponibilité de la base est toujours un 503. Le corps d'une `MongoException` est un message générique (« Erreur d'accès à la base de données »), sans détail interne.

### Lecture d'un budget : absence ≠ erreur technique (R5)

- Budget **absent** (lecture par `_id`, ou par compte/mois/année) → `BudgetNotFoundException` → **404**.
- **Erreur Mongo** (timeout, indisponibilité) → remontée telle quelle → **503**. Une erreur technique n'est jamais confondue avec une absence, et **ne déclenche jamais l'initialisation d'un budget vierge** (seule l'absence réelle l'initialise).
- Exceptions à ce principe, non modifiées par la Phase 1 : `chargeSoldesBudgetMensuel` et `chargeIntervalleBudgets` enveloppent encore toute erreur en `BudgetNotFoundException` (404).

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

## 🔐 Sécurité

- **Authentification** : `id_token` Google OIDC passé dans le header `Authorization: Bearer <token>`, validé par l'extension `quarkus-oidc` en mode `service` ([ADR-005](./adr/005-authentification-jwt-extension-quarkus.md), plan 004 Phase 0).
- **Autorisation** : `@RolesAllowed` avec les constantes de `*APIEnum` (ex: `USER_COMPTES`, `USER_OPERATIONS`).
- **Clé d'API** : `X-Api-Key` header validé par API Gateway AWS. `ApiKeyRequired` et l'Usage Plan sont configurés **manuellement dans la console AWS** (et non dans le template SAM).

### Validation du jeton (`quarkus-oidc`)

Configuration dans `{comptes,operations,parametrages,utilisateurs}/src/main/resources/{dev,prod}/application.properties` :

| Propriété | Valeur | Rôle |
|---|---|---|
| `quarkus.oidc.application-type` | `service` | Mode API (Bearer uniquement) |
| `quarkus.oidc.auth-server-url` | `https://accounts.google.com` | Discovery OIDC activée ; clés JWKS mises en cache et rotation gérées par l'extension |
| `quarkus.oidc.client-id` / `token.audience` | `${oidc.jwt.id.appusercontent}.apps.googleusercontent.com` | Audience attendue |
| `quarkus.oidc.token.issuer` | `any` | Google émet `accounts.google.com` **ou** `https://accounts.google.com`, alors que la discovery n'annonce que la seconde |
| `quarkus.oidc.token.principal-claim` | `email` | Identité exposée |
| `quarkus.oidc.connection-timeout` | `3S` | Durée max d'une requête discovery / JWKS |
| `quarkus.oidc.connection-delay` | `4S` | Fenêtre de retry de la connexion initiale (pire cas ~7 s < budget d'init Lambda ~10 s) |

Les propriétés `auth-server-url` et timeouts sont limitées aux profils `%dev`/`%prod` : le profil de test utilise une clé publique locale (`quarkus.oidc.public-key`) et n'appelle pas Google.

`JwtRolesAugmentor` (`communs/.../api/security/`) complète la validation de l'extension. Comme `issuer=any`, il est la **seule barrière sur l'issuer** :
- contrôle **strict** des 2 formes d'issuer (`accounts.google.com`, `https://accounts.google.com`) ; toute autre valeur est refusée (dont `evilaccounts.google.com`, `accounts.google.com.evil.com`) ;
- claim `email` requis (non vide) et `email_verified` = `true` (booléen ou chaîne `"true"`, les deux formes étant émises par Google) ;
- rôles applicatifs (`app.security.roles`, défaut `USER_COMPTES,USER_OPERATIONS,USER_UTILISATEURS`) attribués à **tout utilisateur authentifié** (pas de contrôle de propriété ni de rôle admin : hors périmètre, second temps).

`AbstractAPISecurityFilter` ne décode plus le jeton : il expose l'identité validée (email, jeton brut pour la propagation inter-µServices par `RequestJWTHeaderFactory`) et l'`X-Api-Key`.

### Retiré du chemin d'authentification

`JWTUtils`, le cache et la collection Mongo `jwks_auth_signing_keys`, le refresh des clés JWKS via `/_info` (`parametrages`) et `IJwtSigningKeyService` ne sont plus utilisés pour authentifier. Le code est **conservé jusqu'à la Phase 4** (suppression par le 👤) : inventaire dans [`PHASE_4_SUPPRESSIONS_A_FAIRE.md`](../.claude/plans/004_reports/PHASE_4_SUPPRESSIONS_A_FAIRE.md). `/_info` garde le même contrat (JSON, 200).

### Codes HTTP d'authentification

| Situation | Code |
|---|---|
| Jeton absent, invalide, expiré, mal signé, issuer/audience/email refusé (endpoints `@RolesAllowed`) | **401** (auparavant 403) |
| Endpoint `@PermitAll` **sans** jeton | 200 |
| Endpoint `@PermitAll` **avec** jeton invalide (authentification proactive) | **401** |
| JWKS / discovery Google injoignable | **500** (limite connue, voir ci-dessous) |

> ⚠️ Le passage 403 → 401 impacte l'IHM (déconnexion sur 401) : plan IHM `008_gestion_401_auth_oidc_backend`, **prérequis bloquant avant la PROD**.

### Limites connues

- **JWKS / discovery Google injoignable → HTTP 500** et non 401 (refus appliqué, sans blocage ~2 s). L'IHM ne se déconnecte pas sur ce code. Conservé (décision 👤), à observer en QUA et réévaluer en Phase 4.
- **`kid` inconnu** : non testable avec `public-key` en test, à vérifier en QUA avec le JWKS réel. Démarrage à froid, appel réel à Google et build natif : validation QUA requise.

### Logs : masquage du jeton

`quarkus-oidc` (`io.quarkus.oidc.runtime.OidcProvider`) loggue le jeton complet en WARN sur signature invalide. Masquage par configuration : `quarkus.log.category."io.quarkus.oidc.runtime.OidcProvider".level=ERROR` (présente dans les `application.properties` des modules ; tests de fuite dans `comptes`). Effet de bord : les WARN légitimes d'`OidcProvider` (jeton expiré…) ne sont plus visibles. **À revérifier à chaque montée de version de `quarkus-oidc`** (la protection repose sur la configuration seule). Résiduel : `OidcUtils` WARN `Failed to decrypt a token` (JWE uniquement, non utilisé par Google).

- **Sanitisation des entrées** : tous les path params sont sanitisés via `SecurityUtils.ESCAPE_INPUT_REGEX` avant tout traitement.
- **Données sensibles** : aucun secret dans le code source ; tout via variables d'environnement injectées par SAM/GitHub Secrets.
- **Réseau** : les µServices ne sont accessibles qu'au travers d'API Gateway (pas d'exposition directe).

---

## 🗄️ Base de Données

- **Type** : MongoDB (NoSQL, documents JSON)
- **Driver** : Quarkus MongoDB Panache — pattern **Repository** (pas Active Record)
- **Base de données dev** : `v12-app-dev` (localhost:27017)
- **Base de données prod** : variable d'environnement `QUARKUS_MONGODB_DATABASE`
- **Configuration** :
  - Dev : `src/main/resources/dev/application.properties` dans chaque module
  - Prod : `src/main/resources/prod/application.properties` + SAM template

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

Définis dans le bloc Mongo des 8 `application.properties` (`{comptes,operations,parametrages,utilisateurs}/src/main/resources/{dev,prod}/`) :

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
| Intégration Mongo (`communs`) | @QuarkusTest + Dev Services MongoDB | `communs/src/test/java/.../migrations/` | `mvn test` (Docker requis) |
| Couverture | JaCoCo | `target/jacoco-report/` | `mvn verify -Psonar` |

Les tests `*MongoTest` démarrent leur propre conteneur MongoDB (`MongoDbTestResource`, branché uniquement par le profil de test de la classe, `quarkus.mongodb.devservices.enabled=false`) : ils sont indépendants du profil Maven. Sans Docker, ils échouent (ils ne s'ignorent pas en silence) ; contournement en développement local : `-Dtest='!*MongoTest'`. Les tests Mongo de `communs` (réservation atomique, concurrence de N runners, bail, index V002) s'appuient sur Dev Services.

```bash
# Tous les tests du projet
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

---

## 🚀 Déploiement

| Environnement | Déclencheur CI |
|---|---|
| QUA (staging) | Push / PR sur `master` |
| PROD | Push d'un tag `v*` |

### Pipeline CI/CD (GitHub Actions)

```
1. Build communs        → mvn clean install + publish sur GitHub Packages
2. Génération SAM       → sed sur samconfig.template.toml (stack_name/s3_prefix uniquement)
3. Build µServices (parallèle)
   ├── parametrages    → mvn package -Pnative + build image Docker native
   ├── utilisateurs    → mvn package -Pnative + build image Docker native
   ├── comptes         → mvn package -Pnative + build image Docker native
   └── operations      → mvn package -Pnative + build image Docker native
4. Déploiement SAM      → sam deploy --parameter-overrides (Lambda + API Gateway)
5. SonarCloud           → mvn verify -Psonar (sur master uniquement)
```

> ℹ️ **Tests `*MongoTest` et builds natifs** : la commande `mvn install -B -P native` des workflows `build-on-master.yml` et `build-on-tags.yml` exclut les `*MongoTest` (`-Dtest='!*MongoTest' -Dsurefire.failIfNoSpecifiedTests=false`) : ils ne sont pas rejoués avant la compilation native. Ils restent exécutés par `build-on-all` et par le job Sonar. Les autres tests d'intégration Mongo de `communs` (`TestMigrationRepository*`) ne sont pas exclus.

```bash
# Déploiement manuel (depuis communs/src/aws-deploy/)
sam deploy --config-file samconfig.template.toml --parameter-overrides \
  Env=QUA Version=<version> DatabaseUrl=<url> DatabaseName=<db> \
  AppConfigUrlIhm=<url> AppConfigUrlBackends=<url> \
  OidcJwtIdAppUserContent=<id> QuarkusLogLevel=INFO MongodbLogLevel=INFO
```

### Renovate — synchronisation version Quarkus (pom.xml vs docs)

Le job `build-communs` (`build-on-master.yml`) inclut une étape *"Check Quarkus version sync"* qui échoue si `quarkus.platform.version` (`pom.xml`) diverge des mentions `Quarkus X.Y.Z` codées en dur dans `README.md`, ce document, `.claude/instructions/dev.instructions.md` et `.claude/instructions/orchestrator.instructions.md`.

Pour éviter que Renovate ne bump `pom.xml` seul (automerge minor/patch actif) sans mettre à jour ces 4 fichiers, `renovate.json` déclare un `customManager` regex qui suit les mêmes fichiers avec le `depName`/`datasource` de `quarkus-bom` (`io.quarkus:quarkus-bom`, datasource `maven`) — Renovate regroupe ainsi la mise à jour doc et pom dans la même PR, et le check CI ne peut plus diverger. Voir [`.claude/plans/002_fix_desync_quarkus_renovate.plan.md`](../.claude/plans/002_fix_desync_quarkus_renovate.plan.md).

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
# Nécessite GraalVM/Mandrel installé
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
