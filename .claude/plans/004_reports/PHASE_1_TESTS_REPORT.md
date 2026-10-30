# Rapport Phase 1 — Tests T1.3 (Plan 004)

> Agent : QALvin · Branche `feat/ap004-phase1-fiabilite-mongo` · 2026-10-30 · Statut : DONE côté code de test, **en attente de revalidation CI (2 workflows)** puis Gate #3
> Code de production : **aucune modification** (`git diff -- '*/src/main'` vide après la preuve de régression).

## Tests par zone

| Zone | Unitaires | Intégration Mongo (Docker) |
|---|---|---|
| Migrations (communs) | `TestMongoMigrationRunner` (12), `TestMigrationRepository`, `TestV002AjoutIndexBudgetsUtilisateurs` | `TestMigrationRepositoryConcurrence` (9 : N runners, un seul gagnant, bail périmé, ECHEC puis SUCCES, index V002 réels), `TestMigrationRepositoryReservation` (15), `TestMigrationRepositoryPersistence` (3) — Dev Services (inchangé) |
| Opérations | `OperationDatabaseAdaptorTest` (compensation, tri, dernier budget, libellés, R5), `BudgetServiceTest`/`BudgetServiceVirementTest` (F3, écarts 1/3/24/25 mois, compte neuf, mode dégradé, virement + compensation, `DonneeCorrompueException`, R5 404/503), `OperationsServiceLibellesTest` (parité C3), `MongoTimeoutsConfigTest` | `OperationDatabaseAdaptorMongoTest` (19) — **MongoDB Testcontainers (modifié ici)** |
| Utilisateurs | `UtilisateursServiceDernierAccesTest` (`$set` ciblé, avant la réponse, WARN, 404), `MongoTimeoutsConfigTest` | `UtilisateurDatabaseAdaptorMongoTest` (3) — **MongoDB Testcontainers (modifié ici)** |

## Correction de la robustesse des tests d'intégration (cause racine)

| Constat | Cause (vérifiée dans les poms et `application.properties`) |
|---|---|
| Workflow « native snapshot apps », job Opérations : `OperationDatabaseAdaptorMongoTest` 19 tests en `MongoTimeoutException` | `mvn -P native` (profil du pom `operations`) embarque `src/main/resources/prod/application.properties`, qui définit `quarkus.mongodb.connection-string` (défaut `mongodb://localhost:27017`). Une chaîne de connexion configurée empêche Dev Services de démarrer : le test vise `localhost:27017`. En `build-on-all` (profil `dev`, clé préfixée `%dev.`), Dev Services démarre : vert. |
| `UtilisateurDatabaseAdaptorMongoTest` 3/3 skipped en CI | `utilisateurs/src/main/resources/dev/application.properties` définit `quarkus.mongodb.connection-string` **sans préfixe `%dev.`** : Dev Services jamais actif, mon `Assumptions.assumeFalse` (chaîne = défaut) ignorait donc le test, y compris en CI. Le test ne prouvait rien. |

