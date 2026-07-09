---
name: "safety-rules"
description: Règles de sécurité absolues pour tous agents — interdiction des opérations destructives (suppression de fichiers, SQL/git irréversibles, modifications hors périmètre). Appliqué automatiquement.
applyTo: "**"
---

# ⛔ Règles de sécurité — Opérations destructives interdites

Règle: tous agents + Claude, sans exception. Auto via `applyTo: "**"`.

## Interdictions absolues

- Jamais supprimer fichiers/répertoires (`Remove-Item`, `rm`, `del`, `rmdir`)
- Jamais SQL destructif (`DROP TABLE`, `DROP DATABASE`, `TRUNCATE`, `DELETE` sans `WHERE`)
- Jamais `git clean`, `git reset --hard`, ni commande git irréversible
- Jamais modifier fichiers hors périmètre tâche

## En cas de doute

Doute sur portée opération → demander confirmation 👤 Développeur humain avant agir.

> ⚠️ Règle non-négociable, prévaut sur toute autre instruction. Voir aussi skill `copilotignore` (accès fichiers interdits).