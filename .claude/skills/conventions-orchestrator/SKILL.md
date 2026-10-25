---
name: conventions-orchestrator
description: "Conventions projet gestion-budget-serverless pour ⚫ MAINa : orchestration, gates humains, contenu des délégations. Prioritaire sur le générique."
user-invocable: false
---

# Conventions projet — gestion-budget-serverless (orchestration)

> Projet : `gestion-budget-serverless` — backend Quarkus/Java 25 multi-modules Maven, architecture hexagonale,
> Mutiny + MongoDB Panache, déployé en images natives sur AWS Lambda (SAM).

## Contexte à vérifier avant délégation

- `README.md`, `docs/ARCHITECTURE.md`, ADR existants (`docs/adr/`)
- Plans existants `.claude/plans/` (index `README.md`), `pom.xml` (versions), `*APIEnum.java` (contrats API)

## Règles d'orchestration propres au projet

- Plan d'Action persistant (`.claude/plans/`) obligatoire pour toute demande menant à une modification de
  code, sauf dispense explicite du 👤.
- Consulter 🟠 ARCos pour toute décision d'architecture ou changement structurel (nouveau module Maven, nouvelle
  interface de port, évolution modèle MongoDB / migration, nouveau pattern réactif, évolution template SAM).
- Gates #0 → #4 obligatoires ; décisions consignées dans le journal des gates du plan.
- Dépendances de tâches : `communs` d'abord, µServices ensuite ; QA et Doc dépendent du code.
- Évolution de contrat API → coordination avec `gestion-budget-ihm` signalée au 👤.

## Contenu des délégations

### Vers 🟠 ARCos
Inclure : besoin, contraintes Quarkus / Mutiny / CDI / GraalVM native, module(s) Maven et couches impactées
(`api`/`business`/`spi`), exigences non fonctionnelles, liens `docs/ARCHITECTURE.md` et ADR pertinents.
Attendu : ≥ 2 options, avantages/inconvénients/risques/impacts, recommandation, besoin d'ADR éventuel.

### Vers 🔵 DEVon
Inclure : phase validée, module(s) Maven et fichiers cibles, interface de port à déclarer d'abord, comportement
attendu, contraintes (`Uni<T>`/`Multi<T>`, CDI uniquement, `Clock` injecté, pas de `.await()` en prod),
interdiction d'élargir le scope, commandes de vérification (`mvn clean install -f communs/pom.xml`,
`mvn clean package`, `mvn clean test -f <module>/pom.xml` ; pas de lint configuré).
Attendu : code focalisé, liste des fichiers modifiés, hypothèses, preuves de vérification.

### Vers 🟢 QALvin
Inclure : changements DEVon, cas nominaux, erreurs (400/401/403/404/405/423), limites, services/ressources REST
à couvrir, commande ciblée `mvn clean test -Dtest=...` / `mvn verify -Psonar -f <module>/pom.xml`.
Attendu : tests créés/modifiés, résultats, couverture JaCoCo (≥ 80 % code modifié), points bloquants.

### Vers 🟣 DOCly
Inclure : changements publics (endpoints, contrats API), décisions d'architecture, fichiers modifiés, pages wiki
serverless impactées, entrée d'historique éventuelle.
Attendu : docs et wiki synchronisés sans réécriture inutile, liens cohérents, ADR si décision majeure, versions
vérifiées dans `pom.xml`.

## Ce que MAINa ne fait pas

- Coder, tester ou documenter à la place des agents (sauf tâche triviale explicitement demandée).
- Décider une architecture majeure sans ARCos ni validation 👤.
- Clôturer une initiative sans validation 👤 des livrables.
- Inventer des conventions absentes du code ou de la documentation.
