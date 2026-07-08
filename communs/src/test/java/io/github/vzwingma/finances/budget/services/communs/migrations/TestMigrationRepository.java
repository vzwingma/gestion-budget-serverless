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
 * l'application le temps des tests.
 * <p>
 * <b>Prérequis</b> : Docker (ou Podman) disponible sur la machine exécutant les tests. Sans Docker, le
 * démarrage Quarkus réussit tout de même (Dev Services journalise un simple avertissement et retombe sur
 * la valeur par défaut {@code mongodb://localhost:27017}), mais la requête réelle de ce test échoue alors
 * avec un {@code com.mongodb.MongoTimeoutException} après ~30s (aucun serveur MongoDB à cette adresse).
 * <p>
 * <b>"Base fraîche" ne veut pas dire "collection vide"</b> : {@link MongoMigrationRunner} s'exécute lui
 * aussi à chaque démarrage Quarkus ({@code @Observes StartupEvent}) et applique la migration réelle
 * {@code V001_InitMigrationsCollection}, qui s'enregistre en succès dans {@code _migrations}. Une base
 * fraîche contient donc toujours au moins cette version — ce test vérifie que la requête aboutit et que
 * cette migration connue y figure, pas que la collection est vide.
 *
 * @author DEVon
 */
@QuarkusTest
class TestMigrationRepository {

    private static final String VERSION_MIGRATION_STARTUP = "V001";

    @Inject
    MigrationRepository migrationRepository;

    /**
     * Test de fumée : vérifie que le repository démarre, se connecte réellement à MongoDB (Dev Services)
     * et exécute une requête de bout en bout. Sur une base fraîche, seule la migration réelle appliquée au
     * démarrage ({@code V001_InitMigrationsCollection}) est présente.
     */
    @Test
    void testListerVersionsAppliqueesSurBaseFraiche() {
        List<String> versionsAppliquees = migrationRepository.listerVersionsAppliquees()
                .await().indefinitely();

        assertNotNull(versionsAppliquees);
        assertTrue(versionsAppliquees.contains(VERSION_MIGRATION_STARTUP),
                "La migration réelle appliquée au démarrage (V001) doit figurer dans la liste");
    }
}
