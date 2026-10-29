package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.spi;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.result.UpdateResult;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.data.MockDataUtilisateur;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Uni;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (sans Mongo ni Docker) de {@link UtilisateurDatabaseAdaptor#majDernierAcces(String, LocalDateTime)} :
 * {@code $set} ciblé sur {@code dernierAcces}, encodage de la date symétrique du codec, WARN si aucun document
 * ({@code matchedCount == 0}). L'écriture réelle est couverte par {@code UtilisateurDatabaseAdaptorMongoTest}.
 *
 * @author QALvin
 */
class UtilisateurDatabaseAdaptorTest {

    private static final LocalDateTime DATE = LocalDateTime.of(2026, 7, 10, 10, 15, 30);

    private UtilisateurDatabaseAdaptor adaptor;
    private ReactiveMongoCollection<Utilisateur> collection;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        collection = mock(ReactiveMongoCollection.class);
        adaptor = spy(new UtilisateurDatabaseAdaptor());
        doReturn(collection).when(adaptor).mongoCollection();
    }

    private static BsonDocument bson(Bson bson) {
        return bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());
    }

    /** Capture les enregistrements de log du logger de l'adaptateur pendant l'exécution de l'action */
    private static List<LogRecord> capturerLogs(Runnable action) {
        final List<LogRecord> logs = new ArrayList<>();
        final Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logs.add(record);
            }

            @Override
            public void flush() {
                // rien à vider
            }

            @Override
            public void close() {
                // rien à fermer
            }
        };
        final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(UtilisateurDatabaseAdaptor.class.getName());
        logger.addHandler(capture);
        try {
            action.run();
        } finally {
            logger.removeHandler(capture);
        }
        return logs;
    }

    private void updateRepond(long matchedCount) {
        when(collection.updateOne(any(Bson.class), any(Bson.class)))
                .thenReturn(Uni.createFrom().item(UpdateResult.acknowledged(matchedCount, matchedCount, null)));
    }

    @Test
    void testMajDernierAcces_setCibleSurDernierAcces_filtreSurLogin() {
        updateRepond(1);

        adaptor.majDernierAcces("Test", DATE).await().indefinitely();

        ArgumentCaptor<Bson> filtre = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> miseAJour = ArgumentCaptor.forClass(Bson.class);
        verify(collection).updateOne(filtre.capture(), miseAJour.capture());
        assertEquals("{\"login\": \"Test\"}", bson(filtre.getValue()).toJson());
        BsonDocument update = bson(miseAJour.getValue());
        assertEquals(List.of("$set"), List.copyOf(update.keySet()), "Un seul opérateur $set");
        assertEquals(List.of("dernierAcces"), List.copyOf(update.getDocument("$set").keySet()),
                "Seul dernierAcces est modifié (pas de réécriture de l'utilisateur)");
    }

    @Test
    void testMajDernierAcces_dateEncodeeCommeLeCodecDeLUtilisateur() {
        updateRepond(1);

        adaptor.majDernierAcces("Test", DATE).await().indefinitely();

        ArgumentCaptor<Bson> miseAJour = ArgumentCaptor.forClass(Bson.class);
        verify(collection).updateOne(any(Bson.class), miseAJour.capture());
        long millisAttendues = Date.from(Instant.ofEpochMilli(BudgetDateTimeUtils.getMillisecondsFromLocalDateTime(DATE))).getTime();
        long millisEcrites = bson(miseAJour.getValue()).getDocument("$set").getDateTime("dernierAcces").getValue();
        assertEquals(millisAttendues, millisEcrites, "Même conversion (fuseau Europe/Paris) que UtilisateurPanacheCodec");
        // Symétrie avec le decode du codec : aller-retour sans décalage
        assertEquals(DATE, BudgetDateTimeUtils.getLocalDateTimeFromMillisecond(millisEcrites));
    }

    @Test
    void testMajDernierAcces_aucunDocumentTrouve_warn() {
        updateRepond(0);

        List<LogRecord> logs = capturerLogs(() -> adaptor.majDernierAcces("Inconnu", DATE).await().indefinitely());

        LogRecord warn = logs.stream().filter(l -> l.getLevel().intValue() == Level.WARNING.intValue()).findFirst()
                .orElseThrow(() -> new AssertionError("WARN attendu, logs : " + logs.stream().map(LogRecord::getMessage).toList()));
        assertTrue(warn.getMessage().contains("aucun document trouvé"), warn.getMessage());
        assertTrue(Arrays.toString(warn.getParameters()).contains("Inconnu"));
    }

    @Test
    void testMajDernierAcces_documentTrouve_pasDeWarn() {
        updateRepond(1);

        List<LogRecord> logs = capturerLogs(() -> adaptor.majDernierAcces("Test", DATE).await().indefinitely());

        assertTrue(logs.stream().noneMatch(l -> l.getLevel().intValue() >= Level.WARNING.intValue()), "Aucun WARN quand le document est trouvé");
    }

    @Test
    void testMajDernierAcces_erreurMongoPropagee() {
        MongoTimeoutException timeout = new MongoTimeoutException("timeout");
        when(collection.updateOne(any(Bson.class), any(Bson.class))).thenReturn(Uni.createFrom().failure(timeout));

        Uni<Void> maj = adaptor.majDernierAcces("Test", DATE);

        assertSame(timeout, assertThrows(MongoTimeoutException.class, () -> maj.await().indefinitely()));
    }

    // ====== chargeUtilisateur ======

    @SuppressWarnings("unchecked")
    @Test
    void testChargeUtilisateur_inconnu_null_etConnu_utilisateur() {
        ReactivePanacheQuery<Utilisateur> query = mock(ReactivePanacheQuery.class);
        doReturn(query).when(adaptor).find(anyString(), any(Object[].class));
        when(query.singleResultOptional()).thenReturn(Uni.createFrom().item(Optional.empty()));
        assertNull(adaptor.chargeUtilisateur("Inconnu").await().indefinitely(), "Utilisateur inconnu : null (404 décidé par le service)");

        Utilisateur connu = MockDataUtilisateur.getTestUtilisateur();
        when(query.singleResultOptional()).thenReturn(Uni.createFrom().item(Optional.of(connu)));
        assertSame(connu, adaptor.chargeUtilisateur("Test").await().indefinitely());
    }
}
