# ADR 010 — Observabilité par les logs CloudWatch et retrait d'OpenTelemetry

---

**Date :** 2026-11-28
**Statut :** Proposée
**Décideurs :** 🟠 ARCos + 👤 Développeur humain

---

## Contexte

- `quarkus-opentelemetry` est déclaré dans le pom racine, sans aucune configuration `quarkus.otel.*` et sans aucun usage de l'API.
- Depuis Quarkus 4.0, les traces, métriques et logs OTel sont **activés par défaut** et exportés en OTLP vers `localhost:4318`. Aucun collector n'existe dans les Lambdas (SAM sans layer ni `Tracing`) : **tout est perdu**.
- L'instrumentation par logs existe déjà ([ADR-009](./009-mutualisation-communs-et-resilience-appels-inter-services.md)) : `[HTTP-METRIC]`, `[REST-METRIC]`, lignes `REPORT` des Lambdas, requêtes Logs Insights.
- `quarkus.mongodb.metrics.enabled=true` n'a aucun effet sans Micrometer.
- Le contournement du bug #55278 est corrigé depuis Quarkus 3.37.2, et `false` est redevenu la valeur par défaut en 3.39 et 4.0.
- L'[ADR-001](./001-strategie-modernisation-stack.md) avait pré-choisi X-Ray si l'axe observabilité était repris.
- Usage familial : le coût et le démarrage à froid sont prioritaires.

---

## Décision

**Nous avons décidé de** retirer OpenTelemetry et de faire des logs CloudWatch la source unique d'observabilité, avec une corrélation inter-services par l'en-tête `X-Amzn-Trace-Id` (Gate #0 👤).

- Retirer `quarkus-opentelemetry` et la propriété `quarkus.mongodb.metrics.enabled`.
- Corrélation inter-services :
  - `Root=` extrait de `X-Amzn-Trace-Id` par `AbstractAPIInterceptors`, validé par la regex stricte `^1-[0-9a-f]{8}-[0-9a-f]{24}$`, sinon ignoré (anti-injection de logs) ;
  - placé dans le MDC sous la clé `awsTraceId` ;
  - le format de log utilise `%X{awsTraceId}` à la place de traceId / parentId / spanId ;
  - `operations` propage l'en-tête vers `comptes` et `parametrages` via `RequestJWTHeaderFactory`.
- Bug #55278 : garder `false` **explicitement** (P2), car la valeur par défaut a déjà basculé trois fois.
- Pas de X-Ray (pas de `Tracing: Active`), pas de logs JSON.

---

## Alternatives Considérées

### Option 1 : Retrait d'OTel, logs seuls + corrélation `X-Amzn-Trace-Id` ✅ Retenue

- **Avantages** : image native et initialisation allégées ; plus aucun export vers un endpoint inexistant ; corrélation explicite ; coût AWS nul ; réutilise l'instrumentation par logs existante.
- **Inconvénients** : pas de spans Mongo ni REST ; corrélation maison à maintenir.

### Option 2 (O1b) : OTel réduit à la corrélation

- **Avantages** : corrélation standard.
- **Inconvénients** : effet non prouvé derrière `quarkus-amazon-lambda-rest` ; SDK OTel conservé.
- **Raison du rejet** : bénéfice incertain pour un coût de démarrage conservé.

### Option 3 (O2) : X-Ray natif Lambda

- **Avantages** : service managé, intégration native.
- **Inconvénients** : apport limité par rapport aux lignes `REPORT` ; fin de support du SDK et du daemon en 2027.
- **Raison du rejet** : refusé par le 👤 (apport limité, support à terme).

### Option 4 (O3) : ADOT (AWS Distro for OpenTelemetry)

- **Avantages** : export OTel complet.
- **Inconvénients** : aucun layer pour `provided.al2023` ; démarrage à froid dégradé ; coût.
- **Raison du rejet** : incompatible avec la priorité coût et démarrage à froid.

### Option 5 (O4) : logs JSON

- **Avantages** : champs structurés pour Logs Insights.
- **Inconvénients** : changement de format de tous les logs.
- **Raison du rejet** : reporté, la commande `parse` suffit.

---

## Conséquences

### Positives
- Image native et initialisation allégées.
- Plus aucun export vers un endpoint inexistant.
- Corrélation explicite entre services.
- Coût AWS nul.

### Négatives / Compromis
- Pas de spans Mongo ni REST.
- Corrélation maison à maintenir.
- Tout retour vers OTel demandera un collector.

### Neutres
- `docs/ARCHITECTURE.md` et les ADR-001, 003, 009 à mettre à jour.
- Plan 001, tâche T8.1 : clôturée.

---

## Mise en œuvre

- **Fichiers impactés** : pom racine (dépendance `quarkus-opentelemetry`), `application.properties` / `microprofile-config.properties` (format de log, `quarkus.mongodb.metrics.enabled`, valeur explicite du #55278), `AbstractAPIInterceptors`, `RequestJWTHeaderFactory` (`operations`).
- **Tâches de suivi** : DEVon — T3.2a, T3.2b, T3.3, T3.5 (plan 004, Phase 3) ; DOCly — mise à jour de `ARCHITECTURE.md` et des ADR liés.
- **Date d'effet** : Phase 3 du plan 004, après Gate #1.

---

## Références

- Plan d'Action : [`004_revue_architecture_backend`](../../.claude/plans/004_revue_architecture_backend.plan.md) (Phases 3 et 4)
- [ADR-001](./001-strategie-modernisation-stack.md), [ADR-003](./003-upgrade-java25-mandrel25.md), [ADR-009](./009-mutualisation-communs-et-resilience-appels-inter-services.md)
