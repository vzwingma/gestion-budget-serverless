# Plan d'Action 003 — Remédiation Sonar (314 issues ouvertes)

**Date création :** 2026-07-10
**Statut :** 🔵 En cours (Phase A complétée ; Phase B complétée ; Phase C complétée ; Phase D en attente sollicitation explicite)
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

## Phase B — S8688 `.now()` sans zone (UTC) ✅ complétée

### Contexte

18 issues S8688 (`.now()` sans zone explicite), réparties `communs` + `operations`/`utilisateurs`. Décision Clock UTC tranchée Gate #0 antérieure. ADR-004 (`docs/adr/004-clock-injection-convention.md`) préparé par ARCos, rédigé par DOCly — **produit**, en attente validation humaine (Gate #1 spécifique sous-phase) avant que DEVon démarre B1.

### Critères de réussite

- [x] ADR-004 validé par le développeur humain.
- [x] B1 : `ClockConfig` (bean `@Produces @ApplicationScoped Clock`, package `config` de `communs`) créé ; 5 call sites `communs` migrés vers injection constructeur ; tests avec `Clock.fixed(...)` déterministes.
- [x] B2 (après B1 mergé + `communs` republié GitHub Packages) : 13 call sites `operations`/`utilisateurs` migrés ; vigilance particulière sur `cloneOperationToMoisSuivant`/`cloneOperationPeriodiqueToMoisSuivant` (`BudgetDataUtils`, fragilité documentée `.claude/CLAUDE.md`) — vérifiée explicitement par QALvin, comportement métier inchangé.
- [x] Tests ciblés + suite module verts, pas de régression logique métier budget — `operations` 90/90, `utilisateurs` 36/36.

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

#### T B.2 - Migration operations + utilisateurs ✅ complétée
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

