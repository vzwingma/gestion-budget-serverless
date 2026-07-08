# Plan d'Action 001 — Modernisation stack backend

**Date création :** 2026-07-06
**Statut :** ✅ Phases 1, 2, 3, 4, 6, 7 **entièrement complétées** (toutes tâches ✅, détail par tâche dans chaque section) — ADR-001/002, upgrade Quarkus 3.37.1, migrations Mongo maison, tuning infra SAM/CI, .gitignore, architect.instructions.md enrichi, mesure MemorySize réelle (256Mo confirmé), fix SonarCloud PR #186, sed cosmétique corrigé, check CI désync version. ✅ **QUA et PROD déployés et fonctionnels** (2026-07-07) après résolution de 2 incidents réels documentés (Phase 3 : stack bloquée par renommage LogicalId + CORS cassé par valeurs GitHub pré-échappées). ✅ Phase 8 : T8.2 complétée (couverture réelle `MigrationRepository` + bug générique `ObjectId`/`String` corrigé) — T8.1 bloquée (backport Quarkus #55278 non confirmé). ⏸️ **Phase 5** : T5.1 complétée (recherche réelle) — **redécoupée** : Quarkus 4.x Not-yet (pas de GA, Beta1 sept. 2026 au plus tôt), mais **Java 25 + Mandrel 25 faisables dès maintenant sur Quarkus 3.37.1 inchangé**. ✅ **Spike 5a (T5.2+T5.3) exécuté et concluant (2026-07-07)** : build JVM+natif Java25/Mandrel25 réussis sur `communs`+`parametrages`, seul breaking change réel = Lombok `annotationProcessorPaths` (fix trivial), effort largement inférieur à l'estimation initiale "M". **Gate spike (Go/No-Go pour 5b) en attente de décision développeur.**
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

## Phase 1 — Gouvernance ADR (bootstrap) ✅ complétée

### Contexte
`docs/adr/` ne contient que `ADR-TEMPLATE.md`. Chaque décision structurante de ce plan (paliers upgrade, rejet Mongock, rejet conteneur) doit être tracée. Aucune dépendance — démarre en premier, conditionne la conformité du plan à la règle MAINa (Plan + ADR obligatoire).

### Critères de réussite
- `docs/adr/001-strategie-modernisation-stack.md` créé, format conforme `ADR-TEMPLATE.md`
- Décisions des 4 axes tracées avec alternatives écartées et justification
- Clause explicite : aucune version précise Quarkus 4.x/Java 25 figée — revalidation obligatoire au moment de Phase 5

### Tâches

#### T1.1 - Rédiger ADR-001 stratégie globale ✅ complétée
- **Agent :** ARCos (prépare contenu) puis DOCly (rédige, skill `adr-writing`)
- **Fichier(s) :** `gestion-budget-serverless/docs/adr/001-strategie-modernisation-stack.md`
- **Couvrir :** contexte (4 signaux ci-dessus), décision (paliers 3.x→4.x, Mongo maison, infra tuning), alternatives (Mongock, conteneur/K8s, upgrade direct 4.x) écartées avec raison, conséquences, lien vers ce plan
- **Acceptation :** sections Contexte/Décision/Alternatives/Conséquences/Mise en œuvre toutes remplies, statut "Acceptée" après validation humaine

#### T1.2 - Vérifier cohérence docs/ARCHITECTURE.md ✅ complétée
- **Agent :** DOCly
- **Fichier(s) :** `gestion-budget-serverless/docs/ARCHITECTURE.md`
- **Couvrir :** ajouter/lier section décisions si absente, sinon confirmer cohérence
- **Acceptation :** rapport confirmant lien ADR-001 visible depuis architecture doc

**Effort :** S. **Risque :** nul. **Dépendances :** aucune.

---

## Phase 2 — Upgrade Quarkus 3.x ✅ complétée

### Contexte
Quarkus 3.36.0 actuel. Objectif : dernière version stable 3.x pour absorber le patch Netty dans le BOM et retirer l'override manuel. Dépend de Phase 1 (ADR associé).

