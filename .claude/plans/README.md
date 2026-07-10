# 📋 Plans d'Action (Action Plans)

Index des Plans d'Action (AP) du projet `gestion-budget-serverless`.

Chaque plan orchestre une initiative multi-phases coordonnée entre plusieurs agents (⚫ MAINa, 🟠 ARCos, 🔵 DEVon, 🟢 QALvin, 🟣 DOCly) et produit des rapports de suivi documentant l'exécution.

Cet index est **synthétique** : liste des plans + statut global uniquement (pas de détail par phase — voir le fichier plan lui-même et ses rapports `PHASE_N_*.md`).

---

## 📂 Plans Actifs / En Cours

- [`003_remediation_sonar.plan.md`](./003_remediation_sonar.plan.md) — 🔵 **En cours (Phase A complétée ; Phase B complétée ; Phase C complétée ; Phase D en attente sollicitation explicite)**. Remédiation 314 issues SonarCloud OPEN (`vzwingma_gestion-budget-serverless`). Phase A (blocker + majeurs `MongoMigrationRunner.java`) ✅ complétée et **committée/poussée** (branche `feat/sonar_mcp`, commits `ffa8fdd`+`d449923`) : 8/9 issues corrigées (S125 introuvable), tests ciblés 11/11 verts, `mvn clean test` racine 142/146 (4 erreurs pré-existantes sans lien, validation complète différée scan CI `master`). Phase B (S8688 `.now()` UTC, 18 issues) ✅ **complétée** : [`docs/adr/004-clock-injection-convention.md`](../../docs/adr/004-clock-injection-convention.md) produit (ARCos+DOCly), validé. T B.1 (`communs`, 5 call sites) ✅ — `mvn clean test -f communs/pom.xml` 149 tests/0 failure (4 erreurs infra pré-existantes), compilation 4 microservices confirmée OK par QALvin. T B.2 (`operations`+`utilisateurs`, 13 call sites, 10 fichiers) ✅ — overloads `Clock`+défaut `systemUTC()` (`BudgetDataUtils`, `LigneOperation`, `Utilisateur`), field injection `@Inject Clock clock` sur beans CDI (écart ADR-004 documenté), fix incident cohérence horloge accepté (`initBudgetFromBudgetPrecedent`), vigilance clonage mois suivant vérifiée QALvin (comportement inchangé), `mvn clean test -f operations/pom.xml` 90/90 + `-f utilisateurs/pom.xml` 36/36 verts. Gates #2/#3 obtenues pour T B.1 et T B.2. Phase C (S8924 imports statiques Mockito, 267 issues, mécanique) ✅ **complétée** : 4 lots séquentiels DEVon — T C.1 `operations` 123/123, T C.2 `communs` migrations 74/74, T C.3 `parametrages`+`utilisateurs` 39/39, T C.4 reste éparpillé (dont `comptes`) 31/31 ; 0 résidu Mockito qualifié confirmé indépendamment par QALvin, `mvn clean test` vert sur 5 modules (`communs` hors 4 erreurs infra pré-existantes), aucune régression Phase A/B (assertions `Clock.fixed` intactes). Gates #2/#3 obtenues. **Phase D (reliquats S7467/S6213/S5778/S8700) en attente sollicitation explicite du développeur — non démarrée.**
- [`002_fix_desync_quarkus_renovate.plan.md`](./002_fix_desync_quarkus_renovate.plan.md) — 🔵 **En cours**. Fix désync version Quarkus (`pom.xml` vs README/ARCHITECTURE.md/instructions) sur PR Renovate — check CI "Check Quarkus version sync" cassait car Renovate ne suivait pas les fichiers doc. Phase 1 ✅ (`customManager` Renovate ajouté dans `renovate.json`) ; Phase 2 (vérification + déblocage PR en cours) et Phase 3 (doc) restantes.

---

## 📋 Plans Archivés / Complétés

