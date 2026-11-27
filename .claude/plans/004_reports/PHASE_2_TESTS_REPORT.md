# Rapport Phase 2 — Tests T2.8 (Plan 004)

> Agent : QALvin · Branche `test/ap004-phase2-t28` (depuis `origin/master` fa360e7, Quarkus 4.0.1) · 2026-11-27 · Statut : DONE, **prêt à pousser** (pas de push effectué).
> Code de production : **aucune modification** (`git diff -- '*/src/main'` vide après les preuves de régression).
> Contexte : le code de la Phase 2 est déjà sur master (PR #233/#234/#235) ; les tests Mongo et le natif étaient verts en CI sur Quarkus 3.40.1.2, avant #237/#239/#240.

## Tests ajoutés (74)

| Zone | Fichier | Tests |
|---|---|---|
| Faux aval, timeouts réels 1,5 s / 4 s (operations) | `spi/ServicesDistantsTimeoutsReelsQuarkusTest` | 14 : aval lent (6,5 s) → 503 en moins de 6 s et au moins 3,5 s (read-timeout réel), 1 appel ; 400/401/403/406/500 → 503, 1 appel ; 429/502/503/504 persistants → 2 appels puis 503 ; connexion refusée → 2 tentatives (lignes `connect-error`) puis 503 ; 404 → 404 (comptes et parametrages) ; 403 puis 504 sur parametrages |
| Bout en bout IHM → operations → faux comptes | `api/ResilienceAvalBudgetEndToEndTest` | 7 : `GET /budgets/v2/query` réel (jeton OIDC valide) : compte inconnu 404 ; aval 401/403/500 → 503 (jamais 401/403) ; 502 → 2 appels puis 503 ; connexion refusée 503 ; aval lent 503 < 6 s, 1 appel |
| Aide commune de test | `test/aval/FauxAval`, `test/aval/ProfilFauxAvalTimeoutsReels` | `HttpServer` JDK (statut par numéro d'appel, délai piloté, ports partagés) ; profil qui ne change que les URL |
| TTL court du cache | `spi/CacheCategoriesTtlCourtQuarkusTest` | 4 : `expire-after-write=1S` effectivement appliqué ; 2 lectures dans le TTL = 1 appel (même instance) ; après expiration = 2 appels ; sous-catégorie idem ; une erreur n'est jamais mise en cache (dans le TTL, puis expiration) |
| Codes HTTP par module | `CommunsProvidersRestTest` ×4 (comptes, operations, parametrages, utilisateurs) | +6 chacun (24) : 400, 409, 403, 401 métier, 503 `ServiceDistantIndisponibleException`, 503 `MongoTimeoutException` (404, 406, 500, 503 Mongo et 401 existaient déjà) |
| Configuration effective | `ProfilsConfigurationTest` ×4 | +1 chacun, boucle dev/test/prod : timeouts Mongo, pool, catégorie des métriques INFO, masquage `OidcProvider` ERROR, `info-version`, `tenant-paths` propre au module, tenant désactivé, issuer, principal-claim, application-type. Operations : +1 (timeouts REST 1500/4000 et TTL `1H` sur les 3 profils) |
| Configuration effective (runtime) | `CommunsConfigurationTest` ×4 | +3 chacun (12) : métriques INFO appliqué au logger, `info-version` = version de l'application, **provenance** des valeurs (`getConfigValue().getSourceName()`) : clés communes ← `microprofile-config.properties` de communs, jamais `test-classes` ; `tenant-paths` ← `application.properties` du module ; clés de test ← `test-classes` |
| Classes de communs (couverture) | `APITraceFiltersTest` (3), `ServiceDistantIndisponibleExceptionTest` (2), `EffectiveConfigLoggerTest` (+1 `onStart`) | déclaration `@ServerRequestFilter(preMatching = true)` / `@ServerResponseFilter` contrôlée par réflexion, délégation, 503 de l'exception |
| `getBudgetAndCompteActif` | `BudgetServiceLectureParalleleTest` | +2 : id de budget invalide → 404 sans aucun appel ; compte absent (réponse vide) → 404 |

Déjà couverts, **non dupliqués** : copie défensive de la catégorie en cache (`OperationsServiceTest`, vérifie la valeur en cache intacte), retry 502→200, timeout 500 ms réduit, métrique `status=timeout`, une seule ligne `[HTTP-METRIC]` par module, `HttpMetricsRecorder`.

## Doublons de configuration retirés (item 7)

`*/src/test/resources/application.properties` (4 fichiers) : retrait de `quarkus.oidc.application-type`, `client-id`, `token.audience`, `token.issuer`, `token.principal-claim`, `info-public.tenant-enabled`, `info-public.tenant-paths` et du masquage `OidcProvider` (tous identiques à communs ou au `application.properties` du module). Restent : `oidc.jwt.id.appusercontent=test`, `quarkus.oidc.public-key`, `devservices.enabled=false` (oidc et keycloak). `InfoPublicTenantConfigTest.configFichierTest_tenantInfoPublic` (qui exigeait le doublon) devient `configFichierTest_neRedefinitPasLeTenantInfoPublic` (garde anti-doublon). La preuve de provenance est dans `CommunsConfigurationTest`.

Commentaires périmés (profils Maven `dev/` `prod/`) corrigés : `MongoDbTestResource` (operations, utilisateurs), `OperationDatabaseAdaptorMongoTest`, `UtilisateurDatabaseAdaptorMongoTest`, `OidcSecurityTest` (comptes). Le rôle de `MongoDbTestResource` est reformulé : Dev Services démarrerait un conteneur en profil test (la chaîne de connexion est `%dev,prod.`) ; la ressource reste utile pour fixer l'image `mongo:7.0` et ne pas dépendre de Dev Services.

## Preuve de régression

Neutralisation temporaire dans `operations/src/main` (commentaire de la ligne), tests `ServicesDistantsTimeoutsReels*`, `ResilienceAvalBudgetEndToEnd`, `ServicesDistantsResilience*`, puis rétablissement par édition ; `git status` / `git diff` vides sur `src/main`.

| Neutralisation | Résultat |
|---|---|
| `@RegisterProvider(ServicesDistantsResponseExceptionMapper.class)` des 2 clients | **23 échecs + 1 erreur** sur 33 : `ClientWebApplicationException` au lieu des exceptions typées ; bout en bout : 500 au lieu de 404/503 (5 échecs sur 7) |
| `retryOn = ServiceDistantTransitoireException.class` (annotation `@Retry` retirée) des 2 clients | **9 échecs + 1 erreur** sur 33 : 1 appel au lieu de 2 pour 429/502/503/504, connexion refusée sans retry, 502→200 en échec |

## Résultats locaux (`mvn -B clean install -P sonar -Dmaven.test.failure.ignore=true -Dtest='!*MongoTest,!TestMigrationRepository*,!TestMongoMigrationRunner' -Dsurefire.failIfNoSpecifiedTests=false`, Windows, Quarkus 4.0.1, **sans Docker**)

| Module | Tests | Échecs / erreurs | Ignorés |
|---|---|---|---|
| communs | 240 | 0 | 0 |
| parametrages | 82 | 0 | 0 |
| utilisateurs | 90 | 0 | 0 |
| comptes | 153 | 0 | 0 |
| operations | 306 | 0 | 0 |
| **BUILD** | **871** | **SUCCESS** | |

Non exécutés localement (Docker requis, exclusion demandée) : `OperationDatabaseAdaptorMongoTest`, `UtilisateurDatabaseAdaptorMongoTest`, `TestMigrationRepository*`, `TestMongoMigrationRunner`. **Ils n'ont pas été exécutés sur Quarkus 4.0.1** : seule la CI le dira (derniers verts en 3.40.1.2).

Quarkus 4.0.1 : **aucune incompatibilité** relevée sur les tests de la Phase 2 (résilience, cache, config, OIDC, REST).
Bruit connu et non bloquant : sans Docker, le démarrage de chaque `@QuarkusTest` journalise `Could not find a valid Docker environment` (Dev Services) puis `[MIGRATIONS] Echec inattendu ... MongoTimeoutException` (127.0.0.1:27017) ; aucun test n'échoue.

## Couverture JaCoCo (`-P sonar`, `<module>/target/jacoco-report/jacoco.xml`, seuil 80 %)

Note : les classes de `communs` ne sont mesurées que par les tests de `communs` (les rapports des 4 services ne les incluent pas) ; d'où les tests unitaires ajoutés dans `communs`.

| Classe | Lignes | Branches | Méthodes |
|---|---|---|---|
| `ServicesDistantsResponseExceptionMapper` | 100 % (9/9) | 100 % (4/4) | 100 % |
| `TraductionErreursServiceDistantInterceptor` (+ `Cible`, `Echec`) | 90,9 % (50/55) | 81,2 % (39/48) | 100 % |
| `ServiceDistantIndisponibleException` (communs) | 100 % (2/2) | — | 100 % |
| `ServiceDistantTransitoireException` | 100 % (2/2) | — | 100 % |
| `HttpMetricsRecorder` | 93,3 % (28/30) | 92,9 % (13/14) | 100 % |
| `APITraceFilters` | 100 % (5/5) | — | 100 % |
| `EffectiveConfigLogger` | 91,7 % (22/24) | 100 % (10/10) | 100 % |
| `AbstractRestClientMetricsFilter` + 2 filtres | 100 % | 100 % | 100 % |
| `BudgetDataUtils` (classe entière) | 93,7 % (133/142) | **70,7 % (65/92)** | 100 % |
| `BudgetService` (classe entière) | 97,0 % (350/361) | **77,3 % (99/128)** | 97,7 % |

- Méthodes de la Phase 2 : `getCompteFromBudgetId` / `getMoisFromBudgetId` / `getAnneeFromBudgetId` / `resoudreIdCreation` à 100 % des lignes ; `getBudgetAndCompteActif` : les 4 lignes manquantes (id invalide, compte absent) sont maintenant couvertes.
- Écarts restants, **hors Phase 2** : les branches de `BudgetDataUtils` et `BudgetService` sous 80 % sont dans du code préexistant (`razCalculs`, mensualités, `cloneOperationAEcheanceReportee`, `getMaxDateListeOperations`). Lignes `EffectiveConfigLogger` 79 % → 91,7 % après ajout du test `onStart`. 5 lignes non couvertes dans l'intercepteur (garde-fous `Error`/liaison illisible, natif).

## Tests ignorés ou désactivés

Aucun (`Skipped: 0` sur les 5 modules).

## Risques et points ouverts

1. Tests Mongo / natif non rejoués sur Quarkus 4.0.1 (CI à surveiller après push).
2. `ServicesDistantsTimeoutsReelsQuarkusTest`, `ResilienceAvalBudgetEndToEndTest` et le TTL durent respectivement ~8 s, ~12 s et ~8 s (timeouts réels, attente d'expiration de 1,5 s) : surcoût CI d'environ 30 s ; assertions de durée avec marge (borne 6 s sur un timeout de 4 s, soit 2 s de marge).
3. `FauxAval` : un seul thread de traitement (`HttpServer` par défaut) ; les cas « aval lent » ne partagent pas de serveur avec d'autres cas (fermeture en `@AfterEach`).
4. `MongoDbTestResource` : conservé (reformulé), non supprimé ; son maintien est une décision 🟠 ARCos / 👤 hors périmètre T2.8.

## Pour 🟣 DOCly (après Gate #3)

Comportements publics confirmés : 404 compte inconnu, 503 panne / timeout / 401 / 403 / 400 / 406 / 429 / 5xx de l'aval (jamais 401/403 vers l'IHM), 1 seul retry (connexion refusée, 429/502/503/504), pas de retry sur timeout, 503 en moins de 6 s, cache des catégories (TTL 1 h, erreurs non cachées), configuration commune lue depuis `communs` sur les 4 modules.
