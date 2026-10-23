---
name: conventions-orchestrator
description: "Conventions projet [NOM_DU_PROJET] pour ⚫ MAINa : orchestration, gates humains, contenu des délégations. Prioritaire sur le générique."
user-invocable: false
---

# Conventions projet — [NOM_DU_PROJET] (orchestration)

> **Template** — placeholders `[...]` à remplir via skill `init-harness`. Tant qu'un placeholder subsiste,
> appliquer le comportement générique de l'agent pour la valeur concernée.
> Projet : `[NOM_DU_PROJET]` — [DESCRIPTION_COURTE_DU_PROJET].

## Contexte à vérifier avant délégation

- `README.md`, `docs/ARCHITECTURE.md`, ADR existants (`docs/adr/`)
- [AUTRES_SOURCES_DE_CONTEXTE, ex. CONTRIBUTING.md, tickets]

## Règles d'orchestration propres au projet

- Plan d'Action persistant (`docs/plans/`) obligatoire pour toute demande menant à une modification de
  code, sauf dispense explicite du 👤.
- Consulter 🟠 ARCos pour toute décision d'architecture ou changement structurel.
- Gates #0 → #4 obligatoires ; décisions consignées dans le journal des gates du plan.
- [RÈGLE_PROJET_SUPPLÉMENTAIRE, ex. revue de sécurité obligatoire sur les modules d'authentification]

## Contenu des délégations

### Vers 🟠 ARCos
Inclure : besoin, contraintes [STACK_PRINCIPALE], couches impactées, exigences non fonctionnelles, liens
`docs/ARCHITECTURE.md` et ADR pertinents.
Attendu : ≥ 2 options, avantages/inconvénients/risques/impacts, recommandation, besoin d'ADR éventuel.

### Vers 🔵 DEVon
Inclure : phase validée, fichiers cibles, comportement attendu, contraintes [CONTRAINTES_CODE, ex. typage
strict], interdiction d'élargir le scope, commandes de vérification (`[COMMANDE_BUILD]`, `[COMMANDE_LINT]`,
`[COMMANDE_TEST]`).
Attendu : code focalisé, liste des fichiers modifiés, hypothèses, preuves de vérification.

### Vers 🟢 QALvin
Inclure : changements DEVon, cas nominaux, erreurs, limites, composants/services à couvrir, commande de
test ciblée.
Attendu : tests créés/modifiés, résultats, couverture, points bloquants.

### Vers 🟣 DOCly
Inclure : changements publics, décisions d'architecture, fichiers modifiés, comportements à documenter,
entrée de changelog éventuelle.
Attendu : docs synchronisées sans réécriture inutile, liens cohérents, ADR si décision majeure.

## Ce que MAINa ne fait pas

- Coder, tester ou documenter à la place des agents (sauf tâche triviale explicitement demandée).
- Décider une architecture majeure sans ARCos ni validation 👤.
- Clôturer une initiative sans validation 👤 des livrables.
- Inventer des conventions absentes du code ou de la documentation.
