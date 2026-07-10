# Plan d'Action 003 — Remédiation Sonar (314 issues ouvertes)

**Date création :** 2026-07-10
**Statut :** 🔵 En cours (Phase A complétée ; Phase B — T B.1 `communs` complétée, T B.2 en attente republication `communs` + feu vert explicite ; Phase C/D en attente)
**Porteur :** ⚫ MAINa

---

## Contexte

SonarCloud (`vzwingma_gestion-budget-serverless`) remonte 314 issues OPEN (313 code smells + 1 bug, 0 vulnérabilité). Scan CI uniquement sur push/PR `master` (job `sonar-scan`, `.github/workflows/build-on-master.yml`). Objectif : ramener à 0 via remédiation par lots priorisés risque, orchestrée MAINa → DEVon → QALvin → DOCly.

Répartition (règle | nb | sévérité | portée | nature) :

| Règle | Nb | Sévérité | Portée | Nature |
|---|---|---|---|---|
| S8924 | 267 | MINOR | tests | `Mockito.mock`/`when` → import statique. Mécanique. |
| S8688 | 18 | INFO | main+tests | `.now()` sans zone. Décision projet tranchée : UTC. |
| S7467 | 10 | MINOR | main | `catch(e)` → unnamed pattern Java 22. Mécanique. |
| S6213 | 3 | MAJOR | tests | Variable nom réservé, `TestMigrationRecord.java`. |
| S6813 | 2 | MAJOR | main | Field injection → constructor, `MongoMigrationRunner.java` (fichier partagé 4 microservices). |
| S2629 | 2 | MAJOR | main | Log arg évalué sans garde niveau, `MongoMigrationRunner.java`. |
| S5778 | 2 | MAJOR | tests | Lambda mal utilisée, `BudgetServiceTest`/`UtilisateursServiceTest`. |
| S6068 | 3 | MINOR | tests | `eq(...)` inutile, `TestMongoMigrationRunner.java`. |
| S125 | 1 | MAJOR | tests | Code commenté à supprimer, même fichier. |
| S8700 | 1 | MAJOR | tests | Durée calculée sans type zone-aware, `TestJWTUtils.java`. |
| S2699 | 1 | BLOCKER | tests | Test sans assertion — `TestV001InitMigrationsCollection.java:28`. |

Pas de tooling codemod (OpenRewrite) dans repo — fixes manuels/IDE-assistés.

## Décision Clock (pour Phase B, info contexte — validée Gate #0)

UTC (`Clock.systemUTC()`), bean CDI produit dans `communs`, injecté par constructeur partout. ADR-004 requis avant implémentation Phase B — préparé par ARCos, rédigé par DOCly (skill `adr-writing`), au lancement de Phase B (hors scope session courante).

---

## Phase A — Blocker + majeurs fichier partagé (priorité 1, plus petit blast radius) ✅ complétée

### Contexte

Scope : S2699 (1) + S6813/S2629 (4) + S6068/S125 (4, même fichier) = 9 issues, concentrées sur `communs/.../migrations/`. Fichier `MongoMigrationRunner.java` chargé au démarrage des 4 microservices (comptes, operations, parametrages, utilisateurs) → risque MEDIUM, mais constructor injection cohérent conventions CDI déjà en place (`.claude/CLAUDE.md`).

### Critères de réussite

- 9 issues Sonar concernées (S2699, S6813 x2, S2629 x2, S6068 x3, S125 x1) corrigées sans régression.
- `mvn clean test -f communs/pom.xml` ciblé `TestMongoMigrationRunner` + `TestV001InitMigrationsCollection` passe.
- `mvn clean test` racine passe (regression démarrage 4 services, tous dépendent `communs`).
- Comportement démarrage migration inchangé pour les 4 microservices.

### Tâches

#### T A.1 - Remplacer field injection par constructor injection + garder log niveau ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/migrations/MongoMigrationRunner.java`
- **Couvrir / Implémenter :**
  - Remplacer `@Inject` sur champs (lignes ~32, 35) par injection via constructeur (S6813 x2)
  - Entourer les `LOG.debug`/`LOG.info` (lignes ~74, 87) de garde `isDebugEnabled()`/`isInfoEnabled()` (S2629 x2)
