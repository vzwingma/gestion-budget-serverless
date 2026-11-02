# ADR 008 — Endpoint `/_info` public : tenant OIDC désactivé par chemin

---

**Date :** 2026-10-29
**Statut :** Acceptée (Gate #0 GO 👤 le 2026-10-29, option C ; Gate #1 GO le 2026-10-29)
**Décideurs :** 🟠 ARCos + 👤 Développeur humain

---

## Contexte

`quarkus-oidc` ([ADR-005](./005-authentification-jwt-extension-quarkus.md)) tourne en mode `service`, avec l'authentification proactive par défaut. Tout jeton présent est validé avant l'autorisation. Un jeton invalide, expiré ou `Bearer null` renvoie donc 401, y compris sur `/_info` (`@PermitAll`). Ce comportement avait été accepté au Gate #2 de la Phase 0 du plan 004 ; il est désormais à changer.

- L'IHM envoie toujours `Authorization: Bearer <token|null>` (`ClientHTTP.service.ts:94`). Avec le plan IHM 008, un 401 déclenche `logOut()`.
- `/_info` (`AbstractAPIResource`, `Info`) n'expose que `nom`, `version` et une description fixe : ni config, ni secret, ni accès Mongo.
- Vérifié dans la doc Quarkus *Proactive authentication* : « requests with invalid credentials are rejected, even if the target page is public ».

---

## Décision

**Nous avons décidé de** rendre `/_info` insensible au jeton. Un tenant OIDC nommé `info-public` est déclaré **désactivé** (`quarkus.oidc.info-public.tenant-enabled=false`) et lié par `quarkus.oidc.info-public.tenant-paths=<BASE>/_info` au seul chemin `/_info` de chaque service (`/comptes/v2`, `/budgets/v2`, `/parametres/v2`, `/utilisateurs/v2`).

Comme le tenant est désactivé, `OidcAuthenticationMechanism` renvoie une identité nulle : la requête est anonyme et `@PermitAll` s'applique. Les endpoints protégés restent sur le tenant par défaut.

### Amendement du 2026-11-02 (Gate #2 du plan 005, décision 👤)

**Problème constaté en revue.** Quarkus OIDC résout aussi un tenant quand le **dernier segment du chemin** est égal au nom du tenant (résolution statique). Toute URL qui se termine par `/info-public` est donc envoyée vers le tenant désactivé, et le jeton n'y est plus validé. Exemples :
- `GET /parametres/v2/categories/info-public` avec un jeton invalide renvoie 200 ou 404 au lieu de 401 ;
- sur un endpoint `@RolesAllowed` comme `/comptes/v2/info-public`, un jeton valide est ignoré : l'appel devient anonyme et reçoit 401.

Aucune élévation de privilège n'est possible. Le nom du tenant ne règle pas le problème, puisqu'un `{id}` dans le chemin est choisi par l'appelant.

**Première piste invalide : `null`.** Renvoyer `null` hors `/_info` ne suffit pas. Vérifié dans les sources quarkus-oidc 3.40.1.1 et prouvé par des tests : `StaticTenantResolver` consulte d'abord, en groupe 1, le `TenantResolver` custom puis `PathMatchingTenantResolver` (`tenant-paths`) ; puis, en groupe 2, la résolution par en-tête puis `DefaultStaticTenantResolver`, qui découpe le chemin en segments et retient le premier segment égal au nom d'un tenant. Avec `null`, la chaîne continue jusqu'à `DefaultStaticTenantResolver` : `/parametres/v2/categories/info-public` retombe sur le tenant `info-public` et la faille persiste.

**Décision complémentaire (alternative 1 bis).** Le tenant `info-public` désactivé est conservé. Un `TenantResolver` (`InfoPublicTenantResolver`) est ajouté dans `communs`. Il renvoie `info-public` si `context.normalizedPath()` se termine par `/_info`, et **`Default`** dans **tous** les autres cas. `Default` est la valeur de `OidcUtils.DEFAULT_TENANT_ID`, qui désigne le tenant par défaut. Un identifiant non nul arrête la chaîne : `tenant-paths`, la résolution par segment et la résolution par issuer ne sont plus consultés. Le resolver est appliqué en premier.

`tenant-enabled=false` et `tenant-paths` restent configurés. Pour `/_info`, ils donnent le même résultat que le resolver (défense en profondeur et documentation).

Les variantes `/_info/` et `/_info/abc` sont résolues vers `Default`. L'échec est fermé : sans jeton, `@PermitAll` reste accessible ; avec un jeton invalide, la réponse est 401.

**Risque.** Un second bean `TenantResolver` fait échouer le démarrage avec `IllegalStateException`. L'échec est visible au démarrage, pas silencieux.

> À revérifier à chaque montée de quarkus-oidc (ordre des resolvers).

**Logs.**
- « Aucune identité authentifiée. Accès anonyme. » passe en DEBUG **seulement** pour `/_info`, et reste en WARN ailleurs.
- « Clé API non trouvée » passe aussi en DEBUG pour `/_info`.

**Tests de non-régression.**
- `/parametres/v2/categories/info-public` avec un jeton invalide doit renvoyer 401 ;
- `/comptes/v2/info-public` avec un jeton valide doit être authentifié (comportement du tenant par défaut).

Preuve : avec `null`, ces deux tests échouent ; avec `Default`, ils passent.

---

## Alternatives Considérées

### Option 1 : Tenant désactivé par chemin (insuffisante seule)

> Amendement 2026-11-02 : l'option 1 seule est **insuffisante**. La résolution statique par dernier segment de chemin envoie aussi vers le tenant désactivé toute URL se terminant par `/info-public` (voir amendement, § Décision). Remplacée par l'option 1 bis.

- **Avantages** : configuration seule ; effet limité aux chemins listés ; endpoints protégés inchangés ; échec fermé (une erreur de config ne produit qu'une identité anonyme) ; coût négligeable au cold start.
- **Inconvénients** : un jeton valide est aussi ignoré sur `/_info` ; le comportement « tenant désactivé → identité nulle » vient du code (sources 3.40.1) et non d'une phrase explicite de la doc. D'où des tests de verrouillage et une revérification à chaque montée de version.

### Option 1 bis : Tenant désactivé + `TenantResolver` limité à `/_info` ✅ Retenue (amendement 2026-11-02)

- **Description** : le tenant `info-public` désactivé est conservé ; `InfoPublicTenantResolver` (`communs`) renvoie `info-public` si `normalizedPath()` se termine par `/_info`, et `Default` (`OidcUtils.DEFAULT_TENANT_ID`, tenant par défaut) dans tous les autres cas. Un identifiant non nul arrête la chaîne : `tenant-paths`, la résolution par segment et la résolution par issuer ne sont plus consultés. Renvoyer `null` à la place est insuffisant (la chaîne continue jusqu'à `DefaultStaticTenantResolver`).
- **Avantages** : corrige le défaut de l'option 1 (URL finissant par `/info-public`) ; la sécurité ne dépend plus du comportement implicite de la résolution statique ; échec fermé conservé ; endpoints protégés inchangés.
- **Inconvénients** : un peu de code Java dans `communs` (bean partagé par les 4 services) ; un second bean `TenantResolver` fait échouer le démarrage (`IllegalStateException`, visible) ; un jeton valide reste ignoré sur `/_info` ; dépendance au comportement « tenant désactivé → identité nulle » et à l'ordre des resolvers de quarkus-oidc (à revérifier à chaque montée de version).

### Option 2 : `quarkus.http.auth.proactive=false` + refonte du filtre `@PreMatching`

- **Raison du rejet** : `SecurityIdentity` devient bloquant sur le thread I/O ; l'identité différée renvoie de nouveau 401 dès qu'un composant s'y abonne ; refonte de `communs` et des 4 filtres ; changement global sur tous les `@PermitAll`.

### Option 3 : Permission HTTP `policy=permit` sur `/_info`

- **Raison du rejet** : en mode proactif, l'authentification précède la politique, donc 401 malgré tout.

### Option 4 : Retrait de l'en-tête `Authorization` (route Vert.x) ou mécanisme d'authentification vide par chemin

- **Raison du rejet** : contournement fragile, ordre des handlers non documenté.

### Option 5 : Mapping API Gateway

- **Raison du rejet** : configuration console manuelle, hors SAM, non testable.

---

## Conséquences

### Positives
- `/_info` répond 200 quel que soit le jeton.
- Plus de déconnexion IHM depuis la page Infos.
- Aucun impact sur `@RolesAllowed` ni sur la propagation `RequestJWTHeaderFactory`.

### Négatives / Compromis
- Un jeton valide est ignoré sur `/_info` (accepté par le 👤).
- ~~Le nom du tenant ne doit égaler aucun segment de chemin~~ : contrainte caduque depuis l'amendement 2026-11-02 (un `{id}` choisi par l'appelant ne peut de toute façon pas être contrôlé par le nom du tenant) ; remplacée par le `TenantResolver`.
- Dépendance à un comportement interne de `quarkus-oidc`, dont l'ordre des resolvers : à revérifier à chaque montée de quarkus-oidc.
- Un peu de code Java dans `communs` : un bean `TenantResolver` partagé par les 4 services (amendement 2026-11-02). Un second bean `TenantResolver` ferait échouer le démarrage (`IllegalStateException`).

### Neutres
- Les GET catégories `@PermitAll` de `parametrages` gardent 401 avec un jeton invalide (décision 👤, couvert par le plan IHM 008).
- Le log « Accès anonyme » passe en DEBUG seulement pour `/_info` (WARN ailleurs) ; « Clé API non trouvée » passe aussi en DEBUG pour `/_info` (amendement 2026-11-02).
- La sécurité ne dépend plus d'un comportement implicite de la résolution statique de tenant.
- `docs/ARCHITECTURE.md` est mis à jour.
- Renvoi à ajouter depuis ADR-005.

---

## Mise en œuvre

- **Fichiers impactés** : 8 `application.properties` main, 4 de test, `RootAPIResource` ×3, `AbstractAPISecurityFilter` (log), resolver de `communs` (`communs/.../api/security/InfoPublicTenantResolver.java`).
- **Ajouts de l'amendement 2026-11-02** :
  - `InfoPublicTenantResolver` dans `communs/.../api/security/` : renvoie `info-public` si `normalizedPath()` se termine par `/_info`, sinon `Default` (commit ae34378) ;
  - logs « Accès anonyme » et « Clé API non trouvée » en DEBUG pour `/_info` seulement (commit c24792a) ;
  - tests : `TestInfoPublicTenantResolver` (14 cas) ; `InfoPublicTenantNonRegressionTest` dans `comptes` et dans `parametrages` (`/parametres/v2/categories/info-public` + jeton invalide → 401 ; `/comptes/v2/info-public` + jeton valide → authentifié) ; 3 tests de niveau de log dans `TestAbstractAPISecurityFilter`.
- **Note** : à revérifier à chaque montée de quarkus-oidc (ordre des resolvers).
- **Tâches de suivi** : plan 005 — T1.1 à T1.4 (DEVon), T2.x (QALvin), T3 (DOCly) ; amendement : DEVon (`TenantResolver`, logs), QALvin (tests de non-régression).
- **Date d'effet** : Phases 1 à 3 du plan 005.

---

## Références

- Plan d'Action associé : [`.claude/plans/005_info_endpoint_public.plan.md`](../../.claude/plans/005_info_endpoint_public.plan.md)
- [ADR-005](./005-authentification-jwt-extension-quarkus.md)
- Quarkus — [Proactive authentication](https://quarkus.io/guides/security-proactive-authentication)
- Quarkus — [OIDC multitenancy](https://quarkus.io/guides/security-openid-connect-multitenancy) (`tenant-paths`, `tenant-enabled`)
- Sources Quarkus 3.40.1 : `OidcAuthenticationMechanism`, `StaticTenantResolver`, `TenantContextFactory`, `HttpSecurityRecorder`
