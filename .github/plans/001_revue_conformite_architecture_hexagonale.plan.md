# Plan d'Action : Revue de conformité à l'architecture hexagonale

**Document :** `.github/plans/001_revue_conformite_architecture_hexagonale.plan.md`  
**Date de création :** 2026-05-07  
**Statut :** ✅ Complété  
**Objectif Prioritaire :** HIGH

---

## 🎯 Objectif Global

Valider formellement que `gestion-budget-serverless` respecte l'architecture hexagonale décrite dans `.github/instructions/architect.instructions.md` et `docs/ARCHITECTURE.md`.

Le résultat attendu est un verdict objectivé (conforme, partiellement conforme, non conforme), avec preuves techniques, risques, et plan de remédiation orchestré entre 🔵 DEVon, 🟢 QUALvin et 🟣 DOCly.

---

## Phase 1 — Audit de conformité du code

### Contexte
- Le projet est organisé en couches `api/`, `business/`, `spi/`, mais la conformité réelle doit être vérifiée module par module.
- L'analyse doit couvrir les 5 modules : `communs`, `parametrages`, `utilisateurs`, `comptes`, `operations`.
- Des signaux préliminaires d'écart existent sur certaines dépendances de couche.

### Critères de Réussite
✅ 100% des ressources REST auditées sur le type d'injection (`interface` vs `implémentation`)  
✅ 100% des services audités sur leurs dépendances sortantes (ports/interfaces)  
✅ Matrice de conformité produite avec preuves (fichier + règle violée)  
✅ Chaque écart classé en Critique / Majeur / Mineur

### Tâches (Agent: Arkos (🟠 ARC))

#### T1.1 - Auditer la couche API
- **Fichier(s) :** `**/src/main/java/**/api/**/*Resource.java`
- **Couvrir / Implémenter :**
  - Vérifier que l'API dépend de ports (`I*AppProvider`) et non d'implémentations concrètes
  - Vérifier l'absence d'accès direct à `DatabaseAdaptor`/repository
  - Vérifier les éventuelles fuites de types provenant de `spi` dans les contrats API
- **Acceptation :**
  - ✓ Liste exhaustive des ressources avec statut conforme/non conforme
  - ✓ Preuve pour chaque écart (fichier + import/injection)

#### T1.2 - Auditer la couche business
- **Fichier(s) :** `**/src/main/java/**/business/**/*.java`
- **Couvrir / Implémenter :**
  - Vérifier que les services implémentent les interfaces `business/ports`
  - Vérifier que les dépendances sortantes passent par interfaces (`I*Repository`, `I*ServiceProvider`)
  - Identifier toute dépendance inverse vers `api`
- **Acceptation :**
  - ✓ Matrice business produite (service -> ports -> adaptateurs)
  - ✓ 0 dépendance inverse non justifiée

#### T1.3 - Auditer la couche SPI
- **Fichier(s) :** `**/src/main/java/**/spi/**/*.java`
- **Couvrir / Implémenter :**
  - Vérifier que les adaptateurs implémentent les ports sortants
  - Vérifier l'absence de logique métier non attendue dans SPI
- **Acceptation :**
  - ✓ Liste des adaptateurs et interfaces implémentées
  - ✓ Écarts SPI documentés avec sévérité

---

## Phase 2 — Revue documentation vs code réel

### Contexte
- `docs/ARCHITECTURE.md` existe et sert de référence.
- Une divergence code/documentation peut masquer une non-conformité ou créer une dette d'architecture.

### Critères de Réussite
✅ 100% des sections architecture critiques confrontées au code (`couches`, `flux`, `règles`)  
✅ Écarts doc/code explicitement listés  
✅ Recommandations de mise à jour documentation prêtes pour 🟣 DOCly

### Tâches (Agent: Arkos (🟠 ARC))

#### T2.1 - Vérifier la cohérence de `docs/ARCHITECTURE.md`
- **Fichier(s) :** `docs/ARCHITECTURE.md`, `**/src/main/java/**/*.java`
- **Couvrir / Implémenter :**
  - Comparer la règle de dépendance des couches à la réalité des imports/injections
  - Vérifier que les flux inter-µServices décrits correspondent aux providers SPI réellement utilisés
  - Vérifier les conventions documentées (ports, exceptions à éviter)