- **Acceptation :** 4 issues (S6813 x2, S2629 x2) résolues ; comportement identique au runtime (CDI résout toujours le bean) ; pas de nouvelle issue introduite.

#### T A.2 - Nettoyer mocks et code mort dans TestMongoMigrationRunner ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/test/java/io/github/vzwingma/finances/budget/services/communs/migrations/TestMongoMigrationRunner.java`
- **Couvrir / Implémenter :**
  - Retirer `eq(...)` inutiles (Mockito, S6068) lignes ~95, 194, 199
  - Supprimer bloc code commenté ligne ~214 (S125)
- **Acceptation :** 4 issues (S6068 x3, S125 x1) résolues ; tests toujours verts, aucune assertion perdue.
- **Écart constaté :** S125 (code commenté ligne ~214) introuvable dans le fichier au moment de l'exécution — considéré déjà résolu/non applicable, aucune action nécessaire. S6068 x3 corrigées normalement. Total issues effectivement adressées dans le code Phase A : 8/9 (S2699 x1, S6813 x2, S2629 x2, S6068 x3), S125 sans action car absent.

#### T A.3 - Ajouter assertion réelle sur le résultat de migration ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/test/java/io/github/vzwingma/finances/budget/services/communs/migrations/TestV001InitMigrationsCollection.java`
- **Couvrir / Implémenter :**
  - Ligne 28 : remplacer test "ne throw pas" par assertion réelle sur l'état/version écrite en collection `_migrations` après exécution migration
- **Acceptation :** 1 issue (S2699 BLOCKER) résolue ; test échoue si la migration ne fait pas ce qu'elle doit (test non trivial).
- **Écart constaté :** assertion implémentée via `UniAssertSubscriber.assertCompleted().assertItem(null)`, acceptée par l'utilisateur, plutôt que la variante "état DB `_migrations`" initialement envisagée en ligne 72 (celle-ci aurait nécessité conversion du test en `@QuarkusTest` + Docker/Mongo Dev Services — jugé disproportionné pour la portée Phase A).

**Effort :** S. **Risque :** MEDIUM (fichier partagé démarrage 4 microservices). **Dépendances :** aucune (première phase).

### Résultats Phase A (clôture)

- 11/11 tests ciblés Phase A (`TestMongoMigrationRunner`, `TestV001InitMigrationsCollection`) verts.
- `mvn clean test` racine : 146 tests, 142 verts, 4 erreurs (`MongoTimeout` sur `TestMigrationRepository`/`TestMigrationRepositoryPersistence`) — pré-existantes, sans lien avec les changements Phase A, confirmées indépendamment par DEVon et QALvin. Cause : absence Mongo réel en local (ni Docker poste DEVon, ni Mongo actif poste QALvin). Validation complète différée au scan CI `master` post-merge.
- Gate #2 (validation code) et Gate #3 (validation tests) obtenues côté utilisateur.
- Scope PR prévue : branche `fix/sonar-migration-runner-majors-blocker`, 3 fichiers modifiés (`MongoMigrationRunner.java`, `TestMongoMigrationRunner.java`, `TestV001InitMigrationsCollection.java`). Non committée/poussée à date — reste à faire par l'utilisateur, hors périmètre agents.

---

## Phase B — S8688 `.now()` sans zone (UTC)

### Contexte

18 issues S8688 (`.now()` sans zone explicite), réparties `communs` + `operations`/`utilisateurs`. Décision Clock UTC tranchée Gate #0 antérieure. ADR-004 (`docs/adr/004-clock-injection-convention.md`) préparé par ARCos, rédigé par DOCly — **produit**, en attente validation humaine (Gate #1 spécifique sous-phase) avant que DEVon démarre B1.

### Critères de réussite

