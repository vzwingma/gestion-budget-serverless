---
name: "fleet-guide"
description: "Skill — Guide parallélisation `/fleet` pour tous agents. Appliqué automatiquement."
applyTo: "**"
---

# Skill : Parallélisation avec /fleet

> `/fleet` = mode exécution parallèle CLI Claude. Dispatche plusieurs sous-agents simultanément, réduit temps total.

---

## Quand utiliser /fleet

- **Tâches indépendantes, même agent**: plusieurs composants/services/fichiers, zéro dépendance
- **Délégation multi-agents parallèle**: deux agents démarrent simultané (ex: QALvin + DOCly sur même feature après DEVon)
- **Phases parallèles d'un Plan d'Action**: deux phases exécutent simultané

---

## Quand NE PAS utiliser /fleet

- Tâche B **dépend résultat** tâche A
- Deux sous-tâches **modifient même fichier** (risque conflit)
- Fichier setup commun doit créer d'abord

---

## Comment indiquer usage /fleet

Dans plan ou délégation, signaler explicite tâches parallélisables:

```
💡 Ces tâches sont indépendantes → lancer en /fleet :
- Tâche A (Agent X)
- Tâche B (Agent Y)
```

---

## Règle de décision

| Situation | Mode recommandé |
|---|---|
| Tâche B dépend de tâche A | Séquentiel |
| Tâches A et B sans lien | `/fleet` |
| DEVon terminé → QALvin + DOCly | `/fleet` pour QALvin + DOCly |
| Plusieurs éléments indépendants | `/fleet` |