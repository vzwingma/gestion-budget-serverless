# Plan d'Action 003 — Remédiation Sonar (314 issues ouvertes)

**Date création :** 2026-07-10
**Statut :** 🔵 En cours (Phase A)
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

## Phase A — Blocker + majeurs fichier partagé (priorité 1, plus petit blast radius)

### Contexte

Scope : S2699 (1) + S6813/S2629 (4) + S6068/S125 (4, même fichier) = 9 issues, concentrées sur `communs/.../migrations/`. Fichier `MongoMigrationRunner.java` chargé au démarrage des 4 microservices (comptes, operations, parametrages, utilisateurs) → risque MEDIUM, mais constructor injection cohérent conventions CDI déjà en place (`.claude/CLAUDE.md`).

### Critères de réussite

- 9 issues Sonar concernées (S2699, S6813 x2, S2629 x2, S6068 x3, S125 x1) corrigées sans régression.
- `mvn clean test -f communs/pom.xml` ciblé `TestMongoMigrationRunner` + `TestV001InitMigrationsCollection` passe.
- `mvn clean test` racine passe (regression démarrage 4 services, tous dépendent `communs`).
- Comportement démarrage migration inchangé pour les 4 microservices.

### Tâches

#### T A.1 - Remplacer field injection par constructor injection + garder log niveau
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/migrations/MongoMigrationRunner.java`
- **Couvrir / Implémenter :**
  - Remplacer `@Inject` sur champs (lignes ~32, 35) par injection via constructeur (S6813 x2)
  - Entourer les `LOG.debug`/`LOG.info` (lignes ~74, 87) de garde `isDebugEnabled()`/`isInfoEnabled()` (S2629 x2)
- **Acceptation :** 4 issues (S6813 x2, S2629 x2) résolues ; comportement identique au runtime (CDI résout toujours le bean) ; pas de nouvelle issue introduite.

#### T A.2 - Nettoyer mocks et code mort dans TestMongoMigrationRunner
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/test/java/io/github/vzwingma/finances/budget/services/communs/migrations/TestMongoMigrationRunner.java`
- **Couvrir / Implémenter :**
  - Retirer `eq(...)` inutiles (Mockito, S6068) lignes ~95, 194, 199
  - Supprimer bloc code commenté ligne ~214 (S125)
- **Acceptation :** 4 issues (S6068 x3, S125 x1) résolues ; tests toujours verts, aucune assertion perdue.

#### T A.3 - Ajouter assertion réelle sur le résultat de migration
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/test/java/io/github/vzwingma/finances/budget/services/communs/migrations/TestV001InitMigrationsCollection.java`
- **Couvrir / Implémenter :**
  - Ligne 28 : remplacer test "ne throw pas" par assertion réelle sur l'état/version écrite en collection `_migrations` après exécution migration
- **Acceptation :** 1 issue (S2699 BLOCKER) résolue ; test échoue si la migration ne fait pas ce qu'elle doit (test non trivial).

**Effort :** S. **Risque :** MEDIUM (fichier partagé démarrage 4 microservices). **Dépendances :** aucune (première phase).

---

## Phase B — S8688 `.now()` sans zone (UTC) — hors scope session courante

Nécessite ADR-004 (Clock UTC) préalable. Déclenchement sur nouvelle sollicitation.

## Phase C — S8924 imports statiques Mockito (267, mécanique) — hors scope session courante

## Phase D — Reliquats (S7467 unnamed pattern, S6213 nom réservé, S5778 lambda, S8700 durée zone-aware) — hors scope session courante

---

## Résumé par agent (Phase A uniquement)

| Agent | Tâches | Livrable |
|---|---|---|
| DEVon | T A.1, T A.2, T A.3 | Code corrigé, compilant, 9 issues Sonar résolues |
| QALvin | Post Gate #2 | Exécution + validation suite tests `communs` + racine, rapport |
| DOCly | Différé (Phase B, ADR-004) | Hors scope Phase A |

## Dépendances

```
Phase A (DEVon T A.1-A.3) → Gate #2 (humain) → QALvin (tests) → Gate #3 (humain)
   ↳ Phase B/C/D : attendent nouvelle sollicitation explicite
```

## Critères succès globaux (Phase A)

- [ ] 9 issues Sonar Phase A corrigées (S2699, S6813 x2, S2629 x2, S6068 x3, S125 x1)
- [ ] `mvn clean test -f communs/pom.xml` (ciblé) passe
- [ ] `mvn clean test` racine passe (4 microservices)
- [ ] Gate #2 (validation code) obtenue avant QALvin
- [ ] Gate #3 (validation tests) obtenue avant clôture Phase A
- [ ] Comportement démarrage migration inchangé (constat QALvin)
- [ ] Phases B/C/D explicitement différées, non démarrées

## Plan d'exécution

1. Phase A démarrée immédiatement (cette session) : DEVon implémente T A.1 → T A.3.
2. Gate #2 : validation humaine du code avant tests (peut être implicite si aucune décision requise — sinon pause explicite).
3. QALvin exécute suite tests ciblée + racine, complète rapport.
4. Gate #3 : validation tests → clôture Phase A.
5. Phase B (ADR-004 + S8688 UTC), Phase C (S8924 imports statiques), Phase D (reliquats) : attendent nouvelle sollicitation développeur.

---

## Rapports de phase

Voir `.claude/plans/003_reports/`.
