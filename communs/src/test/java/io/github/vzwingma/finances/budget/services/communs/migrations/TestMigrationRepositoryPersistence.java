package io.github.vzwingma.finances.budget.services.communs.migrations;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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
 * <b>Lecture des enregistrements persistés</b> : volontairement <u>pas</u> via {@code findById}. Voir
 * {@link #recupererRecordParVersion(String)} pour l'explication (incompatibilité de type generic Id
 * sur {@link MigrationRepository}, non contournable sans modifier la classe de production).
 * <p>
 * <b>Nettoyage</b> : {@link #nettoyerCollection()} vide entièrement la collection {@code _migrations}
 * après chaque test via {@link MigrationRepository#deleteAll()} (option explicitement suggérée pour
 * cette tâche). Sûr ici car la base {@code communs-test} / collection {@code _migrations} est dédiée
 * aux tests de ce module, exécutés séquentiellement par Surefire au sein d'un même fork JVM (pas de
 * classes {@code @QuarkusTest} exécutées en parallèle par défaut) : la collection est donc vide à
 * chaque fin d'exécution de cette classe, ce qui satisfait aussi l'hypothèse "base fraîche" du test de
 * fumée de {@link TestMigrationRepository}, quel que soit l'ordre relatif des deux classes.
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
     * Vide la collection {@code _migrations} après chaque test. Voir javadoc de classe pour la
     * justification du choix de {@code deleteAll()} plutôt qu'une suppression ciblée par version.
     */
    @AfterEach
    void nettoyerCollection() {
        migrationRepository.deleteAll().await().indefinitely();
    }

    /**
     * Relit un {@link MigrationRecord} par sa version via {@link MigrationRepository#listAll()} suivi
     * d'un filtrage côté client, plutôt que via {@code findById(version)}.
     * <p>
     * Raison : {@link MigrationRepository} implémente
     * {@code ReactivePanacheMongoRepository<MigrationRecord>}, dont le type d'identifiant générique est
     * <b>figé à {@code org.bson.types.ObjectId}</b> par l'interface Quarkus (voir
     * {@code io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository<Entity>}), alors que le
     * champ {@code @BsonId} réel de {@link MigrationRecord} (le champ {@code version}) est un
     * {@code String}. Conséquence concrète, vérifiée par compilation directe pendant l'écriture de ces
     * tests : {@code migrationRepository.findById("V001")} et {@code deleteById("V001")} ne compilent
     * pas ({@code incompatible types: String cannot be converted to ObjectId}). Ce n'est pas gênant pour
     * le code de production actuel (qui n'appelle jamais ces méthodes typées par Id — seulement
     * {@code persist(...)} et {@code find("statut", ...)}), mais cela rend ces méthodes héritées
     * inutilisables telles quelles depuis l'extérieur. Signalé dans le rapport de tâche plutôt que corrigé
     * silencieusement ici (hors périmètre QA : nécessiterait de faire hériter {@code MigrationRepository}
     * de {@code ReactivePanacheMongoRepositoryBase<MigrationRecord, String>} à la place, une modification
     * du code de production).
     * <p>
     * {@code listAll()} est sans ambiguïté ici : c'est l'opération Panache la plus basique, elle désérialise
     * les documents complets via le codec BSON de l'entité (donc le mapping {@code @BsonId} est
     * correctement pris en compte), sans dépendre d'un nom de champ passé en chaîne de caractères.
     */
    private Optional<MigrationRecord> recupererRecordParVersion(String version) {
        List<MigrationRecord> tousLesRecords = migrationRepository.listAll().await().indefinitely();
        return tousLesRecords.stream()
                .filter(record -> version.equals(record.getVersion()))
                .findFirst();
    }

    @Test
    void testEnregistrerSucces_persisteRecordAvecStatutSucces() {
        String description = "Migration test enregistrée en succès";

        migrationRepository.enregistrerSucces(VERSION_SUCCES, description).await().indefinitely();

        MigrationRecord recordPersiste = recupererRecordParVersion(VERSION_SUCCES)
                .orElseGet(() -> fail("Le MigrationRecord doit être persisté et retrouvable via listAll()"));

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