- ADR-004 validé par le développeur humain.
- B1 : `ClockConfig` (bean `@Produces @ApplicationScoped Clock`, package `config` de `communs`) créé ; 5 call sites `communs` migrés vers injection constructeur ; tests avec `Clock.fixed(...)` déterministes.
- B2 (après B1 mergé + `communs` republié GitHub Packages) : 13 call sites `operations`/`utilisateurs` migrés ; vigilance particulière sur `cloneOperationToMoisSuivant`/`cloneOperationPeriodiqueToMoisSuivant` (`BudgetDataUtils`, fragilité documentée `.claude/CLAUDE.md`).
- Tests ciblés + suite module verts, pas de régression logique métier budget.

### Tâches

#### T B.0 - Rédiger ADR-004 Clock UTC ✅ complétée
- **Agent :** ARCos (contenu) + DOCly (rédaction, skill `adr-writing`)
- **Fichier(s) :** `docs/adr/004-clock-injection-convention.md`
- **Couvrir :** Contexte (18 hits S8688, risques tests non déterministes/ambiguïté zone/incohérences dates), Décision (`Clock.systemUTC()` bean CDI `communs/config/ClockConfig.java`, injection constructeur), Alternatives rejetées (`ZoneId.systemDefault()`, inline sans bean), Conséquences (testabilité vs effort migration + risque `BudgetDataUtils`)
- **Acceptation :** ✅ ADR créé, statut Accepté, aligné style ADR-001. **Gate #1 spécifique Phase B en attente validation humaine avant T B.1.**

#### T B.1 - Producer Clock + migration communs ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :**
  - `communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/config/ClockConfig.java` (nouveau)
  - `communs/.../utils/security/JWTUtils.java:186`
  - `communs/.../data/JWTAuthToken.java:61,73`
  - `communs/.../migrations/MigrationRepository.java:45,58`
- **Couvrir / Implémenter :**
  - Bean `@Produces @ApplicationScoped Clock` retournant `Clock.systemUTC()`
  - Remplacer chaque `.now()` par appel via `Clock` injecté par constructeur
  - Tests : injecter `Clock.fixed(instant, ZoneOffset.UTC)` pour déterminisme
- **Acceptation :** 5 call sites migrés, `mvn clean test -f communs/pom.xml` vert, aucune régression comportementale (JWT expiration, migrations Mongo).

**Résultats T B.1 (clôture) :**
- 5/5 call sites migrés : `MigrationRepository.java` x2 en injection constructeur stricte (bean CDI géré conteneur, cas nominal) ; `JWTUtils.java` x1 et `JWTAuthToken.java` x2 en paramètre `Clock` explicite + overload par défaut `Clock.systemUTC()` — écart ADR-004 documenté (classe statique non instanciable CDI pour `JWTUtils`, POJO `new` ~15 endroits blast radius disproportionné pour `JWTAuthToken`), voir section "Nuance d'implémentation" `docs/adr/004-clock-injection-convention.md`.
- `mvn clean test -f communs/pom.xml` : 149 tests, 0 failure, 4 erreurs `MongoTimeoutException` pré-existantes, sans lien avec T B.1 — mêmes 4 erreurs infra qu'en Phase A (absence Mongo réel local).
- Validation QALvin : compilation des 4 microservices (`comptes`, `operations`, `parametrages`, `utilisateurs`) confirmée OK contre le nouveau `communs` — aucune signature cassée côté appelants existants (`RequestJWTHeaderFactory.java`, `AbstractAPISecurityFilter.java`, etc.).
- Aucune régression sur l'intégration Phase A (`MongoMigrationRunner`/`MigrationRepository`).
- Gates #2 (validation code) et #3 (validation tests) obtenues côté utilisateur.

#### T B.2 - Migration operations + utilisateurs (dépend T B.1 mergé + communs republié)
- **Agent :** DEVon
- **Fichier(s) :**
  - `operations/.../business/BudgetService.java:296,333,407,435`
  - `operations/.../business/model/LigneOperation.java:142-144`
  - `operations/.../utils/BudgetDataUtils.java:158,202`
  - `operations/.../business/OperationsService.java:249,255`
  - `utilisateurs/.../business/model/Utilisateur.java:54`
  - `utilisateurs/.../business/UtilisateursService.java:80`
