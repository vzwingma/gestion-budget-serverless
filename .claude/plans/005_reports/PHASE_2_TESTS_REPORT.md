# Plan 005 — Phase 2 (QALvin) : rapport de tests

> Statut : Phase 2 terminée, **Gate #3 en attente**. Branche `feat/info-endpoint-public`, rebasée sur `origin/master` 45a34a8 (PR #227).

## Tests par module et par cas

| Module | Classe | Tests | Cas |
|---|---|---|---|
| comptes | `InfoEndpointPublicTest` | 15 | sans en-tête ; 10 jetons (Bearer null, `a.b.c`, sans préfixe, expiré, autre clé, non signé, `alg=none`, `email_verified=false`, issuer invalide, valide) → 200 + `nom`/`version` ; variantes de chemin |
| comptes | `InfoEndpointPublicSmokeTest` | 2 | fumée `/_info` |
| comptes | `InfoPublicTenantNonRegressionTest` | 3 | `/comptes/v2/info-public` : jeton valide authentifié, invalide 401, sans jeton 401 |
| operations | `InfoEndpointPublicTest` | 15 | idem comptes |
| operations | `ProtectedEndpointNonRegressionTest` | 7 | `GET /budgets/v2/{id}` : sans jeton / mauvais jeton 401, valide 200 ; `info-public` 401 ; **nouveau** `DELETE .../operations/_info` 401 (sans jeton, jeton invalide) |
| utilisateurs | `InfoEndpointPublicTest` | 15 | idem |
| utilisateurs | `ProtectedEndpointNonRegressionTest` | 4 | 401 sans / mauvais jeton, 200 valide |
| parametrages | `InfoEndpointPublicTest` | 15 | idem |
| parametrages | `ProtectedEndpointNonRegressionTest` | 6 | catégories + `/categories/_info` (200 car `@PermitAll` + chemin finissant par `/_info`, figé) |
| parametrages | `InfoPublicTenantNonRegressionTest` | 3 | `/categories/info-public` : jeton invalide 401 |
| parametrages | `OidcSecurityPermitAllTest` | 10 | `/_info` 200 jeton invalide ; `CATEGORIES` reste 401 ; `email_verified=false` cible CATEGORIES |
| communs | `TestInfoPublicTenantResolver` | 14 | `/_info` → `info-public`, tout autre chemin → `Default` |
| communs | `TestAbstractAPISecurityFilter` | 9 | logs anonyme/clé API DEBUG vs WARN |

Test ajouté dans cette session : `idOperationInfo_ressourceProtegee_401_pasPublic` (operations, 2 cas) — commit `e448a01`.

## Variantes de chemin (documentées, échec fermé)

Dans les 4 `InfoEndpointPublicTest` (`variantesChemin_*`) :
- `/_info/` : jeton invalide → **401** (tenant par défaut) ; sans jeton → 200 (tolérance JAX-RS du `/` final, `@PermitAll`).
- `/_info;x` : 401 (avec et sans jeton).
- `/_INFO` : 401 (avec et sans jeton).

## Grep des ressources se terminant par un `{param}`

Source : `@Path` + enums `*APIEnum` / `*UrlEnum`. Routes finissant par un paramètre :
- comptes `/comptes/v2/{idCompte}` : `@RolesAllowed` ; littéral `_info` prioritaire en JAX-RS.
- operations `/budgets/v2/{idBudget}` (GET, DELETE), `/budgets/v2/{idBudget}/operations/{idOperation}` (POST, DELETE) : `@RolesAllowed`. Seule route où `…/_info` en fin de chemin sélectionne `info-public` (anonyme) → refus par `@RolesAllowed` : **testé** (401).
- parametrages `/parametres/v2/categories/{idCategorie}` : `@PermitAll` ; `/categories/_info` → 200 anonyme, **testé et figé**. Inoffensif : aucun identifiant réel `_info`, même donnée publique.
- utilisateurs : aucune route finissant par un paramètre.

Aucune ressource non annotée (ni `@RolesAllowed`, ni `@PermitAll`) dans les 5 `*Resource` (vérifié sur le source).

## Preuves de régression (T2.4)

1. `quarkus.oidc.info-public.tenant-enabled=false` commenté dans `comptes/src/test/resources/application.properties` : **tests restés verts** (17/17). Le tenant `info-public`, sans `auth-server-url`, ne valide pas de jeton même activé : la propriété seule n'est pas détectée par les tests. Neutralisation non concluante, rétablie.
2. Variante validée par le plan : `InfoPublicTenantResolver` renvoie `Default` aussi pour `/_info` → comptes `InfoEndpointPublicTest` + Smoke : **9 échecs / 17** (jetons invalides → 401). Rétabli, `git diff` vide.
3. Resolver renvoie `null` hors `/_info` : `TestInfoPublicTenantResolver` 10 échecs / 14 ; parametrages `InfoPublicTenantNonRegressionTest.idInfoPublic_jetonInvalide_401` **rouge** (test `/categories/info-public`). Rétabli, `git diff` vide.

## Résultats du build complet

`mvn clean install -Dmaven.test.failure.ignore=true -Dtest='!*MongoTest' -Dsurefire.failIfNoSpecifiedTests=false -P sonar` : BUILD SUCCESS (25 min 48).

| Module | Tests | Échecs | Erreurs |
|---|---|---|---|
| communs | 244 | 0 | 13 (`TestMigrationRepository*`, Mongo/Docker indisponible, préexistant) |
| parametrages | 60 | 0 | 0 |
| utilisateurs | 69 | 0 | 0 |
| comptes | 132 | 0 | 0 |
| operations | 200 | 0 | 0 |

## Couverture JaCoCo (seuil 80 %)

| Classe | Lignes | Branches | Méthodes |
|---|---|---|---|
| `InfoPublicTenantResolver` | 3/3 | 4/4 | 2/2 |
| `AbstractAPISecurityFilter` | 22/22 | 20/20 | 3/3 |
| `RootAPIResource` comptes / operations / parametrages / utilisateurs | 2/2 chacun | — | 2/2 chacun |

Tous 100 %.

## Risques

- Jeton valide sur `/_info` ignoré (anonyme) : accepté Gate #0.
- `/categories/_info` répond 200 sans validation de jeton (`@PermitAll`) : figé par test.
- Le test `tenant-enabled=false` seul n'est pas protégé par un test : la désactivation est vérifiée via le resolver. Question ouverte : ajouter test de config ? (non fait, hors périmètre).
- `communs` : 13 erreurs Mongo préexistantes (CI avec Docker).
- Branche distante `origin/feat/info-endpoint-public` existante (divergente de 23/17 commits) alors que la branche était donnée « jamais poussée » : un push exigera `--force-with-lease` — décision 👤.
- Build natif CI non exécuté localement.
