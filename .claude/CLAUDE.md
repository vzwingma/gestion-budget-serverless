# Instructions Claude — gestion-budget-serverless

> Backend Quarkus/Java 25, app gestion budget, déployé fonctions AWS Lambda natives.
> Frontend dans dépôt compagnon [`gestion-budget-ihm`](../gestion-budget-ihm) (React/TypeScript).
> Config Claude Code (claude.ai/code, CLI, IDEs) — dev orchestré via 5 agents spécialisés.
> Fusion de `CLAUDE.template.md` (harness agents) + contenu projet existant. Audit : `/update-harness`.

## 🗿 Mode communication

Mode caveman **full** actif par défaut pour toute session. Règles :
- Supprimer : articles, remplissage (just/really/basically/actually/simplement), formules de politesse, hedging
- Fragments OK. Synonymes courts. Termes techniques exacts. Blocs de code inchangés.
- Désactiver uniquement sur demande explicite : `stop caveman` ou `normal mode`

---

## Règle obligatoire MAINa — Plan + ADR

Toute initiative architecturale ou infrastructure (nouvelle fonctionnalité, migration, changement de composant) doit produire **avant** de marquer la tâche terminée :
1. Un fichier `Plan d'Action` dans `.claude/plans/NNN_nom.plan.md` (incrémenter le numéro)
2. Un ADR dans `docs/adr/NNN-titre-court.md` si décision architecturale majeure (`Proposée` avant Gate #1, `Acceptée` au `GO`)
3. Une mise à jour de l'index `.claude/plans/README.md`

Ces livrables sont créés dans le même lot que l'implémentation, pas après coup.

> ⚠️ **Emplacement plans projet = `.claude/plans/`** (plans 001→003 existants). Prioritaire sur le défaut
> `.claude/plans/` des skills génériques (`plan-creation`, `gate-protocol`, `.claude/PLANS.md`) : partout où ils
> citent `.claude/plans/`, lire `.claude/plans/`.

---

## 👋 Agents

Projet **gestion-budget-serverless** : architecture multi-agents orchestrée par le **👤 Développeur humain**, via des
**Plans d'Action (AP)** et des gates humains stricts.

| Agent | Rôle | Livrable |
|---|---|---|
| ⚫ **MAINa** | Maître orchestrateur : cadre, consulte ARCos, crée le Plan d'Action, délègue, tient les gates | Plan d'Action validé + orchestration traçable |
| 🟠 **ARCos** | Architecte en lecture seule, consulté par MAINa | ≥ 2 options comparées + recommandation, contenu ADR |
| 🔵 **DEVon** | Implémentation dans le périmètre du plan | Code + preuves (build, lint, tests existants) |
| 🟢 **QALvin** | Tests nominal / erreurs / limites | Tests verts, couverture ≥ 80 %, détection de régression |
| 🟣 **DOCly** | Documentation | README, `docs/`, `docs/ARCHITECTURE.md`, ADR à jour |

Détails et configuration : `.claude/agents/README.md` · vue transverse : `.claude/README.md` · historique : `.claude/CHANGELOG.md`.
Les sous-agents ne s'adressent pas au 👤 : questions + bloc `handoff` rendus à MAINa, qui les pose au gate.

---

## 🔄 Workflow strict

1. **Cadrage** (👤) → besoin + critères d'acceptation
2. **Analyse solutions** (🟠 ARCos) → ≥ 2 options + recommandation → **Gate #0** (choix solution)
3. **Plan d'Action** (⚫ MAINa) + ADR `Proposée` → **Gate #1** (validation plan)
4. **Implémentation** (🔵 DEVon) → revue auto code + sécurité → **Gate #2** (validation code)
5. **Tests** (🟢 QALvin) → **Gate #3** (validation tests)
6. **Documentation** (🟣 DOCly) → **Gate #4** (validation doc + clôture)

Gates : skill `gate-protocol` (preuves, `GO` / `AJUSTER` / `STOP`, journal des gates dans le plan).
Pas de parallélisation entre phases séparées par un gate ; possible *au sein* d'une phase (skill `parallel-guide`).

<!-- claude-only -->
🚀 **Lancement** : `claude --agent MAINa` (MAINa agent principal, gates interactifs) ou `/maina <besoin>`.
Travail ciblé : `/arcos`, `/devon`, `/qalvin`, `/docly` `<demande>` · aide : `/maina-help`.
<!-- /claude-only -->
<!-- copilot-only
🚀 **Lancement** : agent **MAINa** dans le chat, ou `/maina <besoin>` · aide : `/maina-help`.

**Cible vérifiée** : GitHub Copilot CLI uniquement. VS Code et cloud agent sont hors périmètre. La source
reste `.claude/` ; la projection est générée par `scripts/sync-claude-to-github.ps1`, puis contrôlée avec
`-Check`. Les skills sont chargées selon pertinence, donc chaque profil généré porte une consigne explicite
de chargement.
-->

---

## 📋 Plans d'Action

- **Plan** : `.claude/plans/<NO>_<slug>.plan.md` · **Rapports** : `.claude/plans/<NO>_reports/PHASE_N_*.md`
- **Index** (plans + statut global uniquement) : `.claude/plans/README.md` · **Guide** : `.claude/PLANS.md`

---

## 📐 Conventions par rôle

Conventions détaillées projet (stack, commandes, nommage, tests, doc) : **`.claude/instructions/<rôle>.instructions.md`**
(`orchestrator`, `architect`, `dev`, `qa`, `doc`) — remplis pour `gestion-budget-serverless`, **source de vérité actuelle**.

<!-- claude-only -->
Skills `conventions-<rôle>` (`.claude/skills/conventions-{orchestrator,architect,dev,qa,doc}/SKILL.md`), préchargés
par chaque agent, encore à l'état **template** (placeholders `[...]`) → à remplir via `/init-harness` à partir des
fichiers `instructions/`. Tant que placeholder subsiste : appliquer `instructions/<rôle>.instructions.md`.
<!-- /claude-only -->

Ce fichier ne garde que le contexte global.

---

## Présentation du Projet

Backend de l'application de gestion de budget personnel : projet **Maven multi-modules Quarkus / Java 25**,
architecture hexagonale, programmation réactive Mutiny, persistance **MongoDB** (Panache). 4 microservices
(`comptes`, `operations`, `parametrages`, `utilisateurs`) + bibliothèque partagée `communs`, compilés en images
natives GraalVM/Mandrel et déployés en **AWS Lambda** derrière API Gateway (SAM). Sécurité JWT (Google OIDC côté IHM).

---

## Commandes

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

---

## Architecture

Détail : `docs/ARCHITECTURE.md`.

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

### Utilitaires métier clés

#### `BudgetDataUtils` (`operations/.../utils/`)
- `cloneOperationToMoisSuivant(LigneOperation)` – clone opération mois suivant : tous champs `SsCategorie` copiés explicitement (id, libelle, **type**).
- `cloneOperationPeriodiqueToMoisSuivant(...)` – appelle `cloneOperationToMoisSuivant()` interne, puis gère périodicité. Fix sur `cloneOperationToMoisSuivant` propage auto aux deux cas.

> ⚠️ Ajout champ dans `LigneOperation.SsCategorie` ou `LigneOperation.Categorie` → ajouter aussi `cloneOperationToMoisSuivant()`.

---

## Conventions transverses

Uniquement ce qui concerne **tous** les agents :
- Commits : style Conventional Commits observé dans l'historique (`fix(scope): …`) — à suivre.
- Branches : branches de travail préfixées (ex. `fix/…`), merge sur `master` via PR.
- Dépendances : MAJ pilotées par **Renovate** (`renovate.json`) ; check CI « Quarkus version sync » → ne pas dupliquer
  la version Quarkus dans la doc hors fichiers suivis par Renovate (voir AP 002).
- Qualité : SonarCloud (`mvn verify -Psonar`) ; dette acceptée tracée en ADR (ex. ADR-004 injection `Clock`).

---

## État du Projet et Points d'Attention

### Déploiement
- CI build d'abord `communs`, publie GitHub Packages, puis build chaque microservice parallèle image native.
- Images natives déployées AWS Lambda via SAM. Routes API définies `communs/src/aws-deploy/`.
- SonarCloud tourne sur `master` une fois tous builds finis.

### Points d'attention
- Projet en évolution, stack modernisée (AP 001 : Java 25 + Mandrel 25, migrations Mongo maison — ADR-001/002/003).
- Dates : injecter `Clock` (ADR-004), pas de `.now()` direct.
- Contrats API consommés par `gestion-budget-ihm` : toute évolution modèle → vérifier côté IHM.

### 📜 Ancienne structure `.github/` (conservée en parallèle)

Ancienne structure `.github/` (agents v2.x, `copilot-instructions.md`, `instructions/`, `plans/`, `skills/`, `prompts/`) reste en place, **inchangée**, pendant période transition. Décision suppression différée — hors scope migration vers `.claude/`. Voir `.claude/plans/005_migration_claude_sous_projets.plan.md` (dépôt racine workspace) pour contexte migration.

---

## 🗜️ Compact instructions

Lors d'un `/compact` (ou compaction automatique) en session de Plan d'Action, conserver en ≤ 200 mots :
plan courant (numéro, titre, statut), phase active + tâches restantes + agent, décisions validées (gates),
questions ouvertes, prochaine action. Supprimer : détail des tâches ✅, confirmations courtes, lectures de
fichiers et sorties de commandes sans résultat durable.

---

## 🔐 Règles absolues

Tous agents respectent, sans exception ni dérogation :

- ⛔ JAMAIS supprimer fichiers/répertoires (`rm`, `rmdir`, `del`, `Remove-Item`…)
- ⛔ JAMAIS commandes SQL destructives (`DROP`, `TRUNCATE`, `DELETE` sans `WHERE`) — équivalent Mongo : jamais `drop()`, `deleteMany({})`
- ⛔ JAMAIS commande git irréversible (`git clean`, `git reset --hard`, `push --force`, `branch -D`, `stash drop`, `checkout --`, `restore`)
- ⛔ JAMAIS modifier fichiers hors périmètre de la tâche (ni hors de son rôle)
- ⛔ **Respect ABSOLU `.copilotignore`** : jamais lire, rechercher, écrire, exécuter ni référencer les fichiers listés ; jamais contourner (chemins alternatifs, symlinks, reconstitution)

En cas de doute → demander confirmation au 👤 Développeur humain. Opération voulue mais bloquée → le 👤 l'exécute lui-même.

<!-- claude-only -->
**Application déterministe** (pas seulement déclarative) :
- `.claude/settings.json` → `permissions.deny` (commandes destructives Bash/PowerShell, fichiers sensibles, chemins `.copilotignore` en `Read()`/`Edit()`) ; `ask` pour `git rm`.
- Hook `PreToolUse` `.claude/hooks/guard.sh` → bloque les formes que les règles ne voient pas (`sh -c "rm …"`, `/bin/rm`, `git -C . reset --hard`, SQL), les écritures dans `.copilotignore`, et les **frontières de rôle** (`.claude/hooks/role-boundaries.conf`).

> ⚠️ État actuel : `.claude/hooks/` présent mais **`.claude/settings.json` absent** (seul `settings.local.json`, MCP sonarqube)
> → deny + hook non câblés tant que `settings.json` non créé (`/init-harness`).
<!-- /claude-only -->

---

## 📖 Références

- [MAINa](./agents/Maina.agent.md) — Orchestration + Plan d'Action
- [ARCos](./agents/Arcos.agent.md) — Architecture (analyse solutions)
- [DEVon](./agents/Devon.agent.md) — Implémentation
- [QALvin](./agents/Qalvin.agent.md) — Tests
- [DOCly](./agents/Docly.agent.md) — Documentation
- [Plans d'Action](./PLANS.md) — Guide complet
