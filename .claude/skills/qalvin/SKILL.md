---
name: qalvin
description: "Lance 🟢 QALvin (QA / tests) en sous-agent sur la demande passée en argument. Usage : /qalvin <composant, service ou phase à tester>."
argument-hint: "<composant, service ou phase à tester>"
disable-model-invocation: true
context: fork
agent: QALvin
background: false
---

Demande transmise par 👤 via `/qalvin` :

$ARGUMENTS

- Ignorer un éventuel `:` initial dans la demande.
- Demande vide → répondre uniquement : « Quel composant, service ou phase tester ? »
- Attendu : couvrir nominal + erreurs + limites, exécuter les tests, livrer résultats + couverture.
- Questions ou blocages → section finale `❓ Questions pour 👤`.
