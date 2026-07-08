package io.github.vzwingma.finances.budget.services.communs.migrations;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test d'intégration de {@link MigrationRepository} contre un vrai MongoDB (Dev Services), en
 * complément du test de fumée {@link TestMigrationRepository}.
 * <p>
 * Couvre {@link MigrationRepository#enregistrerSucces(String, String)},
 * {@link MigrationRepository#enregistrerEchec(String, String)} et le filtrage opéré par
 * {@link MigrationRepository#listerVersionsAppliquees()} (seules les versions {@code SUCCES} sont
 * retournées — comportement dont dépend l'idempotence de {@link MongoMigrationRunner}).
 * <p>
 * Volontairement séparée de {@link TestMigrationRepository} : le conteneur Dev Services MongoDB est
 * partagé entre toutes les classes {@code @QuarkusTest} du module {@code communs}, et l'ordre
 * d'exécution des méthodes de test au sein d'une même classe JUnit 5 n'est pas garanti (pas
 * d'{@code @TestMethodOrder} déclaré). Isoler ces tests dans leur propre classe évite tout risque
 * d'interférence avec l'hypothèse "base fraîche" du test de fumée, indépendamment de l'ordre réel
 * d'exécution des méthodes ou des classes.
 * <p>
 * <b>Nettoyage</b> : {@link #nettoyerCollection()} supprime uniquement les versions de test créées par
 * cette classe (par {@code deleteById}), pas {@link MigrationRepository#deleteAll()} — la collection
 * contient aussi la migration réelle {@code V001_InitMigrationsCollection} appliquée par
 * {@link MongoMigrationRunner} au démarrage Quarkus, qu'un {@code deleteAll()} effacerait et casserait
 * l'hypothèse "base fraîche" du test de fumée {@link TestMigrationRepository} si celui-ci s'exécute après
 * cette classe.
 * <p>
 * <b>Prérequis</b> : Docker (ou Podman) disponible sur la machine exécutant les tests, cf. javadoc de
 * {@link TestMigrationRepository} pour le détail du mécanisme Dev Services.
 *
 * @author QALvin
 */
@QuarkusTest
class TestMigrationRepositoryPersistence {

    private static final String VERSION_SUCCES = "TEST-SUCCES-001";
    private static final String VERSION_ECHEC = "TEST-ECHEC-001";
    private static final String VERSION_LISTE_SUCCES_1 = "TEST-LISTE-001";
    private static final String VERSION_LISTE_SUCCES_2 = "TEST-LISTE-002";
    private static final String VERSION_LISTE_ECHEC = "TEST-LISTE-003";

    @Inject
    MigrationRepository migrationRepository;

    /**
     * Supprime les versions de test créées par cette classe. Voir javadoc de classe pour la
     * justification du choix d'une suppression ciblée plutôt que {@code deleteAll()}.
     */
    @AfterEach
    void nettoyerCollection() {
        Stream.of(VERSION_SUCCES, VERSION_ECHEC, VERSION_LISTE_SUCCES_1, VERSION_LISTE_SUCCES_2, VERSION_LISTE_ECHEC)
                .forEach(version -> migrationRepository.deleteById(version).await().indefinitely());
    }

    /**
     * Relit un {@link MigrationRecord} par sa version via {@link MigrationRepository#findById(Object)}.
     */
    private Optional<MigrationRecord> recupererRecordParVersion(String version) {
        return Optional.ofNullable(migrationRepository.findById(version).await().indefinitely());
    }

    @Test
    void testEnregistrerSucces_persisteRecordAvecStatutSucces() {
        String description = "Migration test enregistrée en succès";

        migrationRepository.enregistrerSucces(VERSION_SUCCES, description).await().indefinitely();

        MigrationRecord recordPersiste = recupererRecordParVersion(VERSION_SUCCES)
                .orElseGet(() -> fail("Le MigrationRecord doit être persisté et retrouvable via findById()"));

        assertEquals(VERSION_SUCCES, recordPersiste.getVersion());
        assertEquals(description, recordPersiste.getDescription());
        assertEquals(MigrationRecord.MigrationStatutEnum.SUCCES, recordPersiste.getStatut());
        assertNotNull(recordPersiste.getDateExecution());

        List<String> versionsAppliquees = migrationRepository.listerVersionsAppliquees().await().indefinitely();
        assertTrue(versionsAppliquees.contains(VERSION_SUCCES),
                "listerVersionsAppliquees() doit inclure une version enregistrée en succès");
    }

    @Test
    void testEnregistrerEchec_persisteRecordAvecStatutEchec() {
        String description = "Migration test enregistrée en échec";

        migrationRepository.enregistrerEchec(VERSION_ECHEC, description).await().indefinitely();

        MigrationRecord recordPersiste = recupererRecordParVersion(VERSION_ECHEC)
                .orElseGet(() -> fail("Le MigrationRecord doit être persisté même en cas d'échec (traçabilité)"));

        assertEquals(VERSION_ECHEC, recordPersiste.getVersion());
        assertEquals(description, recordPersiste.getDescription());
        assertEquals(MigrationRecord.MigrationStatutEnum.ECHEC, recordPersiste.getStatut());
        assertNotNull(recordPersiste.getDateExecution());

        // Une migration en échec ne doit pas être considérée "appliquée" : permet une nouvelle tentative
        // au prochain démarrage (cf. javadoc MigrationRepository#enregistrerEchec).
        List<String> versionsAppliquees = migrationRepository.listerVersionsAppliquees().await().indefinitely();
        assertFalse(versionsAppliquees.contains(VERSION_ECHEC),
                "listerVersionsAppliquees() ne doit pas inclure une version en échec");
    }

    @Test
    void testListerVersionsAppliquees_neRetourneQueLesVersionsEnSucces() {
        migrationRepository.enregistrerSucces(VERSION_LISTE_SUCCES_1, "Migration succès 1").await().indefinitely();
        migrationRepository.enregistrerSucces(VERSION_LISTE_SUCCES_2, "Migration succès 2").await().indefinitely();
        migrationRepository.enregistrerEchec(VERSION_LISTE_ECHEC, "Migration échec").await().indefinitely();

        List<String> versionsAppliquees = migrationRepository.listerVersionsAppliquees().await().indefinitely();

        assertTrue(versionsAppliquees.contains(VERSION_LISTE_SUCCES_1),
                "Les versions en succès doivent être présentes dans la liste");
        assertTrue(versionsAppliquees.contains(VERSION_LISTE_SUCCES_2),
                "Les versions en succès doivent être présentes dans la liste");
        assertFalse(versionsAppliquees.contains(VERSION_LISTE_ECHEC),
                "Une version en échec ne doit jamais apparaître dans les versions appliquées "
                        + "(utilisé par MongoMigrationRunner pour l'idempotence : une migration en échec doit "
                        + "pouvoir être retentée)");
    }
}