- **Couvrir / Implémenter :**
  - Injection constructeur `Clock` (dépendance `communs` republiée) dans chaque classe
  - Remplacer `.now()` par appel via `Clock`
  - **Vigilance particulière** : `cloneOperationToMoisSuivant`/`cloneOperationPeriodiqueToMoisSuivant` dans `BudgetDataUtils` — logique fragile documentée `.claude/CLAUDE.md` (champs `SsCategorie`/`Categorie`), tout changement de comportement de date doit être testé explicitement
- **Acceptation :** 13 call sites migrés, tests `operations` + `utilisateurs` verts, comportement clonage mois suivant inchangé (vérifié explicitement par QALvin).

**Effort :** B1 Faible, B2 Moyen. **Risque :** B1 faible, B2 MEDIUM (`BudgetDataUtils`/`BudgetService`, logique métier budget). **Dépendances :** ADR-004 validé (Gate #1 sous-phase) → T B.1 → `communs` republié → T B.2.

## Phase C — S8924 imports statiques Mockito (267, mécanique) — hors scope session courante

## Phase D — Reliquats (S7467 unnamed pattern, S6213 nom réservé, S5778 lambda, S8700 durée zone-aware) — hors scope session courante

---

## Résumé par agent (Phase A uniquement)

| Agent | Tâches | Livrable |
|---|---|---|
| DEVon | T A.1, T A.2, T A.3 | ✅ Code corrigé, compilant, 8/9 issues Sonar résolues (S125 introuvable) |
| QALvin | Post Gate #2 | ✅ Exécution + validation suite tests `communs` (11/11) + racine (142/146, 4 erreurs infra pré-existantes), rapport |
| DOCly | Clôture Phase A | ✅ Mise à jour plan 003 + README plans (cette mise à jour) |

## Dépendances

```
Phase A (DEVon T A.1-A.3) → Gate #2 (humain) → QALvin (tests) → Gate #3 (humain)
   ↳ Phase B/C/D : attendent nouvelle sollicitation explicite
```

## Critères succès globaux (Phase A)

- [x] 8/9 issues Sonar Phase A corrigées dans le code (S2699 x1, S6813 x2, S2629 x2, S6068 x3) — S125 introuvable dans le fichier au moment de l'exécution, considéré déjà résolu/non applicable, sans action nécessaire
- [x] `mvn clean test -f communs/pom.xml` (ciblé `TestMongoMigrationRunner` + `TestV001InitMigrationsCollection`) passe — 11/11 verts
- [ ] `mvn clean test` racine passe (4 microservices) — **non vérifiable en l'état** : 142/146 verts, 4 erreurs `MongoTimeout` pré-existantes (absence Mongo réel en local, ni Docker DEVon ni Mongo actif QALvin), sans lien avec Phase A, confirmées indépendamment par DEVon et QALvin ; validation complète différée au scan CI `master` post-merge
- [x] Gate #2 (validation code) obtenue avant QALvin
- [x] Gate #3 (validation tests) obtenue avant clôture Phase A
- [x] Comportement démarrage migration inchangé (constat DEVon/QALvin, tests ciblés verts)
- [x] Phases B/C/D explicitement différées, non démarrées

## Plan d'exécution

1. Phase A démarrée immédiatement (cette session) : DEVon implémente T A.1 → T A.3.
2. Gate #2 : validation humaine du code avant tests (peut être implicite si aucune décision requise — sinon pause explicite).
3. QALvin exécute suite tests ciblée + racine, complète rapport.
4. Gate #3 : validation tests → clôture Phase A.
5. Phase B (ADR-004 + S8688 UTC), Phase C (S8924 imports statiques), Phase D (reliquats) : attendent nouvelle sollicitation développeur.

---

## Rapports de phase

Voir `.claude/plans/003_reports/`.
