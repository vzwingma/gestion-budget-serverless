# Plan d'Action 002 — Fix désync version Quarkus (pom.xml vs docs) sur PR Renovate

**Date création :** 2026-07-08
**Statut :** 🔵 En cours
**Porteur :** ⚫ MAINa

---

## Contexte

CI job **"CI - native snapshot apps"** (`.github/workflows/build-on-master.yml`, job `build-communs`, étape *"Check Quarkus version sync (pom.xml vs docs)"*, lignes 32-51) échoue sur une PR Renovate.

Cette étape est volontaire (ajoutée lors du plan 001 modernisation stack) : elle grep `quarkus.platform.version` dans `pom.xml`, puis compare aux mentions `Quarkus X.Y.Z` dans 4 fichiers doc/instructions. Si divergence → `exit 1`.

**Cause racine** : `renovate.json` a `automerge: true` pour minor/patch. Renovate bump `quarkus.platform.version` dans `pom.xml` automatiquement, mais ne touchait aucun des 4 fichiers doc qui mentionnent la version en dur :
- `README.md:27`
- `docs/ARCHITECTURE.md:14`, `:181`, `:197`
- `.claude/instructions/dev.instructions.md:27`
- `.claude/instructions/orchestrator.instructions.md:24`, `:77`

Sur `master`, tout était cohérent à `3.37.1` (pas de correction requise sur le contenu doc). Le problème apparaissait uniquement sur la branche de la PR Renovate en cours (`pom.xml` bumpé à `3.37.2`, docs restées à `3.37.1`).

