# Phase 3 : Synthèse, décision et backlog de remédiation

**Responsable Agent :** Arkos (🟠 ARC)  
**Date Début :** 2026-05-07  
**Date Fin :** 2026-05-07  
**Statut :** ✅ COMPLÉTÉE

---

## 📝 Tâches

### T3.1 - Produire le rapport d'audit final
**Statut :** ✅ DONE

**Verdict global :** **Partiellement conforme** à l’architecture hexagonale.

**Écarts confirmés :**
1. **A1 — Majeur** : API dépend d’une implémentation concrète  
   - `parametrages/.../api/override/RootAPIResource.java`  
   - Injection constructeur de `ParametragesService` (au lieu d’un port)

2. **A2 — Majeur** : fuite de couche SPI vers API  
   - `operations/.../api/BudgetsResource.java`  
   - Import de `spi.projections.ProjectionBudgetSoldes` dans la ressource REST

3. **B1 — Mineur** : couplage technique business -> api  
   - `comptes/.../business/model/RegisterPanacheCodecs.java`  
   - Import de `communs.api.codecs.ComptePanacheCodec`

4. **D1/D2 — Majeur** : documentation partiellement désalignée  
   - `docs/ARCHITECTURE.md` ne reflète pas certaines exceptions constatées  
   - endpoints `utilisateurs` documentés avec `PUT` alors que le code expose des `GET`

### T3.2 - Préparer les tâches multi-agents de remédiation
**Statut :** ✅ DONE

**Backlog SQL créé :**
- `feat-hex-remediation-dev` (pending)
- `feat-hex-remediation-qa` (pending, dépend de `feat-hex-remediation-dev`)
- `feat-hex-remediation-doc` (pending, dépend de `feat-hex-remediation-dev`)

---

## 📊 Synthèse de Phase

**Tâches Complétées :** 2/2 ✅  
**Critères de Réussite Atteints :** 3/3 ✅  
**Bloqueurs :** Aucun

**Décision proposée pour validation 👤 humaine :**
- Corriger A1 et A2 immédiatement (priorité haute)
- Traiter B1 soit par refactor léger, soit par dérogation documentée (ADR)
- Aligner `docs/ARCHITECTURE.md` après correction code (ou documenter l’écart temporaire)

