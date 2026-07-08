# 🏗️ Architecture — gestion-budget-serverless

---

## 🎯 Vue d'ensemble

**gestion-budget-serverless** est le backend de l'application de gestion de budget personnelle. Il expose une API REST
JSON consommée par le frontend React [`gestion-budget-ihm`](https://github.com/vzwingma/gestion-budget-ihm). Les
microservices sont compiles en binaires natifs GraalVM et deployes sous forme de fonctions **AWS Lambda** via SAM.

| Propriété | Valeur |
|---|---|
| **Type** | Backend – API REST serverless |
| **Stack principale** | Java 25 (Mandrel 25 natif) + Quarkus 3.37.1 + Mutiny + MongoDB Panache |
| **Plateforme cible** | AWS Lambda (natif GraalVM/Mandrel) |
| **Version applicative** | 24.0.0-SNAPSHOT |
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
        → Filtre JWT (AbstractAPISecurityFilter)
        → Ressource REST (JAX-RS)
        → Service métier (Mutiny Uni<T>)
        → Repository MongoDB Panache
        ← MongoDB Atlas / DocumentDB
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
  │   ├── RootAPIResource.java  ← Endpoint /info + refresh JWKS
  │   ├── JwtSecurityFilter.java← Filtre JWT (surcharge AbstractAPISecurityFilter)
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
│   │   │   │   ├── AbstractAPISecurityFilter.java # Validation JWT
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
│   │       │   ├── JWTUtils.java                 # Parsing JWT
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
| Framework | Quarkus | **3.37.1** | Runtime Lambda natif |
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

## 🔄 Intégrations Externes

| Système | Type | Rôle |
|---|---|---|
| MongoDB Atlas / DocumentDB | NoSQL | Persistence des données (budgets, opérations, comptes) |
| AWS Lambda | FaaS | Exécution des microservices (binaires natifs) |
| AWS API Gateway | HTTP | Point d'entrée, routage, X-Api-Key |
| Google OAuth2 / JWKS | JWT | Authentification utilisateurs |
| µService `comptes` | REST interne | Appelé par `operations` via `IComptesServiceProvider` |
| µService `parametrages` | REST interne | Appelé par `operations` via `IParametragesServiceProvider` |
| GitHub Packages | Maven registry | Distribution du module `communs` |

---

## 🔐 Sécurité

- **Authentification** : Token JWT Google OAuth2 passé dans le header `Authorization: Bearer <token>`.
- **Autorisation** : `@RolesAllowed` avec les constantes de `*APIEnum` (ex: `USER_COMPTES`, `USER_OPERATIONS`).
- **Validation JWT** : `AbstractAPISecurityFilter` vérifie la signature via les clés JWKS stockées en MongoDB (cache) et récupérées depuis Google.
- **Clé d'API** : `X-Api-Key` header validé par API Gateway AWS.
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
| `budgets` | `operations` | Budgets mensuels |
| `operations` | `operations` | Lignes d'opérations budgétaires |
| `categories` | `parametrages` | Catégories et sous-catégories d'opérations |
| `utilisateurs` | `utilisateurs` | Profils et préférences utilisateurs |
| `jwkskeys` | `communs` (tous) | Clés JWKS Google (cache) |
| `_migrations` | `communs` (tous) | Suivi d'exécution des migrations MongoDB (voir section [Migrations MongoDB](#-migrations-mongodb)) |

---

## 🔀 Migrations MongoDB

Mécanisme maison (module `communs`, package `migrations/`) — voir [ADR-002](./adr/002-migrations-mongodb-maison.md) pour le détail du choix (Mongock écarté : risque incompatibilité GraalVM native-image).

- **Déclenchement** : automatique au démarrage de chaque microservice, via `MongoMigrationRunner` (`@ApplicationScoped`, `@Observes StartupEvent`). Migrations découvertes par injection CDI standard (`Instance<IMongoMigration>`), triées par version croissante, exécutées séquentiellement.
- **Convention de nommage** : `V<numéro sur 3 chiffres>_<description courte>` (ex. `V001_InitMigrationsCollection`, `V002_AjoutIndexComptes`). Numéro unique, strictement croissant, jamais réutilisé ni modifié une fois publié.
- **Où ajouter une migration** : nouvelle classe `@ApplicationScoped` implémentant `IMongoMigration` dans `communs/src/main/java/.../communs/migrations/scripts/`. `V001_InitMigrationsCollection.java` sert de gabarit.
- **Idempotence** : obligatoire. Le runner garantit la non ré-exécution d'une migration déjà en statut `SUCCES` (suivi dans la collection `_migrations`), mais le code de chaque migration doit rester défensif (ex. vérifier l'existence d'un index avant de le créer).
- **Traçabilité** : chaque exécution (succès ou échec) est enregistrée dans `_migrations` (`MigrationRecord` : version, description, date, statut). Une migration en échec est journalisée en erreur mais ne bloque ni le démarrage de l'application ni l'exécution des migrations suivantes.

---

## 🧪 Tests

| Type | Framework | Emplacement | Commande |
|---|---|---|---|
| Unitaires (service) | JUnit 5 + Mockito | `src/test/java/` | `mvn test` |
| Intégration (REST) | @QuarkusTest + REST Assured | `src/test/java/` | `mvn test` |
| Couverture | JaCoCo | `target/jacoco-report/` | `mvn verify -Psonar` |

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
