# Instructions Claude — gestion-budget-serverless

> Backend Quarkus/Java 25, app gestion budget, déployé fonctions AWS Lambda natives.
> Frontend dans dépôt compagnon [`gestion-budget-ihm`](../gestion-budget-ihm) (React/TypeScript).
> Config Claude Code (claude.ai/code, CLI, IDEs) — infra orchestrée dev via 5 agents spécialisés.

## 🗿 Mode communication

Mode caveman **full** actif défaut. Règles :
- Supprimer : articles, remplissage, politesse, hedging
- Fragments OK. Synonymes courts. Termes techniques exacts. Code inchangé.
- Désactiver uniquement : `stop caveman` ou `normal mode`

---

## Règle obligatoire MAINa — Plan + ADR

Initiative archi/infra doit produire **avant** marquer tâche terminée :
1. Fichier `Plan d'Action` dans `.claude/plans/NNN_nom.plan.md`
2. ADR dans `docs/adr/NNN-titre-court.md` si décision majeure
3. Mise à jour `.claude/plans/README.md`

Créés même lot que implémentation, pas après coup.

---

## Build, Test et Lint

```bash
# Construire tous les modules (mode JVM)
mvn clean package

# Construire un seul module
mvn clean package -f comptes/pom.xml

# Exécuter tous les tests
mvn clean test

# Exécuter une classe de test spécifique
mvn clean test -Dtest=ComptesServiceTest

# Exécuter une méthode de test spécifique
mvn clean test -Dtest=ComptesServiceTest#testGetComptes

# Exécuter les tests d'un seul module
mvn clean test -f operations/pom.xml

# Construire l'exécutable Linux natif pour Lambda (nécessite GraalVM/Mandrel)
mvn clean package -Pnative -Dquarkus.native.container-build=true

# Exécuter l'analyse SonarCloud (nécessite sonar.token)
mvn verify -Psonar
```

## Architecture

### Projet Maven multi-modules
```
gestion-budget-serverless/
├── communs/          # Bibliothèque partagée : classes de base, modèles, sécurité, exceptions
├── parametrages/     # Microservice : paramètres système  → /parametres/v2/
├── utilisateurs/     # Microservice : auth/profils utilisateur → /utilisateurs/v2/
├── comptes/          # Microservice : comptes bancaires   → /comptes/v2/
└── operations/       # Microservice : budgets et opérations → /budgets/v2/
```

Tous microservices (`comptes`, `operations`, `parametrages`, `utilisateurs`) dépendent `communs`, même structure interne couches.

### Couches architecture hexagonale (par microservice)
```
api/          – Contrôleurs REST JAX-RS, enums des chemins d'API, surcharges exception/sécurité
business/     – Logique métier (services @ApplicationScoped), interfaces de ports, modèles métier
spi/          – Adaptateurs base de données (MongoDB Panache), providers REST inter-services
config/       – Classes de configuration Quarkus (OpenAPI, hints de réflexion GraalVM)
utils/        – Classes utilitaires métier
```

### Patterns clés framework

**Ressources REST** étendent `AbstractAPIInterceptors` (de `communs`), annotations JAX-RS standard :
```java
@Path(ComptesAPIEnum.COMPTES_BASE)
public class ComptesResource extends AbstractAPIInterceptors {
    @Inject IComptesAppProvider services;

    @GET
    @RolesAllowed({ComptesAPIEnum.COMPTES_ROLE})
    @Operation(description = "...")
    public Uni<List<CompteBancaire>> getComptes() { ... }
}
```

**Programmation réactive** : toutes méthodes service + appels DB retournent `Uni<T>` (valeur unique) ou `Multi<T>` (flux) Mutiny. Jamais bloquer `.await().indefinitely()` hors tests.

**Injection dépendances** : CDI uniquement (`@Inject`, `@ApplicationScoped`). Aucune annotation Spring.

**Interfaces ports** : logique métier toujours masquée derrière interface dans `business/ports/` (ex. `IBudgetAppProvider`, `IComptesRepository`). Ressources REST injectent interface, pas implémentation.

**Sécurité** : chaque microservice surcharge `AbstractAPISecurityFilter` + `IJwtSecurityContext` de `communs`. Endpoints déclarent `@RolesAllowed` avec constantes rôle de leur propre `*APIEnum`.

**Appels inter-services** : services besoin données autres microservices injectent interface provider dans `spi/` (ex. `IComptesServiceProvider`, `IParametragesServiceProvider`) appuyée client REST Quarkus.

### Base de données
- **MongoDB** via Quarkus MongoDB Panache (pattern repository, pas Active Record).
- Chaîne connexion : variable env `QUARKUS_MONGODB_CONNECTION_STRING` (défaut `localhost:27017` dev).
- Base dev : `v12-app-dev`. Base prod : variable env `QUARKUS_MONGODB_DATABASE`.
- Config dans `src/main/resources/dev/application.properties` et `src/main/resources/prod/application.properties` par module.

