---
name: "plan-creation"
description: "Skill — Procédure création + orchestration Plan d'Action (AP). Pour MAINa — agent orchestrateur responsable création et validation Plan d'Action."
applyTo: "**"
---

# Skill : Création d'un Plan d'Action (AP)

> Skill décrit procédure standard créer, valider, lancer Plan d'Action.
> Réservé à ⚫ MAINa — responsable création et orchestration des Plans d'Action.
> Référence complète format AP : `.claude/PLANS.md`

---

## Avant de créer un plan

1. **Clarifier problème / objectif**
   - Quel besoin utilisateur ou technique ?
   - Quels critères succès mesurables ?
   - Contraintes temps, ressources ou technologie ?

2. **Structurer approche**
   - Quelles phases logiques nécessaires ?
   - Comment phases dépendent entre elles ?
   - Quel agent (DEVon, QALvin, DOCly, ARCos) fait quoi ?

---

## Créer le fichier plan

Créer fichier `.claude/plans/<NO>_<nom>.plan.md` contenant :

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

## Règle de persistance obligatoire

Le livrable du skill est un ensemble de fichiers persistés dans le dépôt :
- `.claude/plans/<NO>_<nom>.plan.md`
- `.claude/plans/<NO>_reports/`
- `.claude/plans/README.md` mis à jour

Si les outils d'édition sont interdits ou si le prompt demande de ne modifier aucun fichier, ne pas produire un faux plan "créé" dans la réponse finale. Demander au développeur humain s'il souhaite :
1. autoriser la création des fichiers ;
2. ou recevoir uniquement un brouillon non formalisé.

### Structurer les tâches

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

## Créer le dossier reporting

```
.claude/plans/<NO>_reports/
```

Dossier contiendra rapport par phase :
- `PHASE_1_COMPLETION_REPORT.md`
- `PHASE_2_COMPLETION_REPORT.md`
- etc.

---

## Présenter et valider le plan

Avant lancer phases :

1. **Soumettre plan** au 👤 Développeur humain pour validation
2. **Points validation clés :**
   - Phases bien séparées logiquement ?
   - Dépendances correctes (pas cycles) ?
   - Tâches claires + mesurables ?
   - Agents assignés appropriés ?
3. **Ajuster** selon feedback

> 💡 **Compact recommandé** après validation plan : `/compact` avec instruction — _"Garde uniquement : titre plan, phases + statut, tâches ouvertes assignées. Supprime détails procédure création."_ Économise ~4.5KB de contexte skill pour tous les tours suivants.

---

## Lancer une phase

Quand plan validé + dépendances satisfaites :

1. **Vérifier dépendances** : Toutes phases précédentes sont ✅
2. **Identifier agent responsable** phase
3. **Créer rapport vide** : `.claude/plans/<NO>_reports/PHASE_N_COMPLETION_REPORT.md`
4. **Déléguer à agent** avec prompt structuré incluant :
   - Lien vers plan complet
   - Liste tâches assignées (T<N>.X à T<N>.Y)
   - Lien vers rapport à remplir
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

## Valider et progresser

Après phase signalée complétée :

1. **Lire rapport** : `.claude/plans/<NO>_reports/PHASE_N_...md`
2. **Vérifier** : Tous critères ✅, aucun bloqueur, livrables présents
3. **Décider** : Phase suivante peut démarrer ?
4. **Mettre à jour** statut plan si changement global

---

## Règle obligatoire — Synchronisation de l'index des plans

- `.claude/plans/README.md` doit contenir **uniquement** liste plans + **statut global**.
- À chaque création plan ou changement statut global, mettre à jour `.claude/plans/README.md` dans **même changement**.

---

## Checklist pour un bon plan

- [ ] Titre explicite + objectif mesurable
- [ ] 3-6 phases bien séparées avec dépendances claires
- [ ] Chaque tâche a : numéro, agent, fichiers, scope, critères acceptation
- [ ] Dépendances explicites (diagramme ou liste)
- [ ] Critères succès globaux (5-7 items)
- [ ] Plan exécution avec triggers démarrage

---

## Références

- 📋 Guide complet : `.claude/PLANS.md`
- 📌 Index des plans : `.claude/plans/README.md`