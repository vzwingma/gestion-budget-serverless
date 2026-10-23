---
name: update-harness
description: "Audite le code source et les fichiers de bonnes pratiques pour mettre à jour CLAUDE.md et les conventions par rôle (valeurs obsolètes, placeholders non remplis, divergences code/doc). Propose un diff, attend validation 👤, puis applique. Usage : /update-harness [périmètre]."
argument-hint: "[périmètre de l'audit]"
disable-model-invocation: true
---

# Mise à jour des instructions du harnais

Périmètre demandé (optionnel) : $ARGUMENTS

Mission : auditer le code et les fichiers de référence, puis proposer les amendements de `.claude/CLAUDE.md`
et des conventions par rôle pour refléter l'état réel du projet.

## 1. Fichiers de référence (si présents)

| Chemin | Rôle |
|---|---|
| `docs/ARCHITECTURE.md` | Architecture et décisions |
| `docs/BEST_PRACTICES.md`, `docs/CODING_STANDARDS.md` | Bonnes pratiques, standards |
| `docs/CONTRIBUTING.md` | Workflow Git, contribution |
| `CHANGELOG.md` | Évolutions récentes |
| Config lint/format (`.eslintrc*`, `eslint.config.*`, `pyproject.toml`…) | Conventions appliquées |

## 2. Lire les instructions existantes

- `.claude/CLAUDE.md` : sections présentes, infos obsolètes, sections « à compléter ».
<!-- claude-only -->
- Conventions par rôle : `.claude/skills/conventions-{orchestrator,architect,dev,qa,doc}/SKILL.md`.
<!-- /claude-only -->
<!-- copilot-only
- Instructions par rôle : `.github/instructions/{orchestrator,architect,dev,qa,doc}.instructions.md` (créer depuis `*.instructions.template.md` si absent).

La cible documentée est GitHub Copilot CLI uniquement ; VS Code et cloud agent sont hors périmètre. La CLI
charge les skills selon pertinence ; ne pas décrire une sélection déterministe par rôle. Chaque profil Copilot
doit porter une consigne explicite de chargement.
-->

## 3. Extraire les conventions réelles du code

Pour chaque couche (interfaces, services, composants, utilitaires, tests, styles) : relever les patterns
réellement utilisés (nommage, injection, gestion d'erreurs, structure, mocks, setup de test).

## 4. Vérifier la cohérence

- Chaque convention documentée est-elle appliquée dans le code ? Divergence → la signaler.
- Conventions présentes dans le code mais absentes des instructions → proposer l'ajout.
- Versions de dépendances, commandes, chemins : à jour ?
- Placeholders `[...]` non remplis → les lister.
- Workflow cohérent avec ⚫ MAINa comme point d'entrée et les gates #0 → #4.

## 5. Règles de rédaction

1. Ne pas supprimer une section sans raison explicite — amender ou compléter.
2. Vérifier chaque convention dans le code avant de l'ajouter.
3. Rester concis : ces fichiers sont chargés à chaque session (CLAUDE.md) ou à chaque lancement d'agent.
4. Français pour le texte narratif ; exemples tirés du code réel.
5. Ne pas dupliquer le contenu des fichiers d'agents (`.claude/agents/`).

## 6. Livraison

1. Présenter un **diff résumé** : sections à ajouter (avec source dans le code), à amender (avant/après),
   à supprimer (demander confirmation), validées ; placeholders non remplis et valeurs obsolètes séparés.
2. **Attendre la validation 👤** avant toute modification.
3. Appliquer les changements validés, puis résumer les modifications en liste à puces.

<!-- copilot-only
Après validation, modifier la source `.claude/`, générer la projection avec
`scripts/sync-claude-to-github.ps1`, puis exécuter `-Check` et `scripts/check-harness.ps1`.
-->
