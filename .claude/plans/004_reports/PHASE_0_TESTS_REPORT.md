# Rapport Phase 0 — Tests de sécurité T0.5 (Plan 004)

> Agent : QALvin · Branche `fix/securite-jwt-lot0` · 2026-10-25 · Statut : DONE, en attente Gate #3
> Code de production : **aucune modification** (`git diff -- '*/src/main'` vide, vérifié après la preuve de régression).

## Périmètre T0.5
(a) tests existants adaptés : fait par DEVon (T0.6). (b) sécurité, (c) classes maison, (d) démarrage à froid : ce rapport.

## Tests créés (84 cas au total, cas paramétrés inclus)

| Fichier | Module | Contenu |
|---|---|---|
| `comptes/.../api/OidcSecurityTest.java` | comptes | 55 cas e2e sur `GET /comptes/v2/tous` (`@RolesAllowed`) : chaîne réelle OIDC -> augmentor -> rôles |
| `comptes/.../api/OidcTokenForge.java` | comptes | Forge de jetons (en-têtes/claims arbitraires, clé de test ou autre clé) |
| `comptes/.../api/LocalOidcServer.java` | comptes | Faux serveur OIDC local (JDK HttpServer) : discovery + JWKS |
| `comptes/.../api/OidcColdStartTest.java` (+ `OidcColdStartSupport`) | comptes | (d) 1re requête authentifiée après démarrage, discovery + JWKS réels (local) |
| `comptes/.../api/OidcColdStartJwksDownTest.java` | comptes | (d) JWKS en panne : rejet sans blocage |
| `parametrages/.../api/OidcSecurityPermitAllTest.java` (+ `OidcTokenForge`) | parametrages | 9 cas `@PermitAll` (`/_info`, `/categories`) |
| `communs/.../security/TestJwtRolesAugmentorJsonValue.java` | communs | `JsonValue` / `JsonString` mockés (email, email_verified) |
| `communs/.../security/TestAbstractAPISecurityFilter.java` | communs | 6 tests unitaires du filtre |
| `operations/.../spi/RequestJWTHeaderFactoryTest.java` | operations | 5 tests unitaires de la propagation Bearer / X-Api-Key |
| `comptes|parametrages/.../JwtTestSupport.java` | (modif. test) | accesseur `privateKey()` (clé de test réutilisée par la forge) |

