# ADR 002 — Mécanisme de migrations MongoDB maison

---

**Date :** 2026-07-07
**Statut :** Acceptée
**Décideurs :** 🟠 ARCos + 👤 Développeur humain

---

## Contexte

Le backend `gestion-budget-serverless` ne disposait d'aucun outillage de versioning de schéma MongoDB : aucune trace des évolutions de collections appliquées, aucun mécanisme pour garantir qu'un changement de schéma (nouvel index, initialisation de collection, réécriture de documents) est exécuté une fois et une seule, de façon traçable, sur chaque environnement (dev, QUA, prod).

Cette lacune a été identifiée comme l'un des 4 axes de la modernisation cadrée par l'[ADR-001](./001-strategie-modernisation-stack.md) (Phase 4 du [Plan d'Action associé](../../.claude/plans/001_modernisation_stack.plan.md)).

Contrainte structurante du projet : les 4 microservices (`comptes`, `operations`, `parametrages`, `utilisateurs`) sont compilés en **binaires natifs GraalVM/Mandrel** et déployés en AWS Lambda. Toute solution doit rester compatible native-image, ce qui exclut par construction les mécanismes reposant sur de la réflexion dynamique non enregistrée ou du scan de classpath à l'exécution.

---

## Décision

**Nous avons décidé de** développer un mécanisme de migrations MongoDB maison, dans le module `communs`, basé exclusivement sur l'injection CDI standard (`Instance<IMongoMigration>`), sans scan de classpath ni lecture de fichiers externes, avec un déclenchement automatique au démarrage de chaque microservice via `@Observes StartupEvent`, et une traçabilité des exécutions dans une collection dédiée `_migrations`.

---

## Alternatives Considérées

### Option 1 : Solution maison CDI (`IMongoMigration` + `MongoMigrationRunner`) ✅ Retenue

- **Avantages** : compatible nativement avec GraalVM native-image (aucune réflexion dynamique, découverte CDI standard déjà utilisée partout dans le projet) ; empreinte minimale (4 classes + 1 collection) ; cohérente avec le pattern d'architecture hexagonale/CDI pur déjà en place ; comportement entièrement maîtrisé et auditable.
- **Inconvénients** : aucun tooling prêt à l'emploi (pas d'UI, pas de rollback automatique, pas de dry-run) — à développer au besoin si un jour nécessaire.

### Option 2 : Mongock

- **Avantages** : outil tiers mature, conventions établies et documentées dans l'écosystème Java/MongoDB, fonctionnalités riches (rollback, changelog, dry-run).
- **Inconvénients** : repose sur de la réflexion et du classpath scanning dynamique pour découvrir les changesets à l'exécution.
- **Raison du rejet** : risque élevé d'incompatibilité avec le build natif GraalVM Lambda. Le scan de classpath dynamique nécessite un enregistrement exhaustif et fragile des hints de réflexion (`reflect-config.json` ou `@RegisterForReflection` sur toutes les classes de changesets, y compris celles ajoutées ultérieurement), avec un risque de régression silencieuse à chaque nouvelle migration si l'enregistrement est oublié. Solution jugée trop fragile pour un déploiement 100 % natif.

### Option 3 : Scripts externes / fichiers JSON de migration chargés dynamiquement

- **Avantages** : découplage total du code Java, migrations modifiables sans recompilation.
- **Inconvénients** : nécessite une lecture de fichiers à l'exécution (ressources classpath ou système de fichiers) et/ou un interpréteur de script embarqué.
- **Raison du rejet** : incompatible avec le modèle de chargement en image native GraalVM, où les ressources doivent être connues et empaquetées à la compilation (`native-image` ne supporte pas le chargement dynamique arbitraire de fichiers de configuration métier sans configuration explicite lourde). Apporte en outre un niveau d'indirection inutile pour le volume de migrations attendu sur ce projet.

---

## Conséquences

### Positives
- Traçabilité complète : chaque migration exécutée (succès ou échec) est enregistrée dans la collection `_migrations` avec version, description, date d'exécution et statut.
- Compatible nativement avec GraalVM/Mandrel : aucun hint de réflexion à maintenir par migration (seul `MigrationRecord` porte `@RegisterForReflection`, une fois pour toutes).
- Mécanisme simple à comprendre et à étendre : ajouter une migration = ajouter une classe CDI dans `communs/.../migrations/scripts/`.
- Idempotence garantie au niveau du runner : une migration déjà en statut `SUCCES` n'est jamais rejouée.
- Échec explicite et non bloquant : une migration en échec est journalisée en erreur mais n'empêche pas le démarrage de l'application ni l'exécution des migrations suivantes.

### Négatives / Compromis
- Moins de tooling qu'un outil dédié : pas de rollback automatique, pas d'UI d'administration, pas de dry-run — ces besoins devront être développés manuellement si nécessaires.
- Chaque migration doit être écrite avec discipline défensive (vérifier l'existence d'un index avant création, etc.) puisque le mécanisme ne fournit pas de garanties transactionnelles fortes au-delà du suivi dans `_migrations`.

### Neutres
- Nécessite de maintenir à jour la convention de nommage documentée dans `IMongoMigration` (Javadoc) et rappelée dans `docs/ARCHITECTURE.md`.

---

## Mise en œuvre

- **Fichiers impactés** :
  - `communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/migrations/IMongoMigration.java` — port de migration (contrat `version()` / `description()` / `migrate()`)
  - `communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/migrations/MigrationRecord.java` — document de suivi (collection `_migrations`)
  - `communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/migrations/MigrationRepository.java` — accès Panache à `_migrations`
  - `communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/migrations/MongoMigrationRunner.java` — orchestrateur déclenché par `@Observes StartupEvent`, tri par version, exécution séquentielle
  - `communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/migrations/MigrationReflectionConfig.java` — hints GraalVM natifs centralisés
  - `communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/migrations/scripts/V001_InitMigrationsCollection.java` — première migration (no-op, valide le mécanisme de bout en bout) et gabarit pour les suivantes
- **Convention de nommage** : `V<numéro incrémental sur 3 chiffres>_<description courte>` (ex. `V001_InitMigrationsCollection`, `V002_AjoutIndexComptes`). Numéro unique, strictement croissant, jamais réutilisé ni modifié une fois publié.
- **Tâches de suivi** : Phase 4 du Plan d'Action, tâches T4.1 à T4.6 (implémentation DEVon, tests QALvin — 15 tests couvrant exécution unique, idempotence, tri par version, échec explicite — documentation DOCly).
- **Date d'effet** : à partir du Gate #3 validé (Phase 4), le 2026-07-07.

---

## Références

- [ADR-001](./001-strategie-modernisation-stack.md) — Stratégie de modernisation du stack backend (cadre les 5 phases, dont cette Phase 4)
- Plan d'Action associé : `gestion-budget-serverless/.claude/plans/001_modernisation_stack.plan.md` (Phase 4 — Migrations MongoDB maison)
- `docs/ARCHITECTURE.md` — section Migrations MongoDB
