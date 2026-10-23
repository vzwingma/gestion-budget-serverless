---
name: conventions-architect
description: "Conventions architecturales du projet gestion-budget-serverless pour 🟠 ARCos : couches, état, accès réseau, routage, bibliothèques autorisées, intégrations externes. Prioritaire sur le générique."
user-invocable: false
---

# Conventions projet — gestion-budget-serverless (architecture)

> Projet : `gestion-budget-serverless` — backend Quarkus/Java 25 multi-modules Maven, architecture hexagonale,
> Mutiny + MongoDB Panache, déployé en images natives sur AWS Lambda (SAM).

## Document d'architecture

Lire `docs/ARCHITECTURE.md` et les ADR `docs/adr/` (001 modernisation stack, 002 migrations Mongo maison,
003 Java 25/Mandrel 25, 004 injection `Clock`). Toute proposition reste cohérente avec l'existant.

## Conventions architecturales

- **Modules Maven** : `communs` (bibliothèque partagée, buildée et publiée en premier sur GitHub Packages) puis
  µServices indépendants `parametrages`, `utilisateurs`, `comptes`, `operations`.
- **Couches** (par µService) : `api/` (REST JAX-RS, `api/enums`, `api/override`) → `business/` (services
  `@ApplicationScoped`, `business/ports/`, `business/model/`) → `spi/` (adaptateurs Mongo Panache, clients REST
  inter-µServices) ; `config/` (OpenAPI, hints réflexion GraalVM), `utils/`.
- **Ports & adapters** : chaque couche dépend de la suivante via **interface** (`IXxxAppProvider`,
  `IXxxRepository`, `IXxxServiceProvider`) ; injection CDI de l'interface, jamais de l'implémentation.
- **État partagé** : aucun état applicatif en mémoire (Lambda sans état) ; persistance MongoDB uniquement ;
  contexte de requête via `BusinessTraceContext` (style MDC, réinitialisé après chaque réponse).
- **Accès réseau / données** : Mongo via `spi/XxxDatabaseAdaptor` ; autre µService via `spi/IXxxServiceProvider`
  (client REST Quarkus, JWT propagé par `RequestJWTHeaderFactory`) ; jamais d'appel direct depuis `api/`.
- **Réactif** : tout service et accès DB retourne `Uni<T>` / `Multi<T>` (Mutiny), jamais bloquant hors tests.
- **Routage / points d'entrée** : JAX-RS, chemins + rôles déclarés dans `api/enums/XxxAPIEnum.java`
  (source de vérité pour l'IHM) ; bases `/parametres/v2/`, `/utilisateurs/v2/`, `/comptes/v2/`, `/budgets/v2/`,
  `/budgets/v2/admin/` ; routes API Gateway/Lambda dans `communs/src/aws-deploy/` (SAM).
- **Sécurité** : surcharge `AbstractAPISecurityFilter` + `IJwtSecurityContext` par µService (JWT Google OIDC,
  JWKS en cache Mongo), `@RolesAllowed` avec constantes `*APIEnum` ; `X-Api-Key` validé par API Gateway.
- **Migrations Mongo** : mécanisme maison `communs/.../migrations/` (`MongoMigrationRunner`, `IMongoMigration`,
  `V<NNN>_<Description>`, idempotent, collection `_migrations`) — ADR-002.
- **Bibliothèques autorisées** : plateforme Quarkus (BOM), Mutiny, MongoDB Panache, Lombok, MicroProfile OpenAPI ;
  tests JUnit 5 / Mockito / REST Assured / JaCoCo. Toute dépendance doit rester compatible GraalVM native-image ;
  nouvelle dépendance structurante = ADR.

## Décisions d'architecture (ADR)

- Fichier `docs/adr/NNN-titre-court.md` pour chaque décision majeure : nouvelle dépendance structurante,
  changement de pattern réactif ou hexagonal, décision de sécurité, évolution majeure du modèle MongoDB,
  évolution du template SAM.
- ARCos prépare le contenu ; 🟣 DOCly rédige le fichier (skill `adr-writing`).

## Intégrations externes

- **IHM `gestion-budget-ihm`** (React/TS) : contrat (URL, paramètres, codes retour) défini avec l'architecte IHM
  **avant** implémentation ; toute évolution de modèle → vérifier l'impact côté IHM.
- **Google OIDC** (JWT/JWKS), **MongoDB** (Atlas/local), **AWS** Lambda + API Gateway (SAM).
- Configuration : `<module>/src/main/resources/{dev,prod}/application.properties` ; variables d'environnement
  `QUARKUS_MONGODB_CONNECTION_STRING`, `QUARKUS_MONGODB_DATABASE` ; paramètres SAM (`Parameters`,
  `--parameter-overrides`, `NoEcho` pour secrets) dans `communs/src/aws-deploy/sam.native.template.yaml`.

## Handoff vers ⚫ MAINa

ARCos ne crée ni tâches ni plan. Il livre : analyse comparative (≥ 2 options) + recommandation, puis
découpage **candidat** par module Maven (tâches logiques + dépendances, `communs` en premier + effort) comme
entrée du Plan d'Action.
