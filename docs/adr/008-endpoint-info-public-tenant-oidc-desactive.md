# ADR 008 — Endpoint `/_info` public : tenant OIDC désactivé par chemin

---

**Date :** 2026-10-29
**Statut :** Proposée (Gate #0 GO 👤 le 2026-10-29, option C ; passage à `Acceptée` au Gate #1)
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

---

## Alternatives Considérées

### Option 1 : Tenant désactivé par chemin ✅ Retenue

- **Avantages** : configuration seule ; effet limité aux chemins listés ; endpoints protégés inchangés ; échec fermé (une erreur de config ne produit qu'une identité anonyme) ; coût négligeable au cold start.
- **Inconvénients** : un jeton valide est aussi ignoré sur `/_info` ; le comportement « tenant désactivé → identité nulle » vient du code (sources 3.40.1) et non d'une phrase explicite de la doc. D'où des tests de verrouillage et une revérification à chaque montée de version.

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
- Le nom du tenant ne doit égaler aucun segment de chemin : sinon tout le service devient anonyme, donc 401 partout, sans fuite.
- Dépendance à un comportement interne de `quarkus-oidc`.

### Neutres
- Les GET catégories `@PermitAll` de `parametrages` gardent 401 avec un jeton invalide (décision 👤, couvert par le plan IHM 008).
- Le log « Accès anonyme » passe en DEBUG.
- `docs/ARCHITECTURE.md` est mis à jour.
- Renvoi à ajouter depuis ADR-005.

---

## Mise en œuvre

- **Fichiers impactés** : 8 `application.properties` main, 4 de test, `RootAPIResource` ×3, `AbstractAPISecurityFilter` (log).
- **Tâches de suivi** : plan 005 — T1.1 à T1.4 (DEVon), T2.x (QALvin), T3 (DOCly).
- **Date d'effet** : Phases 1 à 3 du plan 005.

---

## Références

- Plan d'Action associé : [`.claude/plans/005_info_endpoint_public.plan.md`](../../.claude/plans/005_info_endpoint_public.plan.md)
- [ADR-005](./005-authentification-jwt-extension-quarkus.md)
- Quarkus — [Proactive authentication](https://quarkus.io/guides/security-proactive-authentication)
- Quarkus — [OIDC multitenancy](https://quarkus.io/guides/security-openid-connect-multitenancy) (`tenant-paths`, `tenant-enabled`)
- Sources Quarkus 3.40.1 : `OidcAuthenticationMechanism`, `StaticTenantResolver`, `TenantContextFactory`, `HttpSecurityRecorder`
