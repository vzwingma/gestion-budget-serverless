# Guide des Plans d'Action (Workspace)

Ce workspace regroupe deux projets avec leurs propres plans détaillés.

## Références actives

- Projet frontend IHM : `gestion-budget-ihm/.github/PLANS.md`
- Projet backend serverless : `gestion-budget-serverless/.github/PLANS.md`

## Règle d'utilisation

1. Déterminer le périmètre de la demande (IHM, Serverless, ou transverse).
2. Ouvrir d'abord le guide PLANS du sous-projet cible.
3. En cas de changement transverse, créer des tâches séparées par sous-projet (`*-dev`, `*-qa`, `*-doc`) avec dépendances explicites.
4. Ajouter une tâche documentation wiki pour chaque sous-projet impacté.

## Convention recommandée pour un plan transverse

- `feat-xxx-ihm-dev`, `feat-xxx-ihm-qa`, `feat-xxx-ihm-doc`
- `feat-xxx-sls-dev`, `feat-xxx-sls-qa`, `feat-xxx-sls-doc`
- dépendances inter-projets explicites si contrat API ou schéma partagé
