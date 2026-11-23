# Correctif phase 1 : MongoDB Dev Services en CI

**Plan :** `.claude/plans/006_upgrade_quarkus4_jackson3.plan.md`  
**Responsable :** 🔵 DEVon ; vérifications complémentaires ⚫ MAINa  
**Date :** 2026-11-23  
**Statut :** ✅ COMPLÉTÉ — Gate #2 correctif en attente

## T1.4 — Désactiver MongoDB Dev Services dans le seul profil CI sans Mongo

- `pom.xml` : la configuration Surefire du profil `ci-sans-mongo` définit `quarkus.mongodb.devservices.enabled=false`.
- Les cinq exclusions Mongo préexistantes restent inchangées ; les profils et tests sans `ci-sans-mongo` ne reçoivent pas cette propriété.
- Revue manuelle du diff du POM : aucun défaut bloquant identifié. La revue automatique Copilot n'a pas été exécutée.

## Vérifications

| Vérification locale, sans `clean` | Résultat |
|---|---|
| `mvn install -B -Pci-sans-mongo --file communs/pom.xml` | Succès : 260 tests, aucun échec ni erreur. |
| `mvn install -B -Pci-sans-mongo --file pom.xml` | Succès : six modules, 817 tests, aucun échec ni erreur. |
| `mvn verify -B -Psonar,ci-sans-mongo --file pom.xml` | Succès : six modules, 817 tests, aucun échec ni erreur. |
| `mvn help:effective-pom` | 18 vérifications sur `communs`, `operations` et `utilisateurs`, sans profil, avec `sonar`, `native`, `ci-sans-mongo` et ses combinaisons. La propriété est absente sans `ci-sans-mongo`, présente à `false` avec ce profil ; les cinq exclusions, la propriété de logging et les propriétés JaCoCo sont conservées. |

MongoDB Dev Services avait réellement démarré dans le run Sonar annulé `45213370042`. Le dump montre le thread principal bloqué dans `NettyStream.write` lors de `MongoClientImpl.close` pendant un changement de profil Quarkus. La causalité exacte n'est pas démontrée ; le correctif élimine le démarrage Mongo automatique visé par le profil.

## Synthèse

**Tâches complétées :** 1/1 pour l'implémentation et la validation locale.  
**Limites :** Docker et le hang CI n'ont pas été reproduits localement ; la correction et Sonar doivent être vérifiés sur le runner après publication autorisée. Les cinq classes d'intégration Mongo restent exclues ; aucun résultat Mongo réel n'est revendiqué.  
**Autre changement :** un diff indépendant de l'utilisateur dans `.github/workflows/build-on-master.yml` est présent et n'a pas été modifié ni validé ici.  
**Prochaine étape :** Gate #2 correctif, puis vérifications complémentaires avec 🟢 QALvin après GO.
