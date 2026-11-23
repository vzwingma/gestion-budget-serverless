# Phase 2 : Vérifier les contrats et la non-régression

**Plan :** `.claude/plans/006_upgrade_quarkus4_jackson3.plan.md`  
**Responsable :** 🟢 QALvin ; exécution et preuves complémentaires ⚫ MAINa  
**Dates :** 2026-11-23  
**Statut :** ✅ COMPLÉTÉE — validation 👤 en attente

## T2.1 — Tester les contrats JSON et JWT

- `operations/src/test/java/io/github/vzwingma/finances/budget/serverless/services/operations/api/BudgetsResourceTest.java` : deux tests ajoutés. Ils vérifient les représentations JSON HTTP des dates et horodatages, leur désérialisation, ainsi que les dates nulles et absentes.
- Référence historique : le test temporaire `baselineDateFormats`, exécuté sur une copie isolée du commit pré-upgrade `43b769d` avec Jackson 2, a réussi. Les valeurs HTTP exactes des cinq champs de date ont ensuite été vérifiées sur Quarkus 4.
- JWT : les tests existants de `communs` vérifient notamment l'encodage historique exact, le décodage, les jetons mal formés, les signatures et les claims.
- Détection de régression démontrée dans une copie isolée du commit `4de9e84` incluant les nouveaux tests : suppression temporaire de `JsonPropertyOrder` → `TestJWTUtils#testEncode` échoue ; changement temporaire de format → `BudgetsResourceTest#budgetRetourneDatesLisiblesEtConserveLeursValeurs` échoue. Après restauration des deux mutations, les deux tests réussissent. Les sources du projet initial n'ont pas été modifiées pour ces expériences.

## T2.2 — Vérifier sécurité et compatibilité native

- Les tests existants ciblés couvrent notamment OIDC et ses erreurs, le cold start JWKS, `/_info`, les endpoints protégés et la propagation du jeton.
- Le run PR `45213370042` sur `4de9e84` a réussi les quatre jobs de build natif Mandrel, ainsi que `Build Communs`. Le workflow a également exécuté et réussi le déploiement QUA. Le job Sonar était encore en cours lors de la dernière vérification.
- Ces builds natifs portent sur le code poussé ; ils n'incluent pas les nouveaux tests non commités. La compatibilité Mongo réelle n'est pas validée : les cinq classes d'intégration Mongo restent exclues du profil CI conformément à l'accord 👤.

## Résultats

| Vérification | Résultat |
|---|---|
| Tests ciblés JWT, dates, OIDC, `/_info`, endpoints protégés et propagation avec `-Pci-sans-mongo` | 242 tests, 0 échec, 0 erreur, 0 ignoré. |
| `mvn -B verify -Psonar,ci-sans-mongo --file pom.xml` | Succès ; 817 tests, 0 échec, 0 erreur, 0 ignoré. Cette exécution précède l'ajout des trois assertions HTTP exactes dans un test déjà exécuté. |
| Deux tests de dates après ajout des assertions exactes | 2 tests, 0 échec, 0 erreur. |
| Test historique `baselineDateFormats` sur `43b769d` | 1 test réussi sur une copie isolée de la version pré-upgrade. |
| Mutation ordre JWT ; mutation format date | Chaque test ciblé échoue avec la mutation correspondante, puis réussit après restauration en copie isolée. |
| Couverture JaCoCo ciblée : `JWTAuthPayload`, `JWTAuthToken`, `BudgetMensuel`, `LigneOperation` et classes imbriquées | 93/101 lignes : **92,08 %** ; 26/50 branches : **52 %**. Le seuil de 80 % est atteint pour les lignes, pas pour les branches. |
| Couverture lignes des cinq modules | 87,89 % ; branches : 75,03 %. |

JaCoCo mesure le bytecode, pas directement les annotations ; certains éléments n'ont pas de lignes mesurables. Les fichiers de couverture utilisés ont été générés pendant l'exécution ; aucun `clean` local n'a été exécuté. Les cinq classes Mongo exclues ne sont ni comptées comme réussies, ni considérées comme une preuve de persistance, d'index ou d'atomicité Mongo.

## Synthèse

**Tâches complétées :** 2/2, avec les limites explicitement acceptées ou signalées ci-dessus.  
**Risques résiduels :** tests Mongo réel non exécutés ; couverture des branches ciblées sous 80 % ; Sonar distant encore en cours ; les nouveaux tests et rapports ne sont pas encore commités.  
**Questions pour 👤 :** valider ces limites avant la documentation.  
**Suite :** phase 3 après Gate #3 GO.
