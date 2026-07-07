package io.github.vzwingma.finances.budget.services.communs.migrations;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test d'intégration de {@link MigrationRepository} contre un vrai MongoDB (par opposition aux tests
 * de {@link MongoMigrationRunner} qui mockent entièrement le repository).
 * <p>
 * Aucune dépendance Testcontainers explicite requise : {@code quarkus-mongodb-panache} embarque son
 * propre mécanisme <a href="https://quarkus.io/guides/mongodb#dev-services">Dev Services</a>. En mode
 * {@code @QuarkusTest}, si aucune chaîne de connexion MongoDB n'est configurée (ce qui est le cas ici :
 * ni {@code communs} ni ce module de test ne définissent {@code quarkus.mongodb.connection-string}),
 * Quarkus démarre automatiquement un conteneur MongoDB jetable (via Docker/Podman) et y connecte
 * l'application le temps des tests. Le conteneur est neuf à chaque exécution de la suite de tests du
 * module — d'où l'hypothèse "base fraîche" du test ci-dessous.
 * <p>
 * <b>Prérequis</b> : Docker (ou Podman) disponible sur la machine exécutant les tests. Sans Docker, le
 * démarrage Quarkus réussit tout de même (Dev Services journalise un simple avertissement et retombe sur
 * la valeur par défaut {@code mongodb://localhost:27017}), mais la requête réelle de ce test échoue alors
 * avec un {@code com.mongodb.MongoTimeoutException} après ~30s (aucun serveur MongoDB à cette adresse) —
 * pas un échec silencieux, à valider alors en CI (runners GitHub Actions, Docker disponible nativement).
 * <p>
 * <b>Attention ordre d'exécution</b> : le conteneur Dev Services est partagé par toutes les classes
 * {@code @QuarkusTest} du module {@code communs} au sein d'une même exécution Maven. Si d'autres tests
 * insèrent des {@link MigrationRecord} dans la collection {@code _migrations} avant celui-ci (ordre de
 * classes JUnit non garanti), l'hypothèse "base fraîche" peut ne plus être vraie. À la charge des tests
 * complémentaires (enregistrerSucces/enregistrerEchec) de nettoyer leurs données (ex. {@code @AfterEach}
 * avec suppression par version) pour ne pas polluer ce test de fumée.
 *
 * @author DEVon
 */
@QuarkusTest
class TestMigrationRepository {

    @Inject
    MigrationRepository migrationRepository;

    /**
     * Test de fumée : vérifie que le repository démarre, se connecte réellement à MongoDB (Dev Services)
     * et exécute une requête de bout en bout. Sur une base fraîche (conteneur Dev Services tout juste
     * démarré, collection {@code _migrations} vide), aucune version n'est encore marquée en succès.
     */
    @Test
    void testListerVersionsAppliqueesSurBaseFraiche() {
        List<String> versionsAppliquees = migrationRepository.listerVersionsAppliquees()
                .await().indefinitely();

        assertNotNull(versionsAppliquees);
        assertTrue(versionsAppliquees.isEmpty());
    }
}