- **Acceptation :**
  - ✓ Liste claire “Conforme / À corriger / Ambigu” par section
  - ✓ Preuves attachées aux sections non conformes

---

## Phase 3 — Synthèse, décision et backlog de remédiation

### Contexte
- Après audit code + doc, il faut produire un livrable exploitable pour décision humaine.
- Les corrections potentielles doivent être orchestrées et séquencées entre agents.

### Critères de Réussite
✅ Verdict global publié et argumenté  
✅ Backlog de remédiation complet (DEV/QA/DOC) avec dépendances  
✅ Critères d'acceptation mesurables pour chaque tâche de correction

### Tâches (Agent: Arkos (🟠 ARC))

#### T3.1 - Produire le rapport d'audit final
- **Fichier(s) :** `.github/plans/001_reports/PHASE_3_COMPLETION_REPORT.md`
- **Couvrir / Implémenter :**
  - Synthèse des écarts confirmés (code + doc)
  - Classification par sévérité + impact
  - Recommandation d'arbitrage (corriger immédiatement vs accepter temporairement)
- **Acceptation :**
  - ✓ Verdict unique (conforme / partiellement conforme / non conforme)
  - ✓ Décisions proposées pour validation 👤 développeur humain

#### T3.2 - Préparer les tâches multi-agents de remédiation
- **Fichier(s) :** `todos` SQL + rapport de phase
- **Couvrir / Implémenter :**
  - Créer tâches 🔵 DEVon (corrections de dépendance)
  - Créer tâches 🟢 QUALvin (tests de non-régression)
  - Créer tâches 🟣 DOCly (mise à jour `docs/ARCHITECTURE.md` et ADR si décision majeure)
- **Acceptation :**
  - ✓ Tâches numérotées, dépendances explicites, critères mesurables
  - ✓ Prêtes à exécution après validation humaine

---

## 📊 Résumé des Tâches par Agent

### Arkos (🟠 ARC) Agent
- T1.1 à T1.3 : Audit code par couche
- T2.1 : Audit documentation architecture
- T3.1 à T3.2 : Synthèse, décision, backlog de remédiation
- **Livrable :** Rapport de conformité + plan de remédiation exécutable

### Devon (🔵 DEV) Agent
- Remédiations de code (si écarts confirmés) : dépendances de couches, types exposés, alignement hexagonal
- **Livrable :** Code conforme à la règle d'architecture

### Qalvin (🟢 QUAL) Agent
- Tests de non-régression des zones impactées par remédiation
- **Livrable :** Suite de tests validant l'absence de régression

### Docly (🟣 DOC) Agent
- Mise à jour `docs/ARCHITECTURE.md` et ADR si décision architecturale
- **Livrable :** Documentation alignée sur l'architecture réelle

---

## 📍 Dépendances entre Phases

```
Phase 1 (Audit code)
    ↓
Phase 2 (Audit documentation) ← [Phase 1 recommandée pour preuves techniques]
    ↓
Phase 3 (Synthèse + backlog) ← [Phases 1 et 2 doivent être ✅]
```

---

## ✅ Critères de Succès Globaux

1. Une matrice de conformité hexagonale complète est disponible.
2. Chaque écart est prouvé (fichier + motif + sévérité).
3. Le verdict global est explicite et validable.
4. Les remédiations Dev/QA/Doc sont prêtes à exécution.
5. La documentation d'architecture est alignée avec la réalité validée.

---

## 🚀 Plan d'Exécution

1. Exécuter Phase 1 (audit code)
2. Exécuter Phase 2 (audit documentation)
3. Exécuter Phase 3 (synthèse et backlog)

**Triggers pour démarrer une phase :**
- phase précédente marquée ✅ dans son rapport
- aucun bloqueur non résolu
- validation 👤 développeur humain sur les points d'arbitrage

