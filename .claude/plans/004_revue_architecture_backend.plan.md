# Plan d'Action 004 — Revue architecture backend : sécurisation, fiabilisation, modernisation Quarkus/Mongo

**Date création :** 2026-10-25
**Statut :** 🔵 En cours — Gate #1 GO ; Phase 0 en ajustement (bascule `quarkus-oidc`)
**Porteur :** ⚫ MAINa
**ADR liés :** [ADR-005 — Authentification JWT via extension Quarkus](../../docs/adr/005-authentification-jwt-extension-quarkus.md) (`Acceptée` pour le principe ; choix final oidc / smallrye-jwt par essai Phase 0) · ADR-002 (migrations maison, maintenu) · ADR-004 (`Clock`, maintenu)

---

## Objectif global

Revue ARCos (2026-10-25) du backend `gestion-budget-serverless` : note globale ≈ 2,5/5. Base saine (hexagonal lisible,
Mutiny cohérent, natif Lambda opérationnel) mais faille d'authentification critique (JWT non signés acceptés,
fail-open sans clés, `iss` vérifié par `contains`), flux réactifs non fiables sous Lambda, aucun index Mongo,
appels inter-services sans timeout ni cache, 7 classes dupliquées ×4 modules.

Trajectoire retenue au Gate #0 : **Lot 0 hotfix immédiat, puis option A (gains rapides sans changement de contrat),
puis option B premier temps (modernisation sans changement de contrat)**. Option C (regroupement Lambdas) **exclue**
(4 Lambdas conservées).

**Hors périmètre** (décisions 👤 Gate #0) :
- Contrôle de propriété des données + rôle admin (S3 / ex-L8, changement de contrat) → second temps, plan dédié IHM + serverless.
- Montants `Double` → `BigDecimal`/`Decimal128` → plus tard, hors plan.
- Regroupement Lambdas (C / C') → exclu.
- Choix X-Ray/ADOT : évaluation seulement (Phase 3), pas de décision anticipée.

Contexte 👤 : application familiale, multi-utilisateurs. `ApiKeyRequired` + Usage Plan configurés manuellement console AWS (S6 → documenter, pas de changement infra obligatoire).

---

## Phase 0 — Hotfix sécurité JWT (Lot 0) — 🔵 en cours (autorisé avant Gate #1)

### Contexte
Faille S1/S2/S4/S5 exploitable aujourd'hui. Autorisation 👤 de lancer avant validation du plan complet.
Branche `fix/securite-jwt-lot0` (dépôt serverless), non commitée.

### Critères de réussite
- Jeton non signé, signature invalide, `iss` usurpé, jeton expiré → rejetés (tests automatisés).
- Aucun cache vide → fail-closed, **sans** provoquer d'indisponibilité de l'authentification (démarrage à froid, rotation de clés Google).
- Aucun jeton brut dans les logs.
- `mvn clean install` racine vert (hors 4 tests Mongo nécessitant Docker, préexistants).

### Tâches
#### T0.1 — Fail-closed signature + `iss` strict ✅ (DEVon)
- **Fichier(s) :** `communs/.../utils/security/JWTUtils.java`
- **Acceptation :** non signé / aucune clé → `false` ; `iss` ∈ {`accounts.google.com`, `https://accounts.google.com`}.

#### T0.2 — Cache JWKS thread-safe, TTL, chargement garanti ⚠️ à ajuster (DEVon)
- **Fichier(s) :** `communs/.../utils/security/JwksKeyCache.java` (nouveau), `IJwtSigningKeyService.java`, `*/api/override/JwtSecurityFilter.java`
- **Acceptation :** clés disponibles dès la 1re requête après cold start ; rotation `kid` prise en charge ; pas de blocage event loop ; single-flight + backoff sur échec.
- **Revue Gate #2 :** voir journal — `@Blocking` probablement inopérant sur filtre `@PreMatching`, pas de refresh sur `kid` inconnu, source Google uniquement via `/_info` de `parametrages`.

#### T0.3 — Expiration en UTC (`Instant`) ✅ (DEVon)
- **Fichier(s) :** `communs/.../data/model/jwt/JWTAuthToken.java`, `JWTUtils.java`

#### T0.4 — Plus de jetons dans les logs ✅ (DEVon)
- **Fichier(s) :** `JWTUtils.java`, `AbstractAPISecurityFilter.java`, `AbstractAPIInterceptors.java`

#### T0.6 — Bascule `quarkus-oidc` (Lot 6 avancé, décision 👤 Gate #2 AJUSTER) 🔵 (DEVon)
- **Contexte :** remplace le correctif renforcé maison de T0.2 (constats bloquants de revue). Implémente ADR-005.
- **Fichier(s) :** `pom.xml` (racine/modules), `*/src/main/resources/{dev,prod}/application.properties`, filtres sécurité `communs` + `*/api/override/JwtSecurityFilter.java`, `JwtSecurityContext`, `JWTUtils`, `IJwtSigningKeyService` et adaptateurs JWKS (retirés du chemin d'authentification, sans suppression de fichier sans accord 👤).
- **Implémenter :** `quarkus-oidc` mode `service`, `auth-server-url=https://accounts.google.com`, `client-id`/audience, rotation JWKS gérée par l'extension, identité/rôles via `SecurityIdentity` (+ augmentor si besoin), rejet non signé / `iss` / `aud` / `exp`, aucune donnée de jeton loggée, contrat API inchangé.
- **Repli :** si l'essai montre `quarkus-oidc` inadapté (natif, cold start, multi-issuer…) → `smallrye-jwt` et retour au gate avec la raison.
- **Acceptation :** `mvn clean install` racine vert (hors tests Mongo sans Docker) ; build natif CI vert ; `code-review` + `security-review` sans bloquant.

- **Compléments 👤 (2026-10-25) :**
  - Fichiers JWT maison devenus inutiles : **laissés en place jusqu'à la Phase 4** (suppression par le 👤 ou sur son ordre).
  - Collection Mongo `jwks_auth_signing_keys` + refresh `/_info` : **retirer** le refresh et tout usage de la collection dans la bascule si sans risque. Suppression de données/fichiers interdite aux agents → DEVon liste ce que le 👤 doit supprimer/dropper lui-même.
  - Tests existants adaptés à `quarkus-oidc` par DEVon (périmètre (a) de T0.5), pour garder le build vert.

#### T0.5 — Tests sécurité + configuration OIDC (QALvin, après GO Gate #2 ; (a) par DEVon dans T0.6)
- **(a) Tests existants adaptés** : `JwtTestSupport` ×4 → jetons signés avec clé de test via configuration OIDC de test (profil `%test`, JWKS/clé publique de test ou `quarkus-test-security-oidc`) ; ressources `*ResourceTest` vertes.
- **(b) Tests de sécurité** : jeton non signé, mal signé, `alg=none`, `iss` invalide (dont variantes `contains` : `evilaccounts.google.com`, `accounts.google.com.evil.com`), `exp` dépassé et borne, `aud` inconnu, `kid` inconnu, header `Authorization` absent / mal formé, rôles (accès autorisé / refusé), endpoints publics toujours accessibles, absence de jeton dans les logs.
- **(c) Fichiers maison laissés en place** : leurs tests restent verts ou sont explicitement marqués obsolètes (`@Disabled("Obsolète AP004 T0.6 — à supprimer Phase 4")`), sans suppression.
- **(d) Intégration démarrage à froid** si faisable : première requête authentifiée après démarrage (JWKS de test servi localement) acceptée ; échec de récupération JWKS → rejet (fail-closed) sans blocage.
- **Acceptation :** tous verts, couverture ≥ 80 % des classes de sécurité touchées ; preuve de détection de régression (ex. réintroduire l'acceptation non signée → test rouge).
- **Ancien périmètre (mécanique maison, conservé pour mémoire) :**
- Jeton `alg=none` / sans signature / signature invalide / `kid` inconnu ; `iss` `evilaccounts.google.com`, `accounts.google.com.evil.com` ; bornes `exp` (Clock figée) ; `JwksKeyCache` (concurrence, purge, TTL, cache vide) ; absence de jeton dans les logs ; test d'intégration filtre avec vrai service de clés ; mutualisation `JwtTestSupport` ×4.
- **Acceptation :** tous verts, couverture ≥ 80 % des classes touchées.

**Effort :** S (+ ajustement S/M). **Risque :** ÉLEVÉ (authentification de tout le backend) → validation QUA avant PROD.

---

#### T0.7 — IHM : traiter 401 comme 403 (déconnexion) — ➡️ **sortie du plan 004** (décision 👤 Gate #2 GO, 2026-10-25)
- **Déplacée vers** le plan IHM dédié `gestion-budget-ihm/.claude/plans/008_gestion_401_auth_oidc_backend.plan.md` (⏳ planifié, hors périmètre immédiat). **Prérequis bloquant pour la PROD** du Lot 0 serverless.
- Contenu d'origine conservé pour mémoire :
- **Contexte :** avec `quarkus-oidc`, un jeton absent ou invalide renvoie **401** au lieu de 403 (décision 👤 Gate #2 : adapter l'IHM, pas le backend). De même, les endpoints `@PermitAll` renvoient 401 si le jeton est invalide (auth proactive conservée, décision 👤).
- **Dépôt :** `gestion-budget-ihm` — `src/main/Services/ClientHTTP.service.ts` : déconnexion (`logOut()`) sur 401 comme sur 403 ; tests Vitest associés.
- **Déclencheur :** à planifier avant le déploiement PROD du Lot 0 (sinon un jeton expiré renvoie une erreur au lieu de déconnecter). Traçabilité : ce plan ou un plan IHM dédié, au choix du 👤.
- **Acceptation :** `npm run lint`, `npm run typecheck`, `npx vitest run` verts ; couverture ≥ 80 % sur le service.

## Phase 1 — Fiabilité fonctionnelle + MongoDB (Lots 1 et 2, option A)

### Critères de réussite
- F1-F4 corrigés et couverts par tests de non-régression.
- Requêtes `budgets` en IXSCAN (index déclarés par migration `V002`).
- Migrations sans exécution concurrente (verrou) ; client Mongo avec timeouts < timeout Lambda.

### Tâches
#### T1.1 — Fiabiliser les flux réactifs (DEVon)
- F1 `utilisateurs/.../spi/UtilisateurDatabaseAdaptor.java` / `UtilisateursService.java` : écriture lastAccess chaînée (plus de `subscribe()` fire-and-forget).
- F2 `operations/.../business/BudgetService.java` virement : propagation d'erreur, un seul appel compte destination.
- F3 repli compte inactif (`OperationDatabaseAdaptor.java` erreur vs `ifNull`), F4 récursion bornée.
- Tri `Sort` mal passé (`OperationDatabaseAdaptor.java:~95`), `chargeBudgetMensuel` par `_id`.
- **Acceptation :** `mvn clean test` vert ; comportement documenté dans le rapport.

#### T1.2 — Index + verrou migrations + client Mongo (DEVon)
- Migration `V002` défensive (ADR-002) : `budgets {idCompteBancaire:1, annee:1, mois:1}`, `utilisateurs {login:1}` unique, `comptesbancaires {"proprietaires.login":1}`.
- Verrou `_migrations` (statut `EN_COURS` + index unique) ou exécution unique (F6).
- Timeouts pool / server selection / socket dans `*/src/main/resources/prod/application.properties`.
- Agrégation libellés côté Mongo (P3).
- **Acceptation :** migration idempotente ; `explain` IXSCAN (QUA) ; timeouts < 15 s.

#### T1.3 — Tests Phase 1 (QALvin)
- Tests non-régression F1-F4, test virement en échec partiel, tests adaptateurs Dev Services Mongo (si Docker dispo CI) pour requêtes/index/tri.
- **Acceptation :** verts, couverture ≥ 80 % classes touchées.

---

## Phase 2 — Mutualisation `communs` + appels inter-services (Lots 3 et 4, option A)

### Critères de réussite
- 0 duplication des 7 classes listées (M1) ; configuration commune ; profils Quarkus `%dev`/`%prod` (M2).
- Clients REST `operations` → `comptes`/`parametrages` avec timeouts < timeout Lambda, fault tolerance, cache.

### Tâches
#### T2.1 — Mutualiser dans `communs` (DEVon)
- `JwtSecurityFilter`, `APIExceptionsHandler`, `SPIExceptionsHandler`, `JwtReflectionConfig`, `OpenAPIConfig`, `RootAPIResource`, `JwsSigningKeysDatabaseAdaptor` (variante écriture `parametrages` conservée) ; filtres pre/postMatching globaux ; `META-INF/microprofile-config.properties` commun ; clés `rest-client.*` homogènes.
- **Acceptation :** 5 modules verts, chemins `*APIEnum` inchangés, build natif CI vert.

#### T2.2 — Timeouts, fault tolerance, cache (DEVon)
- `quarkus-smallrye-fault-tolerance` (`@Timeout`, `@Retry` lectures), `quarkus-cache` (catégories, compte), timeouts REST client.
- **Acceptation :** p95 création opération QUA mesuré avant/après.

#### T2.3 — Tests Phase 2 (QALvin)
- Non-régression 5 modules, tests timeout/retry/cache.

---

## Phase 3 — Observabilité : évaluation (Lot 5)

### Critères de réussite
- Note comparative X-Ray / ADOT / retrait OpenTelemetry avec coûts et services AWS disponibles → décision 👤 dédiée (pas de choix anticipé).
- Contournement Quarkus #55278 revérifié (AP 001 T8.1) ; `quarkus.mongodb.metrics.enabled` sans Micrometer corrigé.

### Tâches
#### T3.1 — Évaluer options observabilité (ARCos) — livrable : comparatif + recommandation.
#### T3.2 — Appliquer la décision + retrait #55278 si corrigé (DEVon), logs JSON éventuels.

---

## Phase 4 — Modernisation sans changement de contrat (option B, 1er temps : Lots 6 et 7)

### Critères de réussite
- ADR-005 tranché (`quarkus-oidc` vs `smallrye-jwt`) via spike ; JWTUtils / persistance JWKS / refresh `/_info` retirés du chemin d'authentification.
- Ports repository purs (plus d'héritage Panache ni `org.bson.Document` dans les ports) ; sécurité hors domaine (H1-H3).
- Mises à jour ciblées ou verrouillage optimiste ; virement transactionnel ; Hibernate Validator ; `@ConfigMapping`.

### Tâches
#### T4.1 — ~~Spike auth standard + implémentation ADR-005~~ → avancé en Phase 0 (T0.6). Reste éventuel : nettoyage collection Mongo JWKS (sur décision 👤)
#### T4.0 — Nettoyage post-bascule OIDC (DEVon retouches + 👤 suppressions)
- **Inventaire :** [`004_reports/PHASE_4_SUPPRESSIONS_A_FAIRE.md`](./004_reports/PHASE_4_SUPPRESSIONS_A_FAIRE.md) (fichiers prod, tests, config, drop de la collection `jwks_auth_signing_keys`).
- **Acceptation :** grep de vérification vide ; build + natif verts ; drop exécuté par le 👤 (QUA puis PROD).

#### T4.2 — Ports purs + intégrité écritures (DEVon)
#### T4.3 — Tests Phase 4 dont tests adaptateurs Dev Services (QALvin)

---

## Phase 5 — Documentation (Lot 9, DOCly)

- `docs/ARCHITECTURE.md` (dérive D1 : versions, collections, verbes HTTP, ADR 003-005, Micrometer, clé API configurée console).
- ADR-005 → `Acceptée` (au GO Gate #1 pour principe, choix technique final consigné après spike T4.1).
- Wiki backend (`gestion-budget-serverless.wiki`) : historique architecture ; wiki IHM si impact codes 401/403.
- Mise à jour au fil des phases (pas seulement en fin).

---

## Résumé par agent

| Agent | Phases | Livrables |
|---|---|---|
| 🔵 DEVon | 0, 1, 2, 3, 4 | Code + preuves `mvn clean install` |
| 🟢 QALvin | 0, 1, 2, 4 | Tests attaque JWT, non-régression, Dev Services, couverture ≥ 80 % |
| 🟠 ARCos | 3, 4 | Comparatif observabilité, spike auth |
| 🟣 DOCly | 5 (+ ADR-005) | ARCHITECTURE.md, ADR, wikis |

## Dépendances

```
Phase 0 (hotfix) ──► Phase 1 ──► Phase 2 ──► Phase 4 ──► Phase 5
                                    └──► Phase 3 (évaluation, parallélisable avec Phase 4 après Gate)
```
Chaque phase : Gate #2 (code) → Gate #3 (tests) ; Gate #4 global en fin de plan. Republier `communs` avant les modules.

## Critères de succès globaux

1. Jeton non signé / forgé / expiré → 401 (test automatisé) ; aucune indisponibilité d'auth à la rotation des clés Google.
2. `grep "eyJ"` vide sur logs QUA.
3. Requêtes `budgets` en IXSCAN.
4. 0 classe dupliquée parmi les 7 listées.
5. p95 création d'opération QUA mesuré avant/après Phase 2.
6. Build natif + déploiement QUA verts à chaque phase ; contrat API inchangé.
7. `docs/ARCHITECTURE.md` aligné avec le code ; ADR-005 statut final.

## Plan d'exécution

- Phase 0 : démarrée 2026-10-25 (autorisation 👤 Gate #0). Gate #2 en attente.
- Phase 1 : après GO Gate #1 + Gate #3 Phase 0 + validation QUA du hotfix.
- Phases suivantes : après Gate #3 de la phase précédente.

---

## 🚦 Journal des gates

| Gate | Date | Objet | Décision | Notes |
|---|---|---|---|---|
| #0 | 2026-10-25 | Choix trajectoire (revue ARCos) | ✅ GO | A puis B (B en 2 temps, contrat/propriété hors périmètre immédiat), pas de C ; Lot 0 autorisé avant Gate #1 ; montants Double hors plan ; observabilité à évaluer |
| #2 (Phase 0) | 2026-10-25 | Validation code hotfix JWT | 🔁 AJUSTER | code-review : 3 bloquants (`@Blocking` inopérant sur `@PreMatching`, pas de refresh sur `kid` inconnu, pas de backoff/single-flight). Décision 👤 : bascule immédiate `quarkus-oidc` (T0.6, Lot 6 avancé), repli `smallrye-jwt` si inadapté. Cache 1 h / 30 s validé si mécanisme maison reste nécessaire. security-review obligatoire avant re-présentation |
| #1 | 2026-10-25 | Validation plan 004 + ADR-005 | ✅ GO | ADR-005 → `Acceptée` (principe), choix final par essai. Commit par DEVon uniquement après GO explicite Gate #2 (commit `docs` séparé : plan, ADR, index). Aucun push sans accord. Déploiement QUA avant PROD + test de connexion réel après cold start |
| — | 2026-10-25 | Compléments 👤 Phase 0 | ℹ️ Décision | Fichiers JWT maison laissés jusqu'à Phase 4 ; collection `jwks_auth_signing_keys` + refresh `/_info` retirés de l'usage (drop/suppression par 👤) ; périmètre tests T0.5 (a)-(d) précisé |
| #2 (T0.6) | 2026-10-25 | Validation code bascule `quarkus-oidc` | 🔁 AJUSTER (léger) | security-review : 0 vulnérabilité ≥ 8/10. code-review : 10 constats. Décisions 👤 : 401 conservé, IHM adaptée (T0.7, non démarrée) ; discovery OIDC conservée + timeouts si compatibles, cold start mesuré en QUA ; 401 sur `@PermitAll` accepté ; suppressions Phase 4 par le 👤 (inventaire `PHASE_4_SUPPRESSIONS_A_FAIRE.md`). À corriger : email requis + `email_verified` dans `JwtRolesAugmentor` (+ test), `encodeJWT` sans padding, `@Deprecated` sur `IParametrageAppProvider.refreshJwksSigningKeys`. Aucun commit |
| #2 (T0.6) | 2026-10-25 | Validation code après ajustement | ✅ GO | Build SUCCESS (communs : 4 erreurs Mongo sans Docker, préexistantes ; autres modules verts) ; `TestJwtRolesAugmentor` 6/6 ; `connection-timeout=3S`, `connection-delay=4S`, discovery conservée. 2 commits DEVon (code + docs), sans push ni PR. T0.7 sortie vers le plan IHM 008. Suite : QALvin T0.5 (test e2e, cas `JsonValue` pour `email_verified`) |
