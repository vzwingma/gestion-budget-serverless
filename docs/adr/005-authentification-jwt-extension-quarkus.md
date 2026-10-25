# ADR 005 — Authentification JWT : extension Quarkus `quarkus-oidc` en remplacement de la validation maison

---

**Date :** 2026-10-25
**Statut :** Acceptée (principe — Gate #1 GO 2026-10-25 ; choix final `quarkus-oidc` / repli `smallrye-jwt` tranché par l'essai en Phase 0 du plan 004)
**Décideurs :** 🟠 ARCos + 👤 Développeur humain (Gate #0 : trajectoire A puis B, sans C)

---

## Contexte

La validation des `id_token` Google OIDC est aujourd'hui entièrement maison : `communs/.../utils/security/JWTUtils.java`, `JwtSecurityContext`, `IJwtSigningKeyService`. Avant le hotfix du Lot 0, cette mécanique présentait des défauts de sécurité graves :

- jetons non signés acceptés ;
- fail-open en l'absence de clés de signature ;
- `iss` vérifié par `contains` ;
- expiration (`exp`) décalée par un fuseau Europe/Berlin ;
- jetons écrits dans les logs.

**Architecture actuelle des clés JWKS :**

- Les clés JWKS Google sont rafraîchies uniquement par `parametrages`, sur appel de `/_info` (client REST vers `https://www.googleapis.com/oauth2/v3/certs`), puis stockées dans la collection Mongo `jwks_auth_signing_keys`. Les 4 microservices (`comptes`, `operations`, `parametrages`, `utilisateurs`) lisent cette collection.
- `IJwtSigningKeyService` est implémenté par les 4 services métier : la sécurité est dans le domaine.
- 7 classes sont dupliquées ×4, dont `JwtSecurityFilter`, `JwsSigningKeysDatabaseAdaptor`, `JwtReflectionConfig`.

**Limites du hotfix Lot 0** (fail-closed + cache JWKS avec TTL) : il corrige l'urgence mais conserve la mécanique maison. La revue de code relève encore :

- pas de refresh sur `kid` inconnu (rotation des clés Google non gérée) ;
- cache statique placé dans un port ;
- chargement bloquant dans un filtre `@PreMatching`.

**Contexte runtime :** Quarkus 3.40.1.1, Java 25, natif Mandrel 25, 4 Lambdas AWS. L'IHM React utilise `react-oidc-context` (Google) et envoie `Authorization: Bearer <id_token>` ainsi que `X-Api-Key` (`ApiKeyRequired` + Usage Plan configurés manuellement dans la console AWS).

Sujet de sécurité transverse aux 4 microservices et à `communs` : une décision de fond est nécessaire pour ne pas maintenir indéfiniment du code de sécurité maison.

---

## Décision

**Nous avons décidé de** valider les `id_token` Google via l'extension standard `quarkus-oidc` en mode `service` (`auth-server-url` = `https://accounts.google.com`, `client-id` = audience attendue ; discovery + JWKS mis en cache et rafraîchis par l'extension, rotation des `kid` gérée), en remplaçant `JWTUtils`, la persistance Mongo des clés JWKS, le refresh via `/_info` et `IJwtSigningKeyService`.

Rôles et identité fournis via `SecurityIdentity` (un `SecurityIdentityAugmentor` si des rôles applicatifs sont nécessaires).

Précisions :

- La collection Mongo `jwks_auth_signing_keys` devient obsolète. Elle n'est **pas supprimée** : conservation jusqu'à décision ultérieure.
- Le contrat API est inchangé (mêmes en-têtes `Authorization` et `X-Api-Key`).
- Trajectoire validée au Gate #0 : **A** (hotfix Lot 0) puis **B** (migration extension, Lot 6) ; l'option C (maison corrigée seule) est écartée.
- Le choix définitif entre `quarkus-oidc` et `smallrye-jwt` reste à confirmer au Gate #1 / spike du Lot 6 (voir Points ouverts).

---

## Alternatives Considérées

### Option 1 : `quarkus-oidc` en mode `service` ✅ Retenue (proposée)

- **Avantages** : solution standard avec support officiel natif Quarkus ; discovery OIDC, cache et rotation JWKS gérés par l'extension ; suppression du code de sécurité maison et de sa duplication ×4 ; identité exposée via `SecurityIdentity`.
- **Inconvénients** : 1 appel HTTPS vers Google (discovery/JWKS) au démarrage à froid ; mapping des codes 401/403 à aligner avec l'IHM ; compatibilité en compilation native (Mandrel 25) à valider.

### Option 2 : `smallrye-jwt` (`mp.jwt.verify.publickey.location` = JWKS Google + `issuer`)

- **Avantages** : plus léger, pas de discovery.
- **Inconvénients** : moins de fonctionnalités OIDC ; gestion à prévoir de plusieurs formes d'issuer (`accounts.google.com` / `https://accounts.google.com`).
- **Raison du rejet** : conservée comme repli possible, départagée au spike du Lot 6 si `quarkus-oidc` pose problème (natif, cold start).

### Option 3 : Maison corrigée (Lot 0 seul + refresh JWKS direct)

- **Avantages** : effort minimal.
- **Inconvénients** : code de sécurité maison à maintenir, risque de régression récurrent, duplication ×4 conservée.
- **Raison du rejet** : écartée au Gate #0 (trajectoire sans C) ; le Lot 0 est conservé uniquement comme hotfix transitoire.

### Option 4 : Authorizer API Gateway (JWT authorizer HTTP API / Cognito)

- **Avantages** : validation déportée hors de l'application.
- **Inconvénients** : changement d'infrastructure SAM (REST API vers HTTP API, ou Lambda authorizer), coût supplémentaire.
- **Raison du rejet** : hors trajectoire choisie ; impact infra disproportionné.

---

## Conséquences

### Positives
- Suppression de code de sécurité maison (classes JWT dupliquées ×4, persistance des clés JWKS, refresh via `/_info`).
- Rotation des clés Google (`kid` inconnu) gérée par l'extension ; disparition du couplage `parametrages` → autres services pour le refresh JWKS.
- Sécurité sortie du domaine : `IJwtSigningKeyService` n'est plus implémenté par les services métier.
- Contrat API inchangé (mêmes en-têtes) : pas de modification IHM attendue sur le transport du jeton.

### Négatives / Compromis
- Cold start Lambda : +1 fetch discovery/JWKS (mitigé par le cache de l'extension).
- Tests à adapter : jetons signés de test, `quarkus-test-security-jwt` ou `@TestSecurity`.
- Codes d'erreur 401 potentiellement différents : à vérifier côté IHM.
- Compatibilité native Mandrel 25 de `quarkus-oidc` à valider (spike) ; `JwtReflectionConfig` à reconsidérer.
- Dépendance à la disponibilité de `accounts.google.com` au démarrage à froid.

### Neutres
- Mise à jour de `docs/ARCHITECTURE.md` et de l'index des ADR requise (phase documentation).
- Les wikis (Conception-globale, sécurité) sont à mettre à jour à la mise en œuvre.

---

## Mise en œuvre

- **Fichiers impactés** : `communs/.../utils/security/JWTUtils.java`, `JwtSecurityContext`, `IJwtSigningKeyService` ; classes dupliquées ×4 (`JwtSecurityFilter`, `JwsSigningKeysDatabaseAdaptor`, `JwtReflectionConfig`, etc.) dans `comptes`, `operations`, `parametrages`, `utilisateurs` ; client REST JWKS et refresh via `/_info` dans `parametrages` ; configuration `application.properties` des 4 services.
- **Tâches de suivi** : DEVon — spike natif `quarkus-oidc` vs `smallrye-jwt`, puis migration des 4 services ; QALvin — adaptation des tests de sécurité (401/403, jeton expiré, mauvaise signature, mauvais `iss`/`aud`, `kid` inconnu) ; DOCly — `docs/ARCHITECTURE.md`, index ADR, wikis.
- **Date d'effet** : Lot 6 du Plan d'Action 004, après le hotfix Lot 0 (branche `fix/securite-jwt-lot0`).

### Points ouverts

- `quarkus-oidc` vs `smallrye-jwt` : à trancher au Gate #1 / spike du Lot 6.
- Sort de la collection Mongo `jwks_auth_signing_keys` : conservée, décision ultérieure (aucun `drop`).
- Alignement des codes 401/403 avec le comportement attendu par l'IHM.

---

## Références

- Plan d'Action associé : `.claude/plans/004_revue_architecture_backend.plan.md` (Lot 6)
- ADR-004 (référence de style) : `docs/adr/004-clock-injection-convention.md`
- Quarkus — OpenID Connect Bearer token authentication (`quarkus-oidc`, mode `service`)
