# Plan 004 — Phase 4 : suppressions à faire (par le 👤) — point d'arrêt n° 1 (T4.0-c)

> Inventaire **vérifié sur disque** par 🔵 DEVon le 2026-11-28, après T4.0-a (retouches des modules) et T4.0-b/e (contexte
> de sécurité mince J1, JWKS injoignable → 503). Remplace l'inventaire de MAINa du 2026-10-25 (périmé).
> **Aucun agent ne supprime rien** (règle absolue). Le 👤 supprime les fichiers ci-dessous et exécute lui-même les drops Mongo.
> Tous les chemins sont **complets depuis la racine du dépôt `gestion-budget-serverless`**.
> Les classes de cette liste ne sont plus référencées que par d'autres classes de cette même liste (ou par leurs tests,
> eux aussi listés) : après suppression, le build doit rester vert (vérifié avant remise : voir « Preuves » du handoff).

Ordre : (1) le 👤 supprime les fichiers (section A) → (2) grep de contrôle (section B) → (3) `mvn clean install` depuis la
racine (DEVon le relance) → (4) T4.2 → … → déploiement QUA → (5) drop de la collection (section C) en QUA, puis en PROD.

---

## A. Fichiers à supprimer (👤) — 23 fichiers

### A.1 Production `communs` (11)

Racine : `communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/`

```
communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/utils/security/JWTUtils.java
communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/utils/security/JwksKeyCache.java
communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/business/ports/IJwtSigningKeyService.java
communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/business/ports/IJwtSigningKeyReadRepository.java
communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/business/ports/IJwtSigningKeyWriteRepository.java
communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/data/model/jwt/JwksAuthKey.java
communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/data/model/jwt/JwksAuthKeys.java
communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/data/model/jwt/JwtValidationParams.java
communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/data/model/jwt/JWTAuthToken.java
communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/data/model/jwt/JWTAuthPayload.java
communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/data/model/jwt/JwtAuthHeader.java
```

