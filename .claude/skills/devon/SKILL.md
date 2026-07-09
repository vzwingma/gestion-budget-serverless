---
name: "devon"
description: "Skill — active agent DEVon (développeur) sur prompt user. Déclenché par `/devon`, `/DEVon`, `/DEVon : [prompt]`."
applyTo: "**"
---

# Skill : Activation DEVon

Déclenché par `/devon`, `/DEVon` ou `/DEVon : [prompt]`.

## Action

1. Extraire `[prompt]` : texte après `:` (ou après nom skill si pas de `:`). Vide → demander quelle fonctionnalité implémenter.
2. Invoquer agent **DEVon** (Agent tool, `subagent_type: "DEVon"`) avec prompt tel quel.
3. Pas coder toi-même — DEVon implémente selon patterns/conventions projet.
4. Relayer résultat (code produit, blocages) à user.

Référence rôle complet : [`.claude/agents/Devon.agent.md`](../../agents/Devon.agent.md)