# Plan 004 — Phase 4 : suppressions à faire (par le 👤)

> Inventaire établi par ⚫ MAINa le 2026-10-25, après la bascule `quarkus-oidc` (T0.6, ADR-005).
> **Aucun agent ne supprime rien** (règle absolue). Le 👤 exécute lui-même les suppressions et le drop Mongo, en Phase 4.
> Ordre conseillé : (1) DEVon fait les retouches de code préalables (section A) → (2) le 👤 supprime les fichiers (B, C)
> → (3) build `mvn clean install` vert → (4) déploiement QUA → (5) drop de la collection (D) en QUA, puis en PROD.
> Chemins relatifs à la racine du dépôt `gestion-budget-serverless`. Préfixes abrégés :
> `communs/…` = `communs/src/main/java/io/github/vzwingma/finances/budget/services/communs/`,
> `<module>/…` = `<module>/src/main/java/io/github/vzwingma/finances/budget/serverless/services/<module>/`.

---

## A. Retouches de code préalables (DEVon, Phase 4, sans suppression)

Ces fichiers restent, mais ils référencent du code à supprimer :

| Fichier | Retouche |
|---|---|
| `comptes/…/business/ComptesService.java` | Retirer `implements IJwtSigningKeyService` et l'injection de `IJwtSigningKeyReadRepository` |
| `operations/…/business/BudgetService.java` | Idem |
| `utilisateurs/…/business/UtilisateursService.java` | Idem |
| `parametrages/…/business/ParametragesService.java` | Idem, plus `IJwtSigningKeyWriteRepository`, `IJwtAuthSigningKeyServiceProvider` et la méthode `refreshJwksSigningKeys` |
| `parametrages/…/business/ports/IParametrageAppProvider.java` | Retirer `refreshJwksSigningKeys()` (`@Deprecated` en T0.6) |
| `communs/…/api/security/AbstractAPISecurityFilter.java`, `JwtSecurityContext.java`, `IJwtSecurityContext.java` | Lire l'identité via `SecurityIdentity` / `JsonWebToken` au lieu de reconstruire un `JWTAuthToken` (revue Gate #2). À décider : supprimer le contexte maison, ou le garder comme simple adaptateur. |
| `operations/…/spi/RequestJWTHeaderFactory.java` | Lire le jeton brut via `AccessTokenCredential` / `JsonWebToken` |
| `*/config/JwtReflectionConfig.java` (×4) | Retirer les classes JWKS de la réflexion native. Le fichier est supprimé s'il devient vide (voir B). |
| Tests qui mockent les dépôts de clés : `ComptesResourceTest`, `ComptesServiceTest`, `BudgetsResourceTest`, `AdminBudgetResourceTest`, `ParametragesResourceTest`, `ParametragesServiceTest`, `UtilisateursResourceTest` | Retirer les mocks `IJwtSigningKeyReadRepository` / `JwsSigningKeysDatabaseAdaptor` / `refreshJwksSigningKeys` (QALvin) |
| `JwtTestSupport.java` (×4, tests) | Ne plus dépendre de `JWTUtils.encodeJWT`. Mutualiser (test-jar `communs`) ou générer la paire de clés au démarrage des tests. |

## B. Fichiers de production à supprimer (👤)