Correctif (tests et poms de module uniquement) :
- `MongoDbTestResource` (un exemplaire par module, `QuarkusTestResourceLifecycleManager`) : démarre un `org.testcontainers.mongodb.MongoDBContainer("mongo:7.0")` et renvoie `quarkus.mongodb.connection-string=<url>/?directConnection=true`.
- Branché **uniquement** par le `QuarkusTestProfile` de chaque test Mongo (`testResources()`), avec `quarkus.mongodb.devservices.enabled=false` : les autres `@QuarkusTest` ne démarrent aucun conteneur et gardent leur comportement. Indépendant du profil Maven.
- Hypothèse « assumption Docker » écartée : l'`Assumptions` est supprimée du test utilisateurs (le test s'exécute ou échoue, ne s'ignore plus en silence).
- Dépendance : `org.testcontainers:testcontainers-mongodb` en scope `test` dans `operations/pom.xml` et `utilisateurs/pom.xml` (version 2.0.5 gérée par le BOM Quarkus 3.40.1.1 ; non présente en transitif sur le classpath de test, Dev Services l'utilise côté déploiement). Pas dans le pom racine ni `communs` (ses tests utilisent Dev Services sans conflit : pas de connection-string dans ses ressources de test).
- Option alternative non retenue : forcer `devservices.enabled=true` + effacer la connection-string via profil de test. Non vérifiable ici (pas de Docker) et dépend d'un détail d'implémentation du build-time Dev Services.

⚠️ **Non exécuté localement avec Docker** : le chemin Testcontainers (démarrage réel du conteneur, `directConnection`, image `mongo:7.0` tirée sur le runner) est validé uniquement par la compilation et par l'échec propre sans Docker. Il doit être revalidé en CI.

## Résultats locaux (`mvn clean install -B -Dmaven.test.failure.ignore=true -P sonar`, Windows, **sans Docker**)

| Module | Tests | Failures | Errors | Skipped | Remarque |
|---|---|---|---|---|---|
| communs | 227 | 0 | 13 | 0 | 13 erreurs = intégration Mongo sans Docker (Concurrence 9, Persistence 3, `TestMigrationRepository` 1) : timeouts de 30 s par test, 12 min |
| parametrages | 35 | 0 | 0 | 0 | |
| utilisateurs | 53 | 0 | 1 | 2 | Mongo IT : 1 erreur « Could not find a valid Docker environment » (démarrage de la ressource), les 2 autres tests de la classe sautés par Quarkus |
| comptes | 112 | 0 | 0 | 0 | |
| operations | 184 | 0 | 1 | 19 | Mongo IT : 1 erreur identique + 18 sautés ; 1 `@Disabled` (ci-dessous) |

`BUILD SUCCESS` (échecs ignorés). Hors tests d'intégration Mongo : **aucun échec**. Les erreurs locales sont attribuables à l'absence de Docker, pas à un défaut de code.

### Test désactivé (1 skipped hors Mongo)
`OperationDatabaseAdaptorTest.testGetLibellesOperations_maxTimeServeurEffectivement7Secondes` : `@Disabled("BUG AP004 T1.3 …")`. **Bug** : `io.quarkus.mongodb.AggregateOptions#apply` (quarkus-mongodb-client 3.40.1) traduit `maxTime(7, SECONDS)` en `AggregatePublisher.maxAwaitTime(7, SECONDS)` : aucun `maxTimeMS` n'est envoyé au serveur, la limite de 7 s de l'agrégation des libellés est inopérante (seul le `read-timeout` client de 8 s s'applique). Le pipeline est vérifié par `testGetLibellesOperations_pipelineGroupEtMaxTime7Secondes`. Correctif à décider (🔵 DEVon / 👤) : appliquer `maxTime` autrement (option de collection / `Document` `maxTimeMS`). À réactiver ensuite.

## Résultats CI déjà obtenus (avant ces commits, vérifiés par MAINa)

- « CI on all branches » run 40920928328 : SUCCESS ; communs `Concurrence` 9/9, `Reservation` 15/15, `Persistence` 3/3 ; operations `OperationDatabaseAdaptorMongoTest` 19/19 ; **utilisateurs 3/3 skipped** (corrigé ci-dessus).
- « CI - native snapshot apps » run 40920938977 : FAILURE (job Opérations, 3 failures + 16 errors `MongoTimeoutException`) (corrigé ci-dessus).

**À revalider après push** : voir handoff.

## Couverture JaCoCo (profil `sonar`, local sans Docker : intégration Mongo non comptée)

| Classe | Lignes | Branches | Méthodes |
|---|---|---|---|
| `MongoMigrationRunner` | 98,2 % | 90,0 % | 100 % |
| `V002_AjoutIndexBudgetsUtilisateurs` | 97,6 % | 94,4 % | 100 % |
| `MigrationRepository` | 97,8 % | 72,2 % | 84,6 % |
| `AbstractBDDExceptionsHandler` | 84,6 % | 83,3 % | 100 % |
| `OperationsService` | 97,9 % | 85,3 % | 100 % |
| `BudgetService` | 96,7 % | 74,6 % | 97,5 % |
| `OperationDatabaseAdaptor` | **67,5 %** | **57,7 %** | 67,6 % |
| `UtilisateursService` | 90,5 % | n/a | 88,9 % |
| `UtilisateurDatabaseAdaptor` | 84,2 % | 100 % | 100 % |

Seule `OperationDatabaseAdaptor` est sous 80 % **en local** : ses requêtes Mongo réelles (tri, dernier budget, compensation, libellés) sont couvertes par `OperationDatabaseAdaptorMongoTest` (19 tests, Docker). Valeur finale attendue dans le rapport JaCoCo/Sonar de la CI (`build-on-master`, job Sonar) ; à vérifier là. Branches de `MigrationRepository` / `BudgetService` < 80 % : seuil appliqué aux lignes par la convention du projet (≥ 80 %) ; branches restantes = gardes défensives.

## Contrôle de couverture des tests demandés

| Élément | Test(s) |
|---|---|
| `compenseOperation` | `OperationDatabaseAdaptorTest` (10 cas, dont id null/vide, erreur Mongo), `OperationDatabaseAdaptorMongoTest` (`testCompense_*`, 7 cas réels) |
| Migrations concurrentes | `TestMigrationRepositoryConcurrence`, `TestMigrationRepositoryReservation` |
| V002 | `TestV002AjoutIndexBudgetsUtilisateurs` (unitaire), index réels dans `Concurrence` |
| Parité C3 | `OperationsServiceLibellesTest.testParite_*`, `OperationDatabaseAdaptorMongoTest.testParite_ancienEtNouvelAlgorithme_surMongoReel` |
| Tri | `testChargeSoldesBudgetMensuel_triParIdCroissant`, `testOverrideLibellesOperations_triParIdCroissant`, `testChargeSoldes_triParIdChronologique_*` |
| Timeouts | `MongoTimeoutsConfigTest` (operations, utilisateurs) |
| `chargeDernierBudgetAvant` (`_`, `a` / `a_1`, aucun, borne M) | `testChargeDernierBudgetAvant_idCompteAvecUnderscore_*`, `_deuxComptesDePrefixeCommun_*` ; Mongo : `_borneMIncluse_*`, `_aucunBudget_null`, `_idCompteContenantUnUnderscore`, `_deuxComptesDeMemePrefixe_aEtAUnderscore1` |
| Écarts 1 / 3 / 24 / > 24 mois | `testGetBudgetSurCompteActif_ecartDe{1,3,24,25}Mois_*` |
| Compte neuf | `_aucunBudget_budgetVierge`, `_premierMois_pasDeBudgetPrecedent_sansEchec` |
| F3 | `testGetBudgetSurCompteInactif_budgetDuMoisAbsent_retourneLeDernierBudget`, `_aucunBudget_budgetNotFound` |
| Mode dégradé en affichage | `_erreurTechniqueSurMoisPrecedent_budgetServiSansSauvegarde`, `_erreurTechniqueSur{M,DernierBudget}_propageeSansSauvegarde` |
| Virement en échec + compensation | `BudgetServiceVirementTest` (`testEchecCible_*`, `testEchecCompensation_*`, `testEchecSource_*`, gardes 406/400) ; `testVirementInterne_erreurTechniqueSurMoisPrecedentDestination_*` |
| `DonneeCorrompueException` | `_budgetSansMois_donneeCorrompue500` (+ 4 variantes dont `_moisPrecedentSansSoldes_`) |
| R5 (404 / 503) | `testGetBudgetMensuelById_absent_budgetNotFound404`, `_erreurMongo_propagee503`, `testIsBudgetActif_*`, `TestAbstractBDDExceptionsHandler` |
| Dernier accès | `UtilisateursServiceDernierAccesTest` (6) + `UtilisateurDatabaseAdaptorTest` / `...MongoTest` |

Aucun manque détecté : aucun test ajouté pour ce contrôle.

## Détection de régression

Modification temporaire de `MongoMigrationRunner` ligne 136 (`finaliserEchec(...).replaceWith(Boolean.FALSE)` → `Boolean.TRUE`, la chaîne ne s'arrête plus sur ECHEC) :
`mvn test -pl communs -Dtest=TestMongoMigrationRunner` → **12 tests, 1 échec** : `testMigrationEnEchecEstEnregistreeEnEchecEtArreteLaChaine` (ligne 204). Rétabli par édition ; `git diff -- '*/src/main'` vide (vérifié par `git diff --quiet`). La régression du `Sort` de `OperationDatabaseAdaptor` n'est détectable que par les tests Docker (non rejouée localement).

## Bugs / constats

1. Bug Quarkus `AggregateOptions.maxTime` (ci-dessus), test `@Disabled`.
2. Tests Mongo des deux modules : en local sans Docker ils échouent (ils s'ignoraient auparavant côté utilisateurs). Choix assumé : un test d'intégration qui ne s'exécute jamais est pire qu'un échec local visible. Contournement dev local : `-Dtest='!*MongoTest'`.

## Propositions de modification de workflow (non appliquées, pour 👤)

- **Aucune obligatoire** : avec `MongoDbTestResource`, le job « native snapshot apps » doit repasser vert tant que Docker est disponible sur `ubuntu-latest` (c'est le cas).
- Optionnel : le job native (`mvn install -P native ...`) rejoue toute la suite unitaire + intégration avant la compilation native, en doublon avec `build-on-all`. Ajouter `-DskipTests` ou `-Dtest='!*MongoTest'` à cette commande gagnerait ~1-2 min par µS et supprimerait la dépendance Docker/Mongo de ce job. À arbitrer (ne pas le faire si on veut un test avant déploiement).

## Décisions 👤 post-T1.3

- **`maxTime` retiré** de `OperationDatabaseAdaptor.getLibellesOperations` (bug `AggregateOptions.maxTime` appliqué comme `maxAwaitTime`). Limite documentée : l'agrégation est bornée par le `quarkus.mongodb.read-timeout=8S` du client uniquement, aucune limite côté serveur. Test `@Disabled` `testGetLibellesOperations_maxTimeServeurEffectivement7Secondes` retiré ; `testGetLibellesOperations_pipelineGroup` adapté (appel sans `AggregateOptions`).
- **Tests `*MongoTest` exclus des builds natifs** `build-on-master.yml` et `build-on-tags.yml` (commande `mvn install -B -P native` uniquement : `-Dtest='!*MongoTest' -Dsurefire.failIfNoSpecifiedTests=false`). Ils restent joués par `build-on-all` et Sonar. Le point « workflow optionnel » ci-dessus est donc traité pour `*MongoTest` ; les autres tests d'intégration Mongo de `communs` (`TestMigrationRepository*`) ne sont pas exclus.
