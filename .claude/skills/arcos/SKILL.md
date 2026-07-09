---
name: "arcos"
description: "Skill — Active agent ARCos (architecte) sur prompt utilisateur. Déclenché par `/arcos`, `/ARCos`, `/ARCos : [prompt]`."
applyTo: "**"
---

# Skill : Activation ARCos

Déclenché par `/arcos`, `/ARCos` ou `/ARCos : [prompt]`.

## Action

1. Extraire `[prompt]` : texte après `:` (ou après nom skill si pas de `:`). Si vide, demander quel problème/décision architecturale traiter.
2. Invoquer agent **ARCos** (Agent tool, `subagent_type: "ARCos"`) avec ce prompt tel quel.
3. Ne pas analyser toi-même — ARCos fournit ≥2 options comparées + recommandation motivée.
4. Relayer analyse à l'utilisateur pour décision (Gate #0 si dans workflow MAINa).

Référence rôle complet : [`.claude/agents/Arcos.agent.md`](../../agents/Arcos.agent.md)