**Résultats T B.2 (clôture) :**
- 13/13 call sites migrés, 10 fichiers modifiés.
- `BudgetDataUtils.java` (classe statique) : overloads `Clock` + défaut `systemUTC()` sur `cloneOperationToMoisSuivant`/`cloneOperationPeriodiqueToMoisSuivant`/`cloneOperationAEcheanceReportee`.
- `LigneOperation.java` (POJO) : overloads `Clock` + défaut sur 2 constructeurs.
- `BudgetService.java`/`OperationsService.java`/`UtilisateursService.java` (beans CDI) : champ `@Inject Clock clock = Clock.systemUTC()` (field injection) — écart ADR-004 documenté (pattern Lombok existant respecté), nuance ajoutée par DOCly précédemment.
- `Utilisateur.java` : overload `Clock` + défaut sur constructeur clone.
- Fix incident accepté : cohérence horloge dans `BudgetService.initBudgetFromBudgetPrecedent` (passage `clock` explicite au lieu de l'overload par défaut).
- Vigilance clonage mois suivant (`BudgetDataUtils`, fragilité documentée `.claude/CLAUDE.md`) : QALvin a vérifié en priorité la couverture — comportement métier inchangé, tests `Clock.fixed` couvrant les cas limites pertinents.
- Résultats Maven confirmés (DEVon + QALvin indépendamment) : `mvn clean test -f operations/pom.xml` → 90/90 verts ; `mvn clean test -f utilisateurs/pom.xml` → 36/36 verts.
- Gates #2 (validation code) et #3 (validation tests) obtenues côté utilisateur.

**Effort :** B1 Faible, B2 Moyen. **Risque :** B1 faible, B2 MEDIUM (`BudgetDataUtils`/`BudgetService`, logique métier budget). **Dépendances :** ADR-004 validé (Gate #1 sous-phase) → T B.1 → `communs` republié → T B.2.

### Clôture Phase B

Phase B (T B.0 ADR + T B.1 `communs` + T B.2 `operations`/`utilisateurs`) intégralement complétée. 18/18 issues S8688 adressées. Gates #2/#3 obtenues côté utilisateur pour T B.1 et T B.2.

## Phase C — S8924 imports statiques Mockito (267, mécanique) ✅ complétée

### Contexte

267 issues S8924 "Use static import for mock/when", tests uniquement, mécanique, risque LOW. Pas d'ADR requis (cleanup mécanique pur, aucune décision architecturale). Découpage en 4 lots par module — reviewabilité, pas risque technique.

### Méthode

Remplacer `Mockito.mock(`/`Mockito.when(`/autres `Mockito.xxx(` statiques par imports statiques (`import static org.mockito.Mockito.xxx;`) + appels nus. Conversion **uniquement si pas d'usage mixte restant** dans le fichier après conversion (vérifier avant) ; si usage mixte, convertir toutes les occurrences Mockito du fichier en une fois pour cohérence — pas de conversion partielle. Fichier par fichier ou recherche/remplacement contrôlé (pas de sed aveugle multi-fichiers). Compile + teste après chaque lot.

### Tâches

#### T C.1 - Lot 1 : operations (123 issues) ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `operations/src/test/.../business/*` (105) + `operations/src/test/.../api/*` (18)
- **Acceptation :** imports statiques appliqués, `mvn clean test -f operations/pom.xml` vert.
- **Résultat :** 123/123 issues converties (105 `business/*` + 18 `api/*`). `mvn clean test -f operations/pom.xml` vert.

#### T C.2 - Lot 2 : communs migrations tests (74 issues) ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/test/.../migrations/*`
- **Acceptation :** imports statiques appliqués, `mvn clean test -f communs/pom.xml` vert (hors 4 erreurs infra pré-existantes actées).
- **Résultat :** 74/74 issues converties. `mvn clean test -f communs/pom.xml` vert hors 4 erreurs `MongoTimeoutException` pré-existantes (actées Phase A/B, sans lien).

#### T C.3 - Lot 3 : parametrages + utilisateurs (39 issues) ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `parametrages/src/test/**` (20) + `utilisateurs/src/test/**` (19)
- **Acceptation :** imports statiques appliqués, `mvn clean test -f parametrages/pom.xml` et `mvn clean test -f utilisateurs/pom.xml` verts.
- **Résultat :** 39/39 issues converties (20 `parametrages` + 19 `utilisateurs`). `mvn clean test -f parametrages/pom.xml` et `mvn clean test -f utilisateurs/pom.xml` verts.

#### T C.4 - Lot 4 : reste éparpillé (~31 issues) ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** fichiers restants tous modules (`comptes` compris)
- **Acceptation :** imports statiques appliqués, suites concernées vertes.
- **Résultat :** 31/31 issues converties, dont `comptes`. Suites concernées vertes tous modules.

**Effort :** M (volume, mécanique). **Risque :** LOW uniforme. **Dépendances :** aucune (indépendant Phase A/B). 4 lots traités séquentiellement par DEVon sans repasser par validation humaine entre chaque lot (décision utilisateur, vu risque LOW/mécanique) — Gate #2 consolidée une fois les 4 lots codés.

### QA Phase C (allégée)

QALvin : pas d'ajout de tests nécessaire (changement de style d'import uniquement, comportement des tests inchangé) — juste confirmer compilation + suite verte par module touché (`operations`, `communs`, `parametrages`, `utilisateurs`, `comptes`).

QALvin a validé indépendamment : recherche résiduelle confirmant 0 usage qualifié Mockito restant, `mvn clean test` vert sur les 5 modules (`comptes`, `communs`, `operations`, `parametrages`, `utilisateurs` — mêmes 4 erreurs infra pré-existantes sur `communs`), et absence de régression sur les fichiers déjà remaniés en Phase A/B (assertions `Clock.fixed` intactes).

### Clôture Phase C

- 267/267 issues S8924 corrigées (123 T C.1 + 74 T C.2 + 39 T C.3 + 31 T C.4), 0 résidu Mockito qualifié constaté (DEVon + confirmé indépendamment par QALvin).
- 5 modules (`comptes`, `communs`, `operations`, `parametrages`, `utilisateurs`) validés indépendamment par DEVon et QALvin : tous verts, sauf `communs` qui conserve les 4 erreurs `MongoTimeoutException` pré-existantes (actées Phase A/B, sans lien avec Phase C).
- Aucune régression sur les fichiers déjà remaniés en Phase A/B (assertions `Clock.fixed` intactes, confirmé QALvin).
- Gates #2 (validation code) et #3 (validation tests) obtenues côté utilisateur.

## Phase D — Reliquats (S7467 unnamed pattern, S6213 nom réservé, S5778 lambda, S8700 durée zone-aware)

### Contexte

Dernière phase Plan 003 — ~17 issues restantes, 4 sous-lots indépendants, risque LOW uniforme, fixes isolés.

### Tâches

#### T D.1 - S7467 : catch unnamed pattern Java 22 (10 issues, main)
- **Agent :** DEVon
- **Fichier(s) :** `AbstractAPISecurityFilter.java:53`, `JWTUtils.java:77,138`, `BudgetsResource.java:87,134` + autres identifiés via Grep `catch \(Exception e\)` / règle S7467
- **Couvrir :** remplacer `catch (Exception e)` par `catch (Exception _)` **uniquement** si `e` réellement inutilisé dans le corps du catch — vérifier site par site avant remplacement, ne pas convertir si `e` est loggé/inspecté.
- **Acceptation :** 10 issues résolues, aucun site avec usage réel de l'exception converti à tort.

#### T D.2 - S6213 : renommage variable nom réservé (3 issues, test)
- **Agent :** DEVon
- **Fichier(s) :** `communs/.../TestMigrationRecord.java:19,29,46`
- **Acceptation :** 3 issues résolues, tests toujours verts.

#### T D.3 - S5778 : refactor lambda mal utilisée (2 issues, test)
- **Agent :** DEVon
- **Fichier(s) :** `operations/.../BudgetServiceTest.java:381`, `utilisateurs/.../UtilisateursServiceTest.java:75`
- **Acceptation :** 2 issues résolues, comportement test inchangé.

#### T D.4 - S8700 : conversion type zone-aware avant calcul durée (1 issue, test)
- **Agent :** DEVon
- **Fichier(s) :** `communs/.../TestJWTUtils.java:102`
- **Couvrir :** réutiliser convention `Clock`/zone-aware établie Phase B (ADR-004) plutôt que fix ad hoc.
- **Acceptation :** 1 issue résolue, cohérent avec pattern Phase B.

**Effort :** S. **Risque :** LOW uniforme. **Dépendances :** aucune, indépendant A/B/C. 4 sous-lots traités séquentiellement par DEVon sans repasser par validation humaine entre chaque lot (comme Phase C) — Gate #2 consolidée une fois les 4 lots codés.

### QA Phase D

QALvin : confirme absence de régression sur les 4 sous-lots + module concernés (`comptes`? à vérifier via AbstractAPISecurityFilter, `communs`, `operations`, `utilisateurs`). Ajoute test ciblé si trou de couverture identifié (attention particulière T D.4 S8700, comme fait en Phase B pour Clock).

---

## Action de clôture différée — validation Mongo réelle `TestMigrationRepository`/`TestMigrationRepositoryPersistence`

**Statut :** ⚠️ Point de vigilance ouvert — non bloquant pour avancement Phases A/B/C/D, **bloquant pour clôture définitive Plan 003**.

### Constat

Depuis Phase A, 4 tests échouent systématiquement, tous environnements agents (DEVon, QALvin) + poste utilisateur, sans exception :

- `communs/src/test/java/io/github/vzwingma/finances/budget/services/communs/migrations/TestMigrationRepository.java`
- `communs/src/test/java/io/github/vzwingma/finances/budget/services/communs/migrations/TestMigrationRepositoryPersistence.java`

**Nature échec :** `MongoTimeoutException` / `Connection refused: 127.0.0.1:27017`. Cause : aucun Mongo/Docker (MongoDB Dev Services Quarkus) disponible dans environnements agents ni poste utilisateur utilisés durant exécution intégrale Plan 003.

**Historique :** écart constaté + documenté à chaque clôture de phase (A ligne 82, B ligne 124, C lignes 183/203/208) sans résolution — aucun environnement disponible pendant Plan 003 n'avait Mongo/Docker actif. Validation complète systématiquement différée au scan CI `master` post-merge, **jamais vérifiée réellement avec Mongo actif à date**.

### Action requise avant clôture définitive Plan 003

Après Phase D (ou clôture anticipée si Phase D reste non sollicitée) :

1. Exécuter `TestMigrationRepository` + `TestMigrationRepositoryPersistence` dans environnement Mongo réel disponible :
   - soit scan/build CI `master` (infra complète, Mongo Dev Services) ;
   - soit poste local avec Docker actif.
2. Confirmer passage au vert.
3. Si échec réel révélé (pas seulement infra manquante) : documenter écart ici + ouvrir remédiation dédiée (nouvelle tâche ou nouveau plan selon ampleur).

Tant que cette vérification n'est pas faite, Plan 003 reste **non clôturable définitivement (Gate #4)** malgré Phases A/B/C complétées et D différée.

---

## Résumé par agent (Phases A + B + C)

| Agent | Tâches | Livrable |
|---|---|---|
| DEVon | T A.1, T A.2, T A.3, T B.1, T B.2, T C.1, T C.2, T C.3, T C.4 | ✅ Code corrigé, compilant. Phase A : 8/9 issues Sonar résolues (S125 introuvable). Phase B : 18/18 issues S8688 résolues (5 call sites `communs` + 13 call sites `operations`/`utilisateurs`). Phase C : 267/267 issues S8924 résolues (123 `operations` + 74 `communs` migrations + 39 `parametrages`/`utilisateurs` + 31 reste éparpillé dont `comptes`) |
| ARCos | T B.0 (contenu ADR-004) | ✅ ADR-004 Clock UTC préparé |
| QALvin | Post Gate #2 (Phase A, T B.1, T B.2, Phase C) | ✅ Exécution + validation suites tests : `communs` (149/149 hors infra), `operations` (90/90), `utilisateurs` (36/36), racine Phase A (142/146, 4 erreurs infra pré-existantes). Phase C : 0 résidu Mockito qualifié confirmé, `mvn clean test` vert sur 5 modules (mêmes 4 erreurs infra `communs`), aucune régression Phase A/B |
| DOCly | T B.0 (rédaction ADR), clôture Phase A, clôture Phase B, clôture Phase C | ✅ ADR-004 rédigé ; mise à jour plan 003 + README plans (Phase A, puis Phase B, puis Phase C — cette mise à jour) |

## Dépendances

```
Phase A (DEVon T A.1-A.3) → Gate #2 (humain) → QALvin (tests) → Gate #3 (humain) → ✅ complétée
Phase B (ADR-004 → T B.1 → communs republié → T B.2) → Gate #2/#3 (humain, par sous-tâche) → ✅ complétée
Phase C (DEVon T C.1-C.4, séquentiel) → Gate #2 (humain, consolidée) → QALvin (tests) → Gate #3 (humain) → ✅ complétée
   ↳ Phase D : attend nouvelle sollicitation explicite
```

## Critères succès globaux (Phases A + B + C)

### Phase A

- [x] 8/9 issues Sonar Phase A corrigées dans le code (S2699 x1, S6813 x2, S2629 x2, S6068 x3) — S125 introuvable dans le fichier au moment de l'exécution, considéré déjà résolu/non applicable, sans action nécessaire
- [x] `mvn clean test -f communs/pom.xml` (ciblé `TestMongoMigrationRunner` + `TestV001InitMigrationsCollection`) passe — 11/11 verts
- [ ] `mvn clean test` racine passe (4 microservices) — **non vérifiable en l'état** : 142/146 verts, 4 erreurs `MongoTimeout` pré-existantes (absence Mongo réel en local, ni Docker DEVon ni Mongo actif QALvin), sans lien avec Phase A, confirmées indépendamment par DEVon et QALvin ; validation complète différée au scan CI `master` post-merge
- [x] Gate #2 (validation code) obtenue avant QALvin
- [x] Gate #3 (validation tests) obtenue avant clôture Phase A
- [x] Comportement démarrage migration inchangé (constat DEVon/QALvin, tests ciblés verts)

### Phase B

- [x] ADR-004 validé par le développeur humain
- [x] T B.1 : 5 call sites `communs` migrés, `mvn clean test -f communs/pom.xml` vert (149/149 hors erreurs infra pré-existantes)
- [x] T B.2 : 13 call sites `operations`/`utilisateurs` migrés, `mvn clean test -f operations/pom.xml` (90/90) et `mvn clean test -f utilisateurs/pom.xml` (36/36) verts
- [x] Vigilance clonage mois suivant (`BudgetDataUtils`) vérifiée explicitement par QALvin, comportement métier inchangé
- [x] Gates #2/#3 obtenues pour T B.1 et T B.2

### Phase C

- [x] 267/267 issues S8924 corrigées (123 T C.1 `operations` + 74 T C.2 `communs` migrations + 39 T C.3 `parametrages`/`utilisateurs` + 31 T C.4 reste éparpillé dont `comptes`)
- [x] 0 résidu Mockito qualifié constaté (DEVon + confirmé indépendamment par QALvin)
- [x] 5 modules validés indépendamment DEVon + QALvin : `mvn clean test` vert sur `comptes`, `operations`, `parametrages`, `utilisateurs` ; `communs` vert hors 4 erreurs infra pré-existantes (actées Phase A/B)
- [x] Aucune régression sur fichiers déjà remaniés Phase A/B (assertions `Clock.fixed` intactes)
- [x] Gates #2/#3 obtenues côté utilisateur

### Global

- [x] Phase D explicitement différée, non démarrée, en attente sollicitation explicite du développeur

## Plan d'exécution

1. Phase A (complétée) : DEVon implémente T A.1 → T A.3, Gate #2, QALvin tests, Gate #3.
2. Phase B (complétée) : ARCos+DOCly rédigent ADR-004, validation humaine, DEVon T B.1 (`communs`), Gate #2/#3, republication `communs`, DEVon T B.2 (`operations`/`utilisateurs`), Gate #2/#3.
3. Phase C (complétée) : DEVon traite T C.1 → T C.4 séquentiellement (267 issues S8924, 5 modules), Gate #2 consolidée, QALvin validation indépendante, Gate #3, DOCly clôture doc.
4. Phase D (reliquats) : attend nouvelle sollicitation développeur.

---

## Rapports de phase

Voir `.claude/plans/003_reports/`.