### `communs` module
Partagé tous microservices :
- `api/AbstractAPIResource` – endpoint base `/info`
- `api/AbstractAPIInterceptors` – intercepteurs logs requête/réponse
- `api/security/AbstractAPISecurityFilter` – validation JWT
- `utils/security/JWTUtils`, `SecurityUtils` – parsing JWT, sanitation entrées
- `utils/exceptions/` – exceptions typées (`DataNotFoundException`, `UserNotAuthorizedException`, etc.)
- `data/trace/BusinessTraceContext` – contexte traçage style MDC réinitialisé après chaque réponse
- `aws-deploy/` – templates AWS SAM + config API Gateway

### Conventions test
- Utiliser `@QuarkusTest` sur classes test.
- Mocker dépendances `Mockito.mock()` / `Mockito.spy()` dans `@BeforeEach`.
- Résoudre résultats réactifs tests avec `.await().indefinitely()`.
- `communs` publié GitHub Packages ; POM microservices référencent en dépendance.

## Utilitaires métier clés

### `BudgetDataUtils` (`operations/.../utils/`)
- `cloneOperationToMoisSuivant(LigneOperation)` – clone opération mois suivant : tous champs `SsCategorie` copiés explicitement (id, libelle, **type**).
- `cloneOperationPeriodiqueToMoisSuivant(...)` – appelle `cloneOperationToMoisSuivant()` interne, puis gère périodicité. Fix sur `cloneOperationToMoisSuivant` propage auto aux deux cas.

> ⚠️ Ajout champ dans `LigneOperation.SsCategorie` ou `LigneOperation.Categorie` → ajouter aussi `cloneOperationToMoisSuivant()`.

## Déploiement
- CI build d'abord `communs`, publie GitHub Packages, puis build chaque microservice parallèle image native.
- Images natives déployées AWS Lambda via SAM. Routes API définies `communs/src/aws-deploy/`.
- SonarCloud tourne sur `master` une fois tous builds finis.

---

## 👋 Agents Claude et Rôles

5 agents spécialisés, orchestrés dev humain.

### **⚫ MAINa** [v1.4]

**Rôle** : Maître orchestrateur, créateur Plan d'Action, point entrée principal

**Responsabilités** :
- Comprendre demande, cadrer flux travail
- Consulter ARCos (autres agents) pour analyse solutions avant créer plan
- Créer Plan d'Action complet (skill plan-creation)
- Orchestrer délégations : DEVon → QALvin → DOCly
- Imposer validations humaines entre phases
- Fournir aide via `/maina-help`
- Lire `.claude/instructions/orchestrator.instructions.md` au démarrage

**Quand utiliser** : Workflow complet, orchestration multi-agents

**Livrable** : Plan d'Action validé + orchestration complète, séquencée, traçable

---

### **🟠 ARCos** [v4.7]

**Rôle** : Expert architecture consulté par MAINa

**Responsabilités** :
- Analyser problèmes complexes, concevoir solutions architecturales
- Présenter ≥2 options comparées + recommandation motivée
- Décisions stratégiques techno, structure, approche
- Préparer contenu ADR après décisions archi majeures
- Lire `.claude/instructions/architect.instructions.md` au démarrage
- Lire `docs/ARCHITECTURE.md` au démarrage
- Exécuter tâches T*.* assignées dans Plan d'Action créé par MAINa

**Quand utiliser** : "Analyse options pour...", "Conçois architecture pour...", "Quelle approche pour..."

**Livrable** : Analyse comparative solutions + recommandation motivée

---

### **🔵 DEVon** [v4.3]

**Rôle** : Implémentateur code production

**Responsabilités** :
- Traduire exigences en code fonctionnel testé
- Respecter patterns architecturaux + conventions projet
- Code propre, maintenable, compilant
- Lire `.claude/instructions/dev.instructions.md` au démarrage

**Quand utiliser** : "Implémente cette fonctionnalité", "Code selon architecture"

**Livrable** : Code propre, compilant sans erreurs

---

### **🟢 QALvin** [v4.4]

**Rôle** : Expert assurance qualité et tests

**Responsabilités** :
- Écrire tests unitaires complets (composants, services)
- Couverture test ≥80%
- Tester cas limites, scénarios erreur
- Lire `.claude/instructions/qa.instructions.md` au démarrage

**Quand utiliser** : "Écris tests pour...", "Génère tests unitaires"

**Livrable** : Tests passants, rapports couverture

---

### **🟣 DOCly** [v4.3]

**Rôle** : Gardien documentation

**Responsabilités** :
- Mettre à jour README, `docs/`, guides
- Maintenir `docs/ARCHITECTURE.md` à jour
- Créer ADRs dans `docs/adr/` sur délégation ARCos
- Lire `.claude/instructions/doc.instructions.md` au démarrage

**Quand utiliser** : "Mets à jour doc", "Garde docs en sync"

**Livrable** : Documentation à jour, claire, complète

---

## 🔄 Workflow strict

