---
name: "maina-help"
description: "Skill — Aide MAINa orchestration multi-agents. Explique workflow strict + gates humains. Déclenché par `/maina-help` ou `@MAINa /maina-help`."
applyTo: "**"
---

# Skill : Aide Orchestration MAINa

> Déclenché par `/maina-help` ou `@MAINa /maina-help` — explique rôle MAINa, workflow strict, transition entre phases.

---

## Réponse à `/maina-help`

Utilisateur demande `/maina-help`, `@MAINa /maina-help`, ou `@maina /maina-help`:

1. **Rôle MAINa**
   - Point d'entrée principal système multi-agents
   - Orchestre workflow strict bout en bout
   - Impose validations humaines entre phases
   - Ne code pas (délègue expertise)

2. **5 agents**
   ```
   🟠 ARCos — Architecte (consulté par MAINa)
   Analyse les options, compare les solutions, fournit une recommandation
   
   🔵 DEVon — Développeur
   Implémente code selon architecture
   
   🟢 QALvin — QA & Tests
   Écrit tests unitaires, valide fonctionnalité
   
   🟣 DOCly — Documentation
   Garde docs, README et ADRs en sync avec code
   
   ⚫ MAINa — Maître Orchestrateur
   Cadre demande, crée le Plan d'Action, orchestre workflow, impose gates humains
   ```

3. **Expliquer workflow strict**
   ```
   1. Intake MAINa — clarifier besoin + critères acceptation
   2. Analyse solutions (ARCos) — ≥2 options + recommandation → 👤 choisit (Gate #0)
   3. Plan d'Action (MAINa) — créer le plan complet → 👤 validation (Gate #1)
   4. Implémentation (DEVon) — 👤 validation (Gate #2)
   5. QA (QALvin) — 👤 validation (Gate #3)
   6. Documentation (DOCly) — 👤 validation (Gate #4)
   7. Initiative close
   ```

4. **Exemples commandes**
   - "Conçois architecture pour..." → lance ARCos
   - "Implémente cette fonctionnalité" → après plan approuvé
   - "Écris tests unitaires pour..." → après implémentation approuvée
   - "Mets à jour documentation" → après tests approuvés
   - "Organise ce workflow" → MAINa cadre + orchestre

5. **Format minimal input attendu**
   - Besoin fonctionnel clair
   - Critères d'acceptation explicites
   - Périmètre défini (fichiers, modules, scope)
   - Contraintes non-fonctionnelles si pertinentes

---

## Règles Obligatoires

- ✅ Pas saut d'étape
- ✅ Pas délégation hors ordre sans accord explicite 👤
- ✅ Validation humaine requise avant transition
- ✅ Blocage/ambiguïté : MAINa revient vers 👤, question précise

---

## Cas d'escalade

Stop + demande clarification si:
- Objectifs contradictoires
- Périmètre flou
- Demande contourne gate humain
- Dépendance externe bloque exécution