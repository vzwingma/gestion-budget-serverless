# Plan d'Action 001 — Modernisation stack backend

**Date création :** 2026-07-06
**Statut :** ✅ Phases 1-4 complétées (ADR-001/002, upgrade Quarkus 3.37.1, migrations Mongo maison, tuning infra SAM/CI) · ✅ Phase 6 complétée (.gitignore, architect.instructions.md enrichi) · Phase 7 en cours (T7.2 ✅, T7.4 ✅ fix SonarCloud PR #186, T7.1 ✅ mesure CloudWatch réelle — 256Mo confirmé, échantillon court à revalider après trafic organique, T7.3/T7.5 restantes). ✅ **QUA et PROD déployés et fonctionnels** (2026-07-07) après résolution de 2 incidents réels (stack bloquée par renommage LogicalId Phase 3 + CORS cassé par valeurs GitHub pré-échappées — détail note Phase 3, corrigés développeur, confirmés fonctionnels sur les 2 environnements). **Phase 5 débloquée en principe** (Phase 2 maintenant stable en prod réelle) — à relancer dans une session dédiée si souhaité. **Phase 8** backlog, non lancée.
**Porteur :** ⚫ MAINa
**Analyse préalable :** Consultation ARCos (Gate #0) — recommandations retenues par axe :
- Upgrade versions : palier incrémental (Quarkus 3.x dernière stable d'abord, Quarkus 4.x/Java 25 en dernier, isolé par spike) — **Option A ARCos retenue**
- Infra/déploiement : rester Lambda natif + tuning (mémoire, fiabiliser SAM), conteneur/K8s écarté (sur-ingénierie) — **Option A ARCos retenue**
- Gestion schéma Mongo : solution migrations maison (CDI `StartupEvent`), Mongock écarté (risque incompatibilité GraalVM native-image) — **Option B ARCos retenue**
- Observabilité/OTel : **exclu de ce plan** (reporté initiative future — si repris, backend AWS X-Ray pré-choisi par développeur)

---

## ⚠️ Statut Gate #0

Gate #0 traité en direct avec le 👤 développeur humain dans la session de planification (questions ciblées sur les 4 axes + arbitrages infra/Mongo/observabilité). Confirmation explicite obtenue. **Ce document ne constitue pas franchissement de Gate #1** — validation humaine du plan complet requise avant lancement Phase 1.

---

## Objectif Global

Le backend `gestion-budget-serverless` (Java 21, Quarkus 3.36.0, Lambda natif GraalVM) est déjà une stack récente et bien architecturée (hexagonal/DDD, CDI pur, Mutiny reactive). Aucune dette technique bloquante, mais 4 signaux identifiés par ARCos justifient une initiative de modernisation proactive :

1. Override manuel Netty 4.1.132.Final dans `pom.xml` (patch CVE-2026-33870/33871 posé à la main faute d'upgrade du BOM Quarkus)
2. Gouvernance ADR inexistante (`docs/adr/` ne contient que le template)
3. Paramétrage SAM fragile (`sed` inline dans les workflows CI sur `sam.native.template.yaml`) + `MemorySize: 128` jamais mesuré/validé
4. Absence d'outillage de migration de schéma MongoDB (aucune trace de versioning des évolutions de collections)

**Périmètre exclu de ce plan** : axe observabilité/OTel (reporté), migration infra conteneur/K8s (écartée par ARCos — coût récurrent et complexité disproportionnés pour ce contexte), adoption Mongock (écartée — risque avéré d'incompatibilité réflexion/classpath scanning avec le build natif GraalVM).

---

## Phase 1 — Gouvernance ADR (bootstrap)

### Contexte
`docs/adr/` ne contient que `ADR-TEMPLATE.md`. Chaque décision structurante de ce plan (paliers upgrade, rejet Mongock, rejet conteneur) doit être tracée. Aucune dépendance — démarre en premier, conditionne la conformité du plan à la règle MAINa (Plan + ADR obligatoire).

### Critères de réussite
- `docs/adr/001-strategie-modernisation-stack.md` créé, format conforme `ADR-TEMPLATE.md`
- Décisions des 4 axes tracées avec alternatives écartées et justification
- Clause explicite : aucune version précise Quarkus 4.x/Java 25 figée — revalidation obligatoire au moment de Phase 5

### Tâches

#### T1.1 - Rédiger ADR-001 stratégie globale
- **Agent :** ARCos (prépare contenu) puis DOCly (rédige, skill `adr-writing`)
- **Fichier(s) :** `gestion-budget-serverless/docs/adr/001-strategie-modernisation-stack.md`
- **Couvrir :** contexte (4 signaux ci-dessus), décision (paliers 3.x→4.x, Mongo maison, infra tuning), alternatives (Mongock, conteneur/K8s, upgrade direct 4.x) écartées avec raison, conséquences, lien vers ce plan
- **Acceptation :** sections Contexte/Décision/Alternatives/Conséquences/Mise en œuvre toutes remplies, statut "Acceptée" après validation humaine

#### T1.2 - Vérifier cohérence docs/ARCHITECTURE.md
- **Agent :** DOCly
- **Fichier(s) :** `gestion-budget-serverless/docs/ARCHITECTURE.md`
- **Couvrir :** ajouter/lier section décisions si absente, sinon confirmer cohérence
- **Acceptation :** rapport confirmant lien ADR-001 visible depuis architecture doc

**Effort :** S. **Risque :** nul. **Dépendances :** aucune.

---

## Phase 2 — Upgrade Quarkus 3.x

### Contexte
Quarkus 3.36.0 actuel. Objectif : dernière version stable 3.x pour absorber le patch Netty dans le BOM et retirer l'override manuel. Dépend de Phase 1 (ADR associé).

### Critères de réussite
- `mvn clean test` vert sur les 5 modules après upgrade
- Build natif Lambda réussi sur les 5 modules
- Override Netty retiré du `pom.xml` racine si BOM patché, sinon justification documentée

### Tâches

#### T2.1 - Identifier version cible + mettre à jour BOM
- **Agent :** DEVon
- **Fichier(s) :** `gestion-budget-serverless/pom.xml`
- **Couvrir :** vérifier dernière version stable 3.x (quarkus.io/blog ou repo.maven.apache.org — **à revalider au moment de l'exécution**, ne pas figer ici), mettre à jour `quarkus.platform.version`
- **Acceptation :** version choisie documentée dans le rapport de phase avec source de vérification

#### T2.2 - Build JVM + natif sur les 5 modules
- **Agent :** DEVon
- **Fichier(s) :** `communs`, `parametrages`, `utilisateurs`, `comptes`, `operations` (tous `pom.xml`)
- **Couvrir :** `mvn clean package` puis `mvn clean package -Pnative -Dquarkus.native.container-build=true` ; corriger régressions API mineures/extensions dépréciées
- **Acceptation :** build natif réussi sur les 5 modules sans erreur GraalVM

#### T2.3 - Retirer override Netty si résolu
- **Agent :** DEVon
- **Fichier(s) :** `gestion-budget-serverless/pom.xml` (bloc dependencyManagement Netty)
- **Couvrir :** `mvn dependency:tree -Dincludes=io.netty` sur module représentatif, comparer version BOM vs 4.1.132.Final ; retirer override + commentaire CVE si BOM ≥ version patchée
- **Acceptation :** override retiré (ou conservé avec justification écrite si BOM toujours en retard)

#### T2.4 - Tests non-régression
- **Agent :** QALvin
- **Fichier(s) :** suite de tests des 5 modules
- **Couvrir :** `mvn clean test` complet, comparer couverture JaCoCo avant/après
- **Acceptation :** 0 régression, couverture ≥ niveau actuel

#### T2.5 - Validation build natif Lambda
- **Agent :** QALvin
- **Fichier(s) :** artefacts `*-runner` des 5 modules
- **Couvrir :** confirmer compilation native + tests d'intégration si environnement disponible
- **Acceptation :** build natif confirmé fonctionnel

#### T2.6 - Mise à jour documentation versions
- **Agent :** DOCly
- **Fichier(s) :** `docs/ARCHITECTURE.md`, `.claude/instructions/dev.instructions.md`, `.claude/instructions/orchestrator.instructions.md` (actuellement désynchronisé — mentionne Quarkus 3.35)
- **Acceptation :** version Quarkus mentionnée partout cohérente avec le pom

**Effort :** S. **Risque :** faible (changements mineurs entre versions 3.x). **Dépendances :** Phase 1.

---

## Phase 3 — Tuning infra Lambda

### Contexte
`MemorySize: 128` jamais mesuré. Paramétrage SAM actuel injecté via `sed` dans les workflows CI (`build-on-master.yml` L49/51, `build-on-tags.yml` L57/59) sur `sam.native.template.yaml`/`samconfig.template.toml` — fragile, peu lisible. Dépend de Phase 2 (mesures doivent porter sur la base Quarkus à jour).

### Critères de réussite
- MemorySize ajusté sur mesure documentée (ou maintien 128Mo justifié par mesure)
- `sed` retiré des workflows CI, remplacé par paramétrage SAM natif
- `sam validate` passe sans erreur

### Tâches

#### T3.1 - Mesurer MemorySize optimal
- **Agent :** DEVon (ou ARCos pour la mesure)
- **Fichier(s) :** —  (mesure externe, AWS Lambda Power Tuning ou CloudWatch)
- **Couvrir :** mesurer durée/mémoire réelle sur les 4 fonctions natives post-Phase 2
- **Acceptation :** rapport de mesure chiffré, recommandation palier mémoire

#### T3.2 - Ajuster MemorySize
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/aws-deploy/sam.native.template.yaml` (4 occurrences `MemorySize`)
- **Acceptation :** valeur alignée sur mesure T3.1, pas de valeur arbitraire

#### T3.3 - Paramétrage SAM natif
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/aws-deploy/sam.native.template.yaml`, `communs/src/aws-deploy/samconfig.template.toml`
- **Couvrir :** remplacer placeholders `__ENV__`, `__VERSION__`, `__DATABASE_URL__`, `__DATABASE_NAME__`, `__APP_CONFIG_URL_IHM__`, `__APP_CONFIG_URL_BACKENDS__`, `__OIDC_JWT_ID_APPUSERCONTENT__`, `__QUARKUS_LOG_LEVEL__`, `__MONGODB_LOG_LEVEL__` par sections SAM `Parameters:`/`Mappings:` (secrets en `NoEcho`)
- **Acceptation :** `sam validate` passe, plus aucun placeholder `__xxx__` texte brut

#### T3.4 - Adapter workflows CI
- **Agent :** DEVon
- **Fichier(s) :** `.github/workflows/build-on-master.yml`, `.github/workflows/build-on-tags.yml`
- **Couvrir :** retirer `sed`, passer `sam deploy --parameter-overrides`
- **Acceptation :** aucune commande `sed` restante dans les workflows

#### T3.5 - Validation déploiement à blanc
- **Agent :** QALvin
- **Fichier(s) :** templates SAM
- **Couvrir :** `sam validate`, `sam build` local, test sur environnement QUA avant PROD
- **Acceptation :** validation syntaxique + résolution paramètres confirmée

#### T3.6 - Documentation convention SAM
- **Agent :** DOCly
- **Fichier(s) :** `docs/ARCHITECTURE.md` (section déploiement), `README.md`
- **Acceptation :** nouvelle convention paramétrage documentée

**Effort :** S. **Risque :** secrets CI mal mappés (mitigation : tester QUA avant PROD, garder `sed` commenté en fallback le temps d'un cycle CI complet validé). **Dépendances :** Phase 2.

### ⚠️ Incidents réels post-déploiement (2026-07-07) — résolus

**1. Stack QUA bloquée `UPDATE_ROLLBACK_FAILED`** — cause : simplification LogicalId T3.3 (`XxxNative__ENV__` → `XxxNative` statique) incompatible avec une stack déjà déployée (l'ancien `sed` substituait `__ENV__` *dans* le LogicalId lui-même, ex. `ComptesNativeQUA` réellement déployé). CloudFormation a interprété ça comme un replacement forcé (delete+create), cassé sur `ServerlessRestApiProdStage`, rollback auto en échec. **Résolu** : `aws cloudformation delete-stack budget-app-QUA` (accord développeur, données Mongo externes non impactées) → `DELETE_COMPLETE`, 0 ressource orpheline vérifié → redéploiement CI réussi.

**2. CORS cassé après redéploiement QUA** — cause : valeurs GitHub Actions `APP_CONFIG_URL_IHM`/`APP_CONFIG_URL_BACKENDS` (environnement QUA) stockées avec échappement littéral (`\/`, `\.`, reliquat probable ancien pipeline `sed`), non pertinent depuis passage aux `Parameters` SAM (valeur injectée telle quelle, plus de `sed` dessus). Confirmé par inspection directe env Lambda déployée. **Résolu** : développeur a corrigé les valeurs côté GitHub (QUA). Doc mise à jour (`docs/ARCHITECTURE.md`, sections Variables d'environnement + Paramétrage SAM) pour prévenir récidive.

**3. Stack PROD** — `budget-app-PROD` était `UPDATE_COMPLETE`/vivant (naming pré-Phase 3 encore en place, `ComptesNativePROD` etc., dernier update 2026-05-26) — pas encore touché par le nouveau template. Par cohérence et pour éviter le même incident #1 au premier déploiement PROD, `delete-stack budget-app-PROD` exécuté préventivement (2026-07-07, interruption de service réelle acceptée par le développeur) → `DELETE_COMPLETE`, 0 ressource orpheline vérifié. **Redéploiement via `build-on-tags.yml` (tag de release) restant à déclencher — vérifier au préalable que les valeurs `APP_CONFIG_URL_IHM`/`APP_CONFIG_URL_BACKENDS` sont aussi corrigées côté environnement GitHub PROD (incident #2), pas seulement QUA.**

---

## Phase 4 — Migrations MongoDB maison

### Contexte
Aucun outillage de versioning de schéma MongoDB. Mongock écarté (réflexion/classpath scanning incompatible risque élevé avec GraalVM native-image). Solution maison légère : classes CDI + collection `_migrations` de tracking. Dépend uniquement de Phase 1 (ADR) — **parallélisable avec Phases 2/3**.

### Critères de réussite
- Démarrage Quarkus applique automatiquement migrations non exécutées
- Fonctionne en mode natif GraalVM (aucune réflexion/scan dynamique non enregistrée)
- ADR dédié rédigé

### Tâches

#### T4.1 - Trancher format des migrations
- **Agent :** ARCos
- **Couvrir :** confirmer classes Java CDI (pas fichiers externes — incompatible natif), découverte via `Instance<IMongoMigration>` triée par version (pas de classpath scanning)
- **Acceptation :** approche validée compatible native-image

#### T4.2 - Implémenter mécanisme de migration
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/main/java/.../communs/migrations/IMongoMigration.java`, `MigrationRecord.java`, `MigrationRepository.java`, `MongoMigrationRunner.java` (nouveau package)
- **Couvrir :** `MongoMigrationRunner` en `@ApplicationScoped` avec `@Observes StartupEvent`, exécution triée par version, insertion dans collection `_migrations` après succès
- **Acceptation :** mécanisme complet, idempotent

#### T4.3 - Hints GraalVM reflection
- **Agent :** DEVon
- **Fichier(s) :** `MigrationRecord.java` (ou config dédiée), suivant pattern `JwtReflectionConfig.java` existant
- **Acceptation :** `@RegisterForReflection` posé où nécessaire

#### T4.4 - Migration exemple no-op
- **Agent :** DEVon
- **Fichier(s) :** première classe `V001_InitMigrationsCollection` (nom à préciser en implémentation)
- **Acceptation :** mécanisme validé de bout en bout

#### T4.5 - Tests
- **Agent :** QALvin
- **Fichier(s) :** tests `@QuarkusTest` du nouveau package migrations
- **Couvrir :** migration appliquée une fois, idempotence (relance = pas de ré-exécution), tri par version, échec explicite (pas de blocage silencieux)
- **Acceptation :** suite verte, cas limites couverts

#### T4.6 - Documentation + ADR
- **Agent :** DOCly
- **Fichier(s) :** `docs/adr/002-migrations-mongodb-maison.md`, `docs/ARCHITECTURE.md`
- **Couvrir :** convention numérotation/idempotence documentée, ADR-002 (rejet Mongock motivé)
- **Acceptation :** convention claire pour futures migrations

**Effort :** S-M. **Risque :** faible si injection CDI standard respectée (pas de scan classpath). **Dépendances :** Phase 1 uniquement.

---

## Phase 5 — Upgrade Quarkus 4.x / Java 25

### Contexte
Dernier palier, risque élevé (breaking changes config keys, extensions renommées, maturité Mandrel/Java 25 natif incertaine). Dépend de Phase 2 validée stable. **Découpée en 2 sous-étapes avec Gate humain intermédiaire dédié** (au-delà des Gates standard).

### Critères de réussite
- Spike de compatibilité validé par le développeur avant extension aux 5 modules
- Build natif Quarkus 4.x/Java 25 fonctionnel sur les 5 modules
- ADR-003 documentant la version exacte finalement choisie

### Tâches

**5a — Spike (isolé, un seul module + `communs`)**

#### T5.1 - Revalider versions cibles
- **Agent :** ARCos
- **Couvrir :** version Quarkus 4.x exacte, support Mandrel/GraalVM Java 25 natif — **vérification obligatoire release notes officielles au moment de l'exécution**, aucune version figée dans ce plan
- **Acceptation :** versions cibles documentées avec source

#### T5.2 - Spike upgrade sur module isolé
- **Agent :** DEVon
- **Fichier(s) :** `parametrages` (module le plus petit) + `communs`, sur branche/worktree dédiée
- **Couvrir :** identifier breaking changes config keys, extensions renommées (`quarkus-amazon-lambda-rest` à revérifier), hints GraalVM reflection à revalider (4 fichiers `JwtReflectionConfig.java`)
- **Acceptation :** liste de breaking changes + effort réel estimé

#### T5.3 - Build natif spike
- **Agent :** DEVon
- **Couvrir :** build natif Lambda sur le module spike seul
- **Acceptation :** succès ou liste de blocages précis

**→ Gate spike (Go/No-Go)** : présenter résultats au 👤 développeur avant 5b. Pas de passage automatique.

**5b — Migration complète (si Go)**

#### T5.4 - Étendre aux 4 modules restants
- **Agent :** DEVon
- **Fichier(s) :** `pom.xml` racine + 5 modules (`maven.compiler.release` 21→25, `quarkus.platform.version`)
- **Acceptation :** upgrade appliqué partout

#### T5.5 - Corriger extensions/hints restants
- **Agent :** DEVon
- **Fichier(s) :** 4× `JwtReflectionConfig.java`, `application.properties` si clés renommées
- **Acceptation :** aucune régression reflection

#### T5.6 - Tests + build natif complet
- **Agent :** QALvin
- **Couvrir :** `mvn clean test` + build natif sur les 5 modules
- **Acceptation :** non-régression complète confirmée

#### T5.7 - ADR-003 + documentation
- **Agent :** DOCly
- **Fichier(s) :** `docs/adr/003-upgrade-quarkus4-java25.md`, `docs/ARCHITECTURE.md`, `.claude/instructions/dev.instructions.md`, `.claude/instructions/qa.instructions.md`
- **Acceptation :** version exacte choisie documentée avec justification

**Effort :** L. **Risque :** élevé (mitigation : spike obligatoire isolé avant engagement large, rollback trivial tant que 5b non lancé). **Dépendances :** Phase 2 validée stable.

---

## Phase 6 — Hygiène technique (backlog, non lancée)

### Contexte
2 items mineurs identifiés en cours d'exécution des Phases 1-4, sans dépendance bloquante externe — exécutables dès validation développeur. Indépendants entre eux et des autres phases.

### Critères de réussite
- `.gitignore` racine couvre tous les modules (plus de `target/` untracked résiduel)
- `architect.instructions.md` du sous-projet backend rempli (conventions archi/couches/protocoles), plus de placeholder générique

### Tâches

#### T6.1 - Compléter .gitignore racine
- **Agent :** DEVon
- **Fichier(s) :** `gestion-budget-serverless/.gitignore`
- **Couvrir :** ajouter `utilisateurs/target/`, `comptes/target/`, `operations/target/`, `/target/` (racine) — actuellement seuls `communs/target/` et `parametrages/target/` couverts
- **Acceptation :** `git status` propre après un build complet (`mvn clean package`), aucun `target/` untracked

#### T6.2 - Instancier architect.instructions.md
- **Agent :** ARCos (contenu) puis DEVon (rédaction fichier)
- **Fichier(s) :** `gestion-budget-serverless/.claude/instructions/architect.instructions.md`
- **Couvrir :** remplir depuis le template générique racine avec conventions réelles du projet (couches hexagonale, ports/adapters, patterns Mutiny/CDI/Panache déjà documentés dans `docs/ARCHITECTURE.md` et `.claude/CLAUDE.md`) — suggestion ARCos non traitée en Phase 1
- **Acceptation :** aucun placeholder `[...]` restant, cohérent avec `docs/ARCHITECTURE.md`

**Effort :** XS. **Risque :** faible. **Dépendances :** aucune (indépendante des autres phases).

---

## Phase 7 — Durcissement infra/CI (backlog, non lancée)

### Contexte
5 items liés à la robustesse du paramétrage SAM/CI posé en Phase 3. T7.1 nécessite un déploiement réel effectué (mesure post-Phase 2/3 en prod) ; les 4 autres sont indépendants et exécutables dès maintenant.

### Critères de réussite
- MemorySize 256Mo confirmé ou ajusté par mesure réelle
- Scoping secrets GitHub Environments QUA/PROD confirmé
- `sed` `samconfig.template.toml` n'altère plus le commentaire explicatif
- `--parameter-overrides` durci contre caractères spéciaux
- Désync version Quarkus pom.xml / instructions détectée automatiquement

### Tâches

#### T7.1 - Mesurer MemorySize réel ✅ complétée (mesure CloudWatch, pas Power Tuning)
- **Agent :** 🟠 ARCos
- **Fichier(s) :** `communs/src/aws-deploy/sam.native.template.yaml` (non modifié — voir conclusion)
- **Couvrir :** une fois déployé en prod, mesurer via AWS Lambda Power Tuning (estimation 256Mo posée en Phase 3 à confirmer/ajuster)
- **Méthode réellement utilisée (2026-07-07)** : CloudWatch Logs Insights sur les lignes `REPORT` des 8 log groups actifs (4 fonctions × QUA/PROD), au lieu de l'outil AWS Lambda Power Tuning (nécessite déploiement d'une state machine dédiée, jugé disproportionné vu qu'un vrai trafic post-redéploiement était déjà disponible). Requête agrégée `stats max(@maxMemoryUsed), avg(@maxMemoryUsed), count(*)` par log group + recherche de patterns fatals (`Task timed out`, `Runtime exited`, `out of memory`, etc.).
- **Résultats (Max Memory Used / 256Mo alloués) :**

  | Fonction | Env | Invocations | Max Mémoire | % de 256Mo | Durée max |
  |---|---|---|---|---|---|
  | UtilisateursNative | QUA | 20 | 120 Mo | 46.9% | 727 ms |
  | ParametragesNative | QUA | 12 | 125 Mo | 48.8% | 784 ms |
  | ComptesNative | QUA | 13 | 120 Mo | 46.9% | 749 ms |
  | OperationsNative | QUA | 69 | 154 Mo | 60.2% | 1591 ms |
  | UtilisateursNative | PROD | 15 | 124 Mo | 48.4% | 1003 ms |
  | ParametragesNative | PROD | 11 | 129 Mo | 50.4% | 829 ms |
  | ComptesNative | PROD | 14 | 121 Mo | 47.3% | 373 ms |
  | OperationsNative | PROD | 156 | 152 Mo | 59.4% | 1415 ms |

  Aucune erreur infra (timeout, OOM, crash runtime) détectée sur les 8 log groups. Pic max toutes fonctions confondues : 154 Mo (OperationsNative QUA) = 60.2% du plafond 256Mo.
- **Limite de l'échantillon :** toutes les invocations proviennent d'une seule fenêtre ~20 min juste après le redéploiement du 2026-07-07 (tests de validation développeur post-incident), pas de trafic organique réparti dans le temps ni de cas d'usage extrêmes (gros exports, opérations admin en masse).
- **Conclusion :** 256Mo **confirmé suffisant** (aucun signe de sous-dimensionnement, marge ≥40% sur toutes les fonctions). Signal de sur-dimensionnement possible (OperationsNative à ~60%, les 3 autres <51%) mais **échantillon insuffisant pour ajuster à la baisse avec confiance** (fenêtre courte, trafic de test uniquement, gain de coût de toute façon négligeable vu le très faible volume d'invocations d'une appli à usage personnel ; réduire la mémoire réduit aussi le CPU alloué proportionnellement sur Lambda, cf. note Phase 3 ARCHITECTURE.md — risque de dégrader la latence du binaire natif pour une économie marginale). **Décision : ne pas modifier `sam.native.template.yaml`.** À revalider après plusieurs jours/semaines de trafic réel si optimisation coût souhaitée.
- **Acceptation :** ✅ valeur confirmée par mesure chiffrée réelle (CloudWatch REPORT) — pas d'ajustement (justification documentée ci-dessus)
- **Dépendance :** déploiement réel effectué ✅ (2026-07-07)

#### T7.2 - Confirmer scoping secrets GitHub Environments ✅ complétée
- **Agent :** développeur humain (vérification console GitHub, pas de code)
- **Couvrir :** vérifier GitHub Settings → Environments que secrets `DATABASE_URL`, `DATABASE_NAME`, `OIDC_JWT_ID_APPUSERCONTENT` sont bien scopés `QUA`/`PROD` cohérent avec l'ajout `environment:` en Phase 3
- **Acceptation :** ✅ confirmé par développeur (2026-07-07) — les 3 secrets bien scopés `QUA`/`PROD` en tant que Secret GitHub Environment

#### T7.3 - Corriger sed samconfig.template.toml
- **Agent :** DEVon
- **Fichier(s) :** `.github/workflows/build-on-master.yml`, `build-on-tags.yml`
- **Couvrir :** `sed` global sur `__ENV__` altère aussi le commentaire explicatif ajouté en Phase 3 — cibler la substitution aux seules lignes fonctionnelles (`stack_name`/`s3_prefix`) ou reformuler le commentaire pour éviter le pattern `__ENV__`
- **Acceptation :** commentaire intact après substitution CI, valeurs fonctionnelles toujours correctes

#### T7.4 - Durcir --parameter-overrides ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `.github/workflows/build-on-master.yml`, `build-on-tags.yml`
- **Couvrir :** déclenchée par alerte sécurité SonarCloud réelle (PR #186, "Avoid expanding secrets in a run block"). Secrets/vars déplacés dans bloc `env:` du step "SAM Deploy" + référencés `$VAR` dans `run:` (pattern répliqué du step Sonar existant) — plus d'interpolation `${{ secrets.X }}`/`${{ vars.X }}` directe dans le shell
- **Acceptation :** ✅ YAML valide, aucun secret/var interpolé dans `run:` (grep confirmé), fonctionnellement identique, non commité — prêt revue humaine

#### T7.5 - Check désync version Quarkus
- **Agent :** DEVon
- **Fichier(s) :** nouveau script/étape CI ou lint (ex: `.github/workflows/build-on-master.yml` ou script dédié)
- **Couvrir :** détecter automatiquement écart entre `quarkus.platform.version` du `pom.xml` et versions mentionnées dans `.claude/instructions/*.md`/`docs/ARCHITECTURE.md` (a causé désync 3.35 vs 3.36 non détectée avant cette initiative)
- **Acceptation :** CI échoue ou alerte si désync détectée

**Effort :** S-M. **Risque :** faible. **Dépendances :** T7.1 dépend déploiement réel ; T7.2-T7.5 indépendantes.

---

## Phase 8 — Retrait workaround Jackson + couverture MigrationRepository (backlog, partiellement bloquée)

### Contexte
Regroupe 2 items sans lien fonctionnel entre eux, statuts de blocage différents :
- **T8.1** : bug upstream Quarkus (champs `Long` null disparaissant sérialisation JSON reflection-free) contourné en Phase 2 via `quarkus.rest.jackson.optimization.enable-reflection-free-serializers=false` sur 8 fichiers `application.properties`. Fix mergé sur `main` Quarkus (branche 4.0, PR [quarkusio/quarkus#55278](https://github.com/quarkusio/quarkus/pull/55278)) mais **non backporté** en 3.x à ce jour — **bloquée**.
- **T8.2** : `MigrationRepository` (Phase 4) actuellement 0% couverture réelle, testé uniquement via mock — **non bloquée**, exécutable dès maintenant, déplacée depuis Phase 6.

### Critères de réussite
- Workaround Jackson retiré des 8 fichiers une fois fix backporté en 3.x stable ; test `UtilisateursResourceTest.testForUtilisateurUnkown` toujours vert sans le workaround
- `MigrationRepository` couvert par tests d'intégration réels (pas seulement mocké)

### Tâches

#### T8.1 - Surveiller backport + retirer workaround Jackson
- **Agent :** DEVon
- **Fichier(s) :** `parametrages/src/main/resources/{dev,prod}/application.properties`, `utilisateurs/.../application.properties`, `comptes/.../application.properties`, `operations/.../application.properties` (8 fichiers)
- **Couvrir :** vérifier périodiquement release notes Quarkus 3.x (ou lors de tout upgrade version future) si PR #55278 backporté ; si oui, retirer `enable-reflection-free-serializers=false` des 8 fichiers, relancer `mvn clean test` (5 modules) pour confirmer `testForUtilisateurUnkown` toujours vert sans le workaround
- **Acceptation :** workaround retiré, suite de tests complète verte, aucune régression
- **Dépendance :** backport upstream Quarkus (bloquant, externe, sans date connue — revalider à chaque upgrade Quarkus futur, y compris Phase 5)

#### T8.2 - Couverture réelle MigrationRepository
- **Agent :** DEVon (infra test) puis QALvin (tests)
- **Fichier(s) :** `communs/pom.xml` (dépendance Testcontainers MongoDB si absente), `communs/src/test/java/.../migrations/` (nouveaux tests intégration)
- **Couvrir :** `MigrationRepository` actuellement 0% couverture réelle (testé uniquement via mock dans `TestMongoMigrationRunner`) — ajouter tests d'intégration avec vrai MongoDB (Testcontainers ou devservices Quarkus) validant `listerVersionsAppliquees()`, `enregistrerSucces()`, `enregistrerEchec()`
- **Acceptation :** couverture `MigrationRepository` > 0%, tests verts, pas de régression sur suite existante
- **Dépendance :** aucune

**Effort :** XS (T8.1) + S (T8.2). **Risque :** nul (T8.1) / faible (T8.2).

---

## Résumé par Agent

| Agent | Tâches | Livrable | Effort |
|---|---|---|---|
| ARCos | T1.1(prépa), T4.1, T5.1 | Contenu ADR, choix format migrations, versions cibles revalidées | ~1h |
| DEVon | T2.1-T2.3, T3.1-T3.4, T4.2-T4.4, T5.2-T5.5 | Upgrade Quarkus 3.x/4.x, tuning SAM, migrations Mongo | L (majorité effort) |
| QALvin | T2.4-T2.5, T3.5, T4.5, T5.6 | Non-régression, validation native, tests migrations | M |
| DOCly | T1.1(rédaction), T1.2, T2.6, T3.6, T4.6, T5.7 | ADR-001/002/003, docs à jour | S-M |
| MAINa | Orchestration globale, Gates | Séquencement, Gate spike Phase 5 | continu |

---

## Dépendances

```
Phase 1 (ADR bootstrap)
   ├──> Phase 2 (Upgrade 3.x) ──> Phase 3 (Tuning infra)
   │                         └──> Phase 5 (Upgrade 4.x/Java25, après validation prod stable Phase 2)
   └──> Phase 4 (Migrations Mongo) [parallélisable dès Phase 1 close]
```

Phase 4 indépendante de Phases 2/3/5 — peut démarrer dès Phase 1 close, en parallèle.

---

## Critères de Succès Globaux

1. ADR-001 (stratégie globale), ADR-002 (migrations Mongo) rédigés et acceptés ; ADR-003 (Phase 5) si palier 4.x lancé
2. Override Netty manuel retiré (ou justifié) après upgrade Quarkus 3.x
3. `mvn clean test` + build natif vert sur les 5 modules à chaque phase d'upgrade
4. Paramétrage SAM sans `sed`, `MemorySize` ajusté sur mesure documentée
5. Mécanisme migrations Mongo fonctionnel en mode natif, idempotent, testé
6. Spike Phase 5 validé explicitement (Gate dédié) avant extension aux 5 modules
7. Toute version cible (Quarkus 4.x, Java 25) revalidée au moment de l'exécution, jamais figée a priori

---

## Plan d'Exécution — Triggers

1. **Gate #1** (👤 validation ce plan) → déclenche Phase 1
2. Phase 1 complète (ADR-001 accepté) → déclenche Phase 2 **et** Phase 4 en parallèle
3. Phase 2 complète + rapport `PHASE_2_COMPLETION_REPORT.md` → **Gate #2** → déclenche Phase 3
4. Phase 4 complète (indépendamment) → rapport `PHASE_4_COMPLETION_REPORT.md`
5. Phase 3 et Phase 4 complètes → **Gate #3** validation groupée
6. Phase 2 validée stable en production → déclenche Phase 5 (5a spike)
7. Spike 5a complet → **Gate spike dédié** (Go/No-Go) → déclenche 5b si Go
8. Phase 5 (5b) complète → **Gate #4** → clôture initiative
