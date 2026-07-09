---
name: "qalvin"
description: "Skill — Active agent QALvin (QA/tests) sur prompt utilisateur. Déclenché par `/qalvin`, `/QALvin`, `/QALvin : [prompt]`."
applyTo: "**"
---

# Skill : Activation QALvin

Déclenché par `/qalvin`, `/QALvin` ou `/QALvin : [prompt]`.

## Action

1. Extraire `[prompt]` : texte après `:` (ou après nom skill si pas de `:`). Si vide, demander quel composant/service tester.
2. Invoquer agent **QALvin** (Agent tool, `subagent_type: "QALvin"`) avec ce prompt tel quel.
3. Ne pas écrire les tests toi-même — QALvin couvre nominal + erreurs + limites, cible ≥80% couverture.
4. Relayer résultats (tests, couverture, échecs) à l'utilisateur.

Référence rôle complet : [`.claude/agents/Qalvin.agent.md`](../../agents/Qalvin.agent.md)