**Analyse préalable (Gate #0)** : options comparées avec le développeur — (A) Renovate customManager pour suivre les fichiers doc dans la même PR/branche que le bump `pom.xml` ; (B) désactiver `automerge` pour Quarkus + revue manuelle avant merge ; (C) les deux combinées. **Option A retenue** — élimine la dérive de façon permanente sans toil manuel récurrent.

---

## Objectif Global

Faire en sorte que toute mise à jour Quarkus (`quarkus.platform.version` dans `pom.xml`) déclenchée par Renovate mette aussi à jour, dans la même PR, les mentions de version en dur dans les 4 fichiers doc/instructions — pour que le check CI de synchronisation ne puisse plus jamais échouer par dérive.

**Périmètre exclu** : révision de la politique `automerge` générale de Renovate (hors scope) ; ajout d'un ADR (fix outillage CI, pas décision d'architecture majeure — pas d'ADR requis).

---

## Phase 1 — Config Renovate ✅ complétée

### Contexte
Ajouter un `customManager` regex Renovate suivant les 4 fichiers doc, avec le même `depName`/`datasource` que la dépendance Quarkus détectée par le manager `maven` natif dans `pom.xml` (`groupId=io.quarkus`, `artifactId=quarkus-bom` → `depName: io.quarkus:quarkus-bom`, `datasource: maven`), pour que Renovate regroupe la mise à jour doc + pom dans la même branche/PR.

### Critères de réussite
- `renovate.json` syntaxiquement valide (JSON.parse OK)
- `customManagers[0].depNameTemplate` = `io.quarkus:quarkus-bom` (aligné avec `quarkus.platform.group-id`/`quarkus.platform.artifact-id` du `pom.xml` racine, lignes 30-32)
- Regex `matchStrings` calquée sur celle du check CI (`Quarkus[^0-9]{0,15}[0-9]+\.[0-9]+(\.[0-9]+)?`) pour garantir la symétrie de détection

### Tâches

#### T1.1 - Ajouter bloc `customManagers` dans `renovate.json` ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `gestion-budget-serverless/renovate.json`
- **Couvrir :** 4 `fileMatch` (README.md, docs/ARCHITECTURE.md, dev.instructions.md, orchestrator.instructions.md), `matchStrings` regex `Quarkus[^0-9]{0,15}(?<currentValue>\d+\.\d+\.\d+)`, `depNameTemplate: io.quarkus:quarkus-bom`, `datasourceTemplate: maven`, `versioningTemplate: semver`
- **Acceptation :** ✅ JSON valide (`node -e "JSON.parse(...)"` OK), champs conformes au `pom.xml` racine

**Effort :** XS. **Risque :** faible (config déclarative, aucun impact runtime app). **Dépendances :** aucune.

---

## Phase 2 — Vérification & déblocage PR en cours

### Contexte
Une fois `renovate.json` mergé sur `master`, Renovate doit détecter le nouveau `customManager` au run suivant et regrouper les mises à jour doc avec le prochain bump `quarkus-bom`. Il faut vérifier que ça fonctionne réellement (regex Renovate valide, regroupement en une seule PR) et débloquer la PR Renovate actuellement désynchronisée si le fix n'arrive pas à temps pour l'amender automatiquement.

### Critères de réussite
- `npx --package renovate -- renovate-config-validator` (ou équivalent) passe sur `renovate.json`
- Prochain run Renovate (ou Dependency Dashboard) confirme que doc + pom bougent ensemble sur une même branche/PR
- Job `build-communs` (étape check version sync) passe en vert sur la PR Renovate concernée

### Tâches

#### T2.1 - Valider config Renovate localement
- **Agent :** QALvin
- **Fichier(s) :** `gestion-budget-serverless/renovate.json`
- **Couvrir :** exécuter le validateur Renovate en local/CI si outillage disponible ; sinon revue manuelle stricte de la syntaxe et des champs contre la doc Renovate `customManagers`
- **Acceptation :** validation sans erreur, ou revue manuelle documentée si outil indisponible

#### T2.2 - Débloquer la PR Renovate en cours si nécessaire
- **Agent :** DEVon (ou 👤 humain directement)
- **Fichier(s) :** README.md, docs/ARCHITECTURE.md, .claude/instructions/dev.instructions.md, .claude/instructions/orchestrator.instructions.md (sur la branche de la PR Renovate concernée, uniquement si toujours désynchronisée après merge du fix)
- **Couvrir :** aligner les mentions `Quarkus 3.37.1` → `3.37.2` (ou version alors en cours) si Renovate n'a pas pu amender la PR automatiquement
- **Acceptation :** check CI "Check Quarkus version sync" vert sur cette PR

#### T2.3 - Confirmer non-régression sur un futur bump Quarkus
- **Agent :** QALvin
- **Fichier(s) :** N/A (observation Dependency Dashboard / prochaine PR Renovate Quarkus)
- **Couvrir :** vérifier qu'un futur bump minor/patch Quarkus produit une seule PR touchant `pom.xml` + les 4 fichiers doc simultanément
- **Acceptation :** rapport confirmant le comportement observé (ou risque résiduel documenté si pas encore observable à la clôture)

**Effort :** S. **Risque :** faible. **Dépendances :** Phase 1.

---

## Phase 3 — Documentation ✅ complétée

### Contexte
Documenter le mécanisme pour que les futurs contributeurs comprennent pourquoi les fichiers doc sont suivis par Renovate.

### Critères de réussite
- Section courte dans `docs/ARCHITECTURE.md` expliquant le lien `renovate.json` customManager ↔ check CI version sync

### Tâches

#### T3.1 - Documenter le mécanisme customManager ✅ complétée
- **Agent :** DOCly
- **Fichier(s) :** `gestion-budget-serverless/docs/ARCHITECTURE.md`
- **Couvrir :** courte section (CI/Renovate) expliquant que les mentions Quarkus dans README/ARCHITECTURE/instructions sont suivies par un `customManager` Renovate pour rester synchronisées avec `pom.xml`, en lien avec le check CI existant
- **Acceptation :** ✅ section "Renovate — synchronisation version Quarkus" ajoutée sous "Pipeline CI/CD"

**Effort :** XS. **Risque :** nul. **Dépendances :** Phase 2.

---

## Critères de succès globaux

- [ ] `renovate.json` contient le `customManager` fonctionnel, JSON valide
- [ ] PR Renovate en cours débloquée (check CI vert)
- [ ] Un futur bump Quarkus regroupe pom + docs en une seule PR (confirmé ou à surveiller si pas encore observé)
- [ ] `docs/ARCHITECTURE.md` documente le mécanisme
- [ ] Pas d'ADR requis (fix outillage CI, pas décision architecturale majeure) — décision explicite tracée dans ce plan
- [ ] Validation humaine obtenue à chaque gate avant transition

---

## Rapports de phase

Voir `.claude/plans/002_reports/`.
