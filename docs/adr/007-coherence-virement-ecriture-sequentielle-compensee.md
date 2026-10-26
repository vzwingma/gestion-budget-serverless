# ADR 007 — Cohérence du virement interne : écriture séquentielle compensée, sans transaction MongoDB

---

**Date :** 2026-10-26
**Statut :** Proposée
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

### Neutres
- Mise à jour de la documentation d'API (wiki serverless, `Conception-globale.md`) et de `docs/ARCHITECTURE.md` si le contrat de réponse du virement est modifié.

---

## Mise en œuvre

- **Fichiers impactés** : `operations/.../business/BudgetService.java` (méthode de virement, environ lignes 502-568) et ses tests.
- **Tâches de suivi** : DEVon — implémenter T1.1d (séquentiel, compensation, propagation d'erreurs, gardes) ; QALvin — tests nominal, échec cible avec compensation réussie, échec de compensation (log `ERROR`), gardes 406 / 400 ; DOCly — mise à jour wiki/architecture ; MAINa — plan IHM pour l'impact des gardes.
- **Date d'effet** : à partir de la Phase 1 du Plan d'Action 004, sous réserve du `GO` 👤 (passage du statut à `Acceptée`).

---

## Références

- Plan d'Action associé : [`.claude/plans/004_revue_architecture_backend.plan.md`](../../.claude/plans/004_revue_architecture_backend.plan.md) (Phase 1, T1.1d)
- [ADR-004](./004-clock-injection-convention.md) — Convention d'injection `Clock` UTC (contexte `BudgetService`)
