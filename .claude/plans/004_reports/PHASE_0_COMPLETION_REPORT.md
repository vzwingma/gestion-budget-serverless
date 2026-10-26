# Rapport Phase 0 — Hotfix sécurité JWT + bascule quarkus-oidc (Plan 004, Lots 0 et 6 avancé)

> Agent : DEVon · Branche `fix/securite-jwt-lot0` (rien commité ni poussé) · Statut : DONE, en attente Gate #2 (2e passe)

## Historique
- 1re passe (T0.1-T0.4) : correctif renforcé maison (fail-closed, issuer strict, exp UTC, logs). Gate #2 : AJUSTER (3 bloquants de revue).
- 2e passe (T0.6, ADR-005) : bascule `quarkus-oidc` (mode service). Le mécanisme maison est détaché du chemin d'authentification.

## T0.6 — Bascule quarkus-oidc
- `pom.xml` : `quarkus-oidc` (+ `quarkus-test-security` en test), versions gérées par le BOM.
- `{comptes,operations,parametrages,utilisateurs}/src/main/resources/{dev,prod}/application.properties` : `application-type=service`, `auth-server-url=https://accounts.google.com` (limité aux profils `%dev`/`%prod` pour que le profil test n'appelle pas Google), `client-id` et `token.audience` = `${oidc.jwt.id.appusercontent}.apps.googleusercontent.com` (propriété existante réutilisée), `token.issuer=any`, `token.principal-claim=email`.
- Issuer : Google émet `accounts.google.com` ou `https://accounts.google.com` alors que la discovery n'annonce que la seconde => `issuer=any` + contrôle strict des 2 formes dans `JwtRolesAugmentor` (échec => 401).
- `communs/.../api/security/JwtRolesAugmentor.java` (nouveau) : contrôle issuer + rôles `app.security.roles` (défaut `USER_COMPTES,USER_OPERATIONS,USER_UTILISATEURS`) pour tout jeton valide (même sémantique que l'ancien `isUserInRole`).
- `AbstractAPISecurityFilter` : plus de décodage/validation ; expose l'identité validée (email, jeton brut via `AccessTokenCredential`) au `JwtSecurityContext` (propagation du Bearer par `RequestJWTHeaderFactory`) + X-Api-Key. Ne surcharge plus le `SecurityContext` JAX-RS. `@Blocking` retiré des 4 `JwtSecurityFilter`.
- `/_info` : plus de chargement des clés JWKS (`AbstractAPIResource`) ni de refresh Google (`parametrages` `RootAPIResource`). Contrat inchangé (même JSON, 200). `ParametragesService.refreshJwksSigningKeys` marqué `@Deprecated`, plus appelé.
- Logs : aucun jeton ni X-Api-Key loggé (acquis Lot 0 conservés).

## Tests (périmètre a + c)
- `JwtTestSupport` x4 : clé privée RSA de test fixe, jetons RS256 réellement signés ; clé publique dans `src/test/resources/application.properties` (`quarkus.oidc.public-key`, sans discovery). Choix : validation OIDC réelle (signature, iss, aud, exp) plutôt que `@TestSecurity`, sans dépendance supplémentaire ni serveur JWKS.
- Test `UtilisateursResourceTest.testGetLastAccessDateSansAuth` : 403 => 401.
- Tests des classes maison (JWTUtils, JwtSecurityContext, etc.) laissés en place et verts : aucun `@Disabled` nécessaire.
- `communs` : `quarkus.oidc.enabled=false` en test (pas de Dev Services).

## Preuves
`mvn clean install -Dmaven.test.failure.ignore=true` (racine) : BUILD SUCCESS ; communs 151/0/4 (les 4 erreurs = tests Mongo sans Docker : `TestMigrationRepository` x1, `TestMigrationRepositoryPersistence` x3) ; parametrages 26/0/0 ; utilisateurs 36/0/0 après correction 403->401 (1 échec avant) ; comptes 52/0/0 ; operations 92/0/0.

## Non vérifié
- Build natif : `native-image` et Docker absents du poste => `mvn package -Pnative` non exécuté.
- Appel réel à Google (discovery + JWKS) : non testé (pas de réseau applicatif en test).

## Ajustement Gate #2 (T0.6, AJUSTER léger, 2026-10-25)
1. `JwtRolesAugmentor` : identité refusée (`AuthenticationFailedException` -> 401) si claim `email` absent/vide ou `email_verified` != `true` (booléen `true` ou chaîne `"true"` acceptés, Google émet l'un ou l'autre ; `"TRUE"`, `1`, absent refusés). Nouveau test `TestJwtRolesAugmentor` : nominal (2 formes d'issuer), `email_verified` chaîne, email absent/vide, `email_verified` faux/absent, issuer invalide (dont `evilaccounts.google.com`), identité anonyme inchangée. `JwtTestSupport` x4 : claim `email_verified: true` ajouté aux jetons signés.
2. `JWTUtils.encodeJWT` : `Base64.getUrlEncoder().withoutPadding()` (remplace `.replace("==", "")`, qui ne retirait pas un `=` unique). `decodeJWT` cohérent : `Base64.getUrlDecoder()` accepte avec ou sans padding.
3. Timeouts OIDC (discovery conservée, décision 👤) : `quarkus.oidc.connection-timeout=3S` et `quarkus.oidc.connection-delay=4S` dans `{comptes,operations,parametrages,utilisateurs}/src/main/resources/{dev,prod}/application.properties`. Propriétés vérifiées dans la config-doc du jar `quarkus-oidc-common` 3.40.1.1 (`OidcCommonConfig.connectionTimeout`, `connectionDelay`). Hors `%test` (clé publique locale : aucune connexion OIDC créée, propriétés sans effet).
   - `connection-timeout` (défaut 10 s) : durée max d'une requête (discovery, JWKS). 3 s : une tentative lente ne consomme pas à elle seule le budget d'init Lambda (~10 s).
   - `connection-delay` (défaut : aucun retry) : fenêtre de retry de la connexion initiale, une tentative toutes les ~2 s, uniquement à la création de la connexion. 4 s : tolère une micro-coupure Google (2 retries) ; pire cas bloquant ~4 s + dernière tentative 3 s = ~7 s < 10 s. Si Google répond, aucun coût. Valeurs à valider par mesure du cold start en QUA.
4. `IParametrageAppProvider.refreshJwksSigningKeys()` : `@Deprecated` + commentaire « AP004 T0.6 — à supprimer Phase 4 ».

Preuves ajustement : `mvn clean install -Dmaven.test.failure.ignore=true` (racine) BUILD SUCCESS. communs 157 tests / 0 échec / 4 erreurs (Mongo sans Docker : `TestMigrationRepository` x1, `TestMigrationRepositoryPersistence` x3 ; +6 tests `TestJwtRolesAugmentor` verts) ; parametrages 26/0/0 ; utilisateurs 36/0/0 ; comptes 52/0/0 ; operations 92/0/0. Non vérifié : build natif, cold start réel / appel Google (mesure QUA).
