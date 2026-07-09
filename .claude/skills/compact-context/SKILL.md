---
name: "compact-context"
description: "Instructions preCompact pour sessions plans/SDLC. Préserve état courant, supprime blobs skills révolus. Appliqué automatiquement."
applyTo: "**"
---

# Skill : Compact contextuel — Sessions Plans / SDLC

> Skill donne instructions `/compact` optimisées, sessions Plans d'Action ou workflows SDLC multi-phases.
> But : évite accumulation blobs skills (~4-8KB chacun) entre phases.

---

## Quand compacter

Compacter **avant** phase suivante, situations :

- ✅ Fin phase Plan d'Action (AP) — avant lancer T<N+1>.x
- ✅ Après injection skill workflow (sdlc-tech-design, sdlc-deliverable-validation, etc.)
- ✅ Après création/validation plan complet
- ✅ Après 8+ tours sans compact

---

## Instruction preCompact — Sessions Plans d'Action

Utiliser avec `/compact` :

```
Résume en 200 mots max :
- Plan courant : titre, numéro, statut global
- Phase active : numéro, tâches restantes (T<N>.x à T<N>.y), agent assigné
- Décisions clés prises (architecture, technologie, contraintes validées)
- Prochaine action attendue

Supprime entièrement :
- Blobs skill des phases précédentes (contenu <skill-context>)
- Détails des tâches déjà terminées (✅)
- Confirmations courtes ("oui", "go", "ok", "continue")
- Historique de navigation (lectures fichiers, commandes shell sans résultat durable)
```

---

## Instruction preCompact — Sessions SDLC

Workflows SDLC multi-étapes (design → implémentation → validation) :

```
Résume en 150 mots max :
- Étape SDLC courante et son objectif
- Décisions validées (architecture, techno, contraintes)
- Questions/points ouverts non résolus
- Prochaine étape déclenchée

Supprime : blobs skill étapes précédentes, échanges de validation ("oui détaille", confirmations), historique navigation fichiers.
```

---

## Gains typiques

| Situation | Contexte avant compact | Après compact |
|-----------|----------------------|---------------|
| 4 phases AP, 1 skill/phase (~5KB) | ~20KB skill blobs accumulés | ~400 chars résumé |
| Session 20 tours, 3 skills injectés | ~15KB contexte révolus | ~300 chars état courant |

> 💡 Règle : `usage_input_tokens` dépasse 30K dans `/usage` → compacter immédiat.