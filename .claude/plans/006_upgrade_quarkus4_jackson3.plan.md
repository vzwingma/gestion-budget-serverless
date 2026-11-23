# Plan d'Action 006 — Upgrade Quarkus 4 et migration Jackson 3

**Date de création :** 2026-11-23  
**Statut :** ⚠️ Bloqué — résultat Sonar CI PR en attente  
**Branche :** `renovate/major-quarkus.platform.version`  
**Responsable :** ⚫ MAINa  
**ADR :** aucun nouvel ADR prévu : adaptation à la version majeure déjà proposée par Renovate, sans changement de stratégie d'authentification ni d'architecture. Toute décision structurante découverte sera soumise au 👤.

## Objectif global

Rétablir la compilation et les vérifications de la branche Renovate Quarkus 4, notamment l'échec sur les annotations Jackson de `JWTAuthToken`, sans modifier les contrats JSON ni les protections OIDC existants.

### Solution retenue — Gate #0

Le 2026-11-23, 👤 a choisi l'option A : migrer les usages concernés vers Jackson 3 et les API Quarkus 4, plutôt que d'ajouter une pile Jackson 2 de compatibilité. Les corrections seront guidées par les erreurs constatées et les contrats existants, sans migration spéculative.

### État initial

- `origin/master` a été fusionné dans la branche Renovate ; `git merge-base --is-ancestor origin/master HEAD` a réussi. Le commit de fusion `5f67648` a été vérifié sur la branche distante.
- La CI signalée échoue lors de la compilation de `communs` sur `JsonDeserialize` et `JsonSerialize` dans `JWTAuthToken`. L'échec n'a pas encore été reproduit localement.
- Quarkus 4.0.0 et Java 25 sont déclarés dans le POM racine. Les formats JSON, l'authentification et le natif restent à vérifier.
- Hors périmètre : déploiement, changement fonctionnel de l'authentification, ajout volontaire d'une seconde pile Jackson, et corrections sans lien avec l'upgrade.
- Ajustement accepté par 👤 le 2026-11-23 : isoler explicitement les cinq classes d'intégration Mongo réel dans les builds CI sans MongoDB ni Docker, tout en conservant leur exécution séparée. Le profil sans Mongo ne prouve pas la persistance, les index ou l'atomicité Mongo.

## Phase 1 — Adapter le code et débloquer les builds — ✅

**Dépendance :** Gate #1 GO.  
**Responsable :** 🔵 DEVon.

### Critères

1. Les erreurs de compilation liées à Quarkus 4 et Jackson 3 sont corrigées dans le périmètre nécessaire.
2. Les deux commandes JVM de la CI passent avec le profil explicite sans Mongo : `mvn clean install -B -Pci-sans-mongo --file communs/pom.xml` et `mvn clean install -B -Pci-sans-mongo --file pom.xml`.
3. Le lint existant, s'il est configuré, passe ; sinon son absence est documentée. Les tests hors des cinq classes Mongo isolées passent ; les classes isolées restent disponibles pour exécution réelle séparée et sont signalées.
4. Les écarts, risques et commandes exécutées sont consignés ; aucune garantie de contrat n'est déduite d'une simple compilation.

### T1.1 — Migrer les usages de production concernés

- **Agent :** 🔵 DEVon.
- **Fichiers candidats :** `communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/data/model/jwt/JWTAuthToken.java`, `operations/src/main/java/io/github/vzwingma/finances/budget/serverless/services/operations/business/model/budget/BudgetMensuel.java`, `operations/src/main/java/io/github/vzwingma/finances/budget/serverless/services/operations/business/model/operation/LigneOperation.java`, ainsi que les POM et autres fichiers directement impliqués si les vérifications le démontrent.
- **Scope :** résoudre les incompatibilités Jackson/Quarkus observées, préserver les représentations des dates, les champs exposés et le comportement OIDC ; ne pas remplacer l'authentification par une validation JWT maison.
- **Acceptation :** compilation des modules concernés sans erreur Jackson ; changements limités aux incompatibilités démontrées.

