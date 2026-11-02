# Plan d'Action 005 — `/_info` public, lisible même avec un jeton invalide

**Date création :** 2026-10-29
**Statut :** 🔵 En cours — Gate #1 GO, Phase 1 (DEVon) lancée le 2026-11-02 après le merge de la Phase 1 du plan 004 (#224, #225, #226) et le rebase sur master (37fdbd7)
**Porteur :** ⚫ MAINa
**Branche :** `feat/info-endpoint-public` (depuis `origin/master` 655fcfb, sans upstream ; push avec `git push -u origin feat/info-endpoint-public` après accord 👤)
**ADR liés :** [ADR-008 — Endpoint `/_info` public via tenant OIDC désactivé](../../docs/adr/008-endpoint-info-public-tenant-oidc-desactive.md) (`Proposée`) · ADR-005 (authentification `quarkus-oidc`, renvoi à ajouter)

---

## Objectif global

Le endpoint `/_info` des 4 services (`/comptes/v2/_info`, `/budgets/v2/_info`, `/parametres/v2/_info`,
`/utilisateurs/v2/_info`) doit répondre **200** et être lisible directement :
- sans jeton ;
- **avec un jeton invalide, expiré ou `Authorization: Bearer null`**.

Aujourd'hui, l'authentification proactive de `quarkus-oidc` (ADR-005) valide tout jeton présent avant l'autorisation.
Un jeton invalide donne donc 401, même sur `@PermitAll` (comportement accepté au Gate #2 de la Phase 0 du plan 004,
désormais à changer). `/_info` n'expose que `nom`, `version` et une description fixe : rien de sensible.

