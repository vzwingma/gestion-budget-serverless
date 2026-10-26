# ADR 006 — Réservation atomique par version des migrations MongoDB (exécution concurrente multi-Lambda)

---

**Date :** 2026-10-26
**Statut :** Proposée
**Décideurs :** 🟠 ARCos + 👤 Développeur humain

---

## Contexte

L'[ADR-002](./002-migrations-mongodb-maison.md) a introduit un mécanisme de migrations MongoDB maison, déclenché au démarrage de chaque microservice (`@Observes StartupEvent`) et tracé dans la collection `_migrations`. L'audit de la revue d'architecture backend (Plan [004](../../.claude/plans/004_revue_architecture_backend.plan.md), Phase 1, tâches T1.2a et T1.2b) relève deux défauts de fiabilité de ce mécanisme.

**F6 — exécution concurrente sans verrou** (`MongoMigrationRunner.java:50-69`) : les 4 services (`comptes`, `operations`, `parametrages`, `utilisateurs`) embarquent le module `communs` et lancent les migrations à **chaque cold start**, en asynchrone et sans aucun verrou. Plusieurs Lambdas démarrant simultanément (déploiement, montée en charge) peuvent donc exécuter la même migration en parallèle.

**F6b — rejeu après échec** : `MigrationRecord` porte `@BsonId version` et `MigrationRepository.persist` effectue un simple `insert`. Après un enregistrement en statut `ECHEC`, le succès suivant de la même migration lève une `DuplicateKey` : l'enregistrement ne peut pas passer en `SUCCES`, et la migration est **rejouée à chaque cold start**.

Contrainte de contexte : binaires natifs GraalVM/Mandrel sur AWS Lambda, environnements dev/QUA/prod sur MongoDB (Atlas), instances Lambda pouvant être figées ou interrompues à tout moment.

---

## Décision

**Nous avons décidé de** réserver chaque version de migration de façon atomique avant exécution, par un `findOneAndUpdate` conditionnel sur `_migrations`, avec un état `EN_COURS`, un champ `owner` et un bail de 5 minutes.

Mécanisme (option A3) :

- **Réservation** par version `V` :
  `findOneAndUpdate({_id: V, $or: [{statut: ECHEC}, {statut: EN_COURS, dateExecution < now - bail}]}, {$set: {statut: EN_COURS, owner, dateExecution, description}}, upsert)`.
- Une erreur **E11000** (clé dupliquée) signifie que la version est déjà prise (`SUCCES` ou `EN_COURS` non expirée) : le runner n'exécute pas la version. Si elle est `EN_COURS` chez un autre runner, la chaîne s'arrête pour ce démarrage (voir « Limites connues »).
- **Finalisation** par `updateOne({_id: V, owner}, {$set: {statut}})` : seul le propriétaire de la réservation peut la clore.
- **Ajouts** : état `EN_COURS` dans l'énumération des statuts, champ `owner` dans `MigrationRecord`, bail de **5 minutes** calculé avec une `Clock` injectée ([ADR-004](./004-clock-injection-convention.md)).

---

## Alternatives Considérées

### Option A3 : Réservation atomique par version (`findOneAndUpdate` + bail) ✅ Retenue

- **Avantages** : exclusion mutuelle garantie par l'atomicité d'une opération mono-document MongoDB, sans composant supplémentaire ; corrige F6 et F6b d'un seul mécanisme (un `ECHEC` ou un bail expiré redevient réservable, la finalisation est un `update` et non plus un `insert`) ; reprise automatique après Lambda figée ; aucune amorce d'index/TTL ; compatible native-image (API driver standard).
- **Inconvénients** : ajoute un état (`EN_COURS`) et un champ (`owner`) au document de suivi ; dépend de l'horloge pour le bail (d'où l'injection `Clock`) ; une migration plus longue que le bail pourrait être reprise par une autre instance (les migrations doivent rester courtes et idempotentes, conformément à ADR-002).

### Option A1 : Verrou global avec TTL