### T1.2 — Adapter les auxiliaires des tests existants et vérifier les builds

- **Agent :** 🔵 DEVon.
- **Fichiers candidats :** les quatre `OidcTokenForge.java` dans les répertoires `src/test/java` de `parametrages`, `utilisateurs`, `comptes` et `operations`, uniquement pour rendre compatibles les tests existants ; fichiers de configuration directement liés si nécessaire.
- **Scope :** adapter les API et exceptions Jackson nécessaires à la compilation et à l'exécution des tests existants ; ne pas créer de nouveaux tests dans cette phase.
- **Acceptation :** les deux commandes CI ci-dessus réussissent ; rapport avec commandes, résultats et liste des fichiers modifiés. Avant Gate #2, MAINa fait effectuer la revue automatique du diff disponible et présente ses conclusions.

### T1.3 — Isoler explicitement les tests Mongo réel dans la CI

- **Agent :** 🔵 DEVon.
- **Fichiers candidats :** `pom.xml`, `.github/workflows/build-on-all.yml`, `.github/workflows/build-on-master.yml`, `.github/workflows/build-on-tags.yml`.
- **Scope :** profil Maven `ci-sans-mongo` excluant uniquement `TestMigrationRepository`, `TestMigrationRepositoryConcurrence`, `TestMigrationRepositoryPersistence`, `UtilisateurDatabaseAdaptorMongoTest` et `OperationDatabaseAdaptorMongoTest` ; activation explicite dans les builds CI exécutant des tests ; indication visible de l'exclusion. Sans ce profil, conserver la possibilité d'exécuter les tests d'intégration réels.
- **Acceptation :** builds JVM du profil réussis et nombres de tests/exclusions documentés ; validation de l'absence d'exclusion lorsque le profil est inactif. Les builds natifs et SAM qui utilisent Docker restent un blocage distinct tant qu'une infrastructure ou solution compatible n'est pas validée.

## Phase 2 — Vérifier les contrats et la non-régression — ✅

**Dépendance :** Phase 1 terminée et Gate #2 GO.  
**Responsable :** 🟢 QALvin.

### Critères

1. Des tests ciblés couvrent les comportements modifiés ou corrigés et échouent sans le correctif pertinent ; preuve de détection de régression consignée.
2. Les tests ciblés et les deux builds JVM CI avec profil `ci-sans-mongo` passent ; le rapport indique les nombres de tests et la couverture mesurée des éléments modifiés, avec seuil de 80 % lorsque mesurable ; les tests Mongo exclus sont recensés séparément.
3. Les contrats JSON concernés, les scénarios de sécurité pertinents et le comportement des dates sont vérifiés ; les écarts ou prérequis manquants sont explicités.
4. La validation native Lambda est exécutée avec la commande CI et ses prérequis disponibles, ou le blocage est documenté et soumis au 👤 sans prétendre à une validation.

### T2.1 — Tester les contrats JSON et JWT

- **Agent :** 🟢 QALvin.
- **Fichiers candidats :** tests existants des modèles `BudgetMensuel` et `LigneOperation`, `communs/src/test/java/io/github/vzwingma/finances/budget/services/communs/utils/TestJWTUtils.java`, et tests REST pertinents dans `operations/src/test/java`.
- **Scope :** cas nominaux, dates et formats, valeurs nulles et champs absents selon les contrats existants, sérialisation/désérialisation et claims JWT selon les usages réels. Ajouter seulement les tests apportant une couverture de régression utile.
- **Acceptation :** tests ciblés verts, contrats attendus explicités et preuve de détection de régression.

### T2.2 — Vérifier sécurité et compatibilité native

