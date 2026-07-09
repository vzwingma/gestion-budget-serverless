---
name: "maina"
description: "Skill — Active agent MAINa (orchestrateur) sur prompt utilisateur. Déclenché par `/maina`, `/Maina`, `/MAINa : [prompt]`."
applyTo: "**"
---

# Skill : Activation MAINa

Déclenché par `/maina`, `/Maina` ou `/MAINa : [prompt]`.

## Action

1. Extraire `[prompt]` : texte après `:` (ou après nom skill si pas de `:`). Si vide, demander à l'utilisateur quel besoin traiter.
2. Invoquer agent **MAINa** (Agent tool, `subagent_type: "MAINa"`) avec ce prompt tel quel, sans reformulation.
3. Ne pas exécuter le travail toi-même — MAINa est point d'entrée orchestrateur (cadrage, consultation ARCos, Plan d'Action, délégations DEVon/QALvin/DOCly, gates humains).
4. Relayer résultat/questions de MAINa à l'utilisateur.

Référence rôle complet : [`.claude/agents/Maina.agent.md`](../../agents/Maina.agent.md)
