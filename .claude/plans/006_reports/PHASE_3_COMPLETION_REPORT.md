# Phase 3 : Synchroniser la documentation et clôturer

**Plan :** `.claude/plans/006_upgrade_quarkus4_jackson3.plan.md`  
**Responsable :** 🟣 DOCly  
**Date début :** 2026-11-23  
**Date fin :** 2026-11-23  
**Statut :** ⚠️ DOCUMENTATION COMPLÉTÉE — clôture bloquée par la CI Sonar

## Tâches

### T3.1 — Mettre à jour la documentation dépendante

**Statut :** ✅  
**Fichiers :**
- `README.md` : Quarkus 4.0.0 et Java 25 ; commandes CI et exécution séparée Mongo ; cinq exclusions et limites ; prérequis Docker/Testcontainers/Mandrel.
- `docs/ARCHITECTURE.md` : versions POM, tests et exclusions, prérequis, commandes et pipeline ; correction des affirmations obsolètes sur Renovate et le contrôle de synchronisation.
- `renovate.json` consulté : aucun `customManager` ni fichier d'instructions suivi déclaré ; aucune instruction modifiée.

**Vérifications :** lecture du POM racine, des trois workflows CI, des deux ressources Testcontainers et des rapports des phases 1 et 2 ; recherche des cinq classes et lecture des sections modifiées. Versions Java 25 et Quarkus 4.0.0 et cinq exclusions conformes au POM ; les deux commandes CI correspondent à `build-on-all.yml`. Aucune commande Maven ni vérification réseau exécutée en phase 3 : aucun outil d'exécution de commandes disponible. Les commandes ciblées Mongo sont proposées, explicitement non testées ; Mongo réel reste à valider.

**Liens :** aucun lien nouveau ou modifié ; un lien obsolète vers le plan 002 a été retiré avec la description erronée. Existence vérifiée pour `docs/ARCHITECTURE.md`, son titre `🚀 Déploiement`, et `docs/adr/003-upgrade-java25-mandrel25.md`. Pas de vérification HTTP ou de vérification exhaustive des liens préexistants, faute d'outil d'exécution.

### T3.2 — Préparer la clôture

**Statut :** ✅ — préparation du relais ; Gate #4 reste en attente.  
**Fichiers et preuves :** ce rapport. Plan et index inchangés : statut global toujours « en cours » ; journal Gate #3 GO et Gate #4 en attente déjà présents. Aucun nouvel ADR prévu par le plan.

**État CI issu des rapports précédents :**
- Run branche `45213357903`, commit `4de9e84` : deux builds JVM CI réussis.
- Run PR `45213370042`, même commit : quatre jobs natifs Mandrel et `Build Communs` réussis ; déploiement QUA réussi. Le 👤 a annulé le run après un blocage de `Scan Sonar`. Le dump Quarkus indique une absence d'activité pendant 10 minutes ; le thread principal attend dans `NettyStream.write` lors de `MongoClientImpl.close` pendant un changement de profil `@QuarkusTest`. La cause exacte reste à confirmer.
- Dev Services MongoDB a effectivement démarré dans la CI et les migrations V001/V002 se sont exécutées, bien que les cinq classes d'intégration Mongo soient exclues. Une correction candidate est de désactiver explicitement Dev Services MongoDB dans le seul profil `ci-sans-mongo`, puis de valider la suite et le job CI ; cette correction n'est pas encore implémentée.
- Nouveaux tests de `BudgetsResourceTest` non commités : absents de ces runs CI. Tests locaux phase 2 : 242 ciblés réussis ; `mvn -B verify -Psonar,ci-sans-mongo --file pom.xml` réussi, 817 tests, avant trois assertions complémentaires ensuite testées séparément.

## Synthèse

**Tâches complétées :** 2/2 — documentation et préparation de clôture ; initiative non clôturée.  
**Critères :** documentation alignée sur le POM et les configurations consultées ; liens vérifiés dans la limite indiquée ; preuves et risques préparés pour Gate #4.  
**Risques :** Mongo réel non validé ; commandes ciblées proposées non exécutées ; couverture branches ciblées 52 % (< 80 %) en phase 2 ; Sonar CI annulé et non validé ; nouveaux tests non commités non couverts par la CI distante. Docker requis pour natif CI et SAM. La synchronisation automatique des versions documentaires n'est pas configurée dans `renovate.json` ; vigilance manuelle.  
**Questions pour 👤 :** via MAINa, décider de la validation Mongo réelle et du suivi des limites ; toute publication ou tout nouveau déploiement exige l'accord approprié.  
**Prochaine étape :** faire valider la reprise du code et la correction candidate, puis revérifier la CI avant Gate #4 ; ne pas clôturer avant validation.