- **Avantages** : principe simple (un seul verrou pour l'ensemble des migrations).
- **Inconvénients** : le TTL doit être amorcé (création de l'index) ; le moniteur TTL de MongoDB est imprécis (passage environ toutes les 60 s) ; un verrou orphelin subsiste si une Lambda est figée en cours d'exécution ; le bail doit être renouvelé pendant l'exécution.
- **Raison du rejet** : complexité opérationnelle (amorçage, renouvellement) et risque de verrou orphelin supérieurs au gain ; la granularité globale bloque aussi les versions indépendantes.

### Option A2 : Exécution unique hors démarrage

- **Avantages** : supprime la concurrence à la source (un seul exécutant).
- **Inconvénients** : nouvel artefact à construire et déployer ; accès à MongoDB Atlas depuis la CI ; remet en cause le mode de déclenchement au démarrage fixé par l'ADR-002.
- **Raison du rejet** : changement de modèle d'exploitation disproportionné par rapport au défaut à corriger, et contradictoire avec une décision déjà Acceptée.

---

## Conséquences

### Positives
- Une version de migration n'est exécutée que par une seule instance à la fois, même avec N Lambdas en démarrage simultané.
- Plus de rejeu systématique après un `ECHEC` (F6b) : une migration en échec est réservée, rejouée, puis finalisée en `SUCCES`.
- Une version prise par une Lambda figée est reprise après expiration du bail.

### Négatives / Compromis
- Environ **1 requête par version** au cold start (la réservation), y compris pour les versions déjà en `SUCCES` (rejetées par E11000).
- Introduit une dépendance à l'horloge (`Clock` injectée) et un paramètre de bail (5 min) à calibrer par rapport à la durée des migrations.
- Évolution du schéma de `_migrations` (nouvel état, nouveau champ) : les documents existants (sans `owner`) doivent rester lisibles.

### Limites connues
- **Pas de renouvellement du bail** (pas de heartbeat). Si une Lambda est gelée en cours de migration plus longtemps que le bail (5 min), ou si une migration dure plus de 5 min, une autre instance peut reprendre la version et l'exécuter en parallèle. Le premier `owner` voit alors sa finalisation ignorée, avec un `WARN`.
- **Conséquence** : les migrations doivent rester courtes et idempotentes. Les opérations longues (ex. construction d'index sur une grosse collection) sont lancées sans attendre la fin côté client, ou avec un timeout dédié à l'opération.
- **Ordre garanti** : si une version est réservée par un autre runner, la chaîne s'arrête pour ce démarrage, au lieu de sauter à la version suivante. La reprise se fait au démarrage à froid suivant.
- **Échec d'écriture du statut final** : un échec d'écriture du statut `SUCCES` n'est pas confondu avec un échec de la migration elle-même. La version reste `EN_COURS` jusqu'à expiration du bail ; elle est ensuite reprise, d'où l'exigence d'idempotence.
- **Décision 👤 du 2026-10-26** (Gate #2 de la Phase 1, plan 004) : documenter la limite, sans implémenter de renouvellement. À réévaluer si une migration longue apparaît.

### Neutres
- L'[ADR-002](./002-migrations-mongodb-maison.md) **reste Acceptée** : le présent ADR la complète (fiabilité en exécution concurrente) sans la remplacer.
- `docs/ARCHITECTURE.md` (section Migrations MongoDB) à mettre à jour à l'implémentation.

---

## Mise en œuvre

- **Fichiers impactés** (module `communs`, package `migrations`) : `MongoMigrationRunner.java`, `MigrationRecord.java`, `MigrationRepository.java` (réservation + finalisation), énumération des statuts (ajout `EN_COURS`).
- **Tâches de suivi** : DEVon — implémenter T1.2a/T1.2b ; QALvin — tests de concurrence (réservation simultanée, E11000, reprise après `ECHEC`, reprise après expiration du bail avec `Clock.fixed`) ; DOCly — mise à jour de `docs/ARCHITECTURE.md`.
- **Date d'effet** : à partir de la Phase 1 du Plan d'Action 004, sous réserve du `GO` 👤 (passage du statut à `Acceptée`).

---

## Références

- Plan d'Action associé : [`.claude/plans/004_revue_architecture_backend.plan.md`](../../.claude/plans/004_revue_architecture_backend.plan.md) (Phase 1, T1.2a / T1.2b)
- [ADR-002](./002-migrations-mongodb-maison.md) — Mécanisme de migrations MongoDB maison (complété par cet ADR)
- [ADR-004](./004-clock-injection-convention.md) — Convention d'injection `Clock` UTC
