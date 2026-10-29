package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.spi;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test d'intégration de {@link UtilisateurDatabaseAdaptor#majDernierAcces(String, LocalDateTime)} contre un vrai MongoDB :
 * le {@code $set} ne modifie que {@code dernierAcces}, la date relue par le codec est égale à la date écrite, un login
 * inconnu ne crée aucun document.
 * <p>
 * <b>Contrainte Dev Services (module {@code utilisateurs})</b> : {@code src/main/resources/dev/application.properties} déclare
 * {@code quarkus.mongodb.connection-string=${QUARKUS_MONGODB_CONNECTION_STRING:mongodb://localhost:27017}} <em>sans préfixe
 * de profil</em> (contrairement à {@code operations} et {@code comptes} qui utilisent {@code %dev.}). Une chaîne de connexion
 * explicite désactive Quarkus Dev Services : aucun conteneur MongoDB n'est démarré en test. Les {@code application.properties}
 * de test étant partagés (non modifiables ici), ce test est <b>ignoré (assumption)</b> tant que la chaîne de connexion est la
 * valeur par défaut {@code mongodb://localhost:27017}, avec un message explicite. Il s'exécute dès que Dev Services est actif
 * (préfixer la clé par {@code %dev.} dans le fichier dev du module) ou si {@code QUARKUS_MONGODB_CONNECTION_STRING} pointe sur
 * un MongoDB de test.
 *
 * @author QALvin
 */
@QuarkusTest
class UtilisateurDatabaseAdaptorMongoTest {

    private static final String CHAINE_PAR_DEFAUT = "mongodb://localhost:27017";

    @Inject
    UtilisateurDatabaseAdaptor adaptor;

    @Inject
    ReactiveMongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String nomBase;

    @BeforeEach
    void verifierMongoDisponible() {
        String chaine = ConfigProvider.getConfig().getOptionalValue("quarkus.mongodb.connection-string", String.class).orElse(CHAINE_PAR_DEFAUT);
        Assumptions.assumeFalse(CHAINE_PAR_DEFAUT.equals(chaine),
                "Dev Services MongoDB inactif dans utilisateurs (quarkus.mongodb.connection-string explicite sans préfixe %dev.) : test d'intégration ignoré");
    }

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