1. **Cadrage** (développeur) → Besoin + critères
2. **Orchestration** (MAINa) → Déclencher mode PLAN, consulter ARCos
3. **Analyse solutions** (ARCos) → ≥2 options + recommandation
4. **Gate #0** → Choix solution par développeur
5. **Plan d'Action** (MAINa) → Créer plan complet (skill plan-creation)
6. **Gate #1** → Validation plan avant implémentation
7. **Implémentation** (DEVon) → Code tâches assignées
8. **Gate #2** → Validation code avant tests
9. **Tests** (QALvin) → Écrire tests nominaux + erreurs + limites
10. **Gate #3** → Validation tests avant doc
11. **Documentation** (DOCly) → Mettre à jour docs
12. **Gate #4** → Validation doc + clôture initiative

Parallélisation possible après Gate #2 : QALvin + DOCly travaillent parallèle si tâches indépendantes.

---

## 📋 Plans d'Action

Initiatives majeures orchestrées via Plan d'Action :

- **Fichier plan** : `.claude/plans/<NO>_<nom>.plan.md`
- **Rapports phase** : `.claude/plans/<NO>_reports/PHASE_N_...md`
- **Index** : `.claude/plans/README.md`
- **Guide complet** : `.claude/PLANS.md`

Plans coordonnent travail multi-phases, garantissent traçabilité.

---

## 📐 Instructions Projet (`.claude/instructions/`)

Chaque agent lit démarrage son fichier instructions spécifique :

| Fichier | Agent | Contenu |
|---|---|---|
| `orchestrator.instructions.md` | ⚫ MAINa | Orchestration, gates humains, délégations |
| `architect.instructions.md` | 🟠 ARCos | Conventions archi, couches, protocoles |
| `dev.instructions.md` | 🔵 DEVon | Stack technique, versions, conventions |
| `qa.instructions.md` | 🟢 QALvin | Framework test, commandes, cas à couvrir |
| `doc.instructions.md` | 🟣 DOCly | Fichiers `/docs`, conventions documentation |

---

## 🛠️ Skills Partagés (`.claude/skills/`)

Procédures réutilisables, incluses auto contexte tous agents :

| Skill | Contenu |
|---|---|
| `plan-phase-execution` | Procédure exécution phase (avant/pendant/après, rapports) |
| `plan-creation` | Création Plan d'Action (MAINa — orchestrateur) |
| `fleet-guide` | Guide parallélisation `/fleet` |
| `adr-writing` | Rédaction ADR (ARCos prépare, DOCly rédige) |
| `caveman-default` | Mode caveman règles défaut |
| `compact-context` | Compression contexte mémoire |
| `maina-help` | Aide MAINa + workflow |
| `copilotignore` | Respect fichier `.copilotignore` |
| `safety-rules` | Sécurité : opérations destructives interdites |

---

## 📚 Fichiers clés

### `.claude/agents/`

- `README.md` — Index agents, workflow, exemples
- `Maina.agent.md` — Orchestrateur
- `Arcos.agent.md` — Architecte
- `Devon.agent.md` — Implémentateur
- `Qalvin.agent.md` — Expert QA
- `Docly.agent.md` — Gardien docs

### `.claude/instructions/`

Fichiers instructions projet, remplis (pas templates).

### `.claude/prompts/`

Prompts initialisation/mise à jour instructions.

### `.claude/skills/`

Skills partagés tous agents.

### `.claude/plans/`

Index plans + rapports phases.

---

## 🚀 Démarrage rapide

### Travail simple

Invoquer agent directement :

```
@ARCos "Conçois architecture pour..."
@DEVon "Implémente cette fonctionnalité..."
@QALvin "Écris tests pour..."
@DOCly "Mets à jour documentation..."
```

### Travail complexe (multi-phases)

Utiliser MAINa orchestrateur :

```
@MAINa "J'ai ce besoin : [description]"

# MAINa :
# 1. Clarifie
# 2. Active ARCos pour plan
# 3. Attend validation
# 4. Active DEVon pour code
# ... jusqu'à clôture
```

---

## 🔐 Règles absolues

Tous agents respectent :

- ⛔ JAMAIS supprimer fichiers/répertoires
- ⛔ JAMAIS commandes SQL destructives
- ⛔ JAMAIS `git clean`, `git reset --hard`
- ⛔ JAMAIS modifier fichiers hors périmètre
- ⛔ **Respect ABSOLU `.copilotignore`**

Doute → demander confirmation développeur.

---

## 📖 Références

- [ARCos](./agents/Arcos.agent.md) — Architecture (analyse solutions)
- [DEVon](./agents/Devon.agent.md) — Implémentation
- [QALvin](./agents/Qalvin.agent.md) — Tests
- [DOCly](./agents/Docly.agent.md) — Documentation
- [MAINa](./agents/Maina.agent.md) — Orchestration + Plan d'Action
- [Plans d'Action](./PLANS.md) — Guide complet

---

## 📜 Ancienne structure `.github/` (conservée en parallèle)

Ancienne structure `.github/` (agents v2.x, `copilot-instructions.md`, `instructions/`, `plans/`, `skills/`, `prompts/`) reste en place, **inchangée**, pendant période transition. Décision suppression différée — hors scope migration vers `.claude/`. Voir `.claude/plans/005_migration_claude_sous_projets.plan.md` (dépôt racine workspace) pour contexte migration.