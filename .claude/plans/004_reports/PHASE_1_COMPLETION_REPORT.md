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

## Ajustement Gate #2 (4e passage)

Gate #2 : AJUSTER (décision 👤). Trois commits, non poussés.

| Commit | Contenu |
|---|---|
| `cfcfb43` `fix(operations)` | Absence vs erreur technique, lectures constantes sur écart de mois, retrait du repli par champs, écart > 24 mois, garde `mois` null, tests |
| `eba86c9` `fix(migrations)` | WARN en double supprimé (`MigrationRepository.finaliser` le conserve, `MongoMigrationRunner` ne le répète plus) |
| `9c4a85d` `refactor(operations)` | Nettoyage compensation : commentaires `etat == null`, `echecReinsertion` sans `count`, `idBudget` inutilisé, helper `indexOperation` |

### Points traités (commit `cfcfb43`)
1. **Absence vs erreur** : `chargeBudgetMensuel(compte, mois, année)` ne lève `BudgetNotFoundException` que si le document est absent. Toute autre erreur remonte avec son type d'origine (plus d'enveloppe en `BudgetNotFoundException`). Choix : `AbstractBDDExceptionsHandler` mappe `MongoClientException` (config, timeout, serveur indisponible) en 503 ; envelopper en `BudgetNotFoundException` donnerait un 404 faux, et créer une exception technique ne ferait que dupliquer ce mapping. Les autres `MongoException` suivent le 500 par défaut. Dans `BudgetService`, seul `onFailure(BudgetNotFoundException.class)` déclenche `initNewBudget` : une erreur technique ne peut plus aboutir au `persistOrUpdate` d'un budget vierge.
2. **`recalculSoldeAFinMoisPrecedent`** : seule `BudgetNotFoundException` vaut « premier mois » ; une erreur technique remonte.
3. **`chargeDernierBudgetAvant`** : absence = `Uni` à valeur `null` (compte inactif : `BudgetNotFoundException`, 404) ; erreur technique remonte. Javadoc du port mise à jour.
4. **Écart de k mois** : `dernierBudget` est passé à `initialiserDepuisDernierBudget` puis `creerMoisIntermediaires` (récursion sur l'objet en mémoire, aucune relecture ni nouvel appel `chargeDernierBudgetAvant`). Les mois intermédiaires sont créés déjà clos (opérations prévues reportées, #115) et sauvegardés une fois. La clôture du dernier budget se fait en mémoire (`appliquerEtatBudget`, extrait de `setBudgetActif`), sans `chargeBudgetMensuel(id)`.
5. **Garde NPE** : écart calculé depuis `annee`/`mois` du dernier budget ; `mois` null → `AbstractBusinessException` explicite (« n'a pas de mois »), sans repli sur l'`_id`. Raison : le repli par champs est retiré (point 6), et un `mois` null sur un document lu par `_id` est une donnée corrompue qu'il vaut mieux signaler que masquer.
6. **Repli défensif retiré** : `chargeBudgetParChamps` et `chargeDernierBudgetAvantParChamps` supprimés ; restent la lecture par `_id` et, pour le dernier budget, égalité `idCompteBancaire` + plage d'`_id`. Plus de log de repli.
7. **Écart > 24 mois** : mois intermédiaires non créés, WARN, solde du dernier budget connu repris (`recalculSoldes` en mémoire, dernier budget non modifié ni clos). Aucun budget précédent : budget vierge à solde 0.

### Décompte des appels au dépôt (initialisation du mois M absent)
| Cas | Lectures | Écritures |
|---|---|---|
| Aucun budget antérieur | 2 (`chargeBudgetMensuel(M)`, `chargeDernierBudgetAvant(M-1)`) | 1 (M) |
| Écart 0 (M-1 existe, actif) | 2 | 2 (clôture M-1, M) |
| Écart k (1 ≤ k ≤ 24) | 2 | k + 2 (clôture du dernier budget, k mois créés, M) ; k + 1 si le dernier budget est déjà clos |
| Écart 3 (test) | 2 | 5 |
| Écart 24 (test) | 2 | 26 |
| Écart > 24 | 2 | 1 (M) |

Avant : à chaque niveau, relecture de M-j, un `chargeDernierBudgetAvant` et un `setBudgetActif` (lecture + écriture) : lectures proportionnelles à k, 2 écritures par mois créé.

### Tests (`BudgetServiceTest`, 33 tests, 0 échec)
Adaptés : écart de 3 mois (1 appel `chargeDernierBudgetAvant`, 0 lecture par id, 5 sauvegardes) et de 24 mois (26 sauvegardes). Ajoutés : écart de 25 mois (solde 500 repris, ancien budget inchangé) ; erreur technique sur M (propagée, aucune sauvegarde, aucun `chargeDernierBudgetAvant`) ; erreur technique sur le dernier budget (propagée, aucune sauvegarde) ; erreur technique sur M-1 dans le recalcul (propagée, aucune sauvegarde) ; dernier budget sans `mois` (`AbstractBusinessException`).

### Risques
- **R5** : `chargeBudgetMensuel(String idBudget)` (lecture par id seule, utilisée par `setBudgetActif`, `getBudgetMensuel(id)`, `isBudgetActif`) enveloppe encore toute erreur en `BudgetNotFoundException` (404). Hors périmètre demandé, signalé.
- **R6** : un `mois` null sur le dernier budget donne une `AbstractBusinessException` non typée, rendue en 503 par `AbstractAPIExceptionsHandler`.
- **R7** : une erreur Mongo hors `MongoClientException` (ex. `MongoCommandException`) n'est pas mappée par `AbstractBDDExceptionsHandler` : 500 par défaut.
- **R8** : en cas d'écart de k mois, un échec d'écriture en cours de chaîne laisse les mois déjà créés en base (clos) ; le rejeu repart du dernier mois existant (idempotent).
- **R9** : si le dernier budget d'un écart > 24 mois est encore actif, il reste actif (non clos, pas de report d'opérations).

### Preuves
`mvn clean install -Dmaven.test.failure.ignore=true` (racine) : BUILD SUCCESS. communs 186 tests (4 erreurs `MongoTimeoutException` sans Docker : `TestMigrationRepository` 1, `TestMigrationRepositoryPersistence` 3, préexistantes), parametrages 35, utilisateurs 36, comptes 112, operations 108 : 0 échec / 0 erreur hors communs.
`git diff --name-only 24e359f HEAD` : `communs/.../MongoMigrationRunner.java`, `operations/.../BudgetService.java`, `.../IOperationsRepository.java`, `.../OperationDatabaseAdaptor.java`, `.../BudgetServiceTest.java`.

## Ajustement Gate #2 (5e passage)

Gate #2 : AJUSTER (décision 👤). Commit `37988e6` `fix(operations)`, non poussé.

### Points traités
1. **Écrasement concurrent de M-1** : `cloreBudgetSiActif` relit M-1 par id (`chargeBudgetMensuel(id)`) juste avant la clôture, applique PREVUE -> REPORTEE sur la version relue, recalcule et l'enregistre (comme l'ancien `setBudgetActif`). Si la version relue est déjà close, elle n'est pas réécrite. Choix : relecture + sauvegarde plutôt que `$set` ciblé. Un `$set` ciblé sur `listeOperations` écraserait lui aussi une opération ajoutée entre-temps, car la clôture modifie l'état de toutes les opérations prévues ; il aurait aussi exigé une nouvelle méthode sur le port. La relecture ne laisse que la fenêtre lecture-écriture de la clôture, comme toute écriture du service. Test : une opération ajoutée entre l'instantané et la clôture est conservée, reportée, et reprise dans le nouveau budget.
2. **`mois` null** : nouvelle `DonneeCorrompueException` (communs), mappée en **500** dans `AbstractAPIExceptionsHandler`. Aucune exception existante ne mappe vers 500 : `AbstractBusinessException` donne 503 (fallback), les autres 400/404/406/401/403. Plus petite exception possible : une classe de 10 lignes et une branche du mapping. Gardes : `recalculSoldeAFinMoisPrecedent` (M existant), `initialiserDepuisDernierBudget` (écart), `creerMoisIntermediaires` (précédent, puis version clôturée), `cloreBudgetSiActif` (instantané, puis version relue active).
3. **NPE du log de debug** : `OperationDatabaseAdaptor` : helper `logReceptionBudget` conditionné à `isDebugEnabled()` et null-safe sur `getListeOperations()`, utilisé par les deux `chargeBudgetMensuel`.
4. **Erreur passagère sur M-1 à l'affichage de M** (décision 👤) : toute erreur autre que `BudgetNotFoundException` est loggée en ERROR ; M est servi avec le `soldeAtFinMoisPrecedent` stocké et **n'est pas enregistré** (indicateur `ChargementBudget.aSauvegarder`). L'absence de M-1 reste « premier mois » et M est enregistré. Effet de bord : pour le compte de destination d'un virement, `ecrireOperationVirementCible` enregistre le budget après ajout de l'opération, avec le solde stocké : pas de corruption, solde de M-1 simplement non rafraîchi.
5. **R5** (décision 👤) : `chargeBudgetMensuel(String)` ne lève `BudgetNotFoundException` (404) que sur absence (`firstResult` null) ; les erreurs Mongo remontent. Callers vérifiés (`setBudgetActif`, `getBudgetMensuel(id)`, `isBudgetActif`, `getBudgetAndCompteActif`) : aucun `recover` sur toute erreur, le seul `onFailure(BudgetNotFoundException.class)` restant est celui de `chargerBudgetMensuelSurCompteActif` (M absent) et de `recalculSoldeAFinMoisPrecedent` (M-1 absent). Contrat documenté dans `IOperationsRepository`.
6. **Écart > 24 mois** : inchangé (reprise du solde seul).

