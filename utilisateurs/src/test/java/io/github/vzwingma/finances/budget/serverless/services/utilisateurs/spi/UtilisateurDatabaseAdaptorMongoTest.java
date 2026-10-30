package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.spi;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test d'intégration de {@link UtilisateurDatabaseAdaptor#majDernierAcces(String, LocalDateTime)} contre un vrai MongoDB :
 * le {@code $set} ne modifie que {@code dernierAcces}, la date relue par le codec est égale à la date écrite, un login
 * inconnu ne crée aucun document.
 * <p>
 * <b>MongoDB de test</b> : {@code src/main/resources/dev/application.properties} déclare {@code quarkus.mongodb.connection-string}
 * <em>sans préfixe de profil</em> (contrairement à {@code operations}/{@code comptes} : {@code %dev.}), ce qui désactive Quarkus
 * Dev Services : aucun conteneur, et le test était ignoré (assumption) en CI. {@link MongoDbTestResource} (Testcontainers)
 * fournit désormais la chaîne de connexion, Dev Services est désactivé par le {@link TestProfile} : le test s'exécute
 * réellement quel que soit le profil Maven (Docker requis, donc CI).
 *
 * @author QALvin
 */
@QuarkusTest
@TestProfile(UtilisateurDatabaseAdaptorMongoTest.ProfilMongoTestcontainers.class)
class UtilisateurDatabaseAdaptorMongoTest {

    /** Désactive Dev Services et branche le MongoDB Testcontainers */
    public static class ProfilMongoTestcontainers implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.mongodb.devservices.enabled", "false");
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(new TestResourceEntry(MongoDbTestResource.class));
        }
    }

    @Inject
    UtilisateurDatabaseAdaptor adaptor;

    @Inject
    ReactiveMongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String nomBase;

    private ReactiveMongoCollection<Document> collection() {
        return mongoClient.getDatabase(nomBase).getCollection("utilisateurs");
    }

    private Document insereUtilisateur(String login, Date dernierAcces) {
        Document document = new Document("_id", new ObjectId())
                .append("login", login)
                .append("dernierAcces", dernierAcces)
                .append("prefsUtilisateur", new Document("PREFS_STATUT_NLLE_DEPENSE", "Nouvelle"))
                .append("droits", new Document())
                .append("sentinelle", "ne-doit-pas-changer");
        collection().insertOne(document).await().indefinitely();
        return document;
    }

    private Document relit(String login) {
        return collection().find(new Document("login", login)).collect().first().await().indefinitely();
    }

    @Test
    void testMajDernierAcces_persisteLaDateEtNeModifieQueDernierAcces() {
        String login = "it-" + UUID.randomUUID();
        Date ancienne = Date.from(java.time.Instant.parse("2026-01-02T03:04:05Z"));
        Document avant = insereUtilisateur(login, ancienne);
        LocalDateTime nouvelle = LocalDateTime.of(2026, 7, 10, 10, 15, 30);

        adaptor.majDernierAcces(login, nouvelle).await().indefinitely();

        Document apres = relit(login);
        assertNotNull(apres);
        // Seul dernierAcces a changé
        assertEquals(avant.get("_id"), apres.get("_id"));
        assertEquals("ne-doit-pas-changer", apres.getString("sentinelle"));
        assertEquals(avant.get("prefsUtilisateur"), apres.get("prefsUtilisateur"));
        assertEquals(avant.get("droits"), apres.get("droits"));
        assertEquals(login, apres.getString("login"));
        assertEquals(Date.from(java.time.Instant.ofEpochMilli(
                io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils.getMillisecondsFromLocalDateTime(nouvelle))),
                apres.getDate("dernierAcces"));
    }

    @Test
    void testMajDernierAcces_aller_retour_laDateRelueParLeCodecEstEgaleALaDateEcrite() {
        String login = "it-" + UUID.randomUUID();
        insereUtilisateur(login, Date.from(java.time.Instant.parse("2026-01-02T03:04:05Z")));
        LocalDateTime nouvelle = LocalDateTime.of(2026, 7, 10, 10, 15, 30);

        adaptor.majDernierAcces(login, nouvelle).await().indefinitely();

        Utilisateur relu = adaptor.chargeUtilisateur(login).await().indefinitely();
        assertNotNull(relu);
        assertEquals(nouvelle, relu.getDernierAcces(), "Aucun décalage entre l'écriture ciblée et le codec de lecture");
    }

    @Test
    void testMajDernierAcces_loginInconnu_aucunDocumentCree() {
        String login = "it-inconnu-" + UUID.randomUUID();

        adaptor.majDernierAcces(login, LocalDateTime.of(2026, 7, 10, 10, 15, 30)).await().indefinitely();

        assertNull(relit(login), "Pas d'upsert : un login inconnu ne crée aucun document");
        assertNull(adaptor.chargeUtilisateur(login).await().indefinitely());
    }
}