**Solution retenue (Gate #0, 2026-10-29) : option C.** Un tenant OIDC nommé `info-public`, **désactivé**
(`tenant-enabled=false`), est lié par `tenant-paths` au seul chemin `<BASE>/_info` de chaque service. La requête est
traitée en anonyme et `@PermitAll` s'applique. Les endpoints protégés restent sur le tenant par défaut.
Comportement vérifié par ARCos dans les sources Quarkus 3.40.1 : `OidcAuthenticationMechanism`, `StaticTenantResolver`,
`TenantContextFactory`, `HttpSecurityRecorder`.

### Décisions 👤 (Gate #0)
| # | Sujet | Décision |
|---|---|---|
| 1 | Option | **C** (tenant `info-public` désactivé par chemin). A (`proactive=false` + refonte du filtre) écartée |
| 2 | GET catégories `@PermitAll` de `parametrages` | **Restent en 401** avec un jeton invalide (couvert par le plan IHM 008) |
| 3 | Jeton **valide** ignoré sur `/_info` (identité anonyme) ; version publique | **Accepté** |
| 4 | Log WARN « Aucune identité authentifiée. Accès anonyme. » (`AbstractAPISecurityFilter`) | **Passer en DEBUG** (touche `communs`) |
| 5 | Ordre de merge | **Phase 1 du plan 004 d'abord**, puis rebase de cette branche |
| 6 | Numérotation | Plan **005**, ADR **008** (006/007 réservés par la Phase 1 du plan 004) |

### Précautions
- Le nom du tenant ne doit égaler **aucun segment de chemin** (résolution statique par segment) : `info-public`.
- Les propriétés sont **aussi** ajoutées dans les `src/test/resources/application.properties`.
- Tests qui verrouillent le comportement « tenant désactivé → anonyme ».
- À revérifier à chaque montée de version de `quarkus-oidc` (note dans `ARCHITECTURE.md`).

---

## Phase 1 — Implémentation (DEVon)

### Critères de réussite
- Les 4 `/_info` répondent 200 sans jeton, avec un jeton invalide ou expiré et avec `Bearer null`.
- Les endpoints protégés et les GET catégories `@PermitAll` ne changent pas de comportement.
- `mvn clean install` vert ; aucun changement de contrat d'API.

### Tâches
#### T1.1 — Tenant `info-public` dans la config main
- **Fichier(s) :** `{comptes,operations,parametrages,utilisateurs}/src/main/resources/{dev,prod}/application.properties` (8), **juste après** `quarkus.oidc.connection-delay=4S`
- **Implémenter :** commentaire (renvoi ADR-008) + `quarkus.oidc.info-public.tenant-enabled=false` + `quarkus.oidc.info-public.tenant-paths=<BASE>/_info`, avec `<BASE>` = `/comptes/v2`, `/budgets/v2`, `/parametres/v2`, `/utilisateurs/v2` (vérifier dans les `*APIEnum`).
- **Acceptation :** 8 fichiers ; aucune autre ligne OIDC modifiée.

#### T1.2 — Tenant `info-public` dans la config de test
- **Fichier(s) :** les 4 `src/test/resources/application.properties`, après `quarkus.oidc.token.principal-claim=email` (pas en fin de fichier)
- **Acceptation :** mêmes 2 propriétés.

#### T1.3 — `@PermitAll` explicite
- **Fichier(s) :** `RootAPIResource.java` de `operations`, `parametrages`, `utilisateurs` (`comptes` l'a déjà)
- **Acceptation :** compilation + tests existants verts.

#### T1.4 — Log « Accès anonyme » en DEBUG
- **Fichier(s) :** `communs/.../api/security/AbstractAPISecurityFilter.java` (log WARN → DEBUG)
- **Acceptation :** une seule ligne de log modifiée ; aucune autre logique de sécurité touchée.

---

## Phase 2 — Tests (QALvin)

### Critères de réussite
- Comportement verrouillé par des tests automatisés dans les 4 modules ; preuve de détection de régression.

### Tâches
#### T2.1 — `InfoEndpointPublicTest` (×4, parallélisable)
- `/_info` → **200** + corps JSON (`nom`, `version`) : sans en-tête, `Bearer null`, `Bearer a.b.c`, jeton sans préfixe, expiré, signé par une autre clé, non signé, `alg=none`, `email_verified=false`, issuer invalide, **jeton valide**.
- Variantes `/_info/` et `/_info;x` : documenter le résultat (401 attendu, échec fermé).

#### T2.2 — Non-régression des endpoints protégés
- Un endpoint `@RolesAllowed` par module : **401** sans jeton, **401** avec mauvais jeton, **200** avec jeton valide (preuve que le tenant par défaut reste actif).
- `parametrages` GET catégories : **401** avec jeton invalide, **200** sans jeton ou avec jeton valide.

#### T2.3 — Adapter `OidcSecurityPermitAllTest`
- `INFO` → 200 avec jeton invalide ; `CATEGORIES` reste 401 ; `permitAll_emailVerifiedFalse_401` cible `CATEGORIES` ; Javadoc corrigée (`/parametres/v2`, décision mise à jour).

#### T2.4 — Preuve de régression
- Commenter temporairement `tenant-enabled=false` → les tests « jeton invalide → 200 » passent au rouge ; rétablir par édition, `git diff` vide.
- **Acceptation globale :** `mvn clean install` vert, couverture ≥ 80 % des lignes touchées.

---

## Phase 3 — Documentation (DOCly)

- `docs/ARCHITECTURE.md` : tableau des codes HTTP (`/_info` avec jeton invalide → 200), tableau de config (propriétés `info-public`), note « à revérifier à chaque montée de `quarkus-oidc` », index ADR (008).
- ADR-008 → `Acceptée` (au GO du Gate #1) ; renvoi depuis ADR-005 (note de mise en œuvre + points ouverts).
- Wiki serverless (sécurité) ; wiki IHM `Historique-de-l'Architecture.md`. Note T1.1 du plan IHM 008 déjà corrigée (worktree IHM, non commitée).

---

## Dépendances

```
Gate #1 → Phase 1 (DEVon) → Gate #2 → Phase 2 (QALvin) → Gate #3 → Phase 3 (DOCly) → Gate #4
Merge : Phase 1 du plan 004 d'abord, puis rebase de feat/info-endpoint-public
```

**Conflits avec `feat/ap004-phase1-fiabilite-mongo` :** blocs de config éloignés (Mongo ~10-17 vs OIDC ~39-52) ;
`operations` : seul `RootAPIResource` ; `ARCHITECTURE.md` : index ADR (006/007 vs 008), conflit trivial au rebase.

## Critères de succès globaux
1. `/_info` en 200 sur les 4 services, quel que soit le jeton.
2. Endpoints protégés : 401 sans jeton ou avec mauvais jeton, 200 avec bon jeton (tests).
3. GET catégories `parametrages` inchangés (401 avec jeton invalide).
4. Aucun changement de contrat d'API ; build natif CI vert.
5. Doc et ADR-008 à jour.

## Plan d'exécution
Phase 1 au GO du Gate #1. Tests et doc ensuite. Merge après celui de la Phase 1 du plan 004.

---

## 🚦 Journal des gates

| Gate | Date | Objet | Décision | Notes |
|---|---|---|---|---|
| #0 | 2026-10-29 | Choix de solution (ARCos) | ✅ GO | Option C (tenant `info-public` désactivé). GET catégories gardent 401 ; jeton valide ignoré sur `/_info` accepté ; log « Accès anonyme » en DEBUG ; Phase 1 plan 004 mergée d'abord ; plan 005 / ADR-008 |
| #1 | 2026-10-29 | Validation plan + ADR-008 | ✅ GO | ADR-008 → `Acceptée`. Exécution séquentielle après la Phase 1 du plan 004 (décision 👤). Branche rebasée sur master 37fdbd7 le 2026-11-02 (conflit trivial sur l'index des plans résolu) |
| #2 | 2026-11-02 | Validation code Phase 1 | 🔧 AJUSTER | Décisions 👤 : (1) `TenantResolver` dans `communs` limitant `info-public` à `/_info` (renvoie `Default` ailleurs, `null` insuffisant) ; (2) logs anonyme / clé API en DEBUG pour `/_info` seulement, WARN ailleurs ; (3) QALvin T2.1 ×4 (variantes de chemin).
