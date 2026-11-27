# ADR 009 — Mutualisation dans `communs`, configuration commune et résilience des appels REST inter-services

---

**Date :** 2026-11-20
**Statut :** Acceptée (Gate #1 GO 👤, 2026-11-20)
**Décideurs :** 🟠 ARCos + 👤 Développeur humain

---

## Contexte

### Duplication entre modules

- Chaque module (`comptes`, `operations`, `parametrages`, `utilisateurs`) contient des coquilles ×4 (`JwtSecurityFilter`, `APIExceptionsHandler`, `SPIExceptionsHandler`) qui délèguent à des classes abstraites de `communs`.
- Les filtres pre/post-matching de trace sont redéclarés dans les 4 ressources.
- Les 8 `application.properties` sont identiques à environ 80 %. Les clés `rest-client-reactive.*` (ancien préfixe) et `rest-client.*` coexistent.
- Les profils Maven font que les tests JVM tournent avec la configuration `dev/` et les tests natifs avec `prod/` : les deux ne testent pas la même configuration.

### Appels inter-services

- Les clients REST `operations` → `comptes` / `parametrages` passent par l'API Gateway public et n'ont **aucun timeout**. Les défauts Quarkus (connexion 15 s, lecture 30 s) dépassent les 15 s de la Lambda : la Lambda est tuée avant que le client n'abandonne.
- Les erreurs de ces appels ne sont pas typées (500 probable).
- Sur les écritures, l'appel à `comptes` se fait en séquence après la lecture Mongo.

### Budget de temps

- Budget Lambda : **15 s**, Mongo inclus.
- Le pire cas Mongo est de 16 s : accepté par le 👤.
- Pas de trajectoire C (fusion des services) : on garde 4 Lambdas.

---

## Décision

**Nous avons décidé de** mutualiser dans `communs` le code transverse vivant et générique, de rassembler la configuration commune dans `communs` avec un seul `application.properties` par module, et de rendre les appels REST inter-services résilients (timeouts, retry ciblé, erreurs typées en 503), avec un cache limité aux catégories.

### 1. Mutualisation partielle (A3)

Déplacer dans `communs`, sous forme de classes **concrètes découvertes par Jandex**, ce qui est vivant et générique :
- mappers d'exceptions API et BDD ;
- filtre de sécurité ;
- filtres de trace (`@ServerRequestFilter` / `@ServerResponseFilter`).

Restent dans chaque module :
- `RootAPIResource` : chemin lié à `tenant-paths` ([ADR-008](./008-endpoint-info-public-tenant-oidc-desactive.md)) ;
- `OpenAPIConfig` : titre propre à chaque service, version alignée sur celle de l'application.

Le code JWT maison mort (`JwtReflectionConfig`, `JwsSigningKeysDatabaseAdaptor`) relève de la tâche T4.0 du plan 004.

### 2. Configuration (B1 + B2)

- **B1** : créer `communs/src/main/resources/META-INF/microprofile-config.properties` (ordinal 100, surchargeable par les modules). Les valeurs qui ne doivent pas s'appliquer aux tests y sont préfixées `%dev,prod.`.
- **B2** : remplacer les profils Maven `dev` / `native` par les profils Quarkus `%dev` / `%prod`, avec **un seul `application.properties` par module**. Les tests JVM et natifs ont ainsi la même configuration.

### 3. Résilience REST (C2)

- **Timeouts REST client** : connexion **1,5 s**, lecture **4 s**.
- **`quarkus-smallrye-fault-tolerance`** :
  - un seul `@Retry`, limité aux erreurs rapides (connexion refusée, 502, 503, 504, 429), délai 200 ms avec jitter ;
  - **jamais de retry sur timeout** : un retry provoquerait un deuxième démarrage à froid de l'aval ;
  - pas de circuit breaker : son état est par conteneur, inutile sous Lambda ;
  - pas de fallback.
- **`ResponseExceptionMapper`** sur les 2 clients :

| Réponse de l'aval | Exception | Code renvoyé |
|---|---|---|
| 404 | `DataNotFoundException` | 404 |
| 401 | (401 propagé) — **modifié à la mise en œuvre : 503**, voir la note de mise en œuvre | ~~401~~ → **503** |
| 403, 429, 5xx, échec de connexion, timeout | nouvelle `ServiceDistantIndisponibleException` | **503** |

  Un 403 renvoyé par l'aval **ne doit jamais être transmis en 403** : l'IHM déconnecterait l'utilisateur.
- **Parallélisation** : lancer l'appel à `comptes` en parallèle de la lecture Mongo dans `getBudgetAndCompteActif`, pour que le pire cas REST (environ 7,2 s) reste sous le read-timeout Mongo de 8 s.

### 4. Cache (D1)

`quarkus-cache` sur les **catégories uniquement** (modifiées à la main en base), TTL **1 h**. Le cache du compte est reporté après la mesure : un compte clos resterait vu actif, ce qui contournerait la garde 406.

### 5. Instrumentation et mesure

- Logs `[HTTP-METRIC]` et `[REST-METRIC]`, sans identifiant ni jeton.
- Mesure du p95 de création d'opération en QUA, avant et après, avec CloudWatch Logs Insights et les lignes `REPORT` de la Lambda.
- Pas de X-Ray, pas de métriques détaillées API Gateway.
- Le 👤 lance 50 appels à chaud et 5 à froid.

---

## Alternatives Considérées

### Mutualisation du code (A)

- **A3 : mutualisation partielle** ✅ Retenue. Avantages : une seule source pour le transverse ; les classes spécifiques (`RootAPIResource`, `OpenAPIConfig`) restent locales. Inconvénients : dépendance au SNAPSHOT `communs`.
- **A1 : tout en classes concrètes.** Raison du rejet : impossible pour les classes spécifiques à chaque module.
- **A2 : statu quo.** Raison du rejet : duplication ×4 conservée, coquilles à maintenir en parallèle.

### Configuration (B)

- **B1 + B2** ✅ Retenue (fichier commun `microprofile-config.properties` + profils Quarkus).
- **B1 seul.** Raison du rejet : les profils Maven conserveraient des configurations différentes entre tests JVM et natifs.
- **B2 seul.** Raison du rejet : les 8 `application.properties` resteraient dupliqués à environ 80 %.

### Résilience (C)

- **C2 : timeouts + retry ciblé + erreurs typées** ✅ Retenue.
- **C1 : timeouts seuls.** Raison du rejet : pas de reprise sur erreur rapide, erreurs non typées.
- **C3 avec `@Fallback`.** Raison du rejet : un remboursement non créé passerait sans aucun signal.
- **Retry via Mutiny.** Raison du rejet : écarté par le 👤 au profit de la fault tolerance déclarative.

### Cache (D)

- **D1 : cache des catégories seules** ✅ Retenue.
- **D2 : compte en cache, garde 406.** Raison du rejet : un compte clos resterait vu actif. Reporté après la mesure.
- **D3 : pas de cache.** Raison du rejet : les catégories, modifiées à la main, se prêtent à un cache sans risque.

---

## Conséquences

### Positives
- Une seule source pour le transverse (mappers, filtres, configuration commune).
- T4.0 simplifié.
- Plus de Lambda tuée à cause de l'aval.
- Erreurs de l'aval en 503, cohérent avec R10.
- p95 réduit (appel parallèle, cache des catégories).
- Tests JVM et natifs alignés sur la même configuration.

### Négatives / Compromis
- Configuration répartie sur 2 fichiers (`communs` + module).
- Dépendance au SNAPSHOT `communs` : risque de perte silencieuse des mappers si un module est construit contre une version obsolète. Parades : tests REST des codes HTTP par module, une seule PR pour toute la Phase 2.
- 2 extensions de plus (fault tolerance, cache).
- Codes HTTP modifiés, validés par le 👤 : compte inconnu hors virement 500 → 404 ; panne ou timeout de l'aval 500 → 503. **Impact sur l'IHM.**
- Le TTL du cache (Caffeine) n'utilise pas la `Clock` de l'[ADR-004](./004-clock-injection-convention.md).

### Neutres
- Contrat d'API et `*APIEnum` inchangés.
- Suppressions de fichiers faites par le 👤 (règle absolue des agents).
- `docs/ARCHITECTURE.md` à mettre à jour au passage à `Acceptée`.

---

## Mise en œuvre

- **Fichiers impactés** : `communs` (mappers, filtre de sécurité, filtres de trace, `microprofile-config.properties`, `ServiceDistantIndisponibleException`), 8 `application.properties`, `pom.xml` (profils Maven), clients REST et `getBudgetAndCompteActif` dans `operations`.
- **Tâches de suivi** : plan 004, tâches **T2.0 à T2.9**, en **une seule PR** (décision 👤).
- **Date d'effet** : Phase 2 du plan 004.

### Note de mise en œuvre (2026-11-27)

Code et tests mergés sur `master` (Gates #2 et #3 de la Phase 2 : GO 👤) :

| PR | Contenu |
|---|---|
| #233, #234, #235 | Code de la Phase 2 : instrumentation (T2.0, commit 5bc90cb), providers mutualisés, configuration commune et profils Quarkus, résilience des appels REST, parallélisation, cache des catégories, version OpenAPI (T2.1 à T2.7 ; commit 4cfbc36 conservé tel quel au 2ᵉ passage du Gate #2) |
| #241 | Tests de la Phase 2 (T2.8) : 74 tests ajoutés, faux service aval, preuves de régression (mapper, retry) ; CI verte sur Quarkus 4.0.1 (OIDC, Mongo, natif) |

Master est depuis passé en Quarkus 4.0.1 (#237, #240) ; #239 a ajouté `ConflictException` (409 sur un virement dont l'id d'opération existe déjà sur le compte destination), sans lien avec cette décision.

**Écarts à la décision** (tous validés par le 👤) :
- **401 de l'aval** : la décision (tableau § 3) prévoyait de propager le 401. Gate #2 du 2026-11-22 : **tout 4xx de l'aval hors 404 (401 compris) donne 503, sans retry**. Plus aucune `UserNotAuthorizedException` en sortie d'un client REST inter-services.
- **Échecs de transport** : traduits par un **intercepteur CDI** (`TraductionErreursServiceDistantInterceptor`, lié par `@TraductionErreursServiceDistant` sur chaque méthode de client), car un `ResponseExceptionMapper` ne voit jamais un timeout ni une connexion refusée. Ajout de `ServiceDistantTransitoireException` (dans `operations`, sous-classe de `ServiceDistantIndisponibleException`) pour que `@Retry` ne rejoue que la connexion refusée et les 429/502/503/504. Risque résiduel : une future méthode de client sans cette liaison n'aurait ni traduction des échecs de transport ni retry.
- **Mesure avant/après (§ 5)** : **non réalisée**, décision 👤 du 2026-11-22. Les logs `[HTTP-METRIC]` / `[REST-METRIC]` sont en place pour un suivi ultérieur ; la tâche T2.9 est abandonnée.
- **Docker** : sans chaîne de connexion en profil de test, Dev Services démarre MongoDB pour les `@QuarkusTest` de `parametrages` et `utilisateurs` (Docker accepté comme prérequis, Gate #2). Le retrait de l'exclusion des `*MongoTest` des builds natifs, tenté en T2.3, **n'est pas appliqué** : le profil `ci-sans-mongo` (utilisé par les builds natifs, master et tags) exclut toujours les 5 classes.
- **Pire cas Mongo de 16 s** : accepté (inchangé), hors périmètre.
- **Suppressions de fichiers** (coquilles ×4 et `application.properties` `dev/` `prod/`) : faites par le 👤, aucun agent ne supprime.

Voir `docs/ARCHITECTURE.md` (sections Mutualisation, Appels inter-services et résilience, Instrumentation) et le rapport `.claude/plans/004_reports/PHASE_2_TESTS_REPORT.md`.

---

## Références

- Plan d'Action associé : [`.claude/plans/004_revue_architecture_backend.plan.md`](../../.claude/plans/004_revue_architecture_backend.plan.md) — Phase 2
- [ADR-004](./004-clock-injection-convention.md), [ADR-007](./007-coherence-virement-ecriture-sequentielle-compensee.md), [ADR-008](./008-endpoint-info-public-tenant-oidc-desactive.md)
