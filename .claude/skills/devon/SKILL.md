---
name: devon
description: "Lance 🔵 DEVon (développeur) en sous-agent sur la demande passée en argument. Usage : /devon <fonctionnalité ou tâche T*.* à implémenter>."
argument-hint: "<fonctionnalité ou tâche T*.* à implémenter>"
disable-model-invocation: true
context: fork
agent: DEVon
background: false
---

Demande transmise par 👤 via `/devon` :

$ARGUMENTS

- Ignorer un éventuel `:` initial dans la demande.
- Demande vide → répondre uniquement : « Quelle fonctionnalité ou quelle tâche du Plan d'Action implémenter ? »
- Attendu : implémenter dans le périmètre, livrer fichiers modifiés + preuves (build, lint, tests existants).
- Questions ou blocages → section finale `❓ Questions pour 👤`.
