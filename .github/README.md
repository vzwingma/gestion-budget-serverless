# 📚 Copilot Templates & Agents 

Ce dépôt contient les **modèles réutilisables** et les **instructions d'agents** pour orchestrer le développement avec Copilot en utilisant une architecture **multi-agents coordonnée**.

---

## 📂 Structure

```
.
├── docs/                                # Documentation versionnée du dépôt
│   ├── ARCHITECTURE.md                  # Architecture de ce dépôt transverse
│   ├── ARCHITECTURE.template.md        # Template à copier dans les projets
│   └── adr/                            # Architecture Decision Records
│       └── ADR-TEMPLATE.md             # Template ADR
│
└── .github/
    ├── agents/                              # Définitions des agents Copilot
    │   ├── Arcos.agent.md                   # Agent planificateur (🟠 ARC - Arcos)
    │   ├── Devon.agent.md                   # Agent implémenteur (🔵 DEV - Devon)
    │   ├── Qalvin.agent.md                  # Agent QA et tests (🟢 QUAL - Qalvin)
    │   └── Docly.agent.md                   # Agent documentation (🟣 DOC - Docly)
    │
    ├── instructions/                        # 🆕 Instructions spécifiques projet
    │   ├── architect.instructions.md        # ARCos — conventions architecturales
    │   ├── dev.instructions.md              # DEVon — stack et conventions code
    │   ├── qa.instructions.md               # QUALvin — tests et commandes
    │   └── doc.instructions.md              # DOCly — documentation et /docs
    │
    ├── prompts/                             # Prompts pour initialiser des tâches
    │   ├── init-copilot-instructions.prompt.md      # 🆕 Initialiser copilot-instructions.md
    │   ├── update-copilot-instructions.prompt.md    # Auditer et mettre à jour les instructions
    │   └── migrate-to-template.prompt.md            # Migrer un projet existant vers le template
    │
    ├── plans/                               # (Optionnel) Exemples de Plans d'Action
    │   ├── README.md                        # Index des plans
    │   └── [plans et rapports]
    │
    ├── copilot-instructions.template.md     # 🆕 Template générique à customiser
    ├── copilot-instructions.md              # Template générique (copie du .template.md)
    └── PLANS.md                             # Guide pour les Plans d'Action
```

---

## 🚀 Quick Start : Initialiser Copilot dans un Nouveau Projet

### Étape 1 : Copier le template

Copier `.github/copilot-instructions.template.md` vers votre projet :

```bash
# Depuis le dépôt transverse vers votre projet
cp .github/copilot-instructions.template.md <votre_projet>/.github/copilot-instructions.md
```

### Étape 2 : Utiliser le prompt d'initiation

Utiliser le prompt **`.github/prompts/init-copilot-instructions.prompt.md`** pour **générer automatiquement** les instructions :

```
👤 Utilisateur: "Initialise les instructions Copilot pour ce projet"
```

Ou avec le CLI Copilot :
```bash
copilot prompt run init-copilot-instructions
```

Le prompt va :
1. ✅ Lire le template
2. ✅ Analyser votre code source
3. ✅ Remplir les placeholders automatiquement
4. ✅ Générer `.github/copilot-instructions.md`

### Étape 3 : Valider et enrichir (optionnel)

Si votre projet a des conventions spécifiques non détectées, utilisez le prompt **`.github/prompts/update-copilot-instructions.prompt.md`** pour auditer et enrichir :

```
👤 Utilisateur: "Complète les instructions Copilot depuis le code source"
```

---

## 📖 Fichiers Clés

### Agents (`.github/agents/`)

Chaque fichier agent définit un rôle, ses responsabilités et comment il interagit avec les autres agents.

| Agent | Rôle | Quand l'utiliser |
|---|---|---|
| **Arkos.agent.md** (🟠 ARC) | Planificateur technique | "Conçois une architecture pour..." |
| **Devon.agent.md** (🔵 DEV) | Implémentateur de code | "Implémente cette fonctionnalité" |
| **Qalvin.agent.md** (🟢 QUAL) | Expert QA et tests | "Écris des tests pour ce composant" |
| **Docly.agent.md** (🟣 DOC) | Gestionnaire documentation | "Mets à jour la documentation" |

Tous les agents sont **génériques et réutilisables** dans n'importe quel projet. Les instructions Copilot spécifiques au projet se trouvent dans `.github/copilot-instructions.md`.

> Les agents sont **génériques**. Ils lisent au démarrage leur fichier `instructions/` correspondant pour les spécificités du projet.

### Prompts (`.github/prompts/`)

