# Rapport Phase 1 — Config Renovate customManager

**Plan :** [002_fix_desync_quarkus_renovate.plan.md](../002_fix_desync_quarkus_renovate.plan.md)
**Date :** 2026-07-08
**Agent :** DEVon
**Statut :** ✅ Complétée

## Résumé

Ajout d'un bloc `customManagers` dans `renovate.json` : regex manager suivant `README.md`, `docs/ARCHITECTURE.md`, `.claude/instructions/dev.instructions.md` et `.claude/instructions/orchestrator.instructions.md`, avec `depNameTemplate: io.quarkus:quarkus-bom` et `datasourceTemplate: maven` — alignés sur `quarkus.platform.group-id`/`quarkus.platform.artifact-id` du `pom.xml` racine (`io.quarkus` / `quarkus-bom`, lignes 30-32).

## Fichiers modifiés

- `gestion-budget-serverless/renovate.json` — ajout `customManagers`

## Vérifications effectuées

- `node -e "JSON.parse(require('fs').readFileSync('renovate.json','utf8'))"` → `OK JSON valide`

## Points à valider par le développeur humain (Gate)

- Confirmer que Renovate regroupe bien la prochaine mise à jour Quarkus (pom + 4 docs) dans une seule PR/branche une fois ce fichier mergé sur `master` — observable uniquement au prochain run Renovate réel, pas testable en local.
- Confirmer si la PR Renovate actuellement bloquée nécessite un déblocage manuel (T2.2) ou si Renovate l'amende seul.
