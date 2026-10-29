package io.github.vzwingma.finances.budget.services.communs.migrations;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.UpdateResult;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Uni;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (sans Mongo ni Docker) de {@link MigrationRepository} : réservation atomique (ADR-006),
 * finalisation par owner et bail. La collection Mongo est mockée ; ces tests vérifient le filtre et la mise à jour
 * envoyés au driver et l'interprétation de la réponse. Leur exécution réelle contre MongoDB est couverte par
 * {@link TestMigrationRepositoryConcurrence} (Dev Services, CI).
 * <p>
 * Horloge figée (ADR-004) : le seuil de péremption du bail est déterministe.
 *
 * @author QALvin
 */
class TestMigrationRepositoryReservation {

    private static final Instant MAINTENANT = Instant.parse("2026-07-10T10:00:00Z");
    private static final String OWNER = "owner-A";

    private ReactiveMongoCollection<MigrationRecord> collection;
    private MigrationRepository repository;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        collection = mock(ReactiveMongoCollection.class);
        repository = spy(new MigrationRepository(Clock.fixed(MAINTENANT, ZoneOffset.UTC)));
        // mongoCollection() est amélioré par Quarkus au build ; hors conteneur, on fournit la collection mockée
        doReturn(collection).when(repository).mongoCollection();
    }

    private static MigrationRecord recordDetenuPar(String owner) {
        MigrationRecord record = new MigrationRecord();
        record.setVersion("V001");
        record.setOwner(owner);
        record.setStatut(MigrationRecord.MigrationStatutEnum.EN_COURS);
        return record;
    }

    private static String json(Bson bson) {
        BsonDocument document = bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());
        return document.toJson();
    }

    // ====== reserver ======

    @Test
    void testReserverAcquise_documentRetourneDetenuParLeDemandeur_true() {
        when(collection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
                .thenReturn(Uni.createFrom().item(recordDetenuPar(OWNER)));

        assertTrue(repository.reserver("V001", "desc", OWNER).await().indefinitely(), "Réservation acquise attendue");
    }

    @Test
    void testReserverRefusee_documentDetenuParUnAutreOwner_false() {
        when(collection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
                .thenReturn(Uni.createFrom().item(recordDetenuPar("autre-owner")));

        assertFalse(repository.reserver("V001", "desc", OWNER).await().indefinitely(), "Un autre owner détient la version");
    }

    @Test
    void testReserverRefusee_aucunDocumentRetourne_false() {
        when(collection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
                .thenReturn(Uni.createFrom().nullItem());

        assertFalse(repository.reserver("V001", "desc", OWNER).await().indefinitely());
    }

    @Test
    void testReserverRefusee_cleDupliquee_false() {
        when(collection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
                .thenReturn(Uni.createFrom().failure(new MongoException(11000, "E11000 duplicate key error")));

        assertFalse(repository.reserver("V001", "desc", OWNER).await().indefinitely(),
                "E11000 = version déjà prise, pas une erreur");
    }

    @Test
    void testReserverRefusee_cleDupliqueeEnCauseChainee_false() {
        // Le driver réactif peut encapsuler l'erreur : la cause chaînée doit être inspectée
        RuntimeException enveloppe = new RuntimeException("enveloppe", new MongoException(11000, "E11000 duplicate key error"));
        when(collection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
                .thenReturn(Uni.createFrom().failure(enveloppe));

        assertFalse(repository.reserver("V001", "desc", OWNER).await().indefinitely());
    }

    @Test
    void testReserver_autreErreurMongoPropagee() {
        MongoTimeoutException timeout = new MongoTimeoutException("timeout");
        when(collection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
                .thenReturn(Uni.createFrom().failure(timeout));

        Uni<Boolean> reservation = repository.reserver("V001", "desc", OWNER);
        MongoTimeoutException erreur = assertThrows(MongoTimeoutException.class, () -> reservation.await().indefinitely());
        assertEquals("timeout", erreur.getMessage());
    }

    @Test
    void testReserver_erreurCommandeNonDupliquee_propagee() {
        // Un code d'erreur différent de 11000 ne doit jamais être interprété comme « version déjà prise »
        MongoCommandException commande = new MongoCommandException(
                new org.bson.BsonDocument("ok", new org.bson.BsonDouble(0)).append("code", new org.bson.BsonInt32(13))
                        .append("errmsg", new BsonString("unauthorized")), new ServerAddress());
        when(collection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
                .thenReturn(Uni.createFrom().failure(commande));

        Uni<Boolean> reservation = repository.reserver("V001", "desc", OWNER);
        assertInstanceOf(MongoCommandException.class,
                assertThrows(MongoException.class, () -> reservation.await().indefinitely()));
    }

    @Test
    void testReserver_filtreEtMiseAJourEnvoyesAuDriver_upsertEtDocumentApres() {
        when(collection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
                .thenReturn(Uni.createFrom().item(recordDetenuPar(OWNER)));

        repository.reserver("V001", "Ma description", OWNER).await().indefinitely();

        ArgumentCaptor<Bson> filtre = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> miseAJour = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<FindOneAndUpdateOptions> options = ArgumentCaptor.forClass(FindOneAndUpdateOptions.class);
        verify(collection).findOneAndUpdate(filtre.capture(), miseAJour.capture(), options.capture());

        assertTrue(options.getValue().isUpsert(), "upsert requis : l'unicité repose sur l'index unique de _id");
        assertEquals(ReturnDocument.AFTER, options.getValue().getReturnDocument());

        String filtreJson = json(filtre.getValue());
        assertTrue(filtreJson.contains("\"_id\": \"V001\""), filtreJson);
        assertTrue(filtreJson.contains("\"statut\": \"ECHEC\""), "Une version en ECHEC est réservable : " + filtreJson);
        assertTrue(filtreJson.contains("\"statut\": \"EN_COURS\""), "Une version EN_COURS à bail périmé est réservable : " + filtreJson);

        String miseAJourJson = json(miseAJour.getValue());
        assertTrue(miseAJourJson.contains("\"statut\": \"EN_COURS\""), miseAJourJson);
        assertTrue(miseAJourJson.contains("\"owner\": \"owner-A\""), miseAJourJson);
        assertTrue(miseAJourJson.contains("\"description\": \"Ma description\""), miseAJourJson);
        // dateExecution = maintenant (horloge figée), début du bail
        assertTrue(miseAJourJson.contains("2026-07-10T10:00:00"), miseAJourJson);
    }

    @Test
    void testReserver_bailPerimeApres5Minutes_seuilDeriveDeLHorlogeFigee() {
        when(collection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
                .thenReturn(Uni.createFrom().item(recordDetenuPar(OWNER)));

        repository.reserver("V001", "desc", OWNER).await().indefinitely();

        ArgumentCaptor<Bson> filtre = ArgumentCaptor.forClass(Bson.class);
        verify(collection).findOneAndUpdate(filtre.capture(), any(Bson.class), any(FindOneAndUpdateOptions.class));
        String filtreJson = json(filtre.getValue());

        assertEquals(5, MigrationRepository.BAIL.toMinutes(), "Bail de 5 minutes (ADR-006)");
        // EN_COURS repris seulement si dateExecution < maintenant - 5 min = 09:55 (jamais l'heure courante réelle)
        assertTrue(filtreJson.contains("\"$lt\""), filtreJson);
        assertTrue(filtreJson.contains("2026-07-10T09:55:00"), "Seuil du bail attendu à 09:55 : " + filtreJson);
        assertFalse(filtreJson.contains("2026-07-10T10:00:00"), "Le seuil ne doit pas être l'instant courant : " + filtreJson);
    }

    @Test
    void testReserver_seuilDuBailSuitLHorloge() {
        // Horloge avancée de 1 h : le seuil suit (preuve que le bail dépend bien du Clock injecté)
        MigrationRepository plusTard = spy(new MigrationRepository(Clock.fixed(MAINTENANT.plusSeconds(3600), ZoneOffset.UTC)));
        doReturn(collection).when(plusTard).mongoCollection();
        when(collection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
                .thenReturn(Uni.createFrom().item(recordDetenuPar(OWNER)));

        plusTard.reserver("V001", "desc", OWNER).await().indefinitely();

        ArgumentCaptor<Bson> filtre = ArgumentCaptor.forClass(Bson.class);
        verify(collection).findOneAndUpdate(filtre.capture(), any(Bson.class), any(FindOneAndUpdateOptions.class));
        assertTrue(json(filtre.getValue()).contains("2026-07-10T10:55:00"));
    }

    // ====== finaliser ======

    @Test
    void testFinaliserSucces_documentTrouve_true_filtreSurIdEtOwner() {
        when(collection.updateOne(any(Bson.class), any(Bson.class)))
                .thenReturn(Uni.createFrom().item(UpdateResult.acknowledged(1, 1L, null)));

        assertTrue(repository.finaliserSucces("V001", OWNER).await().indefinitely());

        ArgumentCaptor<Bson> filtre = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> miseAJour = ArgumentCaptor.forClass(Bson.class);
        verify(collection).updateOne(filtre.capture(), miseAJour.capture());
        String filtreJson = json(filtre.getValue());
        assertTrue(filtreJson.contains("\"_id\": \"V001\"") && filtreJson.contains("\"owner\": \"owner-A\""),
                "Seul le détenteur peut finaliser : " + filtreJson);
        assertTrue(json(miseAJour.getValue()).contains("\"statut\": \"SUCCES\""));
    }

    @Test
    void testFinaliserSucces_aucunDocument_bailRepris_false() {
        when(collection.updateOne(any(Bson.class), any(Bson.class)))
                .thenReturn(Uni.createFrom().item(UpdateResult.acknowledged(0, 0L, null)));

        assertFalse(repository.finaliserSucces("V001", OWNER).await().indefinitely(),
                "Aucun document finalisé : la chaîne doit s'arrêter");
    }

    @Test
    void testFinaliserEchec_statutEchecEcrit() {
        when(collection.updateOne(any(Bson.class), any(Bson.class)))
                .thenReturn(Uni.createFrom().item(UpdateResult.acknowledged(1, 1L, null)));

        repository.finaliserEchec("V001", OWNER).await().indefinitely();

        ArgumentCaptor<Bson> miseAJour = ArgumentCaptor.forClass(Bson.class);
        verify(collection).updateOne(any(Bson.class), miseAJour.capture());
        assertTrue(json(miseAJour.getValue()).contains("\"statut\": \"ECHEC\""));
    }

    @Test
    void testFinaliserEchec_aucunDocument_sansErreur() {
        when(collection.updateOne(any(Bson.class), any(Bson.class)))
                .thenReturn(Uni.createFrom().item(UpdateResult.acknowledged(0, 0L, null)));

        repository.finaliserEchec("V001", OWNER).await().indefinitely();
        // Pas d'exception : un simple WARN est émis (réservation reprise par un autre runner)
    }

    @Test
    void testFinaliserSucces_erreurMongoPropagee() {
        when(collection.updateOne(any(Bson.class), any(Bson.class)))
                .thenReturn(Uni.createFrom().failure(new MongoTimeoutException("timeout")));

        Uni<Boolean> finalisation = repository.finaliserSucces("V001", OWNER);
        assertThrows(MongoTimeoutException.class, () -> finalisation.await().indefinitely());
    }
}
