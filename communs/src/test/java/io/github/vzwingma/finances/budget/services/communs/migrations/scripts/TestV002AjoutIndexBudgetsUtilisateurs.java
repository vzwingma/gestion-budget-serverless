package io.github.vzwingma.finances.budget.services.communs.migrations.scripts;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoSocketReadTimeoutException;
import com.mongodb.ServerAddress;
import com.mongodb.client.model.IndexOptions;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.mongodb.reactive.ReactiveMongoDatabase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (sans Mongo ni Docker) de {@link V002_AjoutIndexBudgetsUtilisateurs} : le client Mongo est mocké.
 * La création réelle des index est couverte par {@code TestMigrationRepositoryConcurrence} (Dev Services, CI).
 *
 * @author QALvin
 */
class TestV002AjoutIndexBudgetsUtilisateurs {

    private ReactiveMongoCollection<Document> budgets;
    private ReactiveMongoCollection<Document> utilisateurs;
    private V002_AjoutIndexBudgetsUtilisateurs migration;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        ReactiveMongoClient client = mock(ReactiveMongoClient.class);
        ReactiveMongoDatabase base = mock(ReactiveMongoDatabase.class);
        budgets = mock(ReactiveMongoCollection.class);
        utilisateurs = mock(ReactiveMongoCollection.class);
        when(client.getDatabase("ma-base")).thenReturn(base);
        when(base.getCollection(V002_AjoutIndexBudgetsUtilisateurs.COLLECTION_BUDGETS)).thenReturn(budgets);
        when(base.getCollection(V002_AjoutIndexBudgetsUtilisateurs.COLLECTION_UTILISATEURS)).thenReturn(utilisateurs);
        when(budgets.createIndex(any(Bson.class), any(IndexOptions.class))).thenReturn(Uni.createFrom().item("idx"));
        when(utilisateurs.createIndex(any(Bson.class), any(IndexOptions.class))).thenReturn(Uni.createFrom().item("idx"));
        migration = new V002_AjoutIndexBudgetsUtilisateurs(client, "ma-base");
    }

    /** Index {@code _id_} présent sur toute collection Mongo */
    private static Document indexId() {
        return new Document("v", 2).append("key", new Document("_id", 1)).append("name", "_id_");
    }

    private static Document index(String nom, Document cle) {
        return new Document("v", 2).append("key", cle).append("name", nom);
    }

    private static Document cleBudgets() {
        return new Document("idCompteBancaire", 1).append("annee", 1).append("mois", 1);
    }

    private static String json(Bson bson) {
        return bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry()).toJson();
    }

    // ====== contrat ======

    @Test
    void testVersionEtDescription() {
        assertEquals("V002", migration.version());
        assertTrue(migration.description().contains("budgets") && migration.description().contains("utilisateurs"));
    }

    // ====== création ======

    @Test
    void testIndexAbsents_lesDeuxIndexSontCrees_utilisateursNonUnique() {
        when(budgets.listIndexes()).thenReturn(Multi.createFrom().item(indexId()));
        when(utilisateurs.listIndexes()).thenReturn(Multi.createFrom().item(indexId()));

        migration.migrate().await().indefinitely();

        ArgumentCaptor<Bson> cleBudgetsCreee = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<IndexOptions> optionsBudgets = ArgumentCaptor.forClass(IndexOptions.class);
        verify(budgets).createIndex(cleBudgetsCreee.capture(), optionsBudgets.capture());
        // Ordre des champs significatif : idCompteBancaire, annee, mois (tous croissants)
        assertEquals("{\"idCompteBancaire\": 1, \"annee\": 1, \"mois\": 1}", json(cleBudgetsCreee.getValue()));
        assertFalse(optionsBudgets.getValue().isUnique());

        ArgumentCaptor<Bson> cleUtilisateursCreee = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<IndexOptions> optionsUtilisateurs = ArgumentCaptor.forClass(IndexOptions.class);
        verify(utilisateurs).createIndex(cleUtilisateursCreee.capture(), optionsUtilisateurs.capture());
        assertEquals("{\"login\": 1}", json(cleUtilisateursCreee.getValue()));
        assertFalse(optionsUtilisateurs.getValue().isUnique(), "Index login NON unique (contrôle des doublons à faire avant)");
    }

    // ====== défensif ======

    @Test
    void testIndexDeMemeCleSousUnAutreNom_ignore() {
        // Index créé manuellement (ex. sur Atlas) sous un autre nom : pas de doublon
        when(budgets.listIndexes()).thenReturn(Multi.createFrom().items(indexId(), index("idx_manuel_budgets", cleBudgets())));
        when(utilisateurs.listIndexes()).thenReturn(Multi.createFrom().items(indexId(), index("login_atlas", new Document("login", 1))));

        migration.migrate().await().indefinitely();

        verify(budgets, never()).createIndex(any(Bson.class), any(IndexOptions.class));
        verify(utilisateurs, never()).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    void testIndexDeMemeCleAvecSensStockeEnLongOuDouble_ignore() {
        // Le sens peut être stocké en Long ou Double selon l'origine de l'index
        when(budgets.listIndexes()).thenReturn(Multi.createFrom().items(
                index("a", new Document("idCompteBancaire", 1L).append("annee", 1.0d).append("mois", 1))));
        when(utilisateurs.listIndexes()).thenReturn(Multi.createFrom().items(index("b", new Document("login", 1.0d))));

        migration.migrate().await().indefinitely();

        verify(budgets, never()).createIndex(any(Bson.class), any(IndexOptions.class));
        verify(utilisateurs, never()).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    void testIndexDeCleDifferente_ordreOuSensOuChamps_nEstPasUnIndexEquivalent() {
        // Ordre différent, sens descendant, champ en moins : aucun n'équivaut à la clé attendue
        when(budgets.listIndexes()).thenReturn(Multi.createFrom().items(
                index("ordre", new Document("annee", 1).append("idCompteBancaire", 1).append("mois", 1)),
                index("sens", new Document("idCompteBancaire", -1).append("annee", 1).append("mois", 1)),
                index("partiel", new Document("idCompteBancaire", 1).append("annee", 1))));
        when(utilisateurs.listIndexes()).thenReturn(Multi.createFrom().items(index("autre", new Document("email", 1))));

        migration.migrate().await().indefinitely();

        verify(budgets, times(1)).createIndex(any(Bson.class), any(IndexOptions.class));
        verify(utilisateurs, times(1)).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    @Test
    void testDeuxiemeExecution_lesIndexCreesSontDetectes_aucuneCreation() {
        // 1re exécution : base vierge ; 2e exécution : listIndexes retourne les index créés
        when(budgets.listIndexes()).thenReturn(
                Multi.createFrom().item(indexId()),
                Multi.createFrom().items(indexId(), index("idCompteBancaire_1_annee_1_mois_1", cleBudgets())));
        when(utilisateurs.listIndexes()).thenReturn(
                Multi.createFrom().item(indexId()),
                Multi.createFrom().items(indexId(), index("login_1", new Document("login", 1))));

        migration.migrate().await().indefinitely();
        migration.migrate().await().indefinitely();

        verify(budgets, times(1)).createIndex(any(Bson.class), any(IndexOptions.class));
        verify(utilisateurs, times(1)).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    // ====== erreurs ======

    @Test
    void testTimeoutDeLectureLorsDeLaCreation_echecExplicite() {
        MongoSocketReadTimeoutException timeout = new MongoSocketReadTimeoutException("read timeout", new ServerAddress(), new RuntimeException("socket"));
        when(budgets.listIndexes()).thenReturn(Multi.createFrom().item(indexId()));
        when(budgets.createIndex(any(Bson.class), any(IndexOptions.class))).thenReturn(Uni.createFrom().failure(timeout));

        Uni<Void> execution = migration.migrate();
        IllegalStateException erreur = assertThrows(IllegalStateException.class, () -> execution.await().indefinitely());

        assertSame(timeout, erreur.getCause(), "L'erreur d'origine est conservée en cause");
        assertTrue(erreur.getMessage().contains("Timeout de lecture") && erreur.getMessage().contains("budgets"), erreur.getMessage());
        // La chaîne s'arrête : les index utilisateurs ne sont pas traités
        verify(utilisateurs, never()).listIndexes();
    }

    @Test
    void testTimeoutEnveloppeDansUneCause_detecte() {
        MongoSocketReadTimeoutException timeout = new MongoSocketReadTimeoutException("read timeout", new ServerAddress(), new RuntimeException("socket"));
        when(budgets.listIndexes()).thenReturn(Multi.createFrom().item(indexId()));
        when(budgets.createIndex(any(Bson.class), any(IndexOptions.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("enveloppe", timeout)));

        Uni<Void> execution = migration.migrate();
        assertInstanceOf(IllegalStateException.class, assertThrows(RuntimeException.class, () -> execution.await().indefinitely()));
    }

    @Test
    void testAutreErreurDeCreation_propageeSansTransformation() {
        MongoCommandException refus = new MongoCommandException(
                new BsonDocument("ok", new org.bson.BsonDouble(0)).append("code", new BsonInt32(13)).append("errmsg", new BsonString("unauthorized")),
                new ServerAddress());
        when(budgets.listIndexes()).thenReturn(Multi.createFrom().item(indexId()));
        when(budgets.createIndex(any(Bson.class), any(IndexOptions.class))).thenReturn(Uni.createFrom().failure(refus));

        Uni<Void> execution = migration.migrate();
        assertSame(refus, assertThrows(MongoCommandException.class, () -> execution.await().indefinitely()));
    }

    @Test
    void testErreurDeListIndexes_propagee_aucuneCreation() {
        when(budgets.listIndexes()).thenReturn(Multi.createFrom().failure(new IllegalStateException("listIndexes KO")));

        Uni<Void> execution = migration.migrate();
        assertThrows(IllegalStateException.class, () -> execution.await().indefinitely());
        verify(budgets, never()).createIndex(any(Bson.class), any(IndexOptions.class));
    }

    // ====== utilitaires statiques ======

    @Test
    void testMemeCle_documentCleAbsentOuNonDocument_false() {
        Map<String, Integer> attendue = new LinkedHashMap<>();
        attendue.put("login", 1);
        assertFalse(V002_AjoutIndexBudgetsUtilisateurs.memeCle(new Document("name", "x"), attendue));
        assertFalse(V002_AjoutIndexBudgetsUtilisateurs.memeCle(new Document("key", "pas un document"), attendue));
        assertFalse(V002_AjoutIndexBudgetsUtilisateurs.memeCle(new Document("key", new Document("login", "text")), attendue),
                "Un index texte/hashé n'est pas un index croissant");
    }

    @Test
    void testEstTimeoutLecture() {
        MongoSocketReadTimeoutException timeout = new MongoSocketReadTimeoutException("t", new ServerAddress(), new RuntimeException());
        assertTrue(V002_AjoutIndexBudgetsUtilisateurs.estTimeoutLecture(timeout));
        assertTrue(V002_AjoutIndexBudgetsUtilisateurs.estTimeoutLecture(new RuntimeException(new RuntimeException(timeout))));
        assertFalse(V002_AjoutIndexBudgetsUtilisateurs.estTimeoutLecture(new RuntimeException("autre")));
        assertFalse(V002_AjoutIndexBudgetsUtilisateurs.estTimeoutLecture(new IllegalStateException()));
    }
}
