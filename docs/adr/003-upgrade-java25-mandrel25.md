# ADR 003 — Upgrade Java 21 → 25 + Mandrel 25 (Quarkus inchangé)

---

**Date :** 2026-07-08
**Statut :** Acceptée
**Décideurs :** 🟠 ARCos + 👤 Développeur humain

---

## Contexte

Le [Plan d'Action 001](../../.claude/plans/001_modernisation_stack.plan.md) (Phase 5) prévoyait initialement un upgrade combiné Quarkus 4.x + Java 25, isolé par spike du fait du risque élevé (breaking changes, maturité incertaine). La recherche réelle menée en T5.1 (2026-07-07) a montré que **Quarkus 4.0 n'a pas de version GA** (Beta1 visée au plus tôt septembre 2026, roadmap sans engagement) — axe prématuré pour toute production. En revanche, **Mandrel 25 (Java 25 LTS natif) est disponible dès maintenant et compatible Quarkus ≥ 3.27.0**, donc découplé de Quarkus 4.x et faisable directement sur la ligne 3.x actuelle (3.37.1).

Un spike isolé (worktree jetable `spike/java25-mandrel25`, T5.2/T5.3) a validé l'upgrade sur `communs`+`parametrages` : build JVM et natif Lambda réussis, un seul breaking change réel trouvé (voir Conséquences). Sur cette base, le développeur a validé le passage à la migration complète (5b, T5.4-T5.7) sur les 5 modules.

---

## Décision

**Nous avons décidé de** upgrader `maven.compiler.release`/`source`/`target` de 21 à 25 sur les 5 modules (via la propriété du pom racine, héritée), et de faire compiler les binaires natifs Lambda avec Mandrel 25 (`quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-25`), **en conservant `quarkus.platform.version` inchangé à 3.37.1**. L'upgrade vers Quarkus 4.x reste hors scope, reporté à une initiative future une fois une version GA disponible.

---

## Alternatives Considérées

### Option 1 : Java 25 + Mandrel 25 sur Quarkus 3.37.1 inchangé ✅ Retenue

- **Avantages** : découplé du risque Quarkus 4.x (pas de breaking change de framework) ; profite immédiatement des améliorations JDK 25 LTS (performance, GC, sécurité) ; validé par spike isolé avant engagement large ; rollback trivial en cas de problème (un seul palier de version à revenir en arrière).
- **Inconvénients** : ne résout pas au passage d'éventuelles limitations propres à Quarkus 3.x (aucune identifiée comme bloquante à ce jour).

### Option 2 : Attendre Quarkus 4.x GA pour upgrader Java et Quarkus ensemble

- **Avantages** : un seul palier de migration au lieu de deux, évite de refaire un travail de validation à l'arrivée de Quarkus 4.x.
- **Raison du rejet** : aucune date GA fiable (roadmap "no-commitment", Beta1 au plus tôt septembre 2026) — bloquerait le bénéfice de Java 25 LTS pendant une durée indéterminée sans justification technique.

### Option 3 : Rester sur Java 21 jusqu'à Quarkus 4.x

- **Avantages** : aucun changement, aucun risque.
- **Raison du rejet** : prive le projet des bénéfices Java 25 LTS sans raison technique bloquante (le spike a démontré la faisabilité immédiate et un effort minime) ; retarde inutilement une dette de version évitable.

---

## Conséquences

### Positives
- Backend sur JDK 25 LTS (build JVM + natif Lambda), sans changement de version Quarkus ni breaking change de framework.
- Aucun blocage réel trouvé lors du spike ni de la migration complète : build et tests verts sur les 5 modules, build natif Lambda validé en CI sur les 5 fonctions (Mandrel 25).
- Effort réel très inférieur à l'estimation initiale du plan ("M") : un seul correctif nécessaire (voir ci-dessous), appliqué en quelques minutes une fois la cause identifiée.

### Négatives / Compromis
- **Lombok ne génère plus les getters/setters sous JDK ≥ 23 sans déclaration explicite** : `javac` a changé son comportement d'auto-discovery des annotation processors présents sur le classpath à partir du JDK 23 (l'annotation processor n'est plus activé implicitement). Sans configuration explicite, la compilation échoue en cascade (~40 erreurs `cannot find symbol` sur les getters/setters Lombok de `communs`). **Corrigé** par ajout d'un bloc `annotationProcessorPaths` déclarant Lombok explicitement dans la configuration du `maven-compiler-plugin` du pom racine — 6 lignes, propagées à tous les modules.
- Un bug latent, sans lien avec cet upgrade, a été révélé au passage : `TestMigrationRepository.testListerVersionsAppliqueesSurBaseFraiche` supposait à tort la collection `_migrations` vide au démarrage, alors que `MongoMigrationRunner` y insère réellement la migration `V001` à chaque boot Quarkus. Jamais détecté avant (tests jamais exécutés contre un vrai MongoDB en local, faute de Docker) — révélé par la première exécution réelle en CI (GitHub Actions, Docker disponible). Corrigé au passage (voir Mise en œuvre), indépendamment de cette ADR.

### Neutres
- PR upstream Quarkus [#55278](https://github.com/quarkusio/quarkus/pull/55278) (workaround Jackson posé en Phase 2/T8.1) reste non confirmée backportée en release 3.x stable (mergée sur `main`, labellisée `triage/backport-3.x`, milestone `3.37.2`) — le workaround `enable-reflection-free-serializers=false` (8 fichiers `application.properties`) est conservé sans changement, à ne pas confondre avec le contenu de cette ADR.
- Les 4 fichiers `JwtReflectionConfig.java` (un par microservice, `@RegisterForReflection` standard) ont été revalidés sans modification nécessaire sous Mandrel 25.

---

## Mise en œuvre

- **Fichiers impactés** :
  - `pom.xml` (racine) — `maven.compiler.release`/`source`/`target` 21→25 ; ajout `annotationProcessorPaths` (Lombok) dans la configuration `maven-compiler-plugin`
  - `.github/workflows/build-on-master.yml`, `build-on-tags.yml`, `build-on-all.yml` — JDK 21→25 (`actions/setup-java`), image builder natif `quay.io/quarkus/ubi-quarkus-mandrel-builder-image:24.0-jdk-22` → `:jdk-25`
  - `communs/src/test/java/.../migrations/TestMigrationRepository.java`, `TestMigrationRepositoryPersistence.java` — correction de l'hypothèse "base fraîche" (voir Conséquences)
- **Validation** : spike isolé (worktree/branche jetable, T5.2/T5.3) sur `communs`+`parametrages` avant migration complète ; build JVM + tests + build natif Lambda confirmés en CI sur les 5 modules (T5.4-T5.6) via workflows temporaires supprimés après validation.
- **Tâches de suivi** : Phase 5 du Plan d'Action, tâches T5.1 à T5.7.
- **Date d'effet** : à partir du Gate spike validé par le développeur (2026-07-08), migration complète appliquée sur `feat/upgrade`.

---

## Références

- [ADR-001](./001-strategie-modernisation-stack.md) — Stratégie de modernisation du stack backend (cadre les 5 phases, dont cette Phase 5)
- Plan d'Action associé : `gestion-budget-serverless/.claude/plans/001_modernisation_stack.plan.md` (Phase 5 — Upgrade Java 25)
- [Mandrel 25 is Here!](https://quarkus.io/blog/mandrel-25-released/) — Quarkus Blog
- [PR quarkusio/quarkus#55278](https://github.com/quarkusio/quarkus/pull/55278) — workaround Jackson (Phase 2/T8.1), sans lien direct avec cette ADR