Prompts réutilisables pour des tâches récurrentes.

| Prompt | Rôle | Utilisation |
|---|---|---|
| **init-copilot-instructions.prompt.md** | 🆕 Initialiser `copilot-instructions.md` et les fichiers `instructions/` | `copilot prompt run init-copilot-instructions` |
| **update-copilot-instructions.prompt.md** | Auditer et mettre à jour `copilot-instructions.md` et les fichiers `instructions/` | `copilot prompt run update-copilot-instructions` |
| **migrate-to-template.prompt.md** | Migrer un projet existant | `copilot prompt run migrate-to-template` |

### Templates

| Fichier | Rôle | Utilisation |
|---|---|---|
| **copilot-instructions.template.md** | Template générique avec placeholders | Copier et customiser dans un nouveau projet |
| **copilot-instructions.md** | Version "générique par défaut" | Exemple de fichier de base |
| **instructions/*.instructions.md** | 4 templates à compléter par projet | Copier et remplir les placeholders |
| **docs/ARCHITECTURE.template.md** | Template `docs/ARCHITECTURE.md` | Copier dans `docs/ARCHITECTURE.md` du projet cible |
| **docs/adr/ADR-TEMPLATE.md** | Template ADR | Copier dans `docs/adr/NNN-titre.md` pour chaque décision |

### Exemples (`.github/examples/`)

Exemples concrets d'instructions pour différents types de projets.

| Exemple | Type de projet | Utilisation |
|---|---|---|
| **copilot-instructions-domoticz.example.md** | React Native / Expo | Référence pour projets mobiles |

### Documentation

| Fichier | Rôle |
|---|---|
| **PLANS.md** | Guide complet pour créer et exécuter les Plans d'Action |

---

## 🎯 Workflow Typique avec Copilot

```
1️⃣ Utilisateur cadre le besoin
   ↓
2️⃣ Arkos (🟠 ARC) crée un Plan d'Action
   ↓
3️⃣ Devon (🔵 DEV) implémente les tâches
   ↓
4️⃣ Qalvin (🟢 QUAL) écrit les tests
   ↓
5️⃣ Docly (🟣 DOC) met à jour la documentation
   ↓
6️⃣ Phase suivante du plan (retour à 2️⃣)
```

Pour en savoir plus, lire `.github/PLANS.md`.

---

## ✅ Checklist pour Initialiser un Nouveau Projet

- [ ] Copier `.github/copilot-instructions.template.md` → `.github/copilot-instructions.md`
- [ ] Copier `.github/instructions/` → `.github/instructions/` du projet
- [ ] Utiliser le prompt `init-copilot-instructions` pour remplir les sections
- [ ] Remplir les placeholders dans les 4 fichiers `instructions/`
- [ ] Valider que tous les placeholders sont remplacés
- [ ] (Optionnel) Utiliser `update-copilot-instructions` pour enrichir depuis le code
- [ ] Committer `.github/copilot-instructions.md` dans le repo
- [ ] Les agents sont prêts ! Utiliser `/solve` ou les appeler par nom

---

## 📚 Ressources

- **Architecture** : `docs/ARCHITECTURE.md` — architecture de ce dépôt transverse
- **Templates docs** : `docs/ARCHITECTURE.template.md` + `docs/adr/ADR-TEMPLATE.md`
- **Agents génériques** : Présents dans ce dépôt, prêts à l'emploi
- **Prompts réutilisables** : `.github/prompts/` — s'adapter au contexte du projet
- **Templates** : `.github/copilot-instructions.template.md` — customiser pour votre projet
- **Instructions agents** : `.github/instructions/` — à personnaliser par projet
- **Exemples** : `.github/examples/` — références pour différents types de projets
- **Plans d'Action** : `.github/PLANS.md` — guide pour orchestrer le travail multi-phases

---

## 🔄 Maintenance

### Mettre à jour les agents

Si les versions des agents changent (ex: `Devon [v1.8]`), mettre à jour les fichiers `.github/agents/*.md`.

### Mettre à jour les instructions d'un projet

Utiliser le prompt `update-copilot-instructions` régulièrement pour garder les instructions à jour avec le code réel.

---

## 🤝 Contribution

Pour ajouter un nouvel agent, prompt ou template :

1. Créer le fichier dans le dossier approprié (`.github/agents/`, `.github/prompts/`, etc.)
2. Suivre les conventions existantes (format YAML frontmatter pour agents/prompts)
3. Tester dans un projet de sandbox avant de committer
4. Documenter dans ce README

---

**Dernière mise à jour :** 2026-05-05



