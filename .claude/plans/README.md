# 📋 Plans d'Action (Action Plans)

Index des Plans d'Action (AP) du projet `gestion-budget-serverless`.

Chaque plan orchestre une initiative multi-phases coordonnée entre plusieurs agents (⚫ MAINa, 🟠 ARCos, 🔵 DEVon, 🟢 QALvin, 🟣 DOCly) et produit des rapports de suivi documentant l'exécution.

Cet index est **synthétique** : liste des plans + statut global uniquement (pas de détail par phase — voir le fichier plan lui-même et ses rapports `PHASE_N_*.md`).

---

## 📂 Plans Actifs / En Cours

- [`001_modernisation_stack.plan.md`](./001_modernisation_stack.plan.md) — ✅ Phases 1-7 complétées (ADR-001/002/003, upgrade Quarkus 3.37.1, migrations Mongo, tuning infra SAM/CI, Java 25 + Mandrel 25 sur les 5 modules) ; Phase 8 partiellement complétée (T8.2 ✅, T8.1 bloquée backport upstream Quarkus #55278). QUA/PROD déployés et fonctionnels.

---

## 📋 Plans Archivés / Complétés

_(Aucun plan `.claude/` complété pour l'instant)_

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

**Dernière mise à jour :** 2026-07-06
**Gestionnaire des Plans :** ⚫ MAINa & 👤 Développeur humain