- [`001_modernisation_stack.plan.md`](./001_modernisation_stack.plan.md) — 🏁 **Clôturé (Gate #4, 2026-07-08)**. Phases 1-7 complétées (ADR-001/002/003, upgrade Quarkus 3.37.1, migrations Mongo, tuning infra SAM/CI, Java 25 + Mandrel 25 sur les 5 modules) ; Phase 8 partiellement complétée (T8.2 ✅, T8.1 reste ouverte — backport upstream Quarkus #55278 non confirmé, sans date connue, à surveiller lors d'une prochaine initiative). QUA et PROD déployés et fonctionnels (`master`).

---

## 📜 Plans historiques (`.github/plans/`, non migrés)

L'ancienne structure `.github/plans/` reste en place, inchangée, et contient les plans historiques suivants (non migrés vers `.claude/plans/`, conservés en parallèle) :

- [`001_revue_conformite_architecture_hexagonale.plan.md`](../../.github/plans/001_revue_conformite_architecture_hexagonale.plan.md) — ✅ Complété
- [`002_remediation_hexagonale.plan.md`](../../.github/plans/002_remediation_hexagonale.plan.md) — ✅ Complété

Index historique complet : [`.github/plans/README.md`](../../.github/plans/README.md).

---

## 🚀 Comment Créer un Nouveau Plan

1. **Créer le fichier plan** : `.claude/plans/<NO>_<nom>.plan.md`
   - Utiliser le numéro séquentiel suivant
   - Suivre le format défini dans [`.claude/PLANS.md`](../PLANS.md)

2. **Créer le dossier reporting** : `.claude/plans/<NO>_reports/`
   - Contiendra les rapports de phase complétées

3. **Soumettre pour validation** au 👤 Développeur humain

**Guide complet :** 📖 [`.claude/PLANS.md`](../PLANS.md)

---

## 📚 Documentation Associée

- **Guide complet des Plans d'Action** : [`.claude/PLANS.md`](../PLANS.md)
- **Instructions agent MAINa (⚫)** : [`.claude/agents/Maina.agent.md`](../agents/Maina.agent.md)
- **Instructions agent ARCos (🟠)** : [`.claude/agents/Arcos.agent.md`](../agents/Arcos.agent.md)
- **Instructions agent DEVon (🔵)** : [`.claude/agents/Devon.agent.md`](../agents/Devon.agent.md)
- **Instructions agent QALvin (🟢)** : [`.claude/agents/Qalvin.agent.md`](../agents/Qalvin.agent.md)
- **Instructions agent DOCly (🟣)** : [`.claude/agents/Docly.agent.md`](../agents/Docly.agent.md)
- **Instructions Claude globales** : [`.claude/CLAUDE.md`](../CLAUDE.md)

---

## ✅ Checklist pour un Plan Bien Structuré

Avant de créer un nouveau plan, vérifier :

- [ ] Titre explicite et objectif global clair
- [ ] Phases bien séparées (3-6 phases généralement)
- [ ] Chaque phase a contexte, critères de réussite, tâches
- [ ] Chaque tâche est numérotée T<N>.<M> avec :
  - [ ] Verbe d'action + objet
  - [ ] Fichiers précis
  - [ ] Scope explicite
  - [ ] Critères d'acceptation mesurables
  - [ ] Agent assigné
- [ ] Dépendances explicites et diagramme
- [ ] Critères de succès globaux (5-7 items)
- [ ] Plan d'exécution avec triggers

---

## 🤝 Contribution aux Plans

Pour contribuer ou modifier un plan existant :

1. **Ne pas modifier le fichier plan après son lancement** — créer un nouveau plan pour les changements majeurs
2. **Documenter dans le rapport** : Tout changement de scope ou nouvelle tâche découverte
3. **Notifier l'équipe** : Si un bloqueur ou risque est identifié
4. **Mettre à jour ce README** : Refléter le statut actuel des phases

---

**Dernière mise à jour :** 2026-07-10 (Plan 003 Phase C complétée [T C.1 → T C.4] ; Phase D en attente sollicitation explicite)
**Gestionnaire des Plans :** ⚫ MAINa & 👤 Développeur humain
