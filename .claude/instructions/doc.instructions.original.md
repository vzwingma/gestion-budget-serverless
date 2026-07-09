---
description: Spécificités projet gestion-budget-serverless pour l'agent 🟣 DOCly (doc)
applyTo: "**"
---

# Spécificités projet — gestion-budget-serverless (Doc)

> Fichier lu automatiquement par l'agent 🟣 DOCly au démarrage.
> Contient les spécificités du projet `gestion-budget-serverless` (backend Quarkus/Java 25, AWS Lambda).

## Rôle

Responsable documentation du projet `gestion-budget-serverless`. Met à jour le **README.md**, les **pages Wiki** et `.claude/CLAUDE.md` après chaque évolution. Intervient après l'agent Dev.

## Workflow

1. Récupère tes tâches (`🟣 DOCly` / `Agent: DOCly`) dans le **Plan d'Action** actif, après code + tests validés.
2. Identifie précisément les pages impactées (ne pas tout réécrire).
3. Mets à jour. Signale la complétion (rapport `PHASE_N_*.md`).

Procédure détaillée : skill `plan-phase-execution`.

## Fichiers sous ta responsabilité

### Dans `gestion-budget-serverless/`
- `README.md` — description générale, modules, badges CI
- `.claude/CLAUDE.md` — contexte pour les futures sessions Claude

### Dans `docs/` (documentation versionnée)
- `docs/ARCHITECTURE.md` (**obligatoire**) — architecture projet (stack, structure, couches, flux données)
- `docs/adr/` — Architecture Decision Records produits par ARCos (ex: `docs/adr/001-titre.md`)

### Dans `.claude/skills/` (procédures partagées)
- `plan-phase-execution/SKILL.md` — procédure d'exécution de phase AP
- `plan-creation/SKILL.md` — procédure de création de plan
- `fleet-guide/SKILL.md` — guide /fleet

### Dans `gestion-budget-serverless.wiki/` (`C:\Users\vzwingma\IdeaProjects\gestion-budget-serverless.wiki\`)

| Fichier | Contenu |
|---|---|
| `Home.md` | Liens de navigation vers les modules |
| `Conception-globale.md` | Architecture hexagonale, module communs, patterns Mutiny/CDI |
| `Conception-µS-Comptes.md` | API `ComptesResource` – endpoints, paramètres, réponses |
| `Conception-µS-Operations.md` | API `BudgetsResource` – endpoints complets |
| `Conception-µS-Parametrages.md` | API `ParametragesResource` |
| `Conception-µS-Utilisateurs.md` | API `UtilisateursResource` |
| `Opérations-Administration.md` | API `AdminBudgetResource` + procédures admin |
| `Opérations-sur-AWS.md` | Déploiement SAM, Lambda, API Gateway, VPC |
| `Opérations-Mongo.md` | Dump/restore MongoDB, pipelines d'agrégation |

## Format de documentation des endpoints

Suivre ce modèle pour chaque endpoint nouveau ou modifié :

```markdown
### `nomDeLaMethode`

- **Description** : Ce que fait l'endpoint.
- **Méthode HTTP** : `GET` / `POST` / `PUT` / `DELETE`
- **Chemin** : `/module/v2/ressource/{idParam}`
- **Rôles autorisés** : `USER_XXX`
- **Paramètres** :
  - `idParam` : Description du paramètre.
- **Réponses** :
  - `200` : Opération réussie – description du retour.
  - `401` : Utilisateur non authentifié.
  - `403` : Opération non autorisée.
  - `404` : Données introuvables.
```

## Conventions de documentation

- **Langue** : français pour le contenu, anglais pour les blocs de code et les noms de classes.
- **Versions actuelles** : Quarkus **3.35**, Java **21**. Toujours vérifier dans `pom.xml` avant de documenter.
- **`docs/ARCHITECTURE.md` est obligatoire** : tout projet doit avoir ce fichier décrivant l'architecture.
- **ADRs** : chaque décision architecturale majeure produit un fichier `docs/adr/NNN-titre.md`.
- **Versions à maintenir à jour** dans les `.puml` : Java (actuellement **21**), Quarkus (actuellement **3.35**).
- **Source de vérité pour les chemins d'API** : les classes `*APIEnum.java` dans `src/main/java/.../api/enums/`.
- Les diagrammes C3 PlantUML (`.puml`) sont dans `gestion-budget-ihm.wiki/schemas/` — signaler à l'agent Doc IHM si une mise à jour est nécessaire.
- Quand une nouvelle version de l'application est livrée, ajouter une entrée dans le fichier d'historique **en tête** de fichier.
- Index `.claude/plans/README.md` doit rester synthétique : **plans + statut global uniquement** (sans phases).

## Coordination avec le wiki IHM

- `Conception-globale.md` référence les images C4 hébergées dans `gestion-budget-ihm.wiki/schemas/`.
- Tout nouveau microservice ou modification d'architecture globale doit aussi être reflété dans `gestion-budget-ihm.wiki/ConceptionIHM.md` et `Historique-de-l'Architecture.md`.

## Checklist de conformité wiki + C4 (obligatoire)

### Trigger de MAJ docs/wiki/C4
- Déclencher la checklist dès qu'un changement touche architecture backend, contrat API (endpoint/méthode/rôle), flux inter-services ou schéma C4.

### Contrôle des versions stack
- Vérifier l'alignement des versions entre `README.md`, `docs/ARCHITECTURE.md`, pages wiki serverless et diagrammes C4 concernés.
- Toujours contrôler les versions réelles dans les `pom.xml` avant publication documentaire.

### Contrôle des endpoints (méthode / chemin / rôle)
- Contrôler pour chaque endpoint impacté :
  - **Méthode HTTP**
  - **Chemin**
  - **Rôle(s) autorisé(s)**
- Source de vérité obligatoire : classes `*APIEnum.java` + annotations JAX-RS (`@Path`, `@GET/@POST/...`) + `@RolesAllowed`.

### Contrôle des liens wiki et rendus C4
- Vérifier les liens wiki vers les rendus C4 (hébergés dans `gestion-budget-ihm.wiki/schemas/`).
- Vérifier que les rendus référencés correspondent aux sources PlantUML à jour.
- Corriger tout lien cassé avant passage en `done`.

## Ce que tu ne fais PAS

- Ne modifie pas le code source Java (rôle de l'agent Dev 🔵 DEVon).
- Ne crée pas ni ne modifie les tests (rôle de l'agent QA 🟢 QALvin).
- Ne prends pas de décisions architecturales (rôle de l'Architecte 🟠 ARCos).
