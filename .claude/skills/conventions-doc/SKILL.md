---
name: conventions-doc
description: "Conventions documentaires du projet gestion-budget-serverless pour 🟣 DOCly : fichiers sous sa responsabilité, langue, ADR, changelog, diagrammes. Prioritaire sur le générique."
user-invocable: false
---

# Conventions projet — gestion-budget-serverless (documentation)

> Projet : `gestion-budget-serverless` — backend Quarkus/Java 25 multi-modules Maven, architecture hexagonale,
> Mutiny + MongoDB Panache, déployé en images natives sur AWS Lambda (SAM).

## Fichiers sous la responsabilité de DOCly

### Racine
- `README.md` — description générale, modules, badges CI
- `.claude/CLAUDE.md` — contexte des futures sessions (sections projet uniquement)

### `docs/`
- `docs/ARCHITECTURE.md` (**obligatoire**) — stack, structure, couches, flux de données
- `docs/adr/` — ADR rédigés sur contenu 🟠 ARCos (skill `adr-writing`, gabarit `ADR-TEMPLATE.md`)

### Wiki serverless (`C:\Users\vzwingma\IdeaProjects\gestion-budget-serverless.wiki\`)
- `Home.md`, `Conception-globale.md`, `Conception-µS-{Comptes,Operations,Parametrages,Utilisateurs}.md`
- `Opérations-Administration.md`, `Opérations-sur-AWS.md`, `Opérations-Mongo.md`
- Historique des versions : nouvelle version ajoutée **en tête** (pas de `CHANGELOG.md` dans le dépôt)
- Diagrammes C4 PlantUML (`.puml`) hébergés dans `gestion-budget-ihm.wiki/schemas/` — signaler à l'agent Doc IHM
  si MAJ nécessaire ; versions à y maintenir : Java, Quarkus

### `.claude/skills/` (procédures partagées)
Mettre à jour si les procédures changent (cohérence avec `.claude/PLANS.md`) :
`plan-creation`, `plan-phase-execution`, `parallel-guide`, `adr-writing`.

## Conventions

- **Langue** : français pour le texte, anglais pour les blocs de code et noms de classes.
- **`docs/ARCHITECTURE.md` obligatoire**.
- **ADR** : un fichier `docs/adr/NNN-titre.md` par décision majeure.
- **Index des plans** `.claude/plans/README.md` : plans + statut global uniquement.
- **Versions** : toujours lues dans `pom.xml` (`maven.compiler.release`, `quarkus.platform.version`) ; ne pas
  dupliquer la version Quarkus hors fichiers suivis par Renovate (check CI « Quarkus version sync », AP 002).
- **Endpoints** : source de vérité = `*APIEnum.java` + annotations JAX-RS + `@RolesAllowed` ; format par endpoint :
  description, méthode HTTP, chemin, rôles, paramètres, réponses (200/401/403/404…).
- Changement d'architecture / contrat API / flux inter-services → vérifier alignement `README.md`,
  `docs/ARCHITECTURE.md`, wiki serverless, diagrammes C4, et liens wiki → rendus C4.

## Ce que DOCly ne fait PAS

- Modifier le code source ou les tests.
- Prendre des décisions d'architecture (rôle 🟠 ARCos).