- **Agent :** 🟢 QALvin.
- **Fichiers candidats :** tests OIDC et `/_info` existants des quatre services, configurations de réflexion JWT et workflow `.github/workflows/build-on-master.yml`.
- **Scope :** exécuter les tests pertinents de jetons valides/invalides, absence de fuite et propagation ; vérifier les builds natifs selon les commandes effectives de la CI, sans déployer. Ne pas élargir les tests de sécurité aux comportements non affectés.
- **Acceptation :** résultats et prérequis documentés pour chaque vérification ; toute validation indisponible reste explicitement bloquée.

## Phase 3 — Synchroniser la documentation et clôturer — ⚠️

**Dépendance :** Phase 2 terminée et Gate #3 GO.  
**Responsable :** 🟣 DOCly.

### Critères

1. Les documents directement concernés reflètent la version effectivement retenue dans le POM et les prérequis vérifiés.
2. Les références et liens modifiés sont vérifiés ; les limites de validation sont visibles.
3. Le rapport, le journal des gates et le statut du plan sont à jour pour Gate #4.

### T3.1 — Mettre à jour la documentation dépendante

- **Agent :** 🟣 DOCly.
- **Fichiers candidats :** `README.md`, `docs/ARCHITECTURE.md` et instructions suivies par la configuration Renovate si leurs versions Quarkus sont affectées.
- **Scope :** synchroniser les versions et décrire seulement les changements de comportement ou prérequis confirmés ; ne pas inventer de garanties de compatibilité.
- **Acceptation :** versions documentées cohérentes avec le POM et liens modifiés valides ; rapport des fichiers et vérifications.

### T3.2 — Préparer la clôture

- **Agent :** 🟣 DOCly, avec ⚫ MAINa pour le journal et l'index.
- **Fichiers :** `.claude/plans/006_upgrade_quarkus4_jackson3.plan.md`, `.claude/plans/006_reports/`, `.claude/plans/README.md`.
- **Acceptation :** Gate #4 présenté avec preuves, risques résiduels et état de la CI ; toute publication des nouveaux changements et tout déploiement nécessitent l'accord approprié.

## Dépendances et responsabilités

`Gate #0 GO → plan → Gate #1 → DEVon → Gate #2 → QALvin → Gate #3 → DOCly → Gate #4`

| Agent | Tâches | Livrable |
|---|---|---|
| 🔵 DEVon | T1.1–T1.2 | Code adapté et builds existants |
| 🟢 QALvin | T2.1–T2.2 | Tests ciblés et preuves de non-régression |
| 🟣 DOCly | T3.1–T3.2 | Documentation et rapport |

## Critères globaux de succès

1. La branche contient la version de référence `origin/master` vérifiée au début de l'initiative.
2. Les deux commandes JVM de la CI avec profil `ci-sans-mongo` réussissent avec Java 25 et Quarkus 4 ; les cinq classes Mongo réel sont signalées et restent exécutables séparément.
3. Les contrats JSON et l'authentification concernés sont couverts par des tests de non-régression.
4. La validation native est prouvée ou son blocage est soumis au 👤 ; aucun résultat n'est présumé.
5. La documentation directement impactée est cohérente avec l'implémentation validée.
6. La CI distante est vérifiée après publication autorisée des changements ; les gates sont consignés avant clôture.

## Journal des gates

| Gate | Date | Objet | Décision 👤 | Notes |
|---|---|---|---|---|
| #0 | 2026-11-23 | Choix de solution | ✅ GO — option A | Migration Jackson 3 retenue ; conservation des contrats existants. |
| #1 | 2026-11-23 | Validation du plan | ✅ GO | Démarrage de la phase 1. |
| Ajustement | 2026-11-23 | CI sans Mongo ni Docker | ✅ GO | Isolation explicite des cinq classes Mongo réel ; test HTTP adapté à 400 à la demande du 👤. |
| #2 | 2026-11-23 | Validation du code | ✅ GO | CI de branche verte ; cinq classes Mongo isolées ; CI native en cours. |
| #3 | 2026-11-23 | Validation des tests | ✅ GO | Limites Mongo réel, branches et Sonar distant présentées. |
| #4 | — | Clôture documentaire | ⏳ En attente | — |
