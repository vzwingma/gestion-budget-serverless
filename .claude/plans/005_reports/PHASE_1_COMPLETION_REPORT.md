# Rapport Phase 1 — AP005 `/_info` public (DEVon)

**Statut :** DONE (en attente Gate #2) · **Branche :** `feat/info-endpoint-public` (non poussée)

## Tâches

### T1.1 — Tenant `info-public` (config main) : DONE
8 fichiers `{comptes,operations,parametrages,utilisateurs}/src/main/resources/{dev,prod}/application.properties`.
3 lignes insérées après `quarkus.oidc.connection-delay=4S` (commentaire ADR-008, `tenant-enabled=false`, `tenant-paths=<BASE>/_info`).
`<BASE>` vérifiés dans les `*APIEnum` : `/comptes/v2`, `/budgets/v2`, `/parametres/v2`, `/utilisateurs/v2`. Aucune autre ligne OIDC modifiée.

### T1.2 — Config de test : DONE
4 fichiers `src/test/resources/application.properties`, après `quarkus.oidc.token.principal-claim=email`. `communs` non touché.

### T1.3 — `@PermitAll` explicite : DONE
`RootAPIResource` de `operations`, `parametrages`, `utilisateurs` (`comptes` l'avait déjà).

### T1.4 — Log « Accès anonyme » : DONE
`AbstractAPISecurityFilter` : `logger.warn` -> `logger.debug` (1 ligne).

### Vérification Quarkus 3.40.1.1
`quarkus-oidc-3.40.1.1` : `OidcTenantConfig` (sources jar) expose `boolean tenantEnabled()` (défaut true) et `Optional<List<String>> tenantPaths()` ;
`META-INF/quarkus-config-doc/quarkus-config-model.json` contient `tenant-enabled` et `tenant-paths`.

## Tests touchés (minimum, QALvin complète en phase 2)
- Nouveau `comptes/.../api/InfoEndpointPublicSmokeTest` : `/comptes/v2/_info` -> 200 avec `Bearer null` et sans jeton.
- `parametrages/.../api/OidcSecurityPermitAllTest` (cassé par le changement attendu) :
  - `permitAll_jetonInvalide_401_authProactive` : `@ValueSource` réduit à `CATEGORIES` (INFO retiré) ;
  - `permitAll_emailVerifiedFalse_401` : cible `CATEGORIES` au lieu de `INFO` ;
  - ajout `info_jetonInvalide_200_tenantInfoPublic` (INFO -> 200 avec `Bearer null` et `Bearer a.b.c`) ;
  - Javadoc non modifiée (reste à corriger en T2.3).

## Preuves
`mvn clean install -Dmaven.test.failure.ignore=true -Dtest='!*MongoTest' -Dsurefire.failIfNoSpecifiedTests=false` : BUILD SUCCESS.

| Module | Tests | Échecs | Erreurs |
|---|---|---|---|
| communs | 227 | 0 | 13 (`TestMigrationRepository*`, MongoTimeout, pas de Docker : préexistant) |
| parametrages | 35 | 2 -> 0 après adaptation du test (re-run ciblé + module : 35/0/0) | 0 |
| utilisateurs | 50 | 0 | 0 |
| comptes | 114 | 0 | 0 |
| operations | 178 | 0 | 0 |

(Les 114 de comptes incluent le smoke test, exécuté aussi seul : 2/0/0 ; `OidcSecurityTest` 58/0/0.)

## Risques
- Jeton valide ignoré sur `/_info` (identité anonyme) : accepté Gate #0.
- Comportement dépendant de Quarkus 3.40.1 : à revérifier à chaque montée de `quarkus-oidc`.
- Variantes `/_info/` et `/_info;x` non testées (T2.1).

## Synthèse
Tâches 4/4. Prochaine phase : Phase 2 QALvin après Gate #2.


## Ajustement Gate #2 (2026-11-02, décision 👤 AJUSTER)

**Faille corrigée.** La résolution statique de quarkus-oidc (`StaticTenantResolver.DefaultStaticTenantResolver`) sélectionne le tenant statique dont l'identifiant égale un segment quelconque du chemin. `/parametres/v2/categories/info-public` ou `/comptes/v2/info-public` partaient donc vers le tenant désactivé : jeton non validé.

**Correctif.**
- `InfoPublicTenantResolver` (`communs`, `@ApplicationScoped`) : `info-public` si le chemin normalisé finit par `/_info`, sinon `"Default"`.
- Pourquoi `"Default"` et non `null` (sources quarkus-oidc 3.40.1.1) : le resolver custom est consulté en premier (`StaticTenantResolver` groupe 1) ; un `null` fait continuer sur `tenant-paths` puis sur `DefaultStaticTenantResolver` (groupe 2), qui matche le segment. `"Default"` = `OidcUtils.DEFAULT_TENANT_ID` : non nul, il court-circuite ces résolutions et `getStaticTenantContext` retombe sur le tenant par défaut.
- Bean découvert par les 4 modules : `communs` est indexé (`META-INF/jandex.idx`, jandex-maven-plugin du pom racine).
- `tenant-enabled=false` et `tenant-paths` conservés (défense en profondeur, cohérence ADR) ; `tenant-paths` est redondant.
- Logs `AbstractAPISecurityFilter` : « Accès anonyme » et « Clé API non trouvée » en DEBUG si le chemin finit par `/_info`, WARN sinon.

**Tests ajoutés.**
- `TestInfoPublicTenantResolver` (communs, 14 cas) ; 3 tests de niveau de log dans `TestAbstractAPISecurityFilter`.
- `InfoPublicTenantNonRegressionTest` comptes (`/comptes/v2/info-public` : jeton valide 200 avec identité transmise, jeton invalide 401, sans jeton 401) et parametrages (`/parametres/v2/categories/info-public` : jeton invalide 401, sans jeton 200, jeton valide 200).

**Preuve de la faille.** Resolver rendu temporairement à `null` hors `/_info` : `comptes...idInfoPublic_jetonValide_authentifie_pasAnonyme` et `parametrages...idInfoPublic_jetonInvalide_401` échouent (200 au lieu de 401 ; identité non transmise). Avec `"Default"` : verts.

**Preuves build.** `mvn clean install -Dmaven.test.failure.ignore=true -Dtest='!*MongoTest' -Dsurefire.failIfNoSpecifiedTests=false` : BUILD SUCCESS.

| Module | Tests | Échecs | Erreurs |
|---|---|---|---|
| communs | 244 | 0 | 13 (`TestMigrationRepository*`, MongoTimeout, pas de Docker : préexistant) |
| parametrages | 38 | 0 | 0 |
| utilisateurs | 50 | 0 | 0 |
| comptes | 117 | 0 | 0 |
| operations | 178 | 0 | 0 |

**À reporter par QALvin (T2.1 ×4)** : variantes `/_info/`, `/_info;x`, casse, double slash en e2e.
