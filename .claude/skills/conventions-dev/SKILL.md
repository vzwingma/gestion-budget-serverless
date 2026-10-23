---
name: conventions-dev
description: "Conventions de développement du projet [NOM_DU_PROJET] pour 🔵 DEVon : stack et versions, structure, conventions de code, accès réseau, modèles, commandes de vérification. Prioritaire sur le générique."
user-invocable: false
---

# Conventions projet — [NOM_DU_PROJET] (développement)

> **Template** — placeholders `[...]` à remplir via skill `init-harness`. Tant qu'un placeholder subsiste,
> appliquer le comportement générique de l'agent pour la valeur concernée.
> Projet : `[NOM_DU_PROJET]` — [DESCRIPTION_COURTE_DU_PROJET].

## Stack technique

- **[LANGAGE] [VERSION]** — [PARADIGME, ex. typage strict]
- **[FRAMEWORK_PRINCIPAL] [VERSION]** — [CONSIGNE, ex. composants fonctionnels uniquement]
- **[BIBLIOTHÈQUE_UI] [VERSION]** — seule bibliothèque UI autorisée (si applicable)
- **[BIBLIOTHÈQUE_AUTH] [VERSION]** — [CONSIGNE_AUTH, ex. ne jamais manipuler les jetons directement]

## Commandes de vérification (preuves avant handoff)

```bash
[COMMANDE_BUILD]   # build / compilation
[COMMANDE_LINT]    # lint / format
[COMMANDE_TEST]    # tests existants (non-régression)
```

## Structure et nommage

- Code source : `[DOSSIER_SOURCE]` ; organisation : [ORGANISATION, ex. par fonctionnalité, par couche]
- Nommage fichiers : `[CONVENTION_NOMMAGE_FICHIERS]` ; identifiants : `[CONVENTION_NOMMAGE_CODE]`

## Conventions de code

- [CONVENTION_COMPOSANTS_OU_MODULES]
- Gestion d'erreurs : [PATTERN_ERREURS]
- Accès réseau / données : toujours via `[SERVICE_ACCÈS]` ([EXEMPLE_APPEL])
- Modèles / types de données : `[DOSSIER_MODÈLES]`
- Constantes techniques : `[FICHIER_CONSTANTES]` ; énumérations métier : `[FICHIER_ENUMS]`
- Jamais d'URL, clé ou secret en dur dans le code.

<!-- Exemple illustratif (à remplacer par le code réel du projet) :
```typescript
export const MonComposant = ({ prop1 }: MonComposantProps): JSX.Element => { ... };
```
-->

## Ce que DEVon ne fait PAS

- Modifier les fichiers de test `[MOTIF_FICHIERS_TEST]` (rôle 🟢 QALvin).
- Mettre à jour `README.md`, `docs/` ou les instructions d'agents (rôle 🟣 DOCly).
- Introduire une dépendance ou un pattern structurant sans tâche 🟠 ARCos dans le Plan d'Action.
