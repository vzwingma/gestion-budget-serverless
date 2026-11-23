# Correctif phase 3 : Documentation du profil CI sans Mongo

**Plan :** `.claude/plans/006_upgrade_quarkus4_jackson3.plan.md`  
**Responsable :** 🟣 DOCly  
**Date début :** 2026-11-23  
**Date fin :** 2026-11-23  
**Statut :** ✅ DOCUMENTATION COMPLÉTÉE — Gate #4 en attente

## Audit et tâches

- Gate #3 correctif GO confirmé dans le journal du plan ; rapports correctifs des phases 1 et 2 consultés.
- T3.1 : `README.md` et `docs/ARCHITECTURE.md` décrivent désormais la propriété système Surefire
  `quarkus.mongodb.devservices.enabled=false` limitée aux tests avec `ci-sans-mongo`, les cinq exclusions
  préexistantes, le comportement par défaut inchangé et le contexte du blocage Sonar sans causalité présumée.
- T3.2 : ce rapport prépare le relais. Plan et index restent inchangés : plan en cours, Gate #4 en attente.
  Aucun nouvel ADR prévu par le plan. Les instructions Claude consultées ne nécessitent pas de changement
  pour ce correctif.

## Vérifications

- Lecture du POM : propriété `false` dans le profil Surefire `ci-sans-mongo` et cinq exclusions confirmées.
- Aucun lien ajouté ou modifié ; aucune vérification exhaustive des liens préexistants ou HTTP exécutée.
- Preuves rapportées par les phases précédentes, non rejouées ici : sonde `@QuarkusTest` en copie isolée
  pour `ci-sans-mongo` et `sonar,ci-sans-mongo` : vert → mutation détectée → vert après restauration ;
  propriété système et configuration MicroProfile à `false` en présence du correctif.
- POM effectifs : 18 vérifications rapportées, propriété absente sans profil et présente avec profil,
  notamment combiné à `sonar` et `native`.
- Builds locaux rapportés, sans `clean` :
  `mvn install -B -Pci-sans-mongo --file pom.xml` et
  `mvn verify -B -Psonar,ci-sans-mongo --file pom.xml` : chacun 817 tests, sans échec ni erreur.
  `mvn install -B -Pci-sans-mongo --file communs/pom.xml` : 260 tests, sans échec ni erreur.
- Aucune commande Maven ni sonde exécutée durant cette phase documentaire. Le commit `91d3b62`
  contenant le correctif a été poussé ; les runs CI `45261325421` et `45261337008` sont en cours.

## Synthèse

**Tâches :** T3.1 et préparation T3.2 complétées ; initiative non clôturée.  
**Risques :** CI avec Docker en cours après publication du correctif ; effet sur le runner et disparition du blocage
Sonar non confirmés ; causalité non établie. Les cinq classes Mongo réel restent non validées. La sonde
locale vérifie la configuration reçue, pas l'absence de conteneur sur un runner Docker. Les autres limites
de phase 3, dont la couverture de branches ciblées à 52 %, restent applicables.  
**Périmètre :** aucun code, test, POM, workflow ou fichier d'environnement modifié ; changement indépendant
utilisateur dans `.github/workflows/build-on-master.yml` conservé. Aucun commit ni push.  
**Questions pour 👤 via MAINa :** déterminer le suivi de la validation Mongo réelle et autoriser, le cas
échéant, publication et nouveau run CI ; ne pas présumer de leur résultat.  
**Prochaine étape :** relais à MAINa pour Gate #4, avec validation CI distante restant à effectuer avant clôture.