### Cas couverts par `OidcSecurityTest` (e2e, jetons réellement signés RS256 par la clé de test)
- **Nominal** : jeton valide -> 200 + email transmis au service ; iss `accounts.google.com` et `https://accounts.google.com` -> 200 ; `email_verified` en chaîne `"true"` -> 200 (**type réel OIDC confirmé : String et Boolean acceptés**).
- **Signature** : RS256 sans signature, `alg` = none/None/NONE/nOnE (sans et avec fausse signature), HS256 (confusion), autre clé, payload modifié signature conservée, signature tronquée -> **401**.
- **kid inconnu** : -> **200** (documenté : avec `quarkus.oidc.public-key`, le `kid` n'est pas utilisé ; cas à revérifier en QUA avec le JWKS réel).
- **iss** : `evilaccounts.google.com`, `accounts.google.com.evil.com`, `https://evil.com`, `http://accounts.google.com`, `https://accounts.google.com/`, `https://accounts.google.com.evil.com`, `ACCOUNTS.GOOGLE.COM`, vide, absent -> **401** (`issuer=any` : l'augmentor est la seule barrière, prouvé par la régression ci-dessous).
- **exp** : -1 h, -2 min, absent -> 401. **aud** : inconnu, absent -> 401.
- **Authorization** : absent, `Bearer null`, `Bearer undefined`, `Bearer `, `Bearer`, `Bearer a.b`, `Bearer a.b.c`, `Bearer abc`, `Basic …`, `Token …`, vide, blanc, `Bearer Bearer`, jeton valide sans préfixe -> 401.
- **email / email_verified** : email absent ou blanc, `email_verified` false / absent / `"false"` / `"TRUE"` / `"1"` / `"yes"` -> 401.
- **Logs** : jeton valide, iss invalide, exp dépassé -> aucun jeton / payload / signature / X-Api-Key dans les logs (capture JUL).

### `@PermitAll` (parametrages)
Sans jeton -> 200 (`/_info`, `/categories`) ; jeton valide -> 200 ; jeton invalide (`Bearer null`, mal formé, autre clé, non signé, expiré, iss invalide, `email_verified` false) -> **401** (comportement accepté, décision 👤).

### (d) Démarrage à froid
Faisable sans réseau externe : `@TestProfile` (redémarrage de l'application) + faux serveur OIDC local (discovery + JWKS) + suppression de `quarkus.oidc.public-key`.
- JWKS disponible : 1re requête authentifiée -> **200** ; discovery et JWKS chargés (compteurs HTTP du faux serveur).
- JWKS en panne (500) : requête rejetée, sans blocage (< 20 s, ~2 s constatées), **mais statut 500 et non 401** (voir bug 2).
- **Non vérifié, à faire en QUA** : appel réel à Google, latence réseau, effet de `connection-timeout=3S` / `connection-delay=4S` face à une discovery injoignable au démarrage, refus d'un `kid` inconnu avec le JWKS réel, cold start Lambda mesuré, build natif.

### (c) Classes maison
`TestJWTUtils` ×2, `TestJwtSecurityContext`, `TestJwtModels` : verts, inchangés. **Aucun test marqué « Obsolète »** (rien à supprimer côté tests hors les 2 `TestJWTUtils` avec `JWTUtils`). `PHASE_4_SUPPRESSIONS_A_FAIRE.md` section C complétée.

## Bugs / constats
1. **BLOQUANT (à arbitrer par 👤 / DEVon avant PROD) — fuite du jeton complet dans les logs.** Sur signature invalide (jeton forgé, ou payload modifié), `quarkus-oidc` (`io.quarkus.oidc.runtime.OidcProvider:294`, niveau WARN) logue « Invalid JWS Signature: JsonWebSignature{…}->`<jeton complet>` ». Aussi pour un jeton mal formé (`a.b.c` est loggué). Les jetons expirés / iss invalide / aud inconnu ne fuient pas (testés). Test : `OidcSecurityTest.logs_signatureInvalide_aucuneFuite`, `@Disabled("BUG AP004 T0.5 …")`. Correctif suggéré : `quarkus.log.category."io.quarkus.oidc.runtime.OidcProvider".level=ERROR` dans les `application.properties` prod/dev (à vérifier que le message n'est pas relogué à ERROR). Un jeton forgé est sans valeur, mais la ligne de log est capturée par CloudWatch.
2. **Constat (non bloquant, à valider) — JWKS injoignable => 500 et non 401.** L'`OIDCException` n'est pas convertie en 401. Fail-closed respecté, mais côté IHM un 500 n'entraîne pas de déconnexion (toast d'erreur). Test de caractérisation : `OidcColdStartJwksDownTest` (assert 500 ; passera au rouge si le comportement évolue vers 401).
3. **Théorique, sans impact constaté — `JwtRolesAugmentor` face aux types `jakarta.json`.** `JsonValue.TRUE` et `JsonString("true")` pour `email_verified` sont **refusés** ; `JsonValue.NULL` / `JsonString` blanc pour `email` sont **acceptés** (`toString()` = `null` ou `"   "` avec guillemets, jamais blanc). Le test e2e prouve que le type réel renvoyé par `quarkus-oidc` 3.40 est `Boolean` / `String`, donc aucun impact aujourd'hui (`email` vide en chaîne -> 401). 3 tests `@Disabled("BUG AP004 T0.5 …")` dans `TestJwtRolesAugmentorJsonValue`. À durcir par DEVon par prudence (changement de version de l'extension).
4. `kid` inconnu accepté avec `quarkus.oidc.public-key` (attendu, test uniquement ; cf. ci-dessus).

## Preuves

Commandes (depuis `gestion-budget-serverless`) :
- `mvn clean install -Dmaven.test.failure.ignore=true -Dsonar` (profil `sonar` = JaCoCo) : **BUILD SUCCESS**.

| Module | Tests | Échecs | Erreurs | Ignorés |
|---|---|---|---|---|
| communs | 170 | 0 | 4 (Mongo sans Docker, préexistantes : `TestMigrationRepository` ×1, `TestMigrationRepositoryPersistence` ×3) | 3 (`@Disabled` BUG, bug 3) |
| parametrages | 35 | 0 | 0 | 0 |
| utilisateurs | 36 | 0 | 0 | 0 |
| comptes | 109 | 0 | 0 | 1 (`@Disabled` BUG, bug 1) |
| operations | 97 | 0 | 0 | 0 |

Avant T0.5 : communs 157, parametrages 26, comptes 52, operations 92. Ajoutés : 13 + 9 + 57 + 5 = 84 tests (dont 4 `@Disabled`).

### Couverture (JaCoCo, `target/jacoco-report/jacoco.xml`, seuil 80 %)
| Classe | Lignes | Branches |
|---|---|---|
| `JwtRolesAugmentor` | 18/18 (100 %) | 19/20 (95 %) |
| `AbstractAPISecurityFilter` | 14/14 (100 %) | 10/10 (100 %) |
| `RequestJWTHeaderFactory` | 15/15 (100 %) | 8/8 (100 %) |
| `JWTUtils` (inchangé) | 75/80 (94 %) | 48/56 (86 %) |
| `JwtSecurityContext` | 16/16 (100 %) | 10/12 (83 %) |

Avant les tests T0.5 : `AbstractAPISecurityFilter` 0 %, `RequestJWTHeaderFactory` 0 % (couverts désormais par des tests unitaires dédiés).

### Détection de régression
Contrôle d'issuer de `JwtRolesAugmentor` neutralisé temporairement (`if (iss == null || …)` -> `if (false)`), exécution de `TestJwtRolesAugmentor` + `OidcSecurityTest` : **10 tests rouges** (1 unitaire `issuerInvalide_refuse`, 9 e2e : `issuerInvalide_401` ×8, `issuerAbsent_401`, tous « expected 401 but was 200 »). Fichier rétabli par édition ; `git diff --quiet -- communs/src/main` : vide.

## Choix de commit
Tests (`test(security)`) et rapport + inventaire (`docs(plans)`) en **deux commits séparés** : la revue du code de test et celle du rapport sont indépendantes, et le rapport peut être ajusté après Gate #3 sans retoucher les tests.

## Risques
- Bug 1 (fuite jeton dans les logs) à traiter avant PROD.
- Bug 2 : décision 👤 sur le 500 en cas de panne JWKS (lié au plan IHM 008).
- Validation QUA indispensable (cold start réel, Google réel, kid, natif).
