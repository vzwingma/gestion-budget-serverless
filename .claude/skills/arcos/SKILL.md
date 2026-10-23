---
name: arcos
description: "Lance 🟠 ARCos (architecte, lecture seule) en sous-agent sur la demande passée en argument. Usage : /arcos <problème ou décision architecturale>."
argument-hint: "<problème ou décision architecturale>"
disable-model-invocation: true
context: fork
agent: ARCos
background: false
---

Demande transmise par 👤 via `/arcos` :

$ARGUMENTS

- Ignorer un éventuel `:` initial dans la demande.
- Demande vide → répondre uniquement : « Quel problème ou quelle décision architecturale traiter ? »
- Attendu : ≥ 2 options comparées + recommandation motivée, terminer par le point de décision 👤.
- Questions ou blocages → section finale `❓ Questions pour 👤`.
