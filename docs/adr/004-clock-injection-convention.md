# ADR 004 — Convention d'injection `Clock` UTC pour horodatage

---

**Date :** 2026-07-10
**Statut :** Acceptée
**Décideurs :** 🟠 ARCos + 👤 Développeur humain (Gate #0 antérieure au Plan 003)

---

## Contexte

SonarCloud remonte 18 issues règle S8688 (usage `.now()` sans zone explicite — `LocalDateTime.now()`, `Instant.now()`, etc.), réparties code main + tests, sur `communs` et les 4 microservices (`comptes`, `operations`, `parametrages`, `utilisateurs`). Aucune convention projet actuelle sur gestion temps/timezone.

Risques identifiés :

1. **Tests non déterministes** : appels `.now()` réels dans tests → assertions temporelles fragiles, flaky selon horaire exécution CI.
2. **Ambiguïté de zone selon environnement** : `ZoneId.systemDefault()` (ou équivalent implicite) dépend du fuseau système — potentiellement différent entre Lambda AWS (région déploiement), poste dev local, CI GitHub Actions. Source d'incohérences silencieuses.
3. **Incohérences dates stockées/comparées** : dates persistées MongoDB sans zone garantie uniforme → comparaisons/tris erronés selon origine de la donnée (ex. clonage opérations mois suivant, tokens JWT avec expiration).

Sujet transverse `communs` + 4 microservices : nécessite convention unique avant remédiation.

---

## Décision

**Nous avons décidé de** exposer `Clock.systemUTC()` comme bean CDI produit dans le module `communs`, nouveau package `config` (cohérent avec pattern déjà utilisé au niveau de chaque microservice — `config/OpenAPIConfig.java`, `config/JwtReflectionConfig.java` — distinct de `utils/` qui contient des helpers statiques sans état CDI) :

```java
package io.github.vzwingma.finances.budget.services.communs.config;

@ApplicationScoped
public class ClockConfig {
    @Produces
    @ApplicationScoped
    public Clock clock() {
        return Clock.systemUTC();
    }
}
```

Injection par constructeur (pas field injection) dans toutes les classes concernées — cohérent avec la remédiation constructor injection déjà appliquée en Phase A (S6813 sur `MongoMigrationRunner`) et la convention CDI du projet.

Call sites à migrer, en 2 sous-phases :

- **B1 — `communs`** : `JWTUtils.java:186`, `JWTAuthToken.java:61,73`, `MigrationRepository.java:45,58`
- **B2 — `operations` + `utilisateurs`** (dépend B1 mergé/publié GitHub Packages) : `BudgetService.java:296,333,407,435`, `LigneOperation.java:142-144`, `BudgetDataUtils.java:158,202`, `OperationsService.java:249,255`, `Utilisateur.java:54`, `UtilisateursService.java:80`

---

## Nuance d'implémentation (constatée en T B.1)

L'injection par constructeur, prescrite ci-dessus, suppose que la classe cible est un bean CDI géré par le conteneur — cas nominal, appliqué tel quel à `MigrationRepository` (vrai `@ApplicationScoped`).

Deux fichiers `communs` migrés en T B.1 sortent de ce cas nominal :

- `JWTUtils.java` : classe 100% statique (constructeur privé), jamais instanciée par CDI → injection par constructeur techniquement impossible.
- `JWTAuthToken.java` : POJO instancié via `new` à ~15 endroits répartis sur 5 modules (dont hors `communs`) → changer la signature du constructeur aurait un blast radius disproportionné, hors du périmètre de T B.1.

Pour ces deux cas, alternative pragmatique appliquée (validée développeur humain) : `Clock` passé en **paramètre de méthode explicite**, avec un **overload par défaut** appelant `Clock.systemUTC()` — préserve 100% de la compatibilité des appelants existants, aucune signature cassée. Testabilité conservée via l'overload acceptant un `Clock` explicite (`Clock.fixed(...)` en test).

Règle retenue : injection par constructeur si bean CDI géré par le conteneur ; sinon (classe statique non instanciée par CDI, ou POJO largement instancié via `new` avec blast radius de migration disproportionné hors scope tâche courante), paramètre de méthode explicite + overload par défaut `Clock.systemUTC()`.

---

## Alternatives Considérées

### Option 1 : `Clock.systemUTC()` injecté via bean CDI ✅ Retenue

- **Avantages** : testabilité (`Clock.fixed(instant, ZoneOffset.UTC)` injectable en test → dates déterministes) ; cohérence UTC systématique indépendante de l'environnement (Lambda région/dev/CI) ; single source of truth (`ClockConfig` dans `communs`) ; aligné sur la convention CDI constructor injection déjà en place (Phase A).
- **Inconvénients** : migration de 18 call sites répartis `communs` + 2 microservices, effort non trivial découpé en 2 sous-phases ; `communs` doit être republié avant B2.

### Option 2 : `ZoneId.systemDefault()` passé explicitement à `.now(zone)`

- **Avantages** : corrige littéralement la règle S8688, changement minimal ligne à ligne.
- **Inconvénients** : dépend du fuseau JVM d'exécution, non déterministe entre environnements ; ne résout pas le risque de fond, seulement le symptôme Sonar.
- **Raison du rejet** : traite le symptôme (alerte Sonar) sans traiter le risque (incohérence de zone selon environnement).

### Option 3 : `ZoneOffset.UTC` / `Clock.systemUTC()` inline sans bean injectable

- **Avantages** : correction minimale, pas de nouveau package/bean à introduire.
- **Inconvénients** : pas testable/mockable (impossible de figer le temps en test) ; dispersion de la convention sur 18 call sites sans point de contrôle unique.
- **Raison du rejet** : perd la testabilité, pas de single source of truth, non aligné avec la convention CDI du projet.

---

## Conséquences

### Positives
- Testabilité : `Clock.fixed(instant, ZoneOffset.UTC)` injectable en test → dates déterministes, fin des tests flaky liés au temps.
- Cohérence UTC systématique, indépendante de l'environnement d'exécution (Lambda région, poste dev, CI).
- Single source of truth : `ClockConfig` centralisé dans `communs`.
- Alignement avec la convention CDI constructor injection déjà en place depuis la Phase A.

### Négatives / Compromis
- Migration de 18 call sites répartis `communs` + 2 microservices (`operations`, `utilisateurs`), effort non trivial découpé en 2 sous-phases (B1, B2).
- Risque de régression sur logique métier sensible aux dates, notamment `cloneOperationToMoisSuivant`/`cloneOperationPeriodiqueToMoisSuivant` dans `BudgetDataUtils` (`operations`), déjà signalée fragile dans `.claude/CLAUDE.md` — vigilance équivalente attendue lors de B2.
- `communs` doit être republié (GitHub Packages) avant B2 — B2 dépend strictement de B1 mergé.

### Neutres
- Effort estimé : B1 (`communs`, 5 call sites, 3 fichiers) Faible ; B2 (`operations` + `utilisateurs`, 13 call sites, 6 fichiers) Moyen — dépend de B1 publié, touche logique métier budget (risque MEDIUM sur `BudgetDataUtils`/`BudgetService`).

---

## Mise en œuvre

- **Fichiers impactés** : `communs/.../config/ClockConfig.java` (nouveau) + 3 fichiers `communs` (B1 : `JWTUtils.java`, `JWTAuthToken.java`, `MigrationRepository.java`) + 6 fichiers `operations`/`utilisateurs` (B2 : `BudgetService.java`, `LigneOperation.java`, `BudgetDataUtils.java`, `OperationsService.java`, `Utilisateur.java`, `UtilisateursService.java`).
- **Tâches de suivi** : DEVon — implémenter B1 (`communs`), publier GitHub Packages, puis implémenter B2 (`operations` + `utilisateurs`) une fois B1 mergé.
- **Date d'effet** : à partir de la Phase B du Plan d'Action `003_remediation_sonar.plan.md`.

---

## Références

- Plan d'Action associé : `gestion-budget-serverless/.claude/plans/003_remediation_sonar.plan.md`
- ADR-001 (référence style) : `docs/adr/001-strategie-modernisation-stack.md`
