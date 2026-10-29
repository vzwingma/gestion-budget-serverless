package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.AggregatePublisher;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataBudgets;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import io.quarkus.mongodb.AggregateOptions;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (sans Mongo ni Docker) de {@link OperationDatabaseAdaptor} : la collection et les requêtes Panache sont
 * mockées, les tests vérifient les filtres, tris, mises à jour et options effectivement envoyés au driver.
 * L'exécution réelle contre MongoDB (arrayFilters, encodage BSON des POJO, tri effectif) est couverte par
 * {@code OperationDatabaseAdaptorMongoTest} (Dev Services, CI).
 *
 * @author QALvin
 */
class OperationDatabaseAdaptorTest {

    private static final String ID_OPERATION = "op-42";

    private OperationDatabaseAdaptor adaptor;
    private ReactiveMongoCollection<BudgetMensuel> collection;

    /** Registre avec codec POJO automatique : permet de rendre en JSON les {@code Updates.set(soldes, ...)} */
    private final CodecRegistry registre = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        collection = mock(ReactiveMongoCollection.class);
        adaptor = spy(new OperationDatabaseAdaptor());
        doReturn(collection).when(adaptor).mongoCollection();
    }

    /** Erreur finale d'un {@link Uni} ({@code await()} enveloppe les exceptions contrôlées dans une {@link CompletionException}) */
    private static Throwable erreur(Uni<?> uni) {
        Throwable erreur = assertThrows(Throwable.class, () -> uni.await().indefinitely());
        return erreur instanceof CompletionException && erreur.getCause() != null ? erreur.getCause() : erreur;
    }

    private String json(Bson bson) {
        return bson.toBsonDocument(BsonDocument.class, registre).toJson();
    }

    private static BudgetMensuel budgetRecalcule() {
        BudgetMensuel budget = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        budget.setDateMiseAJour(LocalDateTime.of(2026, 7, 10, 10, 15, 30));
        return budget;
    }

    private static LigneOperation operationOriginale() {
        LigneOperation operation = MockDataOperations.getOperationPrelevement();
        operation.setId(ID_OPERATION);
        return operation;
    }

    private void miseAJourRepond(long... matchedCounts) {
        Uni<UpdateResult>[] resultats = new Uni[matchedCounts.length];
        for (int i = 0; i < matchedCounts.length; i++) {
            resultats[i] = Uni.createFrom().item(UpdateResult.acknowledged(matchedCounts[i], matchedCounts[i], null));
        }
        var stub = when(collection.updateOne(any(Bson.class), any(Bson.class), any(UpdateOptions.class)));
        stub.thenReturn(resultats[0], java.util.Arrays.copyOfRange(resultats, 1, resultats.length));
    }

    // ====== compenseOperation : création ($pull) ======

    @Test
    void testCompenseCreation_pullParId_etChampsCalcules() {
        miseAJourRepond(1);
        BudgetMensuel budget = budgetRecalcule();

        adaptor.compenseOperation(budget, ID_OPERATION, null, -1).await().indefinitely();

        ArgumentCaptor<Bson> filtre = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> miseAJour = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<UpdateOptions> options = ArgumentCaptor.forClass(UpdateOptions.class);
        verify(collection, times(1)).updateOne(filtre.capture(), miseAJour.capture(), options.capture());

        assertEquals("{\"_id\": \"" + budget.getId() + "\"}", json(filtre.getValue()));
        String update = json(miseAJour.getValue());
        assertTrue(update.contains("\"$pull\": {\"listeOperations\": {\"_id\": \"" + ID_OPERATION + "\"}}"), update);
        // Seuls listeOperations et les champs calculés sont modifiés : jamais le document entier
        for (String champ : List.of("soldes", "totauxParCategories", "totauxParSSCategories", "totauxParTypeCategories", "dateMiseAJour")) {
            assertTrue(update.contains("\"" + champ + "\""), "Champ calculé réécrit : " + champ + " dans " + update);
        }
        assertFalse(update.contains("\"actif\"") || update.contains("\"mois\""), "Les autres champs du budget ne sont pas réécrits : " + update);
        assertNull(options.getValue().getArrayFilters(), "Pas d'arrayFilters pour un $pull");
    }

    @Test
    void testCompenseCreation_budgetIntrouvable_budgetNotFoundException() {
        miseAJourRepond(0);

        Uni<Void> compensation = adaptor.compenseOperation(budgetRecalcule(), ID_OPERATION, null, -1);

        assertInstanceOf(BudgetNotFoundException.class, erreur(compensation));
        verify(collection, times(1)).updateOne(any(Bson.class), any(Bson.class), any(UpdateOptions.class));
    }

    // ====== compenseOperation : modification ($set positionnel, puis $push) ======

    @Test
    void testCompenseModification_setPositionnelAvecArrayFilters() {
        miseAJourRepond(1);
        BudgetMensuel budget = budgetRecalcule();

        adaptor.compenseOperation(budget, ID_OPERATION, operationOriginale(), 3).await().indefinitely();

        ArgumentCaptor<Bson> filtre = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> miseAJour = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<UpdateOptions> options = ArgumentCaptor.forClass(UpdateOptions.class);
        verify(collection, times(1)).updateOne(filtre.capture(), miseAJour.capture(), options.capture());

        String filtreJson = json(filtre.getValue());
        assertTrue(filtreJson.contains("\"$elemMatch\": {\"_id\": \"" + ID_OPERATION + "\"}"),
                "La restauration en place n'a lieu que si la ligne est présente : " + filtreJson);
        String update = json(miseAJour.getValue());
        assertTrue(update.contains("\"listeOperations.$[op]\""), "Mise à jour positionnelle filtrée : " + update);
        assertTrue(update.contains("\"" + ID_OPERATION + "\""), update);
        assertNotNull(options.getValue().getArrayFilters());
        assertEquals(1, options.getValue().getArrayFilters().size());
        assertEquals("{\"op._id\": \"" + ID_OPERATION + "\"}", json(options.getValue().getArrayFilters().getFirst()));
    }

    @Test
    void testCompenseModification_ligneDisparue_pushAuRangOriginal() {
        // 1re mise à jour : aucune ligne à restaurer (matched 0) ; 2e : $push accepté
        miseAJourRepond(0, 1);
        BudgetMensuel budget = budgetRecalcule();

        adaptor.compenseOperation(budget, ID_OPERATION, operationOriginale(), 2).await().indefinitely();

        ArgumentCaptor<Bson> filtre = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<Bson> miseAJour = ArgumentCaptor.forClass(Bson.class);
        ArgumentCaptor<UpdateOptions> options = ArgumentCaptor.forClass(UpdateOptions.class);
        verify(collection, times(2)).updateOne(filtre.capture(), miseAJour.capture(), options.capture());

        String filtrePush = json(filtre.getAllValues().get(1));
        assertTrue(filtrePush.contains("\"listeOperations._id\": {\"$ne\": \"" + ID_OPERATION + "\"}"),
                "Ré-insertion uniquement si l'id est absent : " + filtrePush);
        String updatePush = json(miseAJour.getAllValues().get(1));
        assertTrue(updatePush.contains("\"$push\""), updatePush);
        assertTrue(updatePush.contains("\"$position\": 2"), "Rang d'origine conservé : " + updatePush);
        assertTrue(updatePush.contains("\"$each\""), updatePush);
    }

    @Test
    void testCompenseModification_rangInconnu_pushEnFinDeListe() {
        miseAJourRepond(0, 1);

        adaptor.compenseOperation(budgetRecalcule(), ID_OPERATION, operationOriginale(), -1).await().indefinitely();

        ArgumentCaptor<Bson> miseAJour = ArgumentCaptor.forClass(Bson.class);
        verify(collection, times(2)).updateOne(any(Bson.class), miseAJour.capture(), any(UpdateOptions.class));
        String updatePush = json(miseAJour.getAllValues().get(1));
        assertTrue(updatePush.contains("\"$push\""), updatePush);
        assertFalse(updatePush.contains("$position"), "Rang inconnu : pas de $position, insertion en fin de liste : " + updatePush);
    }

    @Test
    void testCompenseModification_ligneReapparue_pushRefuse_illegalStateException() {
        // Ni restauration en place (matched 0), ni ré-insertion (matched 0 : la ligne est réapparue ou le budget a disparu)
        miseAJourRepond(0, 0);

        Uni<Void> compensation = adaptor.compenseOperation(budgetRecalcule(), ID_OPERATION, operationOriginale(), 1);

        IllegalStateException erreur = assertThrows(IllegalStateException.class, () -> compensation.await().indefinitely());
        assertTrue(erreur.getMessage().contains(ID_OPERATION), erreur.getMessage());
        verify(collection, times(2)).updateOne(any(Bson.class), any(Bson.class), any(UpdateOptions.class));
    }

    // ====== compenseOperation : gardes ======

    @Test
    void testCompense_idNull_refuseAvantToutEcriture() {
        Uni<Void> compensation = adaptor.compenseOperation(budgetRecalcule(), null, null, -1);

        assertInstanceOf(IllegalArgumentException.class, assertThrows(RuntimeException.class, () -> compensation.await().indefinitely()));
        verifyNoInteractions(collection);
    }

    @Test
    void testCompense_idVide_refuseAvantToutEcriture() {
        Uni<Void> compensation = adaptor.compenseOperation(budgetRecalcule(), "  ", operationOriginale(), 0);

        assertInstanceOf(IllegalArgumentException.class, assertThrows(RuntimeException.class, () -> compensation.await().indefinitely()));
        verifyNoInteractions(collection);
    }

    @Test
    void testCompense_dateMiseAJourAbsente_nullPointerException() {
        BudgetMensuel budget = budgetRecalcule();
        budget.setDateMiseAJour(null);

        assertThrows(NullPointerException.class, () -> adaptor.compenseOperation(budget, ID_OPERATION, null, -1));
        verifyNoInteractions(collection);
    }

    @Test
    void testCompense_erreurMongoPropagee() {
        MongoTimeoutException timeout = new MongoTimeoutException("timeout");
        when(collection.updateOne(any(Bson.class), any(Bson.class), any(UpdateOptions.class))).thenReturn(Uni.createFrom().failure(timeout));

        Uni<Void> compensation = adaptor.compenseOperation(budgetRecalcule(), ID_OPERATION, null, -1);

        assertSame(timeout, assertThrows(MongoTimeoutException.class, () -> compensation.await().indefinitely()));
    }

    // ====== tri _id ======

    @SuppressWarnings("unchecked")
    private ReactivePanacheQuery<BudgetMensuel> queryVide() {
        ReactivePanacheQuery<BudgetMensuel> query = mock(ReactivePanacheQuery.class);
        ReactivePanacheQuery<Object> projection = mock(ReactivePanacheQuery.class);
        when(query.stream()).thenReturn(Multi.createFrom().empty());
        when(query.project(any())).thenReturn((ReactivePanacheQuery) projection);
        when(projection.stream()).thenReturn((Multi) Multi.createFrom().empty());
        return query;
    }

    @Test
    void testChargeSoldesBudgetMensuel_triParIdCroissant() {
        ReactivePanacheQuery<BudgetMensuel> query = queryVide();
        doReturn(query).when(adaptor).find(anyString(), any(Sort.class), any(Object[].class));

        adaptor.chargeSoldesBudgetMensuel("C1", Month.MARCH, 2026).collect().asList().await().indefinitely();

        ArgumentCaptor<Sort> tri = ArgumentCaptor.forClass(Sort.class);
        verify(adaptor).find(anyString(), tri.capture(), any(Object[].class));
        assertEquals(1, tri.getValue().getColumns().size());
        assertEquals("_id", tri.getValue().getColumns().getFirst().getName(), "Tri sur _id (ordre chronologique des budgets)");
        assertEquals(Sort.Direction.Ascending, tri.getValue().getColumns().getFirst().getDirection());
    }

    @Test
    void testOverrideLibellesOperations_triParIdCroissant() {
        ReactivePanacheQuery<BudgetMensuel> query = queryVide();
        doReturn(query).when(adaptor).find(anyString(), any(Sort.class), any(Object[].class));

        adaptor.overrideLibellesOperations("C1", List.of()).collect().asList().await().indefinitely();

        ArgumentCaptor<Sort> tri = ArgumentCaptor.forClass(Sort.class);
        verify(adaptor).find(eq("idCompteBancaire=?1"), tri.capture(), any(Object[].class));
        assertEquals("_id", tri.getValue().getColumns().getFirst().getName());
        assertEquals(Sort.Direction.Ascending, tri.getValue().getColumns().getFirst().getDirection());
    }

    // ====== chargeDernierBudgetAvant : filtre ======

    @SuppressWarnings("unchecked")
    private Document[] capturerFiltreDernierBudget(String idCompte, Month mois, int annee) {
        ReactivePanacheQuery<BudgetMensuel> query = mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(Uni.createFrom().nullItem());
        doReturn(query).when(adaptor).find(any(Document.class), any(Document.class));

        assertNull(adaptor.chargeDernierBudgetAvant(idCompte, mois, annee).await().indefinitely(), "Aucun budget : valeur null");

        ArgumentCaptor<Document> filtre = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<Document> tri = ArgumentCaptor.forClass(Document.class);
        verify(adaptor).find(filtre.capture(), tri.capture());
        return new Document[]{filtre.getValue(), tri.getValue()};
    }

    @Test
    void testChargeDernierBudgetAvant_idCompteAvecUnderscore_egaliteExacteEtBornesDeLId() {
        Document[] requete = capturerFiltreDernierBudget("compte_1", Month.MARCH, 2026);

        Document filtre = requete[0];
        assertEquals("compte_1", filtre.get("idCompteBancaire"),
                "Égalité exacte sur le compte : 'a' et 'a_1' partagent un préfixe d'_id");
        Document bornes = filtre.get("_id", Document.class);
        // Borne M incluse ($lte), borne basse = préfixe du compte
        assertEquals("compte_1_2026_03", bornes.get("$lte"));
        assertEquals("compte_1_", bornes.get("$gte"));
        assertEquals(new Document("_id", -1), requete[1], "Tri décroissant : le premier résultat est le dernier budget");
    }

    @Test
    void testChargeDernierBudgetAvant_deuxComptesDePrefixeCommun_filtresDistincts() {
        Document[] pourA = capturerFiltreDernierBudget("a", Month.JANUARY, 2026);

        assertEquals("a", pourA[0].get("idCompteBancaire"));
        assertEquals("a_", pourA[0].get("_id", Document.class).get("$gte"));
        // Un budget du compte 'a_1' (_id 'a_1_2026_01') passe la borne d'_id du compte 'a'...
        assertTrue("a_1_2026_01".compareTo(pourA[0].get("_id", Document.class).getString("$lte")) <= 0
                && "a_1_2026_01".compareTo(pourA[0].get("_id", Document.class).getString("$gte")) >= 0,
                "Illustration : la borne d'_id seule ne suffit pas à isoler le compte 'a'");
        // ... c'est l'égalité sur idCompteBancaire qui l'exclut
        assertFalse("a_1".equals(pourA[0].get("idCompteBancaire")));
    }

    // ====== lecture par id : absence vs erreur technique (R5) ======

    @SuppressWarnings("unchecked")
    private void stubFindParId(Uni<BudgetMensuel> resultat) {
        ReactivePanacheQuery<BudgetMensuel> query = mock(ReactivePanacheQuery.class);
        when(query.firstResult()).thenReturn(resultat);
        doReturn(query).when(adaptor).find(anyString(), any(Object[].class));
    }

    @Test
    void testChargeBudgetMensuelParId_absent_budgetNotFound404() {
        stubFindParId(Uni.createFrom().nullItem());

        Uni<BudgetMensuel> chargement = adaptor.chargeBudgetMensuel("C1_2026_03");

        assertInstanceOf(BudgetNotFoundException.class, erreur(chargement));
    }

    @Test
    void testChargeBudgetMensuelParId_erreurMongo_propageeSansEtreConfondueAvecUneAbsence() {
        MongoTimeoutException timeout = new MongoTimeoutException("timeout");
        stubFindParId(Uni.createFrom().failure(timeout));

        Uni<BudgetMensuel> chargement = adaptor.chargeBudgetMensuel("C1_2026_03");

        assertSame(timeout, assertThrows(MongoTimeoutException.class, () -> chargement.await().indefinitely()));
    }

    @Test
    void testChargeBudgetMensuelParCompte_absent_budgetNotFound_erreurMongoPropagee() {
        CompteBancaire compte = MockDataBudgets.getCompteC1();
        stubFindParId(Uni.createFrom().nullItem());
        assertInstanceOf(BudgetNotFoundException.class, erreur(adaptor.chargeBudgetMensuel(compte, Month.MARCH, 2026)));

        MongoTimeoutException timeout = new MongoTimeoutException("timeout");
        stubFindParId(Uni.createFrom().failure(timeout));
        assertSame(timeout, assertThrows(MongoTimeoutException.class,
                () -> adaptor.chargeBudgetMensuel(compte, Month.MARCH, 2026).await().indefinitely()));
    }

    @Test
    void testChargeBudgetMensuelParId_budgetSansListeOperations_pasDeNpeDuLogDeDebug() {
        BudgetMensuel budget = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        budget.setListeOperations(null);
        stubFindParId(Uni.createFrom().item(budget));

        assertSame(budget, adaptor.chargeBudgetMensuel(budget.getId()).await().indefinitely());
    }

    // ====== agrégation des libellés ======

    @SuppressWarnings("unchecked")
    @Test
    void testGetLibellesOperations_pipelineGroupEtMaxTime7Secondes() {
        when(collection.aggregate(any(List.class), eq(Document.class), any(AggregateOptions.class))).thenReturn(Multi.createFrom().empty());

        adaptor.getLibellesOperations("C1").collect().asList().await().indefinitely();

        ArgumentCaptor<List<? extends Bson>> pipeline = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<AggregateOptions> options = ArgumentCaptor.forClass(AggregateOptions.class);
        verify(collection).aggregate(pipeline.capture(), eq(Document.class), options.capture());

        List<? extends Bson> etapes = pipeline.getValue();
        assertEquals(3, etapes.size());
        assertEquals("{\"$match\": {\"idCompteBancaire\": \"C1\"}}", json(etapes.get(0)));
        assertTrue(json(etapes.get(1)).contains("\"$unwind\"") && json(etapes.get(1)).contains("\"$listeOperations\""), json(etapes.get(1)));
        String group = json(etapes.get(2));
        assertTrue(group.contains("\"$group\""), group);
        assertTrue(group.contains("\"libelle\": \"$listeOperations.libelle\""), group);
        assertTrue(group.contains("\"categorieId\": \"$listeOperations.categorie._id\""), group);
        assertTrue(group.contains("\"ssCategorieId\": \"$listeOperations.ssCategorie._id\""), group);
        assertTrue(group.contains("\"n\": {\"$sum\": 1}"), "Compteur d'occurrences : " + group);

        assertEquals(7, OperationDatabaseAdaptor.LIBELLES_MAX_TIME_SECONDES);
    }

    /**
     * Intention de l'AP004 T1.2d (point 6, R4) : l'agrégation des libellés est interrompue côté serveur après 7 s
     * ({@code maxTimeMS}). BUG constaté : {@code io.quarkus.mongodb.AggregateOptions#apply} (quarkus-mongodb-client 3.40.1)
     * traduit {@code maxTime(7, SECONDS)} en {@code AggregatePublisher.maxAwaitTime(7, SECONDS)} (le champ maxTime
     * n'est jamais envoyé via {@code maxTime}) : aucun {@code maxTimeMS} n'est transmis au serveur, la limite de 7 s
     * est donc inopérante (seul le {@code read-timeout} client de 8 s s'applique). Test écrit et désactivé, à réactiver
     * après correction.
     */
    @org.junit.jupiter.api.Disabled("BUG AP004 T1.3 — AggregateOptions.maxTime est appliqué comme maxAwaitTime par Quarkus : aucun maxTimeMS envoyé au serveur (agrégation des libellés non bornée côté serveur)")
    @SuppressWarnings("unchecked")
    @Test
    void testGetLibellesOperations_maxTimeServeurEffectivement7Secondes() {
        when(collection.aggregate(any(List.class), eq(Document.class), any(AggregateOptions.class))).thenReturn(Multi.createFrom().empty());
        adaptor.getLibellesOperations("C1").collect().asList().await().indefinitely();
        ArgumentCaptor<AggregateOptions> options = ArgumentCaptor.forClass(AggregateOptions.class);
        verify(collection).aggregate(any(List.class), eq(Document.class), options.capture());

        AggregatePublisher<Document> publisher = mock(AggregatePublisher.class);
        when(publisher.maxTime(anyLong(), any(TimeUnit.class))).thenReturn(publisher);
        when(publisher.maxAwaitTime(anyLong(), any(TimeUnit.class))).thenReturn(publisher);
        options.getValue().apply(publisher);
        verify(publisher).maxTime(7L, TimeUnit.SECONDS);
    }
}
