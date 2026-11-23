# Phase 1 : Adapter le code et débloquer les builds

**Plan :** `.claude/plans/006_upgrade_quarkus4_jackson3.plan.md`  
**Responsable :** 🔵 DEVon ; vérifications complémentaires ⚫ MAINa  
**Date début :** 2026-11-23  
**Date fin :** 2026-11-23  
**Statut :** ✅ COMPLÉTÉE — validation 👤 en attente

## T1.1 — Migrer les usages de production concernés

**Statut :** adaptations compilées ; contrats détaillés à vérifier en phase 2.

- `communs/.../jwt/JWTAuthToken.java` : annotations databind migrées vers Jackson 3.
- `communs/.../jwt/JWTAuthPayload.java` : ordre historique explicite des claims ; le test existant de JWT encodé, en échec avant correction, passe après correction.
- `operations/.../business/model/budget/BudgetMensuel.java` et `operations/.../business/model/operation/LigneOperation.java` : imports des annotations et des sérialiseurs de dates adaptés à Jackson 3 ; les formats effectifs restent à vérifier.
- Les quatre auxiliaires `OidcTokenForge.java` des services sont migrés vers les API Jackson 3 afin de compiler les tests existants.
- À la demande du 👤, `BudgetsResourceTest.testGetBudgetsUtilisateurByParamsKO` attend désormais HTTP 400, conformément au mapper des paramètres invalides.

## T1.2 — Vérifier les builds existants

Les commandes locales ci-dessous ont été exécutées sans `clean`, afin de préserver les répertoires de build existants. Les commandes CI avec `clean` ont ensuite réussi dans le run de branche.

| Vérification | Résultat |
|---|---|
| `mvn install -B -DskipTests --file communs/pom.xml` puis `mvn install -B -DskipTests --file pom.xml` | Succès de compilation des six modules et de leurs sources de test ; tests non exécutés. |
| `mvn -B --file communs/pom.xml -Dtest=TestJWTUtils test` | 22 tests réussis, aucun échec ni erreur. |
| `mvn install -B --file communs/pom.xml` | Échec : 273 tests, 0 échec, 13 erreurs, 0 ignoré ; les trois classes de migrations ne peuvent pas joindre MongoDB sur `127.0.0.1:27017`. |
| `mvn install -B -Dtest=*,!TestMigrationRepository,!TestMigrationRepositoryConcurrence,!TestMigrationRepositoryPersistence --file pom.xml` | Diagnostic partiel : 260 tests `communs` et 72 `parametrages` réussis ; `utilisateurs` : 83 tests rapportés, 1 erreur Docker/Testcontainers, 2 ignorés ; `comptes` et `operations` non exécutés. Les trois classes Mongo nommées ont été exclues. |
| `mvn install -B -fae -pl comptes,operations --file pom.xml` | `comptes` : 143 tests réussis ; `operations` : 279 tests rapportés, 1 échec HTTP, 1 erreur Docker/Testcontainers, 18 ignorés. |
| `mvn -B -pl operations -Dtest=BudgetsResourceTest#testGetBudgetsUtilisateurByParamsKO test --file pom.xml` | Après adaptation du test HTTP : 1 test réussi, 0 échec, 0 erreur. Des messages indiquent l'indisponibilité de Docker et MongoDB. |
| `mvn install -B -Pci-sans-mongo --file communs/pom.xml` | Succès : 260 tests exécutés, aucun échec ni erreur. |
| `mvn install -B -Pci-sans-mongo --file pom.xml` | Succès : six modules, 815 tests exécutés, aucun échec ni erreur ; cinq classes Mongo réel exclues par le profil. |
| `mvn help:effective-pom` sur `communs`, `utilisateurs`, `operations` | 12 vérifications réussies : zéro exclusion sans profil ; cinq exclusions avec `ci-sans-mongo`, seul ou combiné avec `native` ou `sonar`. |
| CI `45213357903` sur `4de9e84` | Succès : jobs `build-communs` et `build`, avec les deux commandes CI `clean install` et le profil `ci-sans-mongo`. |
| Lint | Aucun plugin de lint courant identifié dans les POM et workflows consultés ; analyse Sonar distante non exécutée. |
| `git diff --stat 5f67648..HEAD` | 17 fichiers, 255 insertions, 32 suppressions. `git diff --check` signale des espaces de fin de ligne Markdown dans le plan et ce rapport. |

Java 25 est disponible via Maven 3.9.3. Aucun serveur MongoDB local, exécutable `mongod`, service MongoDB ou commande Docker n'a été détecté. Une requête de métadonnées GitHub Packages a renvoyé 401 pendant le test JWT, sans empêcher son succès.

## Points bloquants

1. Les tests d'intégration Mongo réel nécessitent MongoDB et Docker/Testcontainers, indisponibles localement. Ils restent exécutables sans le profil `ci-sans-mongo`, mais ne sont pas validés ici.
2. Le 👤 a approuvé l'isolation explicite des cinq classes Mongo réel avec exécution séparée. Le profil et les workflows ont été adaptés et les builds JVM locaux passent sans ces classes. Cette suite ne valide ni la persistance, ni les index, ni l'atomicité Mongo.
3. Le run de branche JVM est vert sur `4de9e84`. Le run PR `45213370042` est encore en cours ; les contrats JSON/dates et les builds natifs ne sont pas encore validés. Les commits `1ec6084` et `4de9e84` ont été poussés. Aucun déploiement n'a été effectué par les agents.
4. Les workflows de builds natifs et SAM utilisent également Docker ; isoler les tests Mongo ne suffit pas à les rendre compatibles avec une CI/CD sans Docker.

## Synthèse

**Tâches complétées :** 3/3 pour la phase de code ; les tests Mongo réel et le natif restent explicitement à valider séparément.  
**Revue du diff :** revue manuelle des modifications Java, POM et workflows ; aucun défaut bloquant identifié. La revue automatique Copilot n'a pas été exécutée.  
**Questions pour 👤 :** préciser les moyens disponibles pour les builds natifs sans Docker et l'exécution séparée des tests Mongo réels.  
**Prochaine étape :** lever les blocages, exécuter les vérifications, puis réaliser la revue avant Gate #2. La phase de nouveaux tests attend Gate #2 GO.