### Codes HTTP par cas
| Cas | HTTP |
|---|---|
| Budget absent par id (`getBudgetMensuel(id)`, `isBudgetActif`, `setBudgetActif`) | 404 |
| Erreur Mongo par id : `MongoConfigurationException`, `MongoTimeoutException`, `MongoServerUnavailableException` | 503 |
| Erreur Mongo par id : `MongoClientException` générique | 500 (branche par défaut de `AbstractBDDExceptionsHandler`) |
| Erreur Mongo hors `MongoClientException` (`MongoSocketReadTimeoutException`, `MongoCommandException`...) | 500 (aucun mapper) |
| `mois` null (budget lu, dernier budget, relu) | 500 (`DonneeCorrompueException`) |
| Erreur technique sur M-1 à l'affichage de M existant | 200, M servi sans enregistrement, log ERROR |

### Décompte des appels au dépôt (initialisation du mois M absent, après relecture)
| Cas | Lectures | Écritures |
|---|---|---|
| Aucun budget antérieur | 2 | 1 (M) |
| Écart 0, M-1 actif | 3 (`chargeBudgetMensuel(M)`, `chargeDernierBudgetAvant(M-1)`, relecture de M-1 par id) | 2 (clôture M-1, M) |
| Écart k (1 ≤ k ≤ 24), dernier budget actif | 3 | k + 2 |
| Écart k, dernier budget déjà clos | 2 | k + 1 |
| Écart 3 (test) | 3 | 5 |
| Écart 24 (test) | 3 | 26 |
| Écart > 24 | 2 | 1 (M) |
| Affichage de M existant, M-1 lu | 2 | 1 |
| Affichage de M existant, erreur technique sur M-1 | 2 | 0 |

