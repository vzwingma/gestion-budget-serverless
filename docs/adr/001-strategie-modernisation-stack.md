# ADR 001 — Stratégie de modernisation du stack backend

---

**Date :** 2026-07-06
**Statut :** Acceptée
**Décideurs :** 🟠 ARCos + 👤 Développeur humain

---

## Contexte

Le backend `gestion-budget-serverless` (Java 21, Quarkus 3.36.0, Lambda natif GraalVM) repose déjà sur une stack récente et une architecture soignée (hexagonale/DDD, CDI pur, Mutiny réactif). Aucune dette technique bloquante n'a été identifiée, mais 4 signaux justifient une initiative de modernisation proactive :

1. **Override manuel Netty 4.1.132.Final** dans `pom.xml` racine, posé à la main pour patcher les CVE-2026-33870/33871, au lieu d'un upgrade propre du BOM Quarkus qui inclurait le correctif nativement.
2. **Gouvernance ADR inexistante** : `docs/adr/` ne contenait jusqu'ici que le fichier `ADR-TEMPLATE.md`, aucune décision architecturale n'était tracée.
3. **Paramétrage SAM fragile** : les workflows CI (`build-on-master.yml`, `build-on-tags.yml`) injectent les valeurs de configuration via des commandes `sed` inline sur `sam.native.template.yaml` / `samconfig.template.toml`. Le `MemorySize: 128` des fonctions Lambda n'a par ailleurs jamais été mesuré ni validé.
4. **Absence d'outillage de versioning de schéma MongoDB** : aucune trace de gestion des évolutions de collections, aucun mécanisme de migration.

Une décision cadre était nécessaire pour séquencer ces 4 axes de manière maîtrisée, sans big-bang, et pour trancher les options d'architecture sous-jacentes (infra Lambda vs conteneur, migrations Mongo maison vs outil tiers, ampleur de l'upgrade de version).

---

## Décision

**Nous avons décidé de** conduire la modernisation en **5 phases séquencées** : (1) bootstrap de la gouvernance ADR (cet ADR), (2) upgrade Quarkus 3.36.0 → dernière version stable 3.x avec retrait de l'override Netty si le BOM est patché, (3) tuning de l'infrastructure Lambda natif (mesure réelle du `MemorySize`, remplacement des `sed` CI par un paramétrage SAM natif), (4) mise en place de migrations MongoDB maison (classes CDI + collection `_migrations`, déclenchées via `@Observes StartupEvent`), et (5) upgrade Quarkus 4.x/Java 25 en dernier, précédé d'un spike de compatibilité isolé avec un Gate Go/No-Go dédié avant extension aux 5 modules.

---

## Alternatives Considérées

### Option 1 : Paliers incrémentaux (3.x d'abord, 4.x isolé après validation) ✅ Retenue

- **Avantages** : risque maîtrisé à chaque étape, rollback simple par palier, permet de valider la stabilité en production avant d'engager le palier le plus risqué (4.x/Java 25).
- **Inconvénients** : délai total plus long qu'un big-bang, effort réparti sur 5 phases.

### Option 2 : Upgrade direct Quarkus 4.x + Java 25 en un seul saut

- **Avantages** : un seul cycle de non-régression à absorber, pas de double effort de migration de version.
- **Inconvénients** : risque élevé — breaking changes cumulés (config keys, extensions renommées), maturité Mandrel/Java 25 en mode natif incertaine à date.
- **Raison du rejet** : surface de risque trop large pour un seul cycle ; en cas de blocage sur Java 25 natif, aucun retour arrière partiel possible.

### Option 3 : Migration infrastructure vers conteneurs ECS/Kubernetes

- **Avantages** : portabilité accrue, écosystème d'orchestration plus riche.
- **Inconvénients** : coût récurrent non nul (vs Lambda pay-per-use), complexité opérationnelle disproportionnée pour le contexte (application de gestion de budget, faible volumétrie), perte de l'intégration actuelle API Gateway + X-Api-Key. Le Lambda natif GraalVM est déjà quasi optimal côté cold start (démarrage natif de l'ordre de la milliseconde).
- **Raison du rejet** : sur-ingénierie par rapport au besoin réel ; aucun signal ne justifie d'abandonner le modèle serverless actuel.

### Option 4 : Mongock pour les migrations de schéma MongoDB

- **Avantages** : outil tiers mature, conventions établies dans l'écosystème Java/Mongo.
- **Inconvénients** : repose sur réflexion et classpath scanning dynamique, risque élevé d'incompatibilité avec GraalVM native-image.
- **Raison du rejet** : l'ensemble du déploiement du projet repose sur des binaires natifs GraalVM ; une solution maison alignée sur le CDI pur déjà en place est plus contrôlée et évite ce risque d'incompatibilité.

### Axe exclu (reporté, non rejeté) : Observabilité / OpenTelemetry

Le développeur a choisi de reporter cet axe à une initiative future, distincte de ce plan. Backend pré-choisi si l'axe est repris : AWS X-Ray, cohérent avec la stack AWS existante.

---

## Conséquences

### Positives
- Sécurité renforcée : retrait de l'override manuel Netty au profit d'un correctif porté nativement par le BOM Quarkus.
- Gouvernance traçable : `docs/adr/` désormais alimenté, chaque décision structurante future sera documentée.
- Infrastructure Lambda dimensionnée sur mesure réelle plutôt que sur une valeur arbitraire (`MemorySize: 128` jamais validé).
- Schéma MongoDB versionné via un mécanisme de migration traçable et idempotent.
- Risque du palier le plus incertain (Quarkus 4.x/Java 25) isolé et maîtrisé par un spike dédié avant engagement large.

### Négatives / Compromis
- Effort réparti sur 5 phases : délai total plus long qu'une approche big-bang.
- Le palier Quarkus 4.x/Java 25 reste incertain par nature — dépend de la maturité de l'écosystème Mandrel/Java 25 au moment de l'exécution, à revalider explicitement (aucune version n'est figée dans ce plan).
- La solution de migrations Mongo maison offre moins de tooling qu'un outil dédié (pas d'UI, pas de rollback automatique — à developper au besoin).

### Neutres
- Nécessite une mise à jour continue de `docs/ARCHITECTURE.md` et des instructions projet (`dev.instructions.md`, `orchestrator.instructions.md` — ce dernier actuellement désynchronisé, il mentionne encore Quarkus 3.35) au fil de l'avancement des phases.

---

## Mise en œuvre

- **Fichiers impactés** : `pom.xml` racine + 5 modules (`communs`, `parametrages`, `utilisateurs`, `comptes`, `operations`), `communs/src/aws-deploy/sam.native.template.yaml`, `communs/src/aws-deploy/samconfig.template.toml`, workflows `.github/workflows/build-on-master.yml` et `build-on-tags.yml`, nouveau package `communs/src/main/java/.../communs/migrations/`.
- **Tâches de suivi** : voir Phases 2 à 5 du Plan d'Action associé (upgrade Quarkus 3.x, tuning infra Lambda, migrations MongoDB, upgrade Quarkus 4.x/Java 25 avec spike Go/No-Go).
- **Date d'effet** : à partir du Gate #1 validé, le 2026-07-06.

---

## Références

- Plan d'Action associé : `gestion-budget-serverless/.claude/plans/001_modernisation_stack.plan.md`
