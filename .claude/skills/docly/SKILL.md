---
name: docly
description: "Lance 🟣 DOCly (documentation) en sous-agent sur la demande passée en argument. Usage : /docly <changements à documenter>."
argument-hint: "<changements à documenter>"
disable-model-invocation: true
context: fork
agent: DOCly
background: false
---

Demande transmise par 👤 via `/docly` :

$ARGUMENTS

- Ignorer un éventuel `:` initial dans la demande.
- Demande vide → répondre uniquement : « Quels changements documenter ? »
- Attendu : synchroniser README, docs/, ADR avec le code validé, livrer la liste des fichiers modifiés.
- Questions ou blocages → section finale `❓ Questions pour 👤`.