### Tests (`BudgetServiceTest`, 118 tests au module, 0 échec)
Adaptés : écarts de 3 et 24 mois (1 relecture par id du dernier budget). Remplacé : erreur technique sur M-1 (M servi, 0 sauvegarde, trace ERROR capturée). Ajoutés : relecture avant clôture (opération ajoutée conservée et reportée, instantané non enregistré) ; version relue déjà close (pas de réécriture) ; `mois` null sur M existant, sur le dernier budget, sur la version relue active, sur la version relue close (chacun : `DonneeCorrompueException`, statut 500 via `APIExceptionsHandler`, 0 sauvegarde) ; R5 : absence -> `BudgetNotFoundException` 404 et erreur Mongo propagée (503 via `SPIExceptionsHandler`) sur `getBudgetMensuel(id)`, `isBudgetMensuelActif`, `setBudgetActif`. `communs` : test du mapping 500.

### Risques
- **R10** : un `MongoClientException` générique et toute `MongoException` hors `MongoClientException` (dont `MongoSocketReadTimeoutException`, levée par le read-timeout du driver) donnent 500 et non 503. Non élargi, signalé.
- **R11** : si M-1 est absent à la relecture avant clôture (suppression concurrente), l'initialisation de M échoue en 404.
- **R12** : les tests de `chargeBudgetMensuel(String)` au niveau adaptateur (absence -> 404, erreur -> propagée) ne sont pas exécutables sans Docker ; couverts au niveau service avec dépôt mocké.

### Preuves
`mvn clean install -Dmaven.test.failure.ignore=true` (racine) : BUILD SUCCESS. communs 187 tests (4 erreurs `MongoTimeoutException` sans Docker : `TestMigrationRepository` 1, `TestMigrationRepositoryPersistence` 3, préexistantes), parametrages 35, utilisateurs 36, comptes 112, operations 118 : 0 échec / 0 erreur hors communs.
`git diff --name-only 9c4a85d HEAD` (avant le commit docs) : `communs/.../AbstractAPIExceptionsHandler.java`, `communs/.../DonneeCorrompueException.java`, `communs/.../TestAbstractAPIExceptionsHandlerComplement.java`, `operations/.../BudgetService.java`, `.../IOperationsRepository.java`, `.../OperationDatabaseAdaptor.java`, `.../BudgetServiceTest.java`.

## Ajustement Gate #2 (6e passage)

Gate #2 : AJUSTER (décision 👤). Commits `fix(operations)` et `fix(communs)`, non poussés. Le fichier non suivi `_nouveaux_tests.txt` n'est ni modifié ni commité.

