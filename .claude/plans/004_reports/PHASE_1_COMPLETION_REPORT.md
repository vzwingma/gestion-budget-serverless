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

## Ajustement Gate #2

Gate #2 : AJUSTER (décision 👤). Points 1-9 :

| Point | Sujet | Statut | Par | Commit |
|---|---|---|---|---|
| 1 | Encodage `dernierAcces` | Conservé (symétrique du codec `UtilisateurPanacheCodec`), justification ajoutée | 👤 | `5ae88f4` |
| 2 | Compensation du virement : restaurer la ligne d'origine en cas de modification, supprimer seulement en cas de création | Fait : ligne d'origine capturée avant l'écriture source | DEVon | `012dee5` |
| 3 | Ordre des migrations | Fait | 👤 | `5ae88f4` |
| 4 | Finalisation des migrations | Fait | 👤 | `5ae88f4` |
| 5 | Renouvellement du bail | Pas de changement (documenté dans l'ADR-006) ; `wait-queue-timeout` abandonné | - | - |
| 6 | Timeout / construction d'index (V002) | Traité par 👤 en `5ae88f4`, sans timeout dédié (voir risque R1). `getLibellesOperations` : `maxTime` serveur de 7 s (< `read-timeout` 8 s) via `AggregateOptions` | 👤 (V002) / DEVon (libellés) | `5ae88f4` / `012dee5` |
| 7 | Compte destination inconnu | Fait : `WebApplicationException` de statut 404 (client REST `getCompteById`) traduite en `DataNotFoundException` (404) ; autres erreurs inchangées | DEVon | `012dee5` |
| 8 | Portée de la compensation | Fait : mise à jour ciblée (`$pull` par id en création, `$set` positionnel `listeOperations.$[op]` filtré par `op._id` en modification) via le nouveau port `IOperationsRepository.compenseOperation` ; plus de `persistOrUpdate` du budget entier | DEVon | `012dee5` |
| 9 | `ErrorCategory` | Fait | 👤 | `5ae88f4` |

Autre : `getMatchedCount` traité par 👤 (`5ae88f4`).

### Compensation : ce qui est recalculé
- Réécrits depuis le snapshot mémoire du budget source (après retrait ou restauration + `recalculSoldes`) : `soldes`, `totauxParCategories`, `totauxParSSCategories`, `totauxParTypeCategories`, `dateMiseAJour`.
- Seule l'opération concernée est modifiée dans `listeOperations`. Les autres champs du budget (`actif`, `mois`, etc.) ne sont pas réécrits.
- Échec si le budget n'existe plus (`matchedCount == 0`) ; un échec de compensation reste loggé en ERROR sans masquer l'erreur d'origine.

### Risques
- **R1 (V002)** : la création d'index reste bornée par le `read-timeout` de 8 s (aucun timeout dédié). Sur timeout, la migration passe en ECHEC et est rejouée au démarrage suivant (construction poursuivie côté serveur). Non traité par un timeout dédié ; le driver n'expose pas de timeout par opération sur ce chemin.
- **R2 (soldes de compensation)** : soldes et totaux calculés sur le snapshot lu avant le virement. Une écriture concurrente sur une autre opération du même budget n'est pas écrasée dans `listeOperations`, mais n'est pas reflétée dans les soldes/totaux stockés jusqu'au prochain recalcul.
- **R3 (non testé)** : l'encodage d'une `LigneOperation` / `Soldes` / `TotauxCategorie` dans `Updates.set` (registre de codecs du client) et l'`arrayFilters` ne sont pas exécutés (pas de Docker) : à valider par test d'intégration (T1.3). Aucun test unitaire de `virementInterneSequentiel` n'existe (rôle QALvin).
- **R4** : `maxTime` 7 s interrompt côté serveur (`MaxTimeMSExpired`) l'agrégation des libellés sur un gros compte : l'appel échoue au lieu d'aboutir lentement.

#### 2e passage (2026-10-29)

Décisions 👤 au Gate #2 (2e passage) :
- **Point 5 accepté = R2** : soldes et totaux de la compensation recalculés sur l'instantané mémoire ; pas de contrôle optimiste sur `dateMiseAJour` ; à réévaluer si le problème est observé. Voir [ADR-007](../../../docs/adr/007-coherence-virement-ecriture-sequentielle-compensee.md), « Limites connues ».
- **V002 accepté (R1)** : construction d'index bornée par `read-timeout=8S`, rejeu au démarrage suivant (migration idempotente), pas de timeout dédié. Voir [ADR-006](../../../docs/adr/006-reservation-atomique-migrations-mongodb.md), « Limites connues ». La chaîne s'arrête sur un ECHEC ou un échec de finalisation (correctif en cours) et continue si une version est passée en SUCCES par un autre runner.
- Compensation ciblée : `$push` si la ligne a disparu en modification (correctif en cours, DEVon).

### Preuves
`mvn clean install -Dmaven.test.failure.ignore=true` (racine) : BUILD SUCCESS. communs 182 tests (4 erreurs Mongo sans Docker, préexistantes), parametrages 35, utilisateurs 36, comptes 112, operations 97 : 0 échec / 0 erreur hors communs.
Périmètre : `git diff --name-only f14cea6 HEAD` = 3 fichiers `operations/src/main` (`BudgetService`, `IOperationsRepository`, `OperationDatabaseAdaptor`).
