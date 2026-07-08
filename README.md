# Micro-services Budget

Backend serverless de gestion du budget. Les microservices Quarkus exposent une API REST JSON et sont déployes sous
forme de fonctions AWS Lambda natives.

| Module                                                            | Version                                                                                                                                               |
|-------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| [IHM](https://github.com/vzwingma/gestion-budget-ihm)             | [![GitHub version](https://badge.fury.io/gh/vzwingma%2Fgestion-budget-ihm.svg)](https://badge.fury.io/gh/vzwingma%2Fgestion-budget-ihm)               |
| [Services](https://github.com/vzwingma/gestion-budget-serverless) | [![GitHub version](https://badge.fury.io/gh/vzwingma%2Fgestion-budget-serverless.svg)](https://badge.fury.io/gh/vzwingma%2Fgestion-budget-serverless) |

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

- Quarkus **3.37.2** / Java **21**
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

- **Java 25** (Mandrel 25 pour le build natif)
- **Apache Maven 3.9+**
- **MongoDB** accessible (local sur `localhost:27017` ou Atlas)
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

# Construire l'exécutable natif Linux pour Lambda (nécessite GraalVM/Mandrel)
mvn clean package -Pnative -Dquarkus.native.container-build=true
```

## Tests

```bash
# Tous les tests
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
