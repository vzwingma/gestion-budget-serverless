---
name: "plan-creation"
description: "Skill — Procédure création + orchestration Plan d'Action (AP). Pour MAINa — agent orchestrateur responsable création et validation Plan d'Action."
applyTo: "**"
---

# Skill : Création d'un Plan d'Action (AP)

> Skill décrit procédure standard créer, valider, lancer Plan d'Action.
> Réservé ⚫ MAINa — responsable création + orchestration Plans d'Action.
> Référence complète format AP : `.claude/PLANS.md`

---

## Avant créer plan

1. **Clarifier problème / objectif**
   - Besoin utilisateur ou technique ?
   - Critères succès mesurables ?
   - Contraintes temps, ressources, technologie ?

2. **Structurer approche**
   - Phases logiques nécessaires ?
   - Dépendances entre phases ?
   - Quel agent (DEVon, QALvin, DOCly, ARCos) fait quoi ?

---

## Créer fichier plan

Créer fichier `.claude/plans/<NO>_<nom>.plan.md` avec :

1. **En-tête** : Titre, date, statut (`⏳ Planifié`), lien document
2. **Objectif Global** : 1-2 paragraphes problème + outcomes attendus
3. **Phases** : 3-6 phases avec :
   - Contexte (situation actuelle, enjeux)
   - Critères Réussite (3-5 conditions mesurables)
   - Tâches (T<N>.<M>) assignées agents
4. **Résumé par Agent** : Qui fait quoi, livrables, durée estimée
5. **Dépendances** : Diagramme ordre exécution
6. **Critères Succès Globaux** : Mesures finales projet
7. **Plan d'Exécution** : Quand démarrer chaque phase, triggers

**Référence complète format** : `.claude/PLANS.md` (section "Format du Fichier Plan")

## Règle persistance obligatoire

Livrable skill = ensemble fichiers persistés dans dépôt :
- `.claude/plans/<NO>_<nom>.plan.md`
- `.claude/plans/<NO>_reports/`
- `.claude/plans/README.md` mis à jour

Outils édition interdits ou prompt demande ne modifier aucun fichier → pas produire faux plan "créé" en réponse finale. Demander développeur humain choix :
1. autoriser création fichiers ;
2. ou recevoir uniquement brouillon non formalisé.

### Structurer tâches

Chaque tâche doit avoir :
- **Numéro unique** : `T<PHASE>.<NUM>` (ex: T1.1, T2.3)
- **Agent assigné** : DEVon, QALvin, DOCly, ARCos
- **Scope explicite** : Fichiers créer/modifier, quoi couvrir
- **Critères mesurables** : "≥90% couverture", "5/5 tests passants", etc.

```markdown
#### T1.1 - <Verbe d'action> <objet>
- **Agent :** [QALvin | DEVon | DOCly | ARCos]
- **Fichier(s) :** Chemin exact
- **Couvrir / Implémenter :**
  - Fonctionnalité 1
  - Cas d'erreur
- **Acceptation :** Condition mesurable (ex: ≥90% couverture)
```

---

## Créer dossier reporting

```
.claude/plans/<NO>_reports/
```

Dossier contient rapport par phase :
- `PHASE_1_COMPLETION_REPORT.md`
- `PHASE_2_COMPLETION_REPORT.md`
- etc.

---

## Présenter + valider plan

Avant lancer phases :

1. **Soumettre plan** 👤 développeur humain pour validation
2. **Points validation clés :**
   - Phases séparées logiquement ?
   - Dépendances correctes (pas cycles) ?
   - Tâches claires + mesurables ?
   - Agents assignés appropriés ?
3. **Ajuster** selon feedback

> 💡 **Compact recommandé** après validation plan : `/compact` avec instruction — _"Garde uniquement : titre plan, phases + statut, tâches ouvertes assignées. Supprime détails procédure création."_ Économise ~4.5KB contexte skill tours suivants.

---

## Lancer phase

Plan validé + dépendances satisfaites :

1. **Vérifier dépendances** : Toutes phases précédentes ✅
2. **Identifier agent responsable** phase
3. **Créer rapport vide** : `.claude/plans/<NO>_reports/PHASE_N_COMPLETION_REPORT.md`
4. **Déléguer agent** prompt structuré incluant :
   - Lien plan complet
   - Liste tâches assignées (T<N>.X à T<N>.Y)
   - Lien rapport remplir
   - Critères réussite + dépendances critiques

**Exemple prompt lancement :**
```
Exécute la Phase N du plan : .claude/plans/<NO>_<nom>.plan.md

Tâches assignées : T<N>.1 à T<N>.M
Rapport à remplir : .claude/plans/<NO>_reports/PHASE_N_COMPLETION_REPORT.md

Critères de réussite :
- ✅ [Critère 1]
- ✅ [Critère 2]
```

---

## Valider + progresser

Phase signalée complétée :

1. **Lire rapport** : `.claude/plans/<NO>_reports/PHASE_N_...md`
2. **Vérifier** : Tous critères ✅, pas bloqueur, livrables présents
3. **Décider** : Phase suivante démarre ?
4. **Mettre à jour** statut plan si changement global

---

## Règle obligatoire — Synchronisation index plans

- `.claude/plans/README.md` contient **uniquement** liste plans + **statut global**.
- Chaque création plan ou changement statut global → mettre à jour `.claude/plans/README.md` **même changement**.

---

## Checklist bon plan

- [ ] Titre explicite + objectif mesurable
- [ ] 3-6 phases séparées, dépendances claires
- [ ] Chaque tâche a : numéro, agent, fichiers, scope, critères acceptation
- [ ] Dépendances explicites (diagramme ou liste)
- [ ] Critères succès globaux (5-7 items)
- [ ] Plan exécution avec triggers démarrage

---

## Références

- 📋 Guide complet : `.claude/PLANS.md`
- 📌 Index plans : `.claude/plans/README.md`