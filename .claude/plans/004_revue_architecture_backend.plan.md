# Plan d'Action 004 — Revue architecture backend : sécurisation, fiabilisation, modernisation Quarkus/Mongo

**Date création :** 2026-10-25
**Statut :** 🔵 En cours — Phase 0 ✅ clôturée (Gate #4 GO 2026-10-26, PR #220 non mergée, QUA/PROD à venir) ; Phase 1 en cours (Gate #1 de phase GO, branche `feat/ap004-phase1-fiabilite-mongo`)
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

## Phase 0 — Hotfix sécurité JWT (Lot 0) — ✅ clôturée (Gate #4 GO 2026-10-26)

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

## Phase 1 — Fiabilité fonctionnelle + MongoDB (Lots 1 et 2, option A) — 🔵 en cours (Gate #1 de phase GO 2026-10-26)

### Contexte
Cadrage détaillé ARCos (2026-10-26) sur le code de `master` (e08db1f). Options retenues au Gate #1 de phase :
**A3** (réservation atomique des migrations par version), **B2** (virement séquentiel avec compensation),
**C3** (libellés : `$group` côté serveur sur le libellé brut, puis normalisation Java en sommant les compteurs),
**timeouts Mongo dans le bloc Mongo existant** (placement D1). Deux défauts nouveaux sont intégrés :
**F4 sur comptes actifs neufs** (récursion jusqu'au timeout Lambda) et **F6b** (une version `ECHEC` ne peut jamais passer `SUCCES` : clé dupliquée, migration rejouée à chaque cold start).

- **Branche :** `feat/ap004-phase1-fiabilite-mongo`, créée depuis `origin/master`, sans le Lot 0. Push uniquement avec accord 👤 (`git push -u origin feat/ap004-phase1-fiabilite-mongo`, car l'upstream par défaut est `origin/master`).
- **Ce plan détaillé** vit sur `fix/securite-jwt-lot0` (PR #220) ; il sera repris dans la branche de Phase 1 après merge de #220 et rebase.
- **Ordre de merge (décision 👤) :** #220 d'abord → rebase de la Phase 1 → CI → QUA Phase 1. **Pas de QUA Phase 1 avant le merge du Lot 0.**

### Règles anti-conflit avec la PR #220 (impératives)
- Ne pas toucher : `communs/.../api/security/*`, `utils/security/*`, `*JwtSecurityFilter`, `RequestJWTHeaderFactory`, `AbstractAPIResource`, `RootAPIResource`, `JwsSigningKeysDatabaseAdaptor`, `IJwtSigningKey*`, `JWTAuthToken`, ni les lignes JWT de `BudgetService.java` (17-18, 53, 77-78, 616-622) et `UtilisateursService.java` (6-7, 25, 36-37, 104-110).
- `*/src/main/resources/{dev,prod}/application.properties` : insertion **uniquement dans le bloc Mongo** (après `quarkus.mongodb.database` / `metrics`), **jamais en fin de fichier**.
- Interdit de modifier : `*/src/test/resources/application.properties`, le `pom.xml` racine, les `*ResourceTest` existants. Configuration de test via `QuarkusTestProfile` ; dépendance éventuelle dans le pom du module.
- Docs (`docs/ARCHITECTURE.md`, index des plans) seulement après le merge de #220. Les nouveaux fichiers ADR (006/007) peuvent être créés sur la branche de Phase 1.

### Décisions 👤 (Gate #1 de phase)
| # | Sujet | Décision |
|---|---|---|
| Q3 | F3 compte inactif sans budget ce mois-là | **200 + dernier budget existant** (au lieu de 404). ⚠️ Changement de comportement visible par l'IHM, à tracer dans un plan IHM (voir T1.5) |
| Q4 | Gardes du virement | **406** si le compte de destination est clos, **400** si source = destination. ⚠️ Impact IHM à tracer dans un plan IHM (voir T1.5) |
| Q5 | Format des `_id` de budgets en PROD | Le 👤 lance lui-même la requête en lecture seule (commande ci-dessous). **T1.1b-ID, T1.1c et F3 bloqués tant que le résultat n'est pas connu** |
| Q6 | Utilisateur inconnu sur lastaccess | **404** (au lieu de 500 sur NPE) |
| Q7 | Tier Atlas / DocumentDB | **Inconnu.** Pool 10 par défaut (valeur ARCos), sans autre hypothèse. Mention « DocumentDB » d'`ARCHITECTURE.md` corrigée seulement après confirmation 👤 |
| Q8 | Timeouts REST client | **Phase 2** (hors Phase 1) |
| Q9 | F4, écart de plusieurs mois | Remplissage des mois intermédiaires conservé, **limite 24 mois** |
| Q10 | Index `utilisateurs.login` | **Non unique** dans V002 (index unique plus tard, après contrôle des doublons) |

#### Commande Q5, à lancer par le 👤 (mongosh, lecture seule, base PROD)
```javascript
// 0. Contrôle préalable du type stocké pour "mois" (attendu : nom d'enum Month, ex. "JANUARY")
db.budgets.findOne({}, { _id: 1, idCompteBancaire: 1, annee: 1, mois: 1 })

// 1. Nombre total de budgets
db.budgets.countDocuments({})

// 2. Budgets dont l'_id ne respecte pas <idCompteBancaire>_<AAAA>_<MM> (MM sur 2 chiffres)
db.budgets.aggregate([
  { $addFields: { _m: { $add: [ { $indexOfArray: [ ["JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE","JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER"], "$mois" ] }, 1 ] } } },
  { $addFields: { _idAttendu: { $concat: [ "$idCompteBancaire", "_", { $toString: "$annee" }, "_",
      { $cond: [ { $lt: ["$_m", 10] }, { $concat: ["0", { $toString: "$_m" }] }, { $toString: "$_m" } ] } ] } } },
  { $match: { $expr: { $ne: ["$_id", "$_idAttendu"] } } },
  { $facet: {
      total: [ { $count: "nbNonConformes" } ],
      exemples: [ { $project: { _id: 1, idCompteBancaire: 1, annee: 1, mois: 1, _idAttendu: 1 } }, { $limit: 20 } ]
  } }
])
```
- Si l'étape 0 montre un `mois` numérique, ne pas interpréter l'étape 2 : transmettre le résultat de l'étape 0 à MAINa, qui adaptera la requête.
- Résultat attendu pour débloquer : `nbNonConformes` absent ou 0. Sinon, exemples à transmettre : ARCos ajustera (repli sur la requête par champs pour les documents historiques, ou migration de normalisation).

### Critères de réussite
- F1-F4 et F6/F6b corrigés et couverts par des tests de non-régression.
- Requêtes `budgets` indexées (V002), sans exécution concurrente des migrations.
- Client Mongo avec timeouts inférieurs au timeout Lambda de 15 s.
- Contrat API inchangé (aucun `*APIEnum`, aucun DTO). Seuls changements de code HTTP : F3 (404 → 200), virement (406 / 400), utilisateur inconnu (500 → 404), compte actif neuf (timeout → 200 avec budget vierge).
- CI `build-on-all` verte sur la branche de phase (preuve des tests d'intégration Dev Services : pas de Docker sur les postes agents).

### Tâches

**Piste A — operations (séquentielle : `BudgetService` et `OperationDatabaseAdaptor` partagés)**

#### T1.1b — Dépôt opérations : tri, lecture par `_id`, dernier budget (DEVon)
- **Fichier(s) :** `operations/.../spi/OperationDatabaseAdaptor.java` (`:59-71`, `:95`, `:203`), `operations/.../business/ports/IOperationsRepository.java`
- **Implémenter :**
  - **T1.1b-tri (non bloqué) :** `find(query, Sort.by("_id"), params...)` en `:95` et `:203` (le tri était passé comme paramètre de requête et ignoré).
  - **T1.1b-ID (bloqué par Q5) :** `chargeBudgetMensuel` lu par `_id` ; nouvelle méthode `chargeDernierBudgetAvant(idCompte, mois, annee)` : 1 requête (filtre compte + borne `_id` ≤ M, tri `_id` descendant, premier résultat).
- **Acceptation :** tri appliqué (test d'intégration) ; « dernier budget ≤ M » en 1 seule requête.

#### T1.1d — Virement F2, option B2 (DEVon)
- **Fichier(s) :** `operations/.../business/BudgetService.java:502-568` + méthode de retrait / compensation dans le dépôt
- **Implémenter :** propagation de l'erreur (F2a) ; écritures **séquentielles** source puis cible, avec compensation de la source si l'écriture cible échoue (F2b) ; un seul `getCompteById(dest)` (F2c) ; plus de modification concurrente de `ligneOperation` (F2d) ; gardes **406** si destination close et **400** si source = destination (Q4) ; NPE évitée si le compte destination est `null`.
- **Acceptation :** échec de l'écriture cible → source restaurée en base + réponse non-2xx ; échec de compensation loggé en ERROR avec les 2 identifiants ; 1 appel REST destination.
- **Note :** réalisable avant T1.1c (indépendant de Q5), mais même fichier que T1.1c : exécution séquentielle.

#### T1.1c — F3 + F4 (DEVon) — ⏸️ bloqué par Q5 (via T1.1b-ID)
- **Fichier(s) :** `operations/.../business/BudgetService.java` (`:168-218`, `:232-252`, `:281-317`)
- **Implémenter :**
  - **F3 :** compte inactif, M absent → **200 + dernier budget ≤ M** (`actif=false`) en 1 appel dépôt ; aucun budget → 404.
  - **F4 :** compte actif → requête préalable « dernier budget ≤ M-1 » ; aucun budget → budget vierge, sans récursion ; écart de k mois → remplissage des mois intermédiaires conservé, **k ≤ 24** (au-delà : budget vierge + WARN) ; premier mois : solde précédent à 0, sans échec (`recalculSoldeAFinMoisPrecedent`).
- **Acceptation :** compte actif sans budget → nombre d'appels dépôt ≤ 3 ; tests des 4 cas F3/F4.

#### T1.2d — Libellés P3, option C3 (DEVon)
- **Fichier(s) :** `OperationDatabaseAdaptor.java:235-263`, `OperationsService.java:381-435`
- **Implémenter :** pipeline `$match idCompteBancaire` → `$unwind listeOperations` → `$group {_id:{libellé, catégorie, sous-catégorie}, n:{$sum:1}}` ; normalisation Java inchangée, en additionnant les compteurs (`nb += n`).
- **Acceptation :** **test de parité** : sortie identique à l'ancien algorithme sur un jeu de données (ex. `operations/src/test/resources/test/alignement-libelles.json`).

**Piste B — communs, migrations (parallèle à A)**

#### T1.2a — Verrou des migrations, option A3, + F6b (DEVon)
- **Fichier(s) :** `communs/.../migrations/{MongoMigrationRunner,MigrationRepository,MigrationRecord}.java`
- **Implémenter :** réservation `findOneAndUpdate({_id:V, $or:[{statut:ECHEC},{statut:EN_COURS, dateExecution < now-bail}]}, {$set:{statut:EN_COURS, owner, dateExecution}}, upsert)` ; E11000 = version déjà prise → version suivante ; finalisation `updateOne({_id:V, owner}, {$set:{statut}})` ; bail de 5 min (Clock injectée, ADR-004) ; nouvel état `EN_COURS` et champ `owner`.
- **Acceptation :** 2 runners concurrents → 1 seule exécution ; `ECHEC` → `SUCCES` possible ; `EN_COURS` périmé repris ; compatible natif.

#### T1.2b — Migration V002 : index (DEVon) — même PR que T1.2a (obligatoire)
- **Fichier(s) :** `communs/.../migrations/scripts/V002_AjoutIndexBudgetsUtilisateurs.java`
- **Implémenter :** `budgets {idCompteBancaire:1, annee:1, mois:1}` ; `utilisateurs {login:1}` **non unique** (Q10) ; index existants listés d'abord, création ignorée si la même clé existe déjà (quel que soit son nom).
- **Acceptation :** index présents après démarrage ; 2e exécution sans erreur ; index préexistant de même clé ignoré.

**Piste C — utilisateurs (parallèle)**

#### T1.1a — F1 lastAccess + utilisateur inconnu (DEVon)
- **Fichier(s) :** `utilisateurs/.../business/ports/IUtilisateursRepository.java`, `.../spi/UtilisateurDatabaseAdaptor.java`, `.../business/UtilisateursService.java` (corps de méthodes uniquement, hors lignes JWT)
- **Implémenter :** `Uni<Void> majDernierAcces(login, date)` avec `$set dernierAcces` ciblé, chaîné avant la réponse ; échec d'écriture → WARN, la réponse reste 200 ; utilisateur inconnu → **404** (Q6).
- **Acceptation :** `.subscribe()` absent de `utilisateurs/src/main` ; date persistée avant l'émission de la réponse (test) ; seul le champ `dernierAcces` écrit.

**Piste D — configuration (parallèle)**

#### T1.2c — Timeouts et pool Mongo (DEVon)
- **Fichier(s) :** les 8 `{comptes,operations,parametrages,utilisateurs}/src/main/resources/{dev,prod}/application.properties`, **dans le bloc Mongo**
- **Valeurs :** `server-selection-timeout=5S`, `connect-timeout=3S`, `read-timeout=8S`, `wait-queue-timeout=2S`, `max-pool-size=10` (Q7 : tier Atlas inconnu), `min-pool-size=0`, `max-connection-idle-time=60S`.
- **Acceptation :** valeurs lues au runtime (test de configuration via `QuarkusTestProfile` ou lecture `ConfigProvider`) ; aucune modification des test resources.

**Tests et documentation**

#### T1.3 — Tests Phase 1 (QALvin)
- **Implémenter :** nouvelles classes de test uniquement ; tests unitaires F1-F4 et F2 (échec partiel + compensation, gardes 406/400) ; tests d'intégration `@QuarkusTest` Dev Services (`OperationDatabaseAdaptorIT` : tri, `_id`, dernier budget, parité C3 ; concurrence du runner ; persistance lastAccess) ; correction de `BudgetServiceTest.java:199-217` (simulation `nullItem` erronée qui masquait F3) et de `UtilisateursServiceTest`.
- **Acceptation :** couverture ≥ 80 % des nouveaux chemins ; **run CI `build-on-all` vert** sur la branche de phase.

#### T1.4 — Documentation Phase 1 (DOCly)
- **ADR-006** « Réservation atomique par version des migrations MongoDB » (complète ADR-002) et **ADR-007** « Cohérence du virement : écriture séquentielle compensée » : créés en `Proposée` sur la branche de Phase 1 dès le lancement ; `Acceptée` au Gate #2 de phase si l'implémentation est conforme.
- Après le merge de #220 : `docs/ARCHITECTURE.md` (§ Migrations : `EN_COURS`, bail ; § Mongo : timeouts ; correction de la collection `operations` inexistante, les opérations étant stockées dans `budgets.listeOperations`). Mention DocumentDB corrigée **seulement après confirmation 👤** (Q7).

#### T1.5 — Traçabilité IHM (MAINa)
- Plan IHM dédié à créer (comme le plan 008) : F3 (404 → 200 avec un budget d'un mois antérieur, `actif=false`), virement (406 destination close, 400 vers soi-même), utilisateur inconnu (404). À synchroniser avec le plan IHM 008 (gestion du 401).

### Ordre et parallélisation
```
Piste A : T1.1b-tri → T1.1d → [Q5] → T1.1b-ID → T1.1c → T1.2d
Piste B : T1.2a → T1.2b
Piste C : T1.1a
Piste D : T1.2c
           ↘ T1.3 (QALvin, au fil des livraisons ; preuve CI) → Gate #2/#3 de phase → [merge #220 + rebase] → QUA → T1.4 docs
```

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
| #3 (T0.5) | 2026-10-26 | Validation tests sécurité | 🔁 AJUSTER | 84 tests ajoutés, couverture sécurité ≥ 83 % en branches, régression détectée. Défaut 1 (jeton complet dans les logs `OidcProvider` WARN) → catégorie passée en ERROR (fix bc15842). Défaut 3 → augmentor durci (`JsonValue`/`JsonString`). **Défaut 2 conservé (décision 👤)** : JWKS injoignable → 500 au lieu de 401, limite connue (voir ci-dessous). Les 4 tests `@Disabled` BUG sont réactivés par QALvin |
| #3 (T0.5) | 2026-10-26 | Validation tests après ajustement | ✅ GO | BUILD SUCCESS, 0 échec, 0 test « BUG AP004 » ignoré (communs : 4 erreurs Mongo sans Docker, préexistantes). `JwtRolesAugmentor` : 100 % lignes / 97 % branches. Tests de fuite rouges sans le correctif. Commits bc15842, b8ff152, 783fc3a. Push et PR faits par le 👤 ; pas de merge, pas de PROD. Suite : DOCly, phase doc limitée à la Phase 0 |
| #4 (Phase 0) | 2026-10-26 | Validation doc + clôture Phase 0 | ✅ GO | `ARCHITECTURE.md` (sécurité, versions, ADR 003-005), ADR-005 `Acceptée` (896e0bd), rapport de tests harmonisé (f5dc86e). Wikis modifiés, commités par le 👤. Restent hors gate : merge PR #220, QUA (cold start, natif, `kid`), plan IHM 008 avant PROD. Phase 1 lancée en parallèle de la QUA, sur une branche distincte depuis `master`, sans toucher à la sécurité/OIDC |

| #1 (Phase 1) | 2026-10-26 | Cadrage Phase 1 (ARCos) + options | ✅ GO | Options A3 / B2 / C3 / timeouts dans le bloc Mongo. Q1 plan sur `fix/securite-jwt-lot0` ; Q2 #220 mergée d'abord, pas de QUA Phase 1 avant ; Q3 F3 → 200 ; Q4 406/400 ; Q5 requête PROD lancée par le 👤 (T1.1b-ID et T1.1c bloqués) ; Q6 404 ; Q7 tier Atlas inconnu (pool 10) ; Q8 REST en Phase 2 ; Q9 limite 24 mois ; Q10 index login non unique. ADR-006 et ADR-007 en `Proposée` |

### ⚠️ Limites connues (Phase 0)

- **JWKS / discovery Google injoignable → HTTP 500** (pas 401). Le refus est appliqué et rien ne bloque (~2 s), mais l'IHM ne se déconnecte pas sur ce code. Décision 👤 Gate #3 : conservé, sans `ExceptionMapper`. À observer en QUA (logs, fréquence) et à réévaluer en Phase 4.
- **Masquage du jeton dans les logs par configuration uniquement** (`quarkus.log.category."io.quarkus.oidc.runtime.OidcProvider".level=ERROR`, 12 fichiers). Revérifier à chaque montée de version de quarkus-oidc (analyse bytecode heuristique en 3.40.1.1). Effet de bord : les WARN légitimes d'`OidcProvider` (jeton expiré…) ne sont plus visibles. Résiduel : `OidcUtils` WARN `Failed to decrypt a token: %s` (jetons JWE uniquement, non utilisés par Google id_token).
- **`kid` inconnu** : non testable avec `public-key` en test ; à vérifier en QUA avec le JWKS réel.
