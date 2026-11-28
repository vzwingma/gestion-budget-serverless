# ADR 011 — Intégrité des écritures de budgets : verrou optimiste par champ `version`

---

**Date :** 2026-11-28
**Statut :** Proposée
**Décideurs :** 🟠 ARCos + 👤 Développeur humain

---

## Contexte

- Une seule collection, `budgets` (service `operations`), est réécrite en **document entier** par `persistOrUpdate`, sans champ de version. Tous les cas d'usage font : lecture, modification en mémoire, réécriture complète. Avec deux écritures concurrentes, la dernière gagne et une opération est perdue.
- **F5** : un GET budget (compte actif) recalcule et sauvegarde.
- **R2 / R2 bis** ([ADR-007](./007-coherence-virement-ecriture-sequentielle-compensee.md)) : la compensation et la clôture réécrivent depuis un instantané.
- Pas de `@Version` dans Mongo Panache réactif (à vérifier par DEVon).
- #239 a introduit `ConflictException` (409) pour un id d'opération en doublon au virement.

---

## Décision

**Nous avons décidé de** protéger les écritures de `budgets` par un verrou optimiste (champ `version`, écritures conditionnelles, relance bornée), variante **V3** proposée par ARCos, à valider au Gate #1.

- Champ `long version` sur `BudgetMensuel`, `@JsonIgnore` : hors contrat d'API.
- Écritures conditionnelles :
  - `replaceOne({_id, version: v}, {..., version: v+1})` ;
  - `insertOne` pour un nouveau budget (E11000 = conflit) ;
  - `$inc version` sur les mises à jour ciblées (compensation, admin).
- Si aucun document n'est modifié : `ModificationConcurrenteException extends ConflictException` (409).
- Relance automatique bornée (2) du cas d'usage sur conflit, puis 409.
- Le GET n'écrit plus que pour un nouveau budget ou un report de solde M-1 changé. Un conflit sur un GET relit et sert le budget, sans erreur.
- Migration **V003 `InitVersionBudgets`** : `updateMany({version:{$exists:false}}, {$set:{version:0}})`, idempotente, sous le verrou de l'[ADR-006](./006-reservation-atomique-migrations-mongodb.md). Filtre tolérant `$or:[{version:0},{version:{$exists:false}}]` pour `v == 0`.

---

## Alternatives Considérées

### Option 1 (V3) : verrou optimiste complet + GET quasi en lecture seule ✅ Retenue (proposée)

- **Avantages** : plus d'opération perdue ; ferme R2 bis et détecte R2 ; GET en lecture seule dans le cas courant.
- **Inconvénients** : nouveau 409 visible par l'IHM ; relance coûteuse en latence ; migration V003.

### Option 2 (V1) : verrou optimiste seul

- **Raison du rejet** : un GET concurrent (F5) échouerait en erreur.

### Option 3 (V2) : mises à jour ciblées seulement

- **Raison du rejet** : totaux faux en cas de concurrence (R2 généralisé) et effort plus important.

### Option 4 : transaction multi-documents

- **Raison du rejet** : déjà rejetée par l'[ADR-007](./007-coherence-virement-ecriture-sequentielle-compensee.md).

---

## Conséquences

### Positives
- Plus d'opération perdue.
- R2 bis fermé, R2 détecté.
- GET en lecture seule dans le cas courant.

### Négatives / Compromis
- **Nouveau 409** « modification concurrente », visible par l'IHM : à tracer dans le plan IHM 011, avec un message distinct du 409 du virement (id d'opération en doublon).
- Latence supplémentaire en cas de relance (appel à `comptes` refait).
- Migration V003.
- Filtre tolérant temporaire (`version` absente ou égale à 0).

### Neutres
- Schéma OpenAPI inchangé (`version` hors contrat).

---

## Mise en œuvre

- **Fichiers impactés** : `BudgetMensuel`, repository / adaptateur Mongo des budgets (`operations`), cas d'usage d'écriture et de lecture de budget, nouvelle `ModificationConcurrenteException`, migration `V003 InitVersionBudgets` (`communs` / `operations`).
- **Tâches de suivi** : DEVon — T4.2-V, T4.3 (plan 004, Phase 4) ; plan IHM 011 (gestion du nouveau 409) ; DOCly — mise à jour de `ARCHITECTURE.md` (limites R2 / R2 bis de la section virement).
- **Date d'effet** : Phase 4 du plan 004, après Gate #1.

---

## Références

- Plan d'Action : [`004_revue_architecture_backend`](../../.claude/plans/004_revue_architecture_backend.plan.md) (Phases 3 et 4)
- [ADR-002](./002-migrations-mongodb-maison.md), [ADR-006](./006-reservation-atomique-migrations-mongodb.md), [ADR-007](./007-coherence-virement-ecriture-sequentielle-compensee.md)