Après suppression, le répertoire `communs/…/data/model/jwt/` est vide (le retirer aussi).
`JWTAuthToken`, `JWTAuthPayload`, `JwtAuthHeader` sont supprimés parce que la décision J1 est retenue (Gate #1).

### A.2 Production des modules (9)

```
comptes/src/main/java/io/github/vzwingma/finances/budget/serverless/services/comptes/spi/JwsSigningKeysDatabaseAdaptor.java
operations/src/main/java/io/github/vzwingma/finances/budget/serverless/services/operations/spi/JwsSigningKeysDatabaseAdaptor.java
parametrages/src/main/java/io/github/vzwingma/finances/budget/serverless/services/parametrages/spi/JwsSigningKeysDatabaseAdaptor.java
utilisateurs/src/main/java/io/github/vzwingma/finances/budget/serverless/services/utilisateurs/spi/JwsSigningKeysDatabaseAdaptor.java
parametrages/src/main/java/io/github/vzwingma/finances/budget/serverless/services/parametrages/spi/IJwtAuthSigningKeyServiceProvider.java
comptes/src/main/java/io/github/vzwingma/finances/budget/serverless/services/comptes/config/JwtReflectionConfig.java
operations/src/main/java/io/github/vzwingma/finances/budget/serverless/services/operations/config/JwtReflectionConfig.java
parametrages/src/main/java/io/github/vzwingma/finances/budget/serverless/services/parametrages/config/JwtReflectionConfig.java
utilisateurs/src/main/java/io/github/vzwingma/finances/budget/serverless/services/utilisateurs/config/JwtReflectionConfig.java
```

Les répertoires `config/` (avec `OpenAPIConfig.java`) et `spi/` (autres adaptateurs) des modules restent : ne pas les supprimer.

### A.3 Tests (3)

```
communs/src/test/java/io/github/vzwingma/finances/budget/services/communs/utils/TestJWTUtils.java
communs/src/test/java/io/github/vzwingma/finances/budget/services/communs/utils/security/TestJWTUtils.java
communs/src/test/java/io/github/vzwingma/finances/budget/services/communs/data/model/jwt/TestJwtModels.java
```

Après suppression, le répertoire `communs/src/test/…/data/model/jwt/` est vide (le retirer aussi).

### A.4 Fichiers qui restent (retouchés par DEVon, ne pas supprimer)

- Étape T4.0-a : `JwtTestSupport` ×4 (autonomes, sans `JWTUtils`), `ComptesService`, `BudgetService`, `UtilisateursService`,
  `ParametragesService`, `IParametrageAppProvider`, `parametrages/src/main/resources/application.properties`,
  tests de ressources / services / configuration des 4 modules.
- Étape T4.0-b/e : `IJwtSecurityContext` (contexte mince), `JwtSecurityContext`, `AbstractAPISecurityFilter`,
  `RequestJWTHeaderFactory` (+ test), `TestJwtSecurityContext` et `TestAbstractAPISecurityFilter` (réécrits),
  `MigrationReflectionConfig` et `JwtRolesAugmentor` (Javadoc), nouvelle classe `OidcIndisponibleFailureHandler` (503),
  `OidcColdStartJwksDownTest` (500 → 503).
- Aucun autre fichier mort détecté : toute classe de production restante est référencée par du code vivant.

---

## B. Grep de contrôle après suppression

À lancer depuis la racine du dépôt `gestion-budget-serverless` (hors `.claude/`, `docs/` et `target/`) : **aucun résultat attendu**.

```bash
grep -rnE "JWTUtils|JwksKeyCache|IJwtSigningKey|JwksAuthKey|JwtValidationParams|JwsSigningKeysDatabaseAdaptor|JwtReflectionConfig|IJwtAuthSigningKeyServiceProvider|refreshJwksSigningKeys|jwt-signing-key-service|jwks_auth_signing_keys|JWTAuthToken|JWTAuthPayload|JwtAuthHeader" \
  --include=*.java --include=*.properties --include=*.json --include=*.xml \
  --exclude-dir=target --exclude-dir=.claude --exclude-dir=docs --exclude-dir=.git --exclude-dir=.idea --exclude-dir=worktrees .
```

Puis, depuis la racine : `mvn clean install` (ajouter `-Pci-sans-mongo` si Docker est absent), à relancer par 🔵 DEVon.

---

## C. Configuration et données (T4.0-d, 👤 uniquement)

| Élément | État |
|---|---|
| `parametrages/src/main/resources/application.properties` : `jwt-signing-key-service.url` et `.scope` | **Retirés** (T4.0-a) |
| `oidc.jwt.id.appusercontent` | **Conservée** (utilisée par `quarkus.oidc.client-id` et `quarkus.oidc.token.audience`) |
| Collection Mongo `jwks_auth_signing_keys` | **Drop par le 👤**, après le déploiement du code sans JWKS persistant, en QUA puis en PROD |

### Procédure de drop (mongosh), par environnement, sur la base `QUARKUS_MONGODB_DATABASE`

La sauvegarde `mongoexport` est **obligatoire avant le drop** (décision Gate #1 Q6). Ne jamais dropper avant le déploiement du code
sans JWKS persistant (risque R16 du plan).

```bash
# 1. SAUVEGARDE OBLIGATOIRE (shell, hors mongosh) : un fichier par environnement
mongoexport --uri="<uri>" --db=<nom_base> --collection=jwks_auth_signing_keys --out=jwks_<env>.json
# Vérifier que le fichier existe et n'est pas vide avant de continuer
```

```javascript
// 2. Vérifier la base et le contenu (mongosh)
use <nom_base>
db.jwks_auth_signing_keys.countDocuments()
db.jwks_auth_signing_keys.getIndexes()
// 3. Drop (seulement si la sauvegarde de l'étape 1 est faite)
db.jwks_auth_signing_keys.drop()
// 4. Contrôle : attendu false
db.getCollectionNames().includes("jwks_auth_signing_keys")
```

---

## D. Vérification finale (après suppression et déploiement)

- Grep de la section B vide.
- `mvn clean install` racine vert, build natif CI vert.
- Connexion réelle validée en QUA après un démarrage à froid (jeton valide → 200, forgé/expiré → 401, Google injoignable → 503).
- Drop QUA puis PROD exécutés avec sauvegarde.