### Points traités
1. **Mode dégradé réservé à l'affichage.** Appelants de `chargerBudgetMensuelSurCompteActif` : `getBudgetMensuel(idCompte, mois, annee)` (GET, affichage) et `ecrireOperationVirementCible` (écriture, compte destination du virement). Aucun autre chemin d'écriture n'utilise ce chargement (les autres écritures passent par `getBudgetAndCompteActif`, qui lit par id sans recalcul de M-1). Choix : **paramètre** `ModeChargement { AFFICHAGE, ECRITURE }` plutôt qu'une méthode distincte : le pipeline (lecture de M, init, recalcul, sauvegarde conditionnelle) est identique, seule la tolérance d'erreur sur M-1 change ; une seconde méthode dupliquerait ~20 lignes et pourrait diverger. Un enum plutôt qu'un booléen pour la lisibilité aux sites d'appel. En `ECRITURE`, l'erreur sur M-1 remonte ; `virementInterneSequentiel` la rattrape déjà (`recoverWithUni`), compense la source, puis propage l'erreur d'origine.
2. **Rattrapage restreint.** `recalculSoldeAFinMoisPrecedent` ne rattrape (mode `AFFICHAGE` seulement) que `com.mongodb.MongoException` et ses sous-classes. `DonneeCorrompueException`, NPE et autres exceptions remontent. Soldes de M ou de M-1 absents (ou M-1 nul) : `DonneeCorrompueException` (500) au lieu d'une NPE.
3. **R10.** `AbstractBDDExceptionsHandler` devient `ExceptionMapper<MongoException>` : toute `MongoException` donne 503 (ordre inchangé : configuration, timeout, serveur indisponible, puis repli 503). Les `SPIExceptionsHandler` des 4 modules sont adaptés à la nouvelle signature.
4. **Repli de `AbstractAPIExceptionsHandler`.** Le log disait `[500]` pour un statut 503. Trois tests existants (`APIExceptionsHandlerTest` de comptes, parametrages, utilisateurs) et `TestAbstractAPIExceptionsHandlerComplement` imposent 503 : le **statut 503 est conservé et le log est aligné sur `[503]`**. Aucune exception de production ne tombe dans ce repli (les 7 sous-classes d'`AbstractBusinessException` sont mappées explicitement). Aucun changement de code HTTP de ce côté.

### Codes HTTP modifiés (observables par l'IHM)
| Cas | Avant | Après |
|---|---|---|
| `MongoException` hors `MongoClientException` (`MongoSocketReadException`, `MongoSocketReadTimeoutException`, `MongoCommandException`...) | 500 (aucun mapper) | 503 |
| `MongoClientException` générique | 500 | 503 |
| Virement : erreur Mongo sur M-1 du compte destination | 200 (M enregistré avec solde périmé) | 503, source compensée |
| M ou M-1 sans soldes | NPE (500 non typé) | 500 (`DonneeCorrompueException`) |
| Affichage, erreur Mongo sur M-1 | 200 (M servi, non enregistré) | inchangé |
| Repli `AbstractBusinessException` | 503 | 503 (log aligné) |

L'IHM (`ClientHTTP.service.ts`) ne traite que 403 et « non 2xx » : 500 -> 503 n'a pas d'effet visible sur elle.

### Tests
`BudgetServiceTest` (46 tests) : virement avec erreur technique sur M-1 de la destination (échec 503, `compenseOperation` appelé une fois, une seule sauvegarde = la source, aucune sauvegarde de M destination) ; M-1 sans soldes -> 500, aucune sauvegarde ; NPE sur M-1 remontée, pas de mode dégradé. Test existant d'affichage dégradé inchangé. `communs` : `TestAbstractBDDExceptionsHandler` (`MongoSocketReadException`, `MongoCommandException`, `MongoClientException` générique, configuration, timeout -> 503) ; tests SPI des 4 modules : générique 500 -> 503. Repli : `testFallback503` inchangé.

### Risques
- **R2 bis** (accepté, 👤) : écriture concurrente sur M-1 entre la relecture et l'enregistrement de la clôture écrasée ; correction par verrou optimiste en Phase 4 / T4.2. Documenté dans l'ADR-007.
- Un `MongoException` en écriture sur le compte destination est maintenant bloquant pour le virement (comportement voulu) : un incident passager sur M-1 refuse le virement au lieu de l'enregistrer avec un solde périmé.
- Le texte de réponse d'une `MongoException` générique n'est plus le message d'origine mais « Erreur d'accès à la base de données » (pas de fuite de détail interne).


### Preuves
`mvn clean install -Dmaven.test.failure.ignore=true` (racine) : BUILD SUCCESS. communs 191 tests (4 erreurs `MongoTimeoutException` sans Docker : `TestMigrationRepository` 1, `TestMigrationRepositoryPersistence` 3, préexistantes), parametrages 35, utilisateurs 36, comptes 112, operations 121 : 0 échec / 0 erreur hors communs.
