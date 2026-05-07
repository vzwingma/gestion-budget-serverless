# Phase 1 : Remédiation du code

**Responsable Agent :** Devon (🔵 DEV)  
**Date Début :** 2026-05-07  
**Date Fin :** 2026-05-07  
**Statut :** ✅ COMPLÉTÉE

---

## 📝 Tâches

### T1.1 - Corriger `parametrages` RootAPIResource
**Statut :** ✅ DONE  
**Résultat :** `RootAPIResource` dépend désormais de `IParametrageAppProvider` et le port expose `refreshJwksSigningKeys()`.

### T1.2 - Déplacer `ProjectionBudgetSoldes`
**Statut :** ✅ DONE  
**Résultat :** le type a été déplacé hors de `spi` vers `business/model/budget`, avec imports/refs mis à jour.

### T1.3 - Neutraliser le codec partagé hors `api`
**Statut :** ✅ DONE  
**Résultat :** `ComptePanacheCodec` a été déplacé vers `communs.spi.codecs`, et les usages/tests ont été adaptés.

---

## 📊 Synthèse de Phase

**Tâches Complétées :** 3/3 ✅  
**Critères de Réussite Atteints :** 4/4 ✅  
**Bloqueurs :** Aucun

