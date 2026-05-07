# Plan d'Action : Remédiation hexagonale

**Document :** `.github/plans/002_remediation_hexagonale.plan.md`  
**Date de création :** 2026-05-07  
**Statut :** 🔄 En cours  
**Objectif Prioritaire :** HIGH

---

## 🎯 Objectif Global

Corriger les écarts hexagonaux confirmés lors de la revue technique AP-001, puis valider et documenter les remédiations.

Le but est de restaurer une séparation stricte des couches, d’aligner la documentation d’architecture avec le code réel, et de garantir qu’aucune régression n’est introduite par les changements de packages/interfaces.

---

## Phase 1 — Remédiation du code

### Contexte
- La revue a confirmé des écarts dans la couche API et des fuites de types techniques.
- Les corrections doivent préserver le comportement métier et les endpoints existants.

### Critères de Réussite
✅ Les ressources API dépendent d’interfaces de ports, pas d’implémentations concrètes  
✅ Les types techniques exposés par l’API sont déplacés hors de `spi`  
✅ Les codecs techniques ne dépendent plus de `communs.api.*`  
✅ Compilation JVM réussie après remédiation

### Tâches (Agent: Devon (🔵 DEV))

#### T1.1 - Corriger `parametrages` RootAPIResource
- **Fichier(s) :** `parametrages/src/main/java/.../api/override/RootAPIResource.java`, `parametrages/src/main/java/.../business/ports/IParametrageAppProvider.java`
- **Couvrir / Implémenter :**
  - Injection via port applicatif
  - Exposition de `refreshJwksSigningKeys()` par le port
- **Acceptation :**
  - ✓ Plus aucune dépendance API → service concret

#### T1.2 - Déplacer `ProjectionBudgetSoldes`
- **Fichier(s) :** `operations/src/main/java/.../spi/projections/ProjectionBudgetSoldes.java` → `operations/src/main/java/.../business/model/budget/ProjectionBudgetSoldes.java`
- **Couvrir / Implémenter :**
  - Mise à jour de tous les imports production et tests
  - Maintien du contrat de retour existant
- **Acceptation :**
  - ✓ Aucun import du type depuis `spi` dans la couche API

#### T1.3 - Neutraliser le codec partagé hors `api`
- **Fichier(s) :** `communs/src/main/java/.../api/codecs/ComptePanacheCodec.java` → `communs/src/main/java/.../spi/codecs/ComptePanacheCodec.java`
- **Couvrir / Implémenter :**
  - Déplacer le codec vers un package technique neutre
  - Mettre à jour `comptes/business/model/RegisterPanacheCodecs.java`
  - Ajuster les tests impactés
- **Acceptation :**
  - ✓ Plus aucun couplage `business -> api` pour le codec

---

## Phase 2 — Validation QA

### Contexte
- Les packages et certaines signatures ont changé.
- Les tests doivent garantir l’absence de régression sur les endpoints et codecs.

### Critères de Réussite
✅ Les tests compilent sur les nouveaux packages  
✅ Les endpoints remédiés restent fonctionnels  
✅ Les codecs déplacés passent les tests encode/decode  
✅ Aucun échec bloquant sur les modules impactés

### Tâches (Agent: Qalvin (🟢 QUAL))

#### T2.1 - Mettre à jour les tests parametrages
- **Fichier(s) :** tests `parametrages`
- **Couvrir / Implémenter :**
  - Ajuster les mocks / assertions liés à `refreshJwksSigningKeys()`
  - Vérifier `_info` et le chemin de service
- **Acceptation :**
  - ✓ Tests compilants et passants

#### T2.2 - Mettre à jour les tests operations
- **Fichier(s) :** tests `operations`
- **Couvrir / Implémenter :**
  - Remplacer les imports de `ProjectionBudgetSoldes`
  - Vérifier les tests de service et API concernés
- **Acceptation :**
  - ✓ Tests de budget passants

#### T2.3 - Mettre à jour les tests comptes/communs
- **Fichier(s) :** tests `comptes` et `communs`
- **Couvrir / Implémenter :**
  - Remplacer les imports du codec déplacé
  - Vérifier le comportement du codec
- **Acceptation :**
  - ✓ Tests du codec passants

---

## Phase 3 — Alignement documentation

### Contexte
- La documentation d’architecture doit refléter la vérité du code après remédiation.

### Critères de Réussite
✅ `docs/ARCHITECTURE.md` aligné avec les endpoints et packages réels  
✅ Les écarts temporaires sont explicitement signalés ou supprimés  
✅ Aucun ADR inutile créé

### Tâches (Agent: Docly (🟣 DOC))

#### T3.1 - Mettre à jour `docs/ARCHITECTURE.md`
- **Fichier(s) :** `docs/ARCHITECTURE.md`
- **Couvrir / Implémenter :**
  - Corriger les endpoints `utilisateurs`
  - Corriger les chemins `ProjectionBudgetSoldes` et `ComptePanacheCodec`
  - Garder le document cohérent avec les corrections code
- **Acceptation :**
  - ✓ Documentation fidèle au code

---

## 📊 Résumé des Tâches par Agent

### Devon (🔵 DEV)
- T1.1 à T1.3 : remédiations de code

### Qalvin (🟢 QUAL)
- T2.1 à T2.3 : mise à jour et exécution des tests

### Docly (🟣 DOC)
- T3.1 : alignement de la documentation d’architecture

---

## 📍 Dépendances entre Phases

```
Phase 1 (Code)
    ↓
Phase 2 (Tests) ← [Phase 1 doit être ✅]
    ↓
Phase 3 (Documentation) ← [Phase 1 doit être ✅]
```

---

## ✅ Critères de Succès Globaux

1. Aucun écart hexagonal critique ne subsiste.
2. Les endpoints impactés continuent de fonctionner.
3. Les tests compilent et passent après remédiation.
4. La documentation reflète la structure réelle du code.
5. Le plan AP-002 est clôturé avec une synthèse fiable.

---

## 🚀 Plan d’Exécution

1. Exécuter Phase 1 (code)
2. Exécuter Phase 2 (tests)
3. Exécuter Phase 3 (documentation)

**Triggers pour démarrer une phase :**
- phase précédente validée
- dépendances techniques résolues
- pas de bloqueur sur les signatures ou packages déplacés

