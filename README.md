# Micro-services Budget

Backend serverless de gestion du budget. Les microservices Quarkus exposent une API REST JSON et sont déployes sous
forme de fonctions AWS Lambda natives.

| Module                                                            | Version                                                                                                                                               |
|-------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| [IHM](https://github.com/vzwingma/gestion-budget-ihm)             | [![GitHub version](https://badge.fury.io/gh/vzwingma%2Fgestion-budget-ihm.svg)](https://badge.fury.io/gh/vzwingma%2Fgestion-budget-ihm)               |
| [Services](https://github.com/vzwingma/gestion-budget-serverless) | [![GitHub version](https://badge.fury.io/gh/vzwingma%2Fgestion-budget-serverless.svg)](https://badge.fury.io/gh/vzwingma%2Fgestion-budget-serverless) |

[![Architecture diagram](https://gitdiagram.com/diagram-badge.svg)](https://gitdiagram.com/vzwingma/gestion-budget-serverless?utm_source=readme&utm_medium=badge)

### Statut

[![Build Status](https://github.com/vzwingma/gestion-budget-serverless/actions/workflows/build-on-master.yml/badge.svg)](https://github.com/vzwingma/gestion-budget-serverless/actions/workflows/build-on-master.yml)
[![Build Status](https://github.com/vzwingma/gestion-budget-serverless/actions/workflows/build-on-tags.yml/badge.svg)](https://github.com/vzwingma/gestion-budget-serverless/actions/workflows/build-on-tags.yml)
[![GitHub issues](https://img.shields.io/github/issues-raw/vzwingma/gestion-budget-serverless.svg?style=flat-square)](https://github.com/vzwingma/gestion-budget-serverless/issues)

[![Known Vulnerabilities](https://snyk.io/test/github/vzwingma/gestion-budget-serverless/badge.svg?targetFile=pom.xml)](https://snyk.io/test/github/vzwingma/gestion-budget-serverless)
[![Dépendences](https://img.shields.io/librariesio/github/vzwingma/gestion-budget-serverless)](https://libraries.io/github/vzwingma/gestion-budget-serverless)

<a href="https://sonarcloud.io/dashboard?id=vzwingma_gestion-budget-serverless"><img alt="Sonar Build Status" src="https://sonarcloud.io/api/project_badges/measure?project=vzwingma_gestion-budget-serverless&metric=coverage" /></a>
<a href="https://sonarcloud.io/dashboard?id=vzwingma_gestion-budget-serverless"><img alt="Sonar Build Status" src="https://sonarcloud.io/api/project_badges/measure?project=vzwingma_gestion-budget-serverless&metric=sqale_rating" /></a>
<a href="https://sonarcloud.io/dashboard?id=vzwingma_gestion-budget-serverless"><img alt="Sonar Build Status" src="https://sonarcloud.io/api/project_badges/measure?project=vzwingma_gestion-budget-serverless&metric=reliability_rating" /></a>
<a href="https://sonarcloud.io/dashboard?id=vzwingma_gestion-budget-serverless"><img alt="Sonar Build Status" src="https://sonarcloud.io/api/project_badges/measure?project=vzwingma_gestion-budget-serverless&metric=security_rating" /></a>

### Frameworks utilisés

- Quarkus **4.0.0** / Java **25**
- Mutiny ([guide](https://quarkus.io/guides/mutiny)): A reactive programming framework for Java.
- MongoDB with Panache ([guide](https://quarkus.io/guides/mongodb-panache)): Simplify your persistence code for MongoDB
  via the active record or the repository pattern
- RESTEasy JAX-RS ([guide](https://quarkus.io/guides/rest-json)): REST endpoint framework implementing JAX-RS and more
- Micrometer metrics ([guide](https://quarkus.io/guides/micrometer)): Instrument the runtime and your application with
  dimensional metrics using Micrometer.

---

## Structure du projet

Projet Maven multi-modules :

| Module          | Description                             | Port dev |
|-----------------|-----------------------------------------|----------|
| `communs`       | Bibliothèque partagée (modèles, sécurité, exceptions) | –    |
| `parametrages`  | Paramètres système → `/parametres/v2/`  | 8091     |
| `utilisateurs`  | Auth / profils utilisateur → `/utilisateurs/v2/` | 8092 |
| `comptes`       | Comptes bancaires → `/comptes/v2/`      | 8093     |
| `operations`    | Budgets et opérations → `/budgets/v2/`  | 8094     |

---

## Prérequis

- **Java 25**
- **Apache Maven 3.9+**
- **MongoDB** accessible (local sur `localhost:27017` ou Atlas)
- Pour les tests Mongo réels : Docker fonctionnel et accès à l'image Testcontainers `mongo:7.0` ; les tests de migrations nécessitent aussi un MongoDB de test accessible ou Dev Services.
- Pour le build natif CI : Docker et image builder Mandrel JDK 25 ; SAM utilise aussi Docker.
- *(Optionnel)* **VS Code** avec les extensions [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) et [Quarkus](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-quarkus)

---

## Démarrage en mode développement

### Via le terminal

```bash
# Un seul microservice
mvn quarkus:dev -f parametrages/pom.xml

# Ou depuis le répertoire du module
cd parametrages && mvn quarkus:dev
```

La Dev UI est disponible sur [http://localhost:8091/q/dev](http://localhost:8091/q/dev).

### Via VS Code (tâches préconfigurées)

Les tâches sont définies dans [.vscode/tasks.json](.vscode/tasks.json).

1. Créez un fichier **`.env`** à la racine du projet (ignoré par git) avec les variables d'environnement :

```dotenv
QUARKUS_MONGODB_CONNECTION_STRING=mongodb+srv://<login>:<mdp>@<cluster>.mongodb.net/
QUARKUS_MONGODB_DATABASE=<database>
```

2. Lancez via **Terminal → Run Task** :

| Tâche | Description |
|---|---|
| `Quarkus Dev - ALL` | Lance les 4 microservices en parallèle |
| `Quarkus Dev - parametrages` | Démarre uniquement `parametrages` |
| `Quarkus Dev - comptes` | Démarre uniquement `comptes` |
| `Quarkus Dev - operations` | Démarre uniquement `operations` |
| `Quarkus Dev - utilisateurs` | Démarre uniquement `utilisateurs` |

### Débogage avec breakpoints

1. Démarrer le microservice en mode debug :

```bash
mvn quarkus:dev -Ddebug -f parametrages/pom.xml
```

2. Dans VS Code, créer `.vscode/launch.json` et attacher le débogueur sur le port **5005** :

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug Quarkus (attach)",
      "request": "attach",
      "hostName": "localhost",
      "port": 5005
    }
  ]
}
```

---

## Build

```bash
# Construire tous les modules (mode JVM)
mvn clean package

# Construire un seul module
mvn clean package -f comptes/pom.xml

# Construire l'exécutable natif Linux pour Lambda (Docker et image builder native)
mvn clean package -Pnative -Dquarkus.native.container-build=true
```

## Tests

La CI JVM utilise explicitement le profil `ci-sans-mongo` :

```bash
mvn clean install -B -Pci-sans-mongo --file communs/pom.xml
mvn clean install -B -Pci-sans-mongo --file pom.xml
```

Surefire exclut cinq classes : `TestMigrationRepository`, `TestMigrationRepositoryConcurrence`,
`TestMigrationRepositoryPersistence`, `UtilisateurDatabaseAdaptorMongoTest` et
`OperationDatabaseAdaptorMongoTest`. Il définit aussi la propriété système
`quarkus.mongodb.devservices.enabled=false` pendant les tests. Sans `ci-sans-mongo`, le comportement par
défaut reste inchangé. Cette CI ne valide pas l'atomicité, la persistance ni les index MongoDB.

Ce réglage répond à un run Sonar où le runner CI a effectivement lancé MongoDB Dev Services, puis où le
job est resté bloqué lors de la fermeture du client MongoDB. Le lien causal n'est pas établi.
La disparition du blocage doit être confirmée par un run CI complet avec Docker.

Pour exécuter séparément ces tests **sans** `ci-sans-mongo`, avec Java 25 et Maven depuis la racine,
utiliser les commandes proposées ci-dessous (non exécutées lors de l'upgrade) :

```bash
# Installer communs pour les modules dépendants
mvn install -B -DskipTests --file communs/pom.xml
mvn test -B --file communs/pom.xml -Dtest=TestMigrationRepository,TestMigrationRepositoryConcurrence,TestMigrationRepositoryPersistence
mvn test -B --file utilisateurs/pom.xml -Dtest=UtilisateurDatabaseAdaptorMongoTest
mvn test -B --file operations/pom.xml -Dtest=OperationDatabaseAdaptorMongoTest
```

Les deux tests d'adaptateurs démarrent un MongoDB `mongo:7.0` via Testcontainers et Docker. Pour les
trois tests de `communs`, prévoir un MongoDB de test joignable ou Dev Services avec Docker, et vérifier
la configuration de connexion effective : une chaîne configurée peut empêcher Dev Services de démarrer.
Utiliser une base de test isolée. Ces tests Mongo réels n'ont pas été validés lors de l'upgrade.

```bash
# Tous les tests, y compris Mongo réel : prérequis ci-dessus
mvn clean test

# Tests d'un seul module
mvn clean test -f operations/pom.xml

# Une classe de test spécifique
mvn clean test -Dtest=ComptesServiceTest

# Une méthode de test spécifique
mvn clean test -Dtest=ComptesServiceTest#testGetComptes
```

## Analyse qualité (SonarCloud)

```bash
mvn verify -Psonar
```

---

## Déploiement AWS

Les microservices sont déployés sur **AWS Lambda** (256 Mo, natif GraalVM) via **SAM**. Les templates sont dans `communs/src/aws-deploy/`. Le paramétrage (URL base, secrets OIDC, niveaux de log...) passe par des `Parameters` SAM injectés au déploiement via `--parameter-overrides` (voir [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md#-déploiement)), plus de placeholders texte remplacés par `sed`.

Dans le pipeline **QUA**, une version d'affichage commune est calculée à partir du numéro majeur du POM racine et de l'heure courante en Europe/Paris (`majeur.yyyyMMdd-HHmm`). Elle est injectée au build Quarkus et transmise à SAM pour la description des Lambda ; elle alimente notamment `/_info` et la version du document OpenAPI. Ce calcul ne modifie ni la version ni les coordonnées Maven, et ne change pas le pipeline **PROD**. Voir le détail dans [Architecture — Déploiement](docs/ARCHITECTURE.md#-déploiement).

```bash
# Déploiement (depuis communs/src/aws-deploy/)
sam deploy --config-file samconfig.template.toml --parameter-overrides \
  Env=QUA Version=<version> DatabaseUrl=<url> DatabaseName=<db> \
  AppConfigUrlIhm=<url> AppConfigUrlBackends=<url> \
  OidcJwtIdAppUserContent=<id> QuarkusLogLevel=INFO MongodbLogLevel=INFO
```

Voir le [Wiki – Opérations sur AWS](https://github.com/vzwingma/gestion-budget-serverless/wiki/Opérations-sur-AWS) pour le détail complet.

---

## Documentation

- [Wiki du projet](https://github.com/vzwingma/gestion-budget-serverless/wiki)
- [Conception globale](https://github.com/vzwingma/gestion-budget-serverless/wiki/Conception-globale)
- [API Comptes](https://github.com/vzwingma/gestion-budget-serverless/wiki/Conception-µS-Comptes)
- [API Opérations](https://github.com/vzwingma/gestion-budget-serverless/wiki/Conception-µS-Operations)
- [API Paramétrages](https://github.com/vzwingma/gestion-budget-serverless/wiki/Conception-µS-Parametrages)
- [API Utilisateurs](https://github.com/vzwingma/gestion-budget-serverless/wiki/Conception-µS-Utilisateurs)
