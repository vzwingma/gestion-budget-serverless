---
name: conventions-architect
description: "Conventions architecturales du projet [NOM_DU_PROJET] pour 🟠 ARCos : couches, état, accès réseau, routage, bibliothèques autorisées, intégrations externes. Prioritaire sur le générique."
user-invocable: false
---

# Conventions projet — [NOM_DU_PROJET] (architecture)

> **Template** — placeholders `[...]` à remplir via skill `init-harness`. Tant qu'un placeholder subsiste,
> appliquer le comportement générique de l'agent pour la valeur concernée.
> Projet : `[NOM_DU_PROJET]` — [DESCRIPTION_COURTE_DU_PROJET].

## Document d'architecture

Lire `docs/ARCHITECTURE.md` (si présent) : stack, couches, composants clés. Toute proposition doit rester
cohérente avec l'existant. Absent → le signaler et suggérer à 🟣 DOCly de le créer en fin d'initiative.

## Conventions architecturales

- **Couches** : `[COUCHE_PRÉSENTATION]` → `[COUCHE_MÉTIER]` → `[COUCHE_ACCÈS_DONNÉES]` → `[COUCHE_UTILITAIRES]`.
- **État partagé** : uniquement via `[MÉCANISME_ÉTAT_PARTAGÉ]` ; pas de nouveau conteneur d'état global
  sans décision explicite.
- **Accès réseau / données** : toujours via `[SERVICE_ACCÈS]` ; pas d'appel direct depuis la présentation.
- **Routage / points d'entrée** : `[STRATÉGIE_ROUTAGE]`, déclarés dans `[FICHIER_ROUTES]`.
- **Bibliothèques autorisées** : `[BIBLIOTHÈQUES_AUTORISÉES]` ; toute nouvelle dépendance structurante =
  décision d'architecture (ADR).

## Décisions d'architecture (ADR)

- Fichier `docs/adr/NNN-titre-court.md` pour chaque décision majeure : nouveau framework ou dépendance
  structurante, changement de pattern, décision de sécurité, choix de structure.
- ARCos prépare le contenu ; 🟣 DOCly rédige le fichier (skill `adr-writing`).

## Intégrations externes

- [SYSTÈME_EXTERNE, ex. API back-end, service tiers] : contrat (URL, paramètres, codes retour) défini avec
  [RESPONSABLE_CONTRAT].
- Configuration des URLs / clés : `[FICHIER_CONFIGURATION]` et variables d'environnement `[FICHIERS_ENV]`.

## Handoff vers ⚫ MAINa

ARCos ne crée ni tâches ni plan. Il livre : analyse comparative (≥ 2 options) + recommandation, puis
découpage **candidat** (tâches logiques + dépendances + effort) comme entrée du Plan d'Action.