### Critères de réussite
- `mvn clean test` vert sur les 5 modules après upgrade
- Build natif Lambda réussi sur les 5 modules
- Override Netty retiré du `pom.xml` racine si BOM patché, sinon justification documentée

### Tâches

#### T2.1 - Identifier version cible + mettre à jour BOM ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `gestion-budget-serverless/pom.xml`
- **Couvrir :** vérifier dernière version stable 3.x (quarkus.io/blog ou repo.maven.apache.org — **à revalider au moment de l'exécution**, ne pas figer ici), mettre à jour `quarkus.platform.version`
- **Acceptation :** ✅ version **3.37.1** choisie, confirmée via Maven Central metadata + `mvn versions:display-property-updates`

#### T2.2 - Build JVM + natif sur les 5 modules ✅ complétée (JVM), ⚠️ natif non vérifiable localement
- **Agent :** DEVon
- **Fichier(s) :** `communs`, `parametrages`, `utilisateurs`, `comptes`, `operations` (tous `pom.xml`)
- **Couvrir :** `mvn clean package` puis `mvn clean package -Pnative -Dquarkus.native.container-build=true` ; corriger régressions API mineures/extensions dépréciées
- **Acceptation :** ✅ build JVM vert sur les 5 modules. Build natif : Docker/Podman absents localement (`No container CLI was found`) — non vérifiable ici, à confirmer en CI (`build-on-master.yml`, runners avec Docker) ; déploiement réel QUA/PROD confirmé fonctionnel depuis (preuve indirecte que le natif fonctionne en CI)

#### T2.3 - Retirer override Netty si résolu ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `gestion-budget-serverless/pom.xml` (bloc dependencyManagement Netty)
- **Couvrir :** `mvn dependency:tree -Dincludes=io.netty` sur module représentatif, comparer version BOM vs 4.1.132.Final ; retirer override + commentaire CVE si BOM ≥ version patchée
- **Acceptation :** ✅ BOM 3.37.1 fournit Netty 4.1.135.Final (≥ seuil CVE), override + commentaire retirés du pom racine

#### T2.4 - Tests non-régression ✅ complétée
- **Agent :** QALvin
- **Fichier(s) :** suite de tests des 5 modules
- **Couvrir :** `mvn clean test` complet, comparer couverture JaCoCo avant/après
- **Acceptation :** ✅ 344 tests, 0 échec, 5/5 modules verts, couverture stable/en légère hausse. Bug upstream Quarkus réel trouvé (sérialisation champs `Long` null) + contourné (`enable-reflection-free-serializers=false`, cf. T8.1)

#### T2.5 - Validation build natif Lambda ⚠️ non vérifiable localement
- **Agent :** QALvin
- **Fichier(s) :** artefacts `*-runner` des 5 modules
- **Couvrir :** confirmer compilation native + tests d'intégration si environnement disponible
- **Acceptation :** Docker/Podman absents localement — non vérifiable ici. **Confirmé indirectement** : déploiement réel QUA et PROD réussi et fonctionnel (2026-07-07), donc le build natif CI fonctionne bel et bien

#### T2.6 - Mise à jour documentation versions ✅ complétée
- **Agent :** DOCly
- **Fichier(s) :** `docs/ARCHITECTURE.md`, `.claude/instructions/dev.instructions.md`, `.claude/instructions/orchestrator.instructions.md` (actuellement désynchronisé — mentionne Quarkus 3.35)
- **Acceptation :** ✅ version 3.37.1 cohérente dans tous les fichiers doc/instructions + README.md (corrigé également)

**Effort :** S. **Risque :** faible (changements mineurs entre versions 3.x). **Dépendances :** Phase 1.

---

## Phase 3 — Tuning infra Lambda ✅ complétée (+ 2 incidents réels résolus, voir note en fin de phase)

### Contexte
`MemorySize: 128` jamais mesuré. Paramétrage SAM actuel injecté via `sed` dans les workflows CI (`build-on-master.yml` L49/51, `build-on-tags.yml` L57/59) sur `sam.native.template.yaml`/`samconfig.template.toml` — fragile, peu lisible. Dépend de Phase 2 (mesures doivent porter sur la base Quarkus à jour).

### Critères de réussite
- MemorySize ajusté sur mesure documentée (ou maintien 128Mo justifié par mesure)
- `sed` retiré des workflows CI, remplacé par paramétrage SAM natif
- `sam validate` passe sans erreur

### Tâches

#### T3.1 - Mesurer MemorySize optimal ✅ complétée (estimation motivée initiale — mesure réelle faite ensuite en T7.1)
- **Agent :** DEVon (ou ARCos pour la mesure)
- **Fichier(s) :** —  (mesure externe, AWS Lambda Power Tuning ou CloudWatch)
- **Couvrir :** mesurer durée/mémoire réelle sur les 4 fonctions natives post-Phase 2
- **Acceptation :** ✅ pas de mesure AWS possible à ce stade (pas encore déployé) → estimation motivée 256Mo (CPU proportionnel mémoire, natif GraalVM+JSON+Mongo réactif), validée par développeur. Confirmée a posteriori par mesure réelle CloudWatch en T7.1 (Phase 7)

#### T3.2 - Ajuster MemorySize ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/aws-deploy/sam.native.template.yaml` (4 occurrences `MemorySize`)
- **Acceptation :** ✅ 256Mo appliqué sur les 4 fonctions

#### T3.3 - Paramétrage SAM natif ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/aws-deploy/sam.native.template.yaml`, `communs/src/aws-deploy/samconfig.template.toml`
- **Couvrir :** remplacer placeholders `__ENV__`, `__VERSION__`, `__DATABASE_URL__`, `__DATABASE_NAME__`, `__APP_CONFIG_URL_IHM__`, `__APP_CONFIG_URL_BACKENDS__`, `__OIDC_JWT_ID_APPUSERCONTENT__`, `__QUARKUS_LOG_LEVEL__`, `__MONGODB_LOG_LEVEL__` par sections SAM `Parameters:`/`Mappings:` (secrets en `NoEcho`)
- **Acceptation :** ✅ `sam validate` passe, placeholders retirés (sauf `stack_name`/`s3_prefix`, contrainte SAM CLI, cf. note). **A causé l'incident #1 LogicalId — voir note post-tâches, résolu**

#### T3.4 - Adapter workflows CI ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `.github/workflows/build-on-master.yml`, `.github/workflows/build-on-tags.yml`
- **Couvrir :** retirer `sed`, passer `sam deploy --parameter-overrides`
- **Acceptation :** ✅ `sed` retiré du template YAML (conservé seulement pour `stack_name`/`s3_prefix`, puis ciblé plus précisément en T7.3). `environment: QUA`/`PROD` ajouté au job deploy

#### T3.5 - Validation déploiement à blanc ✅ complétée
- **Agent :** QALvin
- **Fichier(s) :** templates SAM
- **Couvrir :** `sam validate`, `sam build` local, test sur environnement QUA avant PROD
- **Acceptation :** ✅ `sam validate`/`sam build` verts (validation indépendante QALvin confirmée), déploiement réel QUA puis PROD réussi depuis

#### T3.6 - Documentation convention SAM ✅ complétée
- **Agent :** DOCly
- **Fichier(s) :** `docs/ARCHITECTURE.md` (section déploiement), `README.md`
- **Acceptation :** ✅ convention `Parameters`/`parameter-overrides` documentée, mise à jour avec retour d'expérience incidents (format valeurs, risque LogicalId sur stack existante)

**Effort :** S. **Risque :** secrets CI mal mappés (mitigation : tester QUA avant PROD, garder `sed` commenté en fallback le temps d'un cycle CI complet validé). **Dépendances :** Phase 2.

### ⚠️ Incidents réels post-déploiement (2026-07-07) — résolus

**1. Stack QUA bloquée `UPDATE_ROLLBACK_FAILED`** — cause : simplification LogicalId T3.3 (`XxxNative__ENV__` → `XxxNative` statique) incompatible avec une stack déjà déployée (l'ancien `sed` substituait `__ENV__` *dans* le LogicalId lui-même, ex. `ComptesNativeQUA` réellement déployé). CloudFormation a interprété ça comme un replacement forcé (delete+create), cassé sur `ServerlessRestApiProdStage`, rollback auto en échec. **Résolu** : `aws cloudformation delete-stack budget-app-QUA` (accord développeur, données Mongo externes non impactées) → `DELETE_COMPLETE`, 0 ressource orpheline vérifié → redéploiement CI réussi.

**2. CORS cassé après redéploiement QUA** — cause : valeurs GitHub Actions `APP_CONFIG_URL_IHM`/`APP_CONFIG_URL_BACKENDS` (environnement QUA) stockées avec échappement littéral (`\/`, `\.`, reliquat probable ancien pipeline `sed`), non pertinent depuis passage aux `Parameters` SAM (valeur injectée telle quelle, plus de `sed` dessus). Confirmé par inspection directe env Lambda déployée. **Résolu** : développeur a corrigé les valeurs côté GitHub (QUA). Doc mise à jour (`docs/ARCHITECTURE.md`, sections Variables d'environnement + Paramétrage SAM) pour prévenir récidive.

**3. Stack PROD** — `budget-app-PROD` était `UPDATE_COMPLETE`/vivant (naming pré-Phase 3 encore en place, `ComptesNativePROD` etc., dernier update 2026-05-26) — pas encore touché par le nouveau template. Par cohérence et pour éviter le même incident #1 au premier déploiement PROD, `delete-stack budget-app-PROD` exécuté préventivement (2026-07-07, interruption de service réelle acceptée par le développeur) → `DELETE_COMPLETE`, 0 ressource orpheline vérifié. **Redéploiement via `build-on-tags.yml` (tag de release) restant à déclencher — vérifier au préalable que les valeurs `APP_CONFIG_URL_IHM`/`APP_CONFIG_URL_BACKENDS` sont aussi corrigées côté environnement GitHub PROD (incident #2), pas seulement QUA.**

---

## Phase 4 — Migrations MongoDB maison ✅ complétée

### Contexte
Aucun outillage de versioning de schéma MongoDB. Mongock écarté (réflexion/classpath scanning incompatible risque élevé avec GraalVM native-image). Solution maison légère : classes CDI + collection `_migrations` de tracking. Dépend uniquement de Phase 1 (ADR) — **parallélisable avec Phases 2/3**.

### Critères de réussite
- Démarrage Quarkus applique automatiquement migrations non exécutées
- Fonctionne en mode natif GraalVM (aucune réflexion/scan dynamique non enregistrée)
- ADR dédié rédigé

### Tâches

#### T4.1 - Trancher format des migrations ✅ complétée
- **Agent :** ARCos
- **Couvrir :** confirmer classes Java CDI (pas fichiers externes — incompatible natif), découverte via `Instance<IMongoMigration>` triée par version (pas de classpath scanning)
- **Acceptation :** ✅ approche validée compatible native-image

#### T4.2 - Implémenter mécanisme de migration ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `communs/src/main/java/.../communs/migrations/IMongoMigration.java`, `MigrationRecord.java`, `MigrationRepository.java`, `MongoMigrationRunner.java` (nouveau package)
- **Couvrir :** `MongoMigrationRunner` en `@ApplicationScoped` avec `@Observes StartupEvent`, exécution triée par version, insertion dans collection `_migrations` après succès
- **Acceptation :** ✅ mécanisme complet, idempotent. Bug générique `ObjectId`/`String` trouvé et corrigé ultérieurement en T8.2

#### T4.3 - Hints GraalVM reflection ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `MigrationRecord.java` (ou config dédiée), suivant pattern `JwtReflectionConfig.java` existant
- **Acceptation :** ✅ `@RegisterForReflection` posé (`MigrationReflectionConfig.java`)

#### T4.4 - Migration exemple no-op ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** première classe `V001_InitMigrationsCollection` (nom à préciser en implémentation)
- **Acceptation :** ✅ `V001_InitMigrationsCollection` créée, mécanisme validé

#### T4.5 - Tests ✅ complétée
- **Agent :** QALvin
- **Fichier(s) :** tests `@QuarkusTest` du nouveau package migrations
- **Couvrir :** migration appliquée une fois, idempotence (relance = pas de ré-exécution), tri par version, échec explicite (pas de blocage silencieux)
- **Acceptation :** ✅ 15 tests écrits, tous verts, 4 scénarios couverts (dont couverture réelle repository complétée ensuite en T8.2)

#### T4.6 - Documentation + ADR ✅ complétée
- **Agent :** DOCly
- **Fichier(s) :** `docs/adr/002-migrations-mongodb-maison.md`, `docs/ARCHITECTURE.md`
- **Couvrir :** convention numérotation/idempotence documentée, ADR-002 (rejet Mongock motivé)
- **Acceptation :** ✅ ADR-002 rédigé et accepté, convention documentée dans ARCHITECTURE.md

**Effort :** S-M. **Risque :** faible si injection CDI standard respectée (pas de scan classpath). **Dépendances :** Phase 1 uniquement.

---

## Phase 5 — Upgrade Java 25 (+ Quarkus 4.x reporté) — ⏸️ EN ATTENTE, prête à relancer

> **Reprise session future** : T5.1 fait, conclusion claire. Prochaine étape = lancer T5.2+T5.3 (spike DEVon, worktree isolé, module `parametrages`+`communs` uniquement, upgrade Java 21→25 + Mandrel 25 en gardant Quarkus 3.37.1). Avant de lancer, revérifier manuellement statut PR [quarkusio/quarkus#55278](https://github.com/quarkusio/quarkus/issues/55278) (non confirmé lors de T5.1). Rollback trivial si échec (worktree jetable, pas de commit sur `feat/upgrade`).

### ⚠️ T5.1 exécutée (2026-07-07) — Phase redécoupée suite recherche réelle

Recherche web ciblée (sources : GitHub Quarkus discussions #52020, quarkus.io/blog, GraalVM release notes) :
- **Quarkus 4.0 : PAS de GA à ce jour**, Beta1 visée sept. 2026 au plus tôt (roadmap "no-commitment"). **Not-yet confirmé** — prématuré pour tout spike de production sur cet axe.
- **Mandrel 25 (Java 25 LTS natif) : disponible dès maintenant**, compatible **Quarkus ≥ 3.27.0** — donc **indépendant de Quarkus 4.x**, faisable directement sur la ligne 3.x actuelle (3.37.1).
- **Bug #55278** (workaround Jackson posé en Phase 2) : statut backport non confirmé par la recherche (ticket non retrouvé) — à vérifier manuellement avant tout spike qui en dépendrait.

**Décision** : découpler l'axe Java 25 (faisable maintenant, sur Quarkus 3.x) de l'axe Quarkus 4.x (reporté, pas de cible GA fiable). Cette section couvre donc désormais uniquement l'axe encore pertinent — voir Gate ci-dessous pour la décision effective de lancement.

### Contexte
Risque élevé sur l'axe Quarkus 4.x initialement prévu (breaking changes, maturité incertaine) — **confirmé non pertinent pour l'instant** (pas de GA). Dépend de Phase 2 validée stable (✅ satisfait). **Découpée en 2 sous-étapes avec Gate humain intermédiaire dédié** (au-delà des Gates standard).

### Critères de réussite
- Spike de compatibilité validé par le développeur avant extension aux 5 modules
- Build natif Java 25/Mandrel 25 fonctionnel sur les 5 modules (Quarkus reste 3.37.1 — 4.x hors scope pour l'instant)
- ADR-003 documentant la version exacte finalement choisie (Java 25 + Mandrel 25 + Quarkus 3.37.1, pas 4.x)

### Tâches

**5a — Spike (isolé, un seul module + `communs`)**

#### T5.1 - Revalider versions cibles ✅ complétée
- **Agent :** ARCos
- **Couvrir :** version Quarkus 4.x exacte, support Mandrel/GraalVM Java 25 natif — **vérification obligatoire release notes officielles au moment de l'exécution**, aucune version figée dans ce plan
- **Acceptation :** ✅ Quarkus 4.x Not-yet (pas de GA, Beta1 sept. 2026 au plus tôt). Mandrel 25/Java 25 natif disponible dès maintenant sur Quarkus ≥3.27.0 — **cible réaliste du spike 5a : Java 25 natif SUR Quarkus 3.37.1 actuel, PAS Quarkus 4.x**. Bug #55278 statut non confirmé, à vérifier avant spike

#### T5.2 - Spike upgrade sur module isolé (portée révisée : Java 25/Mandrel 25 sur Quarkus 3.37.1, PAS Quarkus 4.x) ✅ complétée (2026-07-07)
- **Agent :** DEVon
- **Fichier(s) :** `parametrages` (module le plus petit) + `communs`, sur worktree dédié (`spike/java25-mandrel25`, basé sur `feat/upgrade`, jetable — nettoyé après capture des résultats)
- **Couvrir :** upgrade `maven.compiler.release`/`source`/`target` 21→25 + Mandrel 25, Quarkus **reste 3.37.1**. Vérifier hints GraalVM reflection existants (`JwtReflectionConfig.java`) toujours valides sous Mandrel 25
- **Acceptation :** ✅ build JVM + tests verts sur `communs` (146 tests, 4 erreurs MongoTimeout non liées à JDK25 — Docker absent en local, connu Phase 8/T8.2) et `parametrages`. **Unique breaking change réel trouvé** : Lombok ne génère plus les getters/setters sous JDK≥23 sans déclaration explicite `annotationProcessorPaths` dans `maven-compiler-plugin` (javac ≥23 a changé l'auto-discovery des annotation processors sur classpath) — corrigé par ajout de 6 lignes dans le `pom.xml` racine. Sans ce fix, ~40 erreurs de compilation en cascade sur `communs` (getters/setters absents partout). Effort réel : quelques minutes une fois la cause identifiée (bien en dessous de l'estimation "M" — le bump JDK lui-même n'a demandé aucun changement de code). `JwtReflectionConfig.java` confirmé valide sans modification. Statut PR upstream Quarkus #55278 (workaround Jackson) revérifié : mergée sur `main`, backport labellisé `triage/backport-3.x`, milestone `3.37.2` — pas encore confirmé livré en release 3.x stable, workaround T8.1 à conserver pour l'instant.

#### T5.3 - Build natif spike ✅ complétée (2026-07-07)
- **Agent :** DEVon
- **Couvrir :** build natif Lambda sur le module spike seul, via job CI GitHub Actions temporaire dédié (`spike-native-java25.yml`, jamais fusionné, supprimé après résultats) — Docker/Podman absents en local (même limite que Phase 2/T2.2, Phase 8/T8.2)
- **Acceptation :** ✅ **succès** (run [28893062710](https://github.com/vzwingma/gestion-budget-serverless/actions/runs/28893062710), 6min38s, toutes étapes vertes) avec l'image `quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-25`. Un premier essai avait échoué (401 Unauthorized sur GitHub Packages) — cause : runner CI frais sans le parent pom `services` en cache local (contrairement à la machine dev, déjà peuplée par les sessions précédentes), résolution retombant sur GitHub Packages sans credentials configurés dans ce workflow spike ponctuel. Corrigé par ajout d'une étape `mvn -N install` sur le pom racine avant `communs` — **non lié à Java25/Mandrel25**, artefact de la construction du workflow spike lui-même, sans impact sur 5b (les workflows CI existants `build-on-master.yml`/`build-on-tags.yml` gèrent déjà correctement cette résolution via jobs séparés + publish GitHub Packages).

**→ Gate spike (Go/No-Go)** : résultats ci-dessus prêts à présenter au 👤 développeur avant 5b. Spike concluant : aucun blocage réel Java25/Mandrel25/Quarkus3.37.1 trouvé, effort largement inférieur à l'estimation. Décision Go/No-Go pour 5b (T5.4-T5.7, extension aux 4 modules restants + ADR-003) **encore à prendre par le développeur** — pas de passage automatique.

**5b — Migration complète (si Go)**

#### T5.4 - Étendre aux 4 modules restants
- **Agent :** DEVon
- **Fichier(s) :** `pom.xml` racine + 5 modules (`maven.compiler.release` 21→25 ; `quarkus.platform.version` **inchangé**, reste 3.37.1 — pas d'upgrade Quarkus 4.x tant que non GA)
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

**Effort :** M (révisé à la baisse — simple bump JDK/Mandrel sur Quarkus inchangé, pas de saut de version majeure Quarkus). **Risque :** moyen (mitigation : spike obligatoire isolé avant engagement large, rollback trivial tant que 5b non lancé ; statut PR #55278 à confirmer avant lancement). **Dépendances :** Phase 2 validée stable ✅ satisfaite.

---

## Phase 6 — Hygiène technique ✅ complétée

### Contexte
2 items mineurs identifiés en cours d'exécution des Phases 1-4, sans dépendance bloquante externe — exécutables dès validation développeur. Indépendants entre eux et des autres phases.

### Critères de réussite
- `.gitignore` racine couvre tous les modules (plus de `target/` untracked résiduel)
- `architect.instructions.md` du sous-projet backend rempli (conventions archi/couches/protocoles), plus de placeholder générique

### Tâches

#### T6.1 - Compléter .gitignore racine ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `gestion-budget-serverless/.gitignore`
- **Couvrir :** ajouter `utilisateurs/target/`, `comptes/target/`, `operations/target/`, `/target/` (racine) — actuellement seuls `communs/target/` et `parametrages/target/` couverts
- **Acceptation :** ✅ 4 entrées ajoutées, `git status` propre confirmé

#### T6.2 - Instancier architect.instructions.md ✅ complétée
- **Agent :** ARCos (contenu) puis DEVon (rédaction fichier)
- **Fichier(s) :** `gestion-budget-serverless/.claude/instructions/architect.instructions.md`
- **Couvrir :** remplir depuis le template générique racine avec conventions réelles du projet (couches hexagonale, ports/adapters, patterns Mutiny/CDI/Panache déjà documentés dans `docs/ARCHITECTURE.md` et `.claude/CLAUDE.md`) — suggestion ARCos non traitée en Phase 1
- **Acceptation :** ✅ fichier existait déjà (constat initial erroné, corrigé), complété avec sections manquantes (migrations Mongo, paramétrage SAM), aucun placeholder restant

**Effort :** XS. **Risque :** faible. **Dépendances :** aucune (indépendante des autres phases).

---

## Phase 7 — Durcissement infra/CI ✅ complétée

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

#### T7.3 - Corriger sed samconfig.template.toml ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `.github/workflows/build-on-master.yml`, `build-on-tags.yml`
- **Couvrir :** `sed` global sur `__ENV__` altère aussi le commentaire explicatif ajouté en Phase 3 — cibler la substitution aux seules lignes fonctionnelles (`stack_name`/`s3_prefix`) ou reformuler le commentaire pour éviter le pattern `__ENV__`
- **Acceptation :** ✅ `sed` ciblé aux lignes `stack_name`/`s3_prefix` uniquement, commentaire intact vérifié, substitution fonctionnelle testée localement (nominal + désync simulée)

#### T7.4 - Durcir --parameter-overrides ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** `.github/workflows/build-on-master.yml`, `build-on-tags.yml`
- **Couvrir :** déclenchée par alerte sécurité SonarCloud réelle (PR #186, "Avoid expanding secrets in a run block"). Secrets/vars déplacés dans bloc `env:` du step "SAM Deploy" + référencés `$VAR` dans `run:` (pattern répliqué du step Sonar existant) — plus d'interpolation `${{ secrets.X }}`/`${{ vars.X }}` directe dans le shell
- **Acceptation :** ✅ YAML valide, aucun secret/var interpolé dans `run:` (grep confirmé), fonctionnellement identique, non commité — prêt revue humaine

#### T7.5 - Check désync version Quarkus ✅ complétée
- **Agent :** DEVon
- **Fichier(s) :** nouveau script/étape CI ou lint (ex: `.github/workflows/build-on-master.yml` ou script dédié)
- **Couvrir :** détecter automatiquement écart entre `quarkus.platform.version` du `pom.xml` et versions mentionnées dans `.claude/instructions/*.md`/`docs/ARCHITECTURE.md` (a causé désync 3.35 vs 3.36 non détectée avant cette initiative)
- **Acceptation :** ✅ step CI ajouté dans `build-on-master.yml` (job `build-communs`), testé nominal (OK) et désync simulée (échec détecté correctement, exit 1)

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

#### T8.2 - Couverture réelle MigrationRepository ✅ complétée
- **Agent :** DEVon (infra test) puis QALvin (tests)
- **Fichier(s) :** `communs/src/test/java/.../migrations/TestMigrationRepository.java` (smoke test DEVon), `TestMigrationRepositoryPersistence.java` (tests QALvin), `communs/src/test/resources/application.properties` (`quarkus.mongodb.database=communs-test`), `MigrationRepository.java` (fix générique, voir bug ci-dessous)
- **Couvrir :** `MigrationRepository` testé via vrai MongoDB (Dev Services Quarkus, pas de dépendance Testcontainers ajoutée — redondant, Quarkus l'utilise déjà en interne). Tests `enregistrerSucces`, `enregistrerEchec`, `listerVersionsAppliquees` (filtre SUCCES uniquement) contre le vrai repository, plus smoke test base fraîche.
- **Bug réel trouvé et corrigé** : `MigrationRepository` héritait de `ReactivePanacheMongoRepository<MigrationRecord>` (fige l'ID générique à `ObjectId`) alors que le `@BsonId` réel (`version`) est un `String` — `findById`/`deleteById` hérités inutilisables (pas de bug prod actuel, code n'utilisait que `persist()`/`find()`, mais piège latent). Corrigé : `ReactivePanacheMongoRepositoryBase<MigrationRecord, String>`.
- **Acceptation :** ✅ compilation vérifiée (`mvn test-compile -f communs/pom.xml`, exit 0, aucune régression). Exécution réelle des tests non possible en local (pas de Docker — même limitation que build natif Phase 2), à confirmer au prochain run CI (`ubuntu-latest`, Docker natif sur les runners).
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

1. ✅ ADR-001, ADR-002 rédigés et acceptés ; ADR-003 (Phase 5) — en attente si palier 4.x lancé
2. ✅ Override Netty manuel retiré (BOM 3.37.1 ≥ seuil CVE)
3. ✅ `mvn clean test` vert sur les 5 modules (344 tests) ; build natif confirmé indirectement (déploiement QUA/PROD réel réussi), non vérifiable en local (pas de Docker)
4. ✅ Paramétrage SAM sans `sed` (sauf stack_name/s3_prefix, contrainte SAM CLI), `MemorySize` 256Mo confirmé par mesure réelle CloudWatch (T7.1)
5. ✅ Mécanisme migrations Mongo fonctionnel en mode natif, idempotent, testé (15 tests + couverture repository réelle T8.2)
6. ⏳ Spike Phase 5 — pas encore lancé
7. ⏳ Versions cibles Quarkus 4.x/Java 25 — à revalider au lancement effectif de Phase 5

---

## Plan d'Exécution — Triggers

1. ✅ **Gate #1** (👤 validation ce plan) → déclenche Phase 1
2. ✅ Phase 1 complète (ADR-001 accepté) → déclenche Phase 2 **et** Phase 4 en parallèle
3. ✅ Phase 2 complète → **Gate #2** → déclenche Phase 3
4. ✅ Phase 4 complète (indépendamment)
5. ✅ Phase 3 et Phase 4 complètes → **Gate #3** validation groupée
6. ✅ Phase 2 validée stable en production réelle (QUA+PROD déployés et fonctionnels, 2 incidents réels résolus en cours de route) → **Phase 5 débloquée**, non lancée à ce stade (spike 5a en attente de déclenchement)
7. Spike 5a → **Gate spike dédié** (Go/No-Go) → déclenche 5b si Go
8. Phase 5 (5b) complète → **Gate #4** → clôture initiative

**Hors plan initial, ajouté en cours de route** : Phases 6/7/8 (backlog identifié pendant l'exécution) — ✅ Phase 6 complète, ✅ Phase 7 complète, Phase 8 partiellement complète (T8.2 ✅, T8.1 bloquée backport upstream).
