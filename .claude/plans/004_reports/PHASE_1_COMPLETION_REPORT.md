# Rapport de phase 1 — Plan 004 (DEVon, tâches non bloquées)

**Branche :** `feat/ap004-phase1-fiabilite-mongo` (base eb51127). **Statut :** partiel, T1.1b-ID et T1.1c bloqués par Q5.
Tests nouveaux : T1.3 (QALvin). Docs/ADR : T1.4 (DOCly).

## Commits (un par piste)

| Piste | Commit | Tâches |
|---|---|---|
| C | 2e82f64 | T1.1a |
| B | 9dd380e | T1.2a, T1.2b |
| D | 331f259 | T1.2c |
| A | c86abc6 | T1.1b-tri, T1.1d, T1.2d |

### T1.1a — F1 lastAccess + utilisateur inconnu — DONE
- `IUtilisateursRepository.majDernierAcces(login, date)` : `Uni<Void>`, `updateOne` + `$set dernierAcces` ciblé (date encodée comme le codec existant).
- `UtilisateursService.getLastAccessDate` : utilisateur null -> `DataNotFoundException` (404) ; écriture chaînée avant la réponse ; échec -> WARN, réponse 200 avec l'ancienne date (valeur lue, comme avant).
- Supprimé : `majUtilisateur` (+ `.subscribe()` fire-and-forget). Plus aucun `.subscribe()` dans `utilisateurs/src/main`.
- Test existant `UtilisateursServiceTest` adapté (mock `majDernierAcces`, captor de date).

### T1.2a — A3 + F6b — DONE
- `MigrationRepository.reserver(version, description, owner)` : `findOneAndUpdate` + upsert, filtre `{_id, $or:[ECHEC, EN_COURS & dateExecution < now-5min]}` ; E11000 (code 11000, cause chaînée) -> `false` (version déjà prise).
- `finaliserSucces/finaliserEchec(version, owner)` : `updateOne({_id, owner})`, WARN si 0 document (bail repris).
- `MigrationRecord` : champ `owner`, statut `EN_COURS`. Bail `BAIL = 5 min`, `Clock` injectée (constructeur, ADR-004).
- `MongoMigrationRunner` : `owner` = UUID par instance ; réserve avant exécution ; version non réservée -> ignorée, passe à la suivante. `enregistrerSucces/Echec` supprimés (insert -> E11000 pour une version ECHEC = F6b).
- Tests existants adaptés : `TestMongoMigrationRunner`, `TestMigrationRecord` (3 statuts), `TestMigrationRepositoryPersistence`.

### T1.2b — V002 — DONE
- `V002_AjoutIndexBudgetsUtilisateurs` : `listIndexes` puis création seulement si aucune clé identique (champs, ordre, sens ; nom ignoré). `budgets {idCompteBancaire,annee,mois}`, `utilisateurs {login}` non unique. Base via `@ConfigProperty quarkus.mongodb.database` + `ReactiveMongoClient`. Pas de nouvelle dépendance.

### T1.2c — Timeouts Mongo — DONE (avec écart)
- 8 `application.properties` main (dev/prod), dans le bloc Mongo, bloc OIDC intact : `server-selection-timeout=5S`, `connect-timeout=3S`, `read-timeout=8S`, `max-pool-size=10`, `min-pool-size=0`, `max-connection-idle-time=60S`.
- **Écart :** `wait-queue-timeout` NON ajouté. Absent de `quarkus-mongodb-client` 3.40.1 (vérifié dans `quarkus-config-model.json` du jar : seuls connect, read, server-selection, max/min-pool-size, max-connection-idle-time, max-connection-life-time). Une clé inconnue = warning de config. Alternative : option URI `waitQueueTimeoutMS` dans la chaîne de connexion (env, hors repo).

### T1.1b-tri — DONE
- `find(query, Sort.by("_id"), params...)` dans `chargeSoldesBudgetMensuel` et `overrideLibellesOperations`.

### T1.1d — Virement B2 — DONE
- Lecture source (`getBudgetAndCompteActif`) + 1 seul `getCompteById(dest)` ; gardes avant toute écriture ; écriture source (recalcul + save) ; écriture cible ; si cible KO -> compensation (retrait de l'op source + recalcul + save) puis erreur d'origine propagée ; compensation KO -> ERROR avec ids source, destination, opération, erreur d'origine reste propagée.
- F2a : `DataNotFoundException` de `addOrReplaceOperation` propagée (le `tuple.mapItem1` sans effet est supprimé). F2d : plus d'exécution concurrente sur `ligneOperation`.
- Gardes : destination close -> `CompteClosedException` (mappée 406 dans `AbstractAPIExceptionsHandler`) ; source = destination -> `BadParametersException` (mappée 400, déjà utilisée pour « pas un virement interne » = paramètre invalide) ; destination null -> `DataNotFoundException` (404) au lieu de NPE (500).

### T1.2d — Libellés C3 — DONE
- Pipeline : `$match` -> `$unwind $listeOperations` -> `$group {_id:{libelle,categorieId,ssCategorieId}, n:{$sum:1}}`.
- Java : même normalisation (`deleteTagFromString`, `split("-")[0].trim()`), même filtre sous-catégories actives et même clé `libellé|cat|ssCat` ; fusion par `nb += n` ; reste de la chaîne (>1, meilleur par libellé, tri) inchangé.
- Port `getLibellesOperations` : le contrat des `Document` retournés change (groupes `{_id, n}` au lieu de `operationLibelleAttributes`). Aucun test existant ne l'utilise.
- Parité : à démontrer par le test de parité T1.3 (jeu `alignement-libelles.json`). Seul résiduel : en cas d'égalité de compteur entre deux (cat, ssCat) pour un même libellé, le choix dépend de l'ordre d'itération de la `HashMap` (déjà le cas avant, non déterministe par construction).

## Preuves

`mvn clean install -Dmaven.test.failure.ignore=true` (racine) : BUILD SUCCESS.

| Module | Tests | Échecs / erreurs |
|---|---|---|
| communs | 182 | 0 / 4 (tous Mongo Dev Services, pas de Docker : `MongoTimeoutException`, `TestMigrationRepository` x1, `TestMigrationRepositoryPersistence` x3, préexistants) |
| parametrages | 35 | 0 / 0 |
| utilisateurs | 36 | 0 / 0 |
| comptes | 112 | 0 / 0 |
| operations | 97 | 0 / 0 |

Non exécuté faute de Docker : `reserver`, `finaliser*`, V002 (`listIndexes`/`createIndex`), `majDernierAcces` (`updateOne`), pipeline `$group`, tri Sort. Leur validation repose sur la CI `build-on-all` et les tests d'intégration T1.3.

## Incident de commit
Le premier commit de la piste A avait embarqué le fichier du plan 004 (modification de MAINa déjà indexée). Corrigé avant tout push : `git reset --soft HEAD~1` puis `git reset HEAD -- <plan>` et nouveau commit (c86abc6). Le plan et `plans/README.md` restent modifiés, non commités ; ADR 006/007 non suivis, non commités.
