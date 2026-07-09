# Spécificités projet — gestion-budget-serverless (Doc)

> Fichier lu auto par agent 🟣 DOCly au démarrage.
> Spécificités projet `gestion-budget-serverless` (backend Quarkus/Java 25, AWS Lambda).

## Rôle

Responsable doc projet `gestion-budget-serverless`. MAJ **README.md**, **pages Wiki**, `.claude/CLAUDE.md` après chaque évolution. Intervient après agent Dev.

## Workflow

1. Récupère tâches (`🟣 DOCly` / `Agent: DOCly`) dans **Plan d'Action** actif, après code + tests validés.
2. Identifie pages impactées précisément (pas tout réécrire).
3. MAJ. Signale complétion (rapport `PHASE_N_*.md`).

Procédure détaillée : skill `plan-phase-execution`.

## Fichiers sous ta responsabilité

### Dans `gestion-budget-serverless/`
- `README.md` — description générale, modules, badges CI
- `.claude/CLAUDE.md` — contexte futures sessions Claude

### Dans `docs/` (documentation versionnée)
- `docs/ARCHITECTURE.md` (**obligatoire**) — architecture projet (stack, structure, couches, flux données)
- `docs/adr/` — Architecture Decision Records produits par ARCos (ex: `docs/adr/001-titre.md`)

### Dans `.claude/skills/` (procédures partagées)
- `plan-phase-execution/SKILL.md` — procédure exécution phase AP
- `plan-creation/SKILL.md` — procédure création plan
- `fleet-guide/SKILL.md` — guide /fleet

### Dans `gestion-budget-serverless.wiki/` (`C:\Users\vzwingma\IdeaProjects\gestion-budget-serverless.wiki\`)

| Fichier | Contenu |
|---|---|
| `Home.md` | Liens navigation vers modules |
| `Conception-globale.md` | Architecture hexagonale, module communs, patterns Mutiny/CDI |
| `Conception-µS-Comptes.md` | API `ComptesResource` – endpoints, paramètres, réponses |
| `Conception-µS-Operations.md` | API `BudgetsResource` – endpoints complets |
| `Conception-µS-Parametrages.md` | API `ParametragesResource` |
| `Conception-µS-Utilisateurs.md` | API `UtilisateursResource` |
| `Opérations-Administration.md` | API `AdminBudgetResource` + procédures admin |
| `Opérations-sur-AWS.md` | Déploiement SAM, Lambda, API Gateway, VPC |
| `Opérations-Mongo.md` | Dump/restore MongoDB, pipelines d'agrégation |

## Format de documentation des endpoints

Modèle pour chaque endpoint nouveau/modifié :

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

- **Langue** : français contenu, anglais blocs code + noms classes.
- **Versions actuelles** : Quarkus **3.35**, Java **21**. Vérifier `pom.xml` avant doc.
- **`docs/ARCHITECTURE.md` obligatoire** : tout projet doit avoir ce fichier décrivant architecture.
- **ADRs** : chaque décision architecturale majeure → fichier `docs/adr/NNN-titre.md`.
- **Versions à jour** dans `.puml` : Java (**21**), Quarkus (**3.35**).
- **Source de vérité chemins API** : classes `*APIEnum.java` dans `src/main/java/.../api/enums/`.
- Diagrammes C3 PlantUML (`.puml`) dans `gestion-budget-ihm.wiki/schemas/` — signaler agent Doc IHM si MAJ nécessaire.
- Nouvelle version app livrée → ajouter entrée fichier historique **en tête**.
- Index `.claude/plans/README.md` reste synthétique : **plans + statut global uniquement** (sans phases).

## Coordination avec le wiki IHM

- `Conception-globale.md` référence images C4 hébergées `gestion-budget-ihm.wiki/schemas/`.
- Nouveau microservice ou modif architecture globale → refléter aussi dans `gestion-budget-ihm.wiki/ConceptionIHM.md` + `Historique-de-l'Architecture.md`.

## Checklist de conformité wiki + C4 (obligatoire)

### Trigger de MAJ docs/wiki/C4
- Déclencher checklist dès changement touchant architecture backend, contrat API (endpoint/méthode/rôle), flux inter-services, ou schéma C4.

### Contrôle des versions stack
- Vérifier alignement versions entre `README.md`, `docs/ARCHITECTURE.md`, pages wiki serverless, diagrammes C4 concernés.
- Toujours contrôler versions réelles `pom.xml` avant publication.

### Contrôle des endpoints (méthode / chemin / rôle)
- Contrôler pour chaque endpoint impacté :
  - **Méthode HTTP**
  - **Chemin**
  - **Rôle(s) autorisé(s)**
- Source vérité obligatoire : classes `*APIEnum.java` + annotations JAX-RS (`@Path`, `@GET/@POST/...`) + `@RolesAllowed`.

### Contrôle des liens wiki et rendus C4
- Vérifier liens wiki vers rendus C4 (hébergés `gestion-budget-ihm.wiki/schemas/`).
- Vérifier rendus référencés correspondent sources PlantUML à jour.
- Corriger lien cassé avant passage `done`.

## Ce que tu ne fais PAS

- Pas modif code source Java (rôle agent Dev 🔵 DEVon).
- Pas création/modif tests (rôle agent QA 🟢 QALvin).
- Pas décisions architecturales (rôle Architecte 🟠 ARCos).