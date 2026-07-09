---
name: "maina"
description: "Skill — Active agent MAINa (orchestrateur) sur prompt utilisateur. Déclenché par `/maina`, `/Maina`, `/MAINa : [prompt]`."
applyTo: "**"
---

# Skill : Activation MAINa

Déclenché par `/maina`, `/Maina` ou `/MAINa : [prompt]`.

## Action

1. Extraire `[prompt]` : texte après `:` (ou après nom skill si pas de `:`). Vide → demander utilisateur quel besoin traiter.
2. Invoquer agent **MAINa** (Agent tool, `subagent_type: "MAINa"`) avec prompt tel quel, sans reformulation.
3. Pas exécuter travail toi-même — MAINa point d'entrée orchestrateur (cadrage, consultation ARCos, Plan d'Action, délégations DEVon/QALvin/DOCly, gates humains).
4. Relayer résultat/questions MAINa à utilisateur.

Référence rôle complet : [`.claude/agents/Maina.agent.md`](../../agents/Maina.agent.md)