# Correctif phase 2 : Vérifier MongoDB Dev Services en CI

**Plan :** `.claude/plans/006_upgrade_quarkus4_jackson3.plan.md`  
**Responsable :** 🟢 QALvin ; exécution complémentaire ⚫ MAINa  
**Date :** 2026-11-23  
**Statut :** ✅ VÉRIFICATIONS LOCALES COMPLÉTÉES — Gate #3 correctif en attente

## Vérifications

- Une sonde `@QuarkusTest` créée uniquement dans une copie isolée vérifie que la propriété système et la configuration MicroProfile reçues par Quarkus valent toutes deux `false`.
- Avec `ci-sans-mongo`, puis avec `sonar,ci-sans-mongo`, la sonde réussit : un test exécuté par commande, aucun échec ni test ignoré.
- Dans cette copie, la suppression temporaire de la propriété du POM fait échouer explicitement la sonde dans les deux profils : valeur système attendue `false`, obtenue `null`. Après restauration de la propriété, les deux commandes réussissent à nouveau. Aucun fichier du dépôt initial n'a été muté pour cette preuve.
- Les 18 vérifications de POM effectifs menées en phase 1 correctif montrent que la propriété est absente sans `ci-sans-mongo`, présente à `false` avec le profil, y compris combiné à `sonar` ou `native`. Les cinq exclusions et les propriétés de logging et JaCoCo sont conservées.
- Les builds locaux complets avec le profil CI, puis avec les profils Sonar et CI, ont chacun exécuté 817 tests sans échec ni erreur. Les cinq classes Mongo réel sont exclues par décision 👤.

## Limites et relais

La sonde a vérifié la valeur reçue par Quarkus, pas l'absence de démarrage d'un conteneur sur un runner disposant de Docker. La CI avec Docker et la disparition du blocage Sonar restent donc à vérifier après publication autorisée. Sans le profil, les tests Mongo restent configurés comme auparavant ; ils n'ont pas été exécutés contre Mongo réel.

La sonde temporaire n'est pas ajoutée au dépôt : une sonde durable hors découverte normale nécessiterait aussi une invocation CI explicite pour ne pas devenir une vérification silencieusement absente. La couverture de bytecode n'est pas pertinente pour cette propriété Maven.

**Résultats :** sonde avec et sans Sonar : vert → mutation détectée → vert après restauration ; builds locaux verts.  
**Risque :** lien causal avec le blocage CI non confirmé tant que le job distant n'a pas été rejoué.  
**Prochaine étape :** Gate #3 correctif, puis synchronisation documentaire après GO.
