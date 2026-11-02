# ADR 007 — Cohérence du virement interne : écriture séquentielle compensée, sans transaction MongoDB

---

**Date :** 2026-10-26
**Statut :** Acceptée (Gate #4 du 2026-10-30)
**Décideurs :** 🟠 ARCos + 👤 Développeur humain

---

## Contexte

La revue d'architecture backend (Plan [004](../../.claude/plans/004_revue_architecture_backend.plan.md), Phase 1, tâche T1.1d) relève plusieurs défauts dans le traitement du **virement interne** entre deux comptes, dans `operations/.../business/BudgetService.java` (environ lignes 502-568) :

- **erreur avalée** : un échec d'écriture n'est pas propagé à l'appelant ;
- **écritures parallèles** : les 2 écritures (source et cible) sont lancées en parallèle via `Uni.combine` ; un **débit sans crédit** est donc possible si l'une échoue et pas l'autre ;
- **double `getCompteById`** : le compte destination est lu deux fois ;
- **`ligneOperation` modifiée de façon concurrente** : le même objet est muté pendant les écritures parallèles ;
- **aucune garde** : ni contrôle du statut du compte destination, ni contrôle source ≠ destination.

Contraintes : base MongoDB Atlas (replica set, mais le **tier n'est pas confirmé** — ne pas supposer M0) ; Panache réactif ; développement local en MongoDB standalone.

---

## Décision

**Nous avons décidé de** réaliser le virement par écritures séquentielles (source puis cible) avec compensation en cas d'échec de la cible, sans transaction MongoDB (option B2).

Règles :

- **Écritures séquentielles** : opération source d'abord, puis opération cible.
- **Compensation** : si l'écriture cible échoue, l'opération source est retirée.
- **Échec de compensation** : log `ERROR` portant les **2 identifiants** (opération source et opération cible) pour reprise manuelle.
- **Erreurs propagées** à l'appelant (plus d'erreur avalée) ; **un seul appel** `getCompteById` pour le compte destination.
- **Mode dégradé réservé à l'affichage** : à l'affichage d'un budget M existant (GET budget), une erreur Mongo sur M-1 est loggée en `ERROR` et M est servi avec son solde stocké, sans enregistrement. Sur un chemin d'**écriture** (chargement du budget du compte destination du virement), une erreur technique sur M-1 **fait échouer** l'opération (503) et déclenche la compensation de la source : un solde de report périmé n'est jamais enregistré. Les exceptions non Mongo sur M-1 (donnée corrompue : 500, NPE) remontent dans les deux modes.
- **Gardes** : **406** si le compte destination est clos ; **400** si source = destination. Ces gardes sont une décision 👤 ; leur **impact IHM est à tracer dans un plan IHM**.

---

## Alternatives Considérées

### Option B2 : Écriture séquentielle compensée ✅ Retenue

- **Avantages** : aucune dépendance à l'infrastructure (fonctionne en replica set comme en standalone local) ; compatible Panache réactif sans gestion de session ; supprime le débit sans crédit dans le cas nominal d'échec de la cible ; erreurs enfin visibles de l'appelant ; périmètre de modification limité à `BudgetService`.
- **Inconvénients** : pas d'atomicité stricte : si la compensation elle-même échoue, une incohérence persiste ; logique de compensation à maintenir et à tester.

### Option B1 : Transaction multi-documents MongoDB

- **Avantages** : atomicité stricte (tout ou rien).
- **Inconvénients** : Atlas est en replica set, mais le tier n'est pas confirmé ; Panache réactif ne fournit pas de `ClientSession` (utilisation du driver réactif brut) ; gestion à prévoir de `TransientTransactionError` (retry) ; le développement local est en standalone, où les transactions ne sont pas disponibles.
- **Raison du rejet** : coût et risque d'implémentation et d'infrastructure disproportionnés. **Évolution possible** si une atomicité stricte devient exigée.

### Option B3 : Mise à jour ciblée idempotente

- **Avantages** : reprise sûre en cas de rejeu.
- **Inconvénients** : soldes calculés en Java ; complexité accrue ; changement du format d'identifiant des opérations.
- **Raison du rejet** : complexité et impact sur le modèle de données hors proportion avec le besoin.

---

## Conséquences

### Positives
- Fin des erreurs avalées et des écritures parallèles sur le même objet (`ligneOperation`).
- Cas nominal d'échec de la cible compensé : plus de débit sans crédit.
- Un seul accès au compte destination.
- Entrées invalides refusées explicitement (406 destination close, 400 source = destination).

### Négatives / Compromis
- Une **fenêtre d'incohérence rare** reste possible si la compensation échoue ; elle est tracée en `ERROR` avec les 2 identifiants (reprise manuelle).
- Nouveaux codes de retour (406, 400) : **impact IHM** (gestion des erreurs, messages) à traiter dans un plan IHM dédié.
- Latence légèrement accrue (écritures séquentielles au lieu de parallèles).

### Limites connues
- **Risque R2 accepté — soldes et totaux de la compensation** : la compensation du virement réécrit `soldes`, `totauxParCategories`, `totauxParSSCategories`, `totauxParTypeCategories` et `dateMiseAJour` avec l'instantané en mémoire, recalculé après retrait ou restauration de l'opération. Si une écriture concurrente touche une **autre opération du même budget** entre l'écriture source et la compensation, sa ligne est conservée dans `listeOperations`, mais elle n'apparaît pas dans les soldes et totaux stockés tant qu'aucun recalcul n'a eu lieu.
- **Décision 👤 du 2026-10-29** (Gate #2, 2e passage, plan 004) : risque accepté et documenté, **sans contrôle optimiste sur `dateMiseAJour`**. À réévaluer si le problème est observé.
- **Compensation ciblée** (commit `012dee5`) : `$pull` de la ligne en cas de création ; en cas de modification, `$set listeOperations.$[op]` avec `arrayFilters` (ou `$push` si la ligne a disparu, voir ci-dessous). Le budget entier n'est plus réécrit.
- **Réinsertion par `$push` conservée** — décision 👤 du 2026-10-29 (Gate #2, Phase 1, plan 004). Si la ligne d'origine ne se trouve plus dans `listeOperations` au moment de la compensation, elle est réinsérée par `$push` avec `$position`, à son rang d'origine (en fin de liste si ce rang est inconnu), et uniquement si aucune ligne de même id n'est présente.
  - Cas résiduel accepté : une suppression concurrente de cette ligne, faite entre l'écriture source et la compensation, est alors annulée (la ligne revient). Un `WARN` est loggé.
  - Depuis la garde 400 sur `etat == null`, appliquée avant toute écriture, ce chemin ne s'exécute plus que dans ce cas concurrent.
- **Virement interne avec `etat == null`** — décision 👤 du 2026-10-29 (Gate #2, Phase 1, plan 004) : refusé en 400 (`BadParametersException`) avant toute écriture.
- **Compte destination inconnu** — décision 👤 du 2026-10-29 (Gate #2, Phase 1, plan 004) : un 404 du client REST est traduit en `DataNotFoundException` (404).

- **R2 bis — écriture concurrente sur M-1 lors de la clôture** — décision 👤 du 2026-10-29 (Gate #2, 6e passage, plan 004) : la clôture de M-1 relit le document juste avant de le clore, puis l'enregistre en entier (sans champ de version). Une écriture concurrente sur M-1 entre la relecture et l'enregistrement reste possible et serait écrasée. Fenêtre réduite, risque accepté. Correction prévue par un verrou optimiste en Phase 4 / T4.2.

### Neutres
- Documentation mise à jour au Gate #4 : `docs/ARCHITECTURE.md` (section « Codes HTTP et gestion des erreurs ») et wiki serverless (`Conception-globale.md`).
- Impact IHM des codes 406 / 400 / 404 tracé dans le plan IHM 009 (en attente).

---

## Mise en œuvre

- **Fichiers impactés** : `operations/.../business/BudgetService.java` (méthode de virement, environ lignes 502-568) et ses tests.
- **Tâches de suivi** : DEVon — implémenter T1.1d (séquentiel, compensation, propagation d'erreurs, gardes) ; QALvin — tests nominal, échec cible avec compensation réussie, échec de compensation (log `ERROR`), gardes 406 / 400 ; DOCly — mise à jour wiki/architecture ; MAINa — plan IHM pour l'impact des gardes.
- **Date d'effet** : Phase 1 du Plan d'Action 004. Statut `Acceptée` au Gate #4 du 2026-10-30 (implémentation conforme, Gates #2 et #3 `GO`).

### Note de mise en œuvre

Implémentée sur la branche `feat/ap004-phase1-fiabilite-mongo` (PR #224, correctif de tests PR #225), commits principaux (SHA de la branche, à retrouver par message si l'historique a été réécrit au merge) :

| Commit | Contenu |
|---|---|
| `c86abc6` | Virement séquentiel avec compensation, un seul `getCompteById`, propagation des erreurs, gardes 406 / 400 (T1.1d) |
| `012dee5` | Compensation ciblée (`$pull`, `$set` positionnel, port `IOperationsRepository.compenseOperation`), 404 pour un compte destination inconnu |
| `7304d6c` | Compensation avec ligne supprimée en concurrence (`$push`) et identifiant d'opération garanti |
| `4e7d639` | Virement sans `etat` refusé en 400 avant toute écriture, compensation fiabilisée |
| `37988e6` | Relecture de M-1 avant clôture, `DonneeCorrompueException` (500), R5 (absence 404 / erreur Mongo 503) |
| `bfa9b4e` | Mode dégradé réservé à l'affichage : une erreur technique sur M-1 du compte destination fait échouer le virement (503) et compense la source |

Écarts par rapport au texte initial : la compensation est ciblée et non plus une réécriture du budget entier ; la garde 400 couvre aussi `etat == null` ; le compte destination inconnu donne un 404. Les limites R2 et R2 bis sont documentées dans « Limites connues ». Détail : [rapport de phase](../../.claude/plans/004_reports/PHASE_1_COMPLETION_REPORT.md).

---

## Références

- Plan d'Action associé : [`.claude/plans/004_revue_architecture_backend.plan.md`](../../.claude/plans/004_revue_architecture_backend.plan.md) (Phase 1, T1.1d)
- [ADR-004](./004-clock-injection-convention.md) — Convention d'injection `Clock` UTC (contexte `BudgetService`)
