# Phase 1 : Adapter le code et débloquer les builds

**Plan :** `.claude/plans/006_upgrade_quarkus4_jackson3.plan.md`  
**Responsable :** 🔵 DEVon ; vérifications complémentaires ⚫ MAINa  
**Date début :** 2026-11-23  
**Date fin :** à déterminer  
**Statut :** 🔄 EN_COURS — profil CI sans MongoDB approuvé, builds complets non validés

## T1.1 — Migrer les usages de production concernés

**Statut :** partiel, compilation vérifiée ; contrats à vérifier.

- `communs/.../jwt/JWTAuthToken.java` : annotations databind migrées vers Jackson 3.
- `communs/.../jwt/JWTAuthPayload.java` : ordre historique explicite des claims ; le test existant de JWT encodé, en échec avant correction, passe après correction.
- `operations/.../business/model/budget/BudgetMensuel.java` et `operations/.../business/model/operation/LigneOperation.java` : imports des annotations et des sérialiseurs de dates adaptés à Jackson 3 ; les formats effectifs restent à vérifier.
- Les quatre auxiliaires `OidcTokenForge.java` des services sont migrés vers les API Jackson 3 afin de compiler les tests existants.
- À la demande du 👤, `BudgetsResourceTest.testGetBudgetsUtilisateurByParamsKO` attend désormais HTTP 400, conformément au mapper des paramètres invalides.

## T1.2 — Vérifier les builds existants

Toutes les commandes ci-dessous ont été exécutées sans `clean`, afin de préserver les répertoires de build existants. Les commandes CI exactes avec `clean` restent donc non vérifiées.

| Vérification | Résultat |
|---|---|
| `mvn install -B -DskipTests --file communs/pom.xml` puis `mvn install -B -DskipTests --file pom.xml` | Succès de compilation des six modules et de leurs sources de test ; tests non exécutés. |
| `mvn -B --file communs/pom.xml -Dtest=TestJWTUtils test` | 22 tests réussis, aucun échec ni erreur. |
| `mvn install -B --file communs/pom.xml` | Échec : 273 tests, 0 échec, 13 erreurs, 0 ignoré ; les trois classes de migrations ne peuvent pas joindre MongoDB sur `127.0.0.1:27017`. |
| `mvn install -B -Dtest=*,!TestMigrationRepository,!TestMigrationRepositoryConcurrence,!TestMigrationRepositoryPersistence --file pom.xml` | Diagnostic partiel : 260 tests `communs` et 72 `parametrages` réussis ; `utilisateurs` : 83 tests rapportés, 1 erreur Docker/Testcontainers, 2 ignorés ; `comptes` et `operations` non exécutés. Les trois classes Mongo nommées ont été exclues. |
| `mvn install -B -fae -pl comptes,operations --file pom.xml` | `comptes` : 143 tests réussis ; `operations` : 279 tests rapportés, 1 échec HTTP, 1 erreur Docker/Testcontainers, 18 ignorés. |
| `mvn -B -pl operations -Dtest=BudgetsResourceTest#testGetBudgetsUtilisateurByParamsKO test --file pom.xml` | Après adaptation du test HTTP : 1 test réussi, 0 échec, 0 erreur. Des messages indiquent l'indisponibilité de Docker et MongoDB. |
| Lint | Aucun plugin de lint courant identifié dans les POM et workflows consultés ; analyse Sonar distante non exécutée. |
| `git diff --stat` | 9 fichiers suivis, 28 insertions, 24 suppressions ; le plan et ses rapports non suivis ne figurent pas dans cette statistique. |

Java 25 est disponible via Maven 3.9.3. Aucun serveur MongoDB local, exécutable `mongod`, service MongoDB ou commande Docker n'a été détecté. Une requête de métadonnées GitHub Packages a renvoyé 401 pendant le test JWT, sans empêcher son succès.

## Points bloquants

1. Les builds complets et les tests d'intégration nécessitent MongoDB et Docker/Testcontainers, indisponibles localement. Le build racine complet n'a pas été validé.
2. Le 👤 indique que Docker et MongoDB sont également absents de la CI/CD et a approuvé l'isolation explicite des cinq classes Mongo réel avec exécution séparée. Une suite sans Mongo ne valide pas la persistance, les index ni l'atomicité Mongo ; le profil et les workflows restent à adapter et vérifier.
3. Les commandes CI exactes avec `clean`, les contrats JSON/dates, les builds natifs et la CI distante ne sont pas validés. Aucun déploiement, commit ou push des corrections n'a été effectué.
4. Les workflows de builds natifs et SAM utilisent également Docker ; isoler les tests Mongo ne suffit pas à les rendre compatibles avec une CI/CD sans Docker.

## Synthèse

**Tâches complétées :** 0/3 au regard des critères du plan.  
**Questions pour 👤 :** préciser les moyens disponibles pour les builds natifs sans Docker et l'exécution séparée des tests Mongo réels.  
**Prochaine étape :** lever les blocages, exécuter les vérifications, puis réaliser la revue avant Gate #2. La phase de nouveaux tests attend Gate #2 GO.
