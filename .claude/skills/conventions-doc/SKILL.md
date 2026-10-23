---
name: conventions-doc
description: "Conventions documentaires du projet [NOM_DU_PROJET] pour 🟣 DOCly : fichiers sous sa responsabilité, langue, ADR, changelog, diagrammes. Prioritaire sur le générique."
user-invocable: false
---

# Conventions projet — [NOM_DU_PROJET] (documentation)

> **Template** — placeholders `[...]` à remplir via skill `init-harness`. Tant qu'un placeholder subsiste,
> appliquer le comportement générique de l'agent pour la valeur concernée.
> Projet : `[NOM_DU_PROJET]` — [DESCRIPTION_COURTE_DU_PROJET].

## Fichiers sous la responsabilité de DOCly

### Racine
- `README.md` — présentation, prérequis, démarrage rapide
- `.claude/CLAUDE.md` — contexte des futures sessions (sections projet uniquement)

### `docs/`
- `docs/ARCHITECTURE.md` (**obligatoire**) — stack, structure, couches, flux de données
- `docs/adr/` — ADR rédigés sur contenu 🟠 ARCos (skill `adr-writing`)
- `[FICHIER_CHANGELOG]` — nouvelle version ajoutée **en tête**
- `[AUTRES_DOCS, ex. déploiement, exploitation]`
- `[DIAGRAMMES, ex. docs/diagrams/*.puml]` — versions des frameworks à maintenir : [LISTE_VERSIONS]

### `.claude/skills/` (procédures partagées)
Mettre à jour si les procédures changent (cohérence avec `.claude/PLANS.md`) :
`plan-creation`, `plan-phase-execution`, `parallel-guide`, `adr-writing`.

## Conventions

- **Langue** : [LANGUE_DOC, ex. français] pour le texte, anglais pour les blocs de code.
- **`docs/ARCHITECTURE.md` obligatoire** dans tout projet.
- **ADR** : un fichier `docs/adr/NNN-titre.md` par décision majeure.
- **Index des plans** `docs/plans/README.md` : plans + statut global uniquement.
- [CONVENTION_PROJET_SUPPLÉMENTAIRE, ex. terminologie imposée]

## Ce que DOCly ne fait PAS

- Modifier le code source ou les tests.
- Prendre des décisions d'architecture (rôle 🟠 ARCos).