| Fichier | Raison |
|---|---|
| `communs/…/utils/security/JWTUtils.java` | Validation maison remplacée par `quarkus-oidc` (à supprimer après la retouche de `JwtTestSupport`) |
| `communs/…/utils/security/JwksKeyCache.java` | Cache JWKS ajouté au Lot 0, inutilisé depuis T0.6 |
| `communs/…/business/ports/IJwtSigningKeyService.java` | Port de chargement des clés, devenu inutile |
| `communs/…/business/ports/IJwtSigningKeyReadRepository.java` | Lecture de la collection JWKS |
| `communs/…/business/ports/IJwtSigningKeyWriteRepository.java` | Écriture de la collection JWKS |
| `communs/…/data/model/jwt/JwksAuthKey.java` | Entité `@MongoEntity(collection = "jwks_auth_signing_keys")` |
| `communs/…/data/model/jwt/JwksAuthKeys.java` | DTO de la réponse Google certs |
| `communs/…/data/model/jwt/JwtValidationParams.java` | À confirmer par grep : paramètres de la validation maison |
| `comptes/…/spi/JwsSigningKeysDatabaseAdaptor.java` | Adaptateur Mongo des clés |
| `operations/…/spi/JwsSigningKeysDatabaseAdaptor.java` | Idem |
| `parametrages/…/spi/JwsSigningKeysDatabaseAdaptor.java` | Idem (variante avec écriture) |
| `utilisateurs/…/spi/JwsSigningKeysDatabaseAdaptor.java` | Idem |
| `parametrages/…/spi/IJwtAuthSigningKeyServiceProvider.java` | Client REST vers `https://www.googleapis.com/oauth2/v3/certs` |
| `*/config/JwtReflectionConfig.java` (×4) | Seulement s'il ne référence plus que des classes supprimées |
| `communs/…/data/model/jwt/JWTAuthToken.java`, `JWTAuthPayload.java`, `JwtAuthHeader.java` | **Seulement si** la section A retire le contexte maison. Sinon, à conserver. |

## C. Tests à supprimer (👤)

| Fichier | Raison |
|---|---|
| `communs/src/test/java/…/communs/utils/TestJWTUtils.java` | Teste `JWTUtils` |
| `communs/src/test/java/…/communs/utils/security/TestJWTUtils.java` | Idem |
| Tests marqués `@Disabled("Obsolète AP004 T0.6 — à supprimer Phase 4")` par QALvin en T0.5 | **Aucun** : les tests des classes maison (`TestJWTUtils` ×2, `TestJwtSecurityContext`, `TestJwtModels`, tests `JwksKeyCache`/`JwtSecurityContext`) restent verts (QALvin T0.5, 2026-10-25). Seuls les 2 `TestJWTUtils` seront à supprimer avec `JWTUtils` (section B). |
| `*/…/api/JwtTestSupport.java` (×4, tests) | Dépendent encore de `JWTUtils.encodeJWT` / `JWTUtils.SHA_256_WITH_RSA` (section A). Les nouveaux tests T0.5 (`OidcTokenForge` ×2, comptes + parametrages) dépendent de `JwtTestSupport.privateKey()` et `epochSecondsIn` : à reprendre lors de la mutualisation. |
| Tests T0.5 `@Disabled("BUG AP004 T0.5 …")` (3 dans `TestJwtRolesAugmentorJsonValue`, 1 dans `OidcSecurityTest`) | **Pas des suppressions** : bugs documentés à traiter par DEVon (voir `PHASE_0_TESTS_REPORT.md`), `@Disabled` à retirer une fois corrigés. |

## D. Configuration et données

| Élément | Action |
|---|---|
| `parametrages/src/main/resources/{dev,prod}/application.properties` : `quarkus.rest-client.jwt-signing-key-service.url` et `.scope` | Retirer les 2 lignes (DEVon, Phase 4) |
| Propriétés `oidc.jwt.*` qui ne servent plus qu'à l'ancienne validation | Vérifier par grep. **Garder** `oidc.jwt.id.appusercontent` (utilisée par `quarkus.oidc.client-id` / `audience`). |
| Collection Mongo `jwks_auth_signing_keys` | **Drop par le 👤**, après le déploiement du code sans JWKS, en QUA puis en PROD |

Commande de drop (mongosh), à lancer par le 👤 sur la base configurée par `QUARKUS_MONGODB_DATABASE` :

```javascript
// 1. Vérifier la base et le contenu
use <nom_de_la_base>
db.jwks_auth_signing_keys.countDocuments()
// 2. (optionnel) Sauvegarde
//    mongoexport --uri="<uri>" --db=<nom_de_la_base> --collection=jwks_auth_signing_keys --out=jwks_backup.json
// 3. Drop
db.jwks_auth_signing_keys.drop()
```

## E. Vérification après suppression

- `grep -rn "JwksAuthKey\|IJwtSigningKey\|JWTUtils\|JwksKeyCache\|jwt-signing-key-service" --include=*.java --include=*.properties .` → aucun résultat (hors `.claude/`)
- `mvn clean install` racine vert, build natif CI vert, connexion réelle validée en QUA après un démarrage à froid.
