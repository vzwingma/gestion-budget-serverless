---
name: "qalvin"
description: "Skill — Active agent QALvin (QA/tests) sur prompt utilisateur. Trigger: `/qalvin`, `/QALvin`, `/QALvin : [prompt]`."
applyTo: "**"
---

# Skill : Activation QALvin

Trigger: `/qalvin`, `/QALvin` ou `/QALvin : [prompt]`.

## Action

1. Extraire `[prompt]` : texte après `:` (ou après nom skill si pas de `:`). Vide → demander quel composant/service tester.
2. Invoquer agent **QALvin** (Agent tool, `subagent_type: "QALvin"`) avec prompt tel quel.
3. Pas écrire tests toi-même — QALvin couvre nominal + erreurs + limites, cible ≥80% couverture.
4. Relayer résultats (tests, couverture, échecs) à utilisateur.

Rôle complet : [`.claude/agents/Qalvin.agent.md`](../../agents/Qalvin.agent.md)