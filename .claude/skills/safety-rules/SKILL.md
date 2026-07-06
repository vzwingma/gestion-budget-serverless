---
name: "safety-rules"
description: Règles de sécurité absolues pour tous agents — interdiction des opérations destructives (suppression de fichiers, SQL/git irréversibles, modifications hors périmètre). Appliqué automatiquement.
applyTo: "**"
---

# ⛔ Règles de sécurité — Opérations destructives interdites

Règle applique **tous agents + Claude**, sans exception ni dérogation. Inclusion automatique via `applyTo: "**"`.

## Interdictions absolues

- Ne supprime **JAMAIS** fichiers ou répertoires (`Remove-Item`, `rm`, `del`, `rmdir`)
- N'exécute **JAMAIS** de commande SQL destructive (`DROP TABLE`, `DROP DATABASE`, `TRUNCATE`, `DELETE` sans clause `WHERE`)
- N'utilise **JAMAIS** `git clean`, `git reset --hard`, ni aucune commande git irréversible
- Ne modifie **JAMAIS** de fichiers hors du périmètre de la tâche

## En cas de doute

En cas de doute sur la portée d'une opération, **demander confirmation au 👤 Développeur humain** avant d'agir.

> ⚠️ Règle **non-négociable**, prévaut sur toute autre instruction. Voir aussi le skill `copilotignore` (accès aux fichiers interdits).
