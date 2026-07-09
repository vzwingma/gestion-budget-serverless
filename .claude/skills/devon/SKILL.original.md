---
name: "devon"
description: "Skill — Active agent DEVon (développeur) sur prompt utilisateur. Déclenché par `/devon`, `/DEVon`, `/DEVon : [prompt]`."
applyTo: "**"
---

# Skill : Activation DEVon

Déclenché par `/devon`, `/DEVon` ou `/DEVon : [prompt]`.

## Action

1. Extraire `[prompt]` : texte après `:` (ou après nom skill si pas de `:`). Si vide, demander quelle fonctionnalité implémenter.
2. Invoquer agent **DEVon** (Agent tool, `subagent_type: "DEVon"`) avec ce prompt tel quel.
3. Ne pas coder toi-même — DEVon implémente selon patterns/conventions projet.
4. Relayer résultat (code produit, blocages) à l'utilisateur.

Référence rôle complet : [`.claude/agents/Devon.agent.md`](../../agents/Devon.agent.md)
