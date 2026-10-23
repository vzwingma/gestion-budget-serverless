# Bloc handoff — fin de réponse de chaque agent exécutant

Chaque agent (🟠 ARCos, 🔵 DEVon, 🟢 QALvin, 🟣 DOCly) termine sa réponse par ce bloc, rempli.
⚫ MAINa s'en sert pour contrôler les preuves et conduire le gate (skill `gate-protocol`).

```yaml
handoff:
  plan: .claude/plans/<NO>_<slug>.plan.md
  phase: <N>
  agent: <ARCos | DEVon | QALvin | DOCly>
  tasks: [T<N>.1, T<N>.2]
  status: done            # done | partial | blocked
  report: .claude/plans/<NO>_reports/PHASE_<N>_COMPLETION_REPORT.md
  files:                  # créés / modifiés
    - path: src/feature/service.ts
      change: "ajout méthode fetchItems + gestion 404"
  criteria:               # critères d'acceptation des tâches
    - id: T<N>.1
      met: true
      evidence: "npm test -- service : 12/12 verts"
  evidence:               # commandes lancées et résultats
    - command: "npm run lint"
      result: "0 erreur"
    - command: "npm test"
      result: "148 passed, 0 failed"
  risks:                  # écarts au plan, dettes, hypothèses
    - "hypothèse : l'API renvoie 404 (non 204) si vide — à confirmer"
  questions: []           # ❓ Questions pour 👤 (posées par MAINa au gate)
  next: "Gate #2 puis 🟢 QALvin — T<N+1>.1 à T<N+1>.3"
```

Règles :
- `status: blocked` → `risks` explique le blocage et `questions` dit ce qu'il faut décider.
- Pas de preuve → critère `met: false`, jamais `true` « par défaut ».
- Agent en lecture seule (ARCos) : `files` vide, `report` = contenu rendu à MAINa.
