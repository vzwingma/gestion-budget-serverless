package io.github.vzwingma.finances.budget.serverless.services.operations.business;

import com.mongodb.MongoTimeoutException;
import io.github.vzwingma.finances.budget.serverless.services.operations.api.override.APIExceptionsHandler;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.IdsCategoriesEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IComptesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IParametragesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataBudgets;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BadParametersException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.CompteClosedException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du virement interne (AP004 T1.1d, ADR-007) : gardes avant toute écriture (400 / 404 / 406),
 * écritures séquentielles source puis cible, compensation ciblée de la source (création : retrait ; modification :
 * restauration de la ligne d'origine à son rang), échec de compensation journalisé sans masquer l'erreur d'origine.
 * Dépôt, comptes et paramétrages mockés ; {@link OperationsService} réel (espionné).
 *
 * @author QALvin
 */
class BudgetServiceVirementTest {

    private BudgetService budgetService;
    private OperationsService operationsService;
    private IOperationsRepository repository;
    private IComptesServiceProvider comptes;

    private BudgetMensuel budgetSource;
    private BudgetMensuel budgetDestination;

    private final Clock clockFixe = Clock.fixed(Instant.parse("2026-07-10T10:15:30Z"), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        repository = mock(IOperationsRepository.class);
        comptes = mock(IComptesServiceProvider.class);

        operationsService = spy(new OperationsService());
        operationsService.setDataOperationsProvider(repository);
        operationsService.setClock(clockFixe);

        budgetService = spy(new BudgetService());
        budgetService.setDataOperationsProvider(repository);
        budgetService.setComptesService(comptes);
        budgetService.setOperationsAppProvider(operationsService);
        budgetService.setParametragesService(mock(IParametragesServiceProvider.class));
        budgetService.setClock(clockFixe);

        budgetSource = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        budgetDestination = MockDataBudgets.getBudgetActifCompteC2et0operationPrevue();

        when(comptes.getCompteById("C1")).thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(comptes.getCompteById("C2")).thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC2()));
        when(repository.chargeBudgetMensuel(budgetSource.getId())).thenReturn(Uni.createFrom().item(budgetSource));
        when(repository.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.JANUARY), eq(2022)))
                .thenReturn(Uni.createFrom().item(budgetDestination));
        when(repository.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.DECEMBER), eq(2021)))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetPrecedentCompteC1()));
        when(repository.sauvegardeBudgetMensuel(any(BudgetMensuel.class)))
                .thenAnswer(inv -> Uni.createFrom().item(inv.<BudgetMensuel>getArgument(0)));
        when(repository.compenseOperation(any(), any(), any(), anyInt())).thenReturn(Uni.createFrom().voidItem());
    }

    private static LigneOperation virement(String id, String libelle) {
        LigneOperation operation = MockDataOperations.getOperationIntercompte();
        operation.setId(id);
        operation.setLibelle(libelle);
        operation.setEtat(OperationEtatEnum.PREVUE);
        return operation;
    }

    /**
     * La destination est enregistrée une 1re fois au chargement du mois (recalcul du solde de M-1), puis une 2e fois avec
     * l'opération : l'échec est simulé sur ce 2e enregistrement (l'opération de la source a alors déjà été écrite).
     */
    private void cibleEchoueALEcritureDeLOperation(Throwable echec) {
        when(repository.sauvegardeBudgetMensuel(budgetDestination))
                .thenReturn(Uni.createFrom().item(budgetDestination), Uni.createFrom().failure(echec));
    }

    @Test
    void testEchecCible_auChargementDuMois_sourceEgalementCompensee() {
        MongoTimeoutException timeout = new MongoTimeoutException("cible indisponible au chargement");
        when(repository.sauvegardeBudgetMensuel(budgetDestination)).thenReturn(Uni.createFrom().failure(timeout));

        assertSame(timeout, erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), virement("virement-chargement", "x"), "C2", "userTest")));

        verify(repository, times(1)).compenseOperation(any(BudgetMensuel.class), eq("virement-chargement"), isNull(), eq(-1));
    }

    private Throwable erreur(Uni<?> uni) {
        Throwable erreur = assertThrows(Throwable.class, () -> uni.await().indefinitely());
        return erreur instanceof CompletionException && erreur.getCause() != null ? erreur.getCause() : erreur;
    }

    private static void assertAucuneEcriture(IOperationsRepository repository) {
        verify(repository, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
        verify(repository, never()).compenseOperation(any(), any(), any(), anyInt());
    }

    // ====== nominal ======

    @Test
    void testVirement_nominal_sourceEtCibleEcritesDansLOrdre_sansCompensation() {
        LigneOperation virement = virement("virement-1", "Épargne");

        BudgetMensuel resultat = budgetService.createOperationsVirementInterne(budgetSource.getId(), virement, "C2", "userTest")
                .await().indefinitely();

        assertSame(budgetSource, resultat);
        InOrder ordre = inOrder(repository);
        ordre.verify(repository).sauvegardeBudgetMensuel(budgetSource);
        // La destination est enregistrée deux fois : au chargement du mois (recalcul du solde de M-1), puis après l'ajout de l'opération
        ordre.verify(repository, times(2)).sauvegardeBudgetMensuel(budgetDestination);
        verify(repository, times(3)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
        verify(repository, never()).compenseOperation(any(), any(), any(), anyInt());
        // Un seul appel de lecture du compte de destination
        verify(comptes, times(1)).getCompteById("C2");
        assertTrue(budgetSource.getListeOperations().stream().anyMatch(op -> "[vers C2] Épargne".equals(op.getLibelle())));
        assertTrue(budgetDestination.getListeOperations().stream().anyMatch(op -> op.getLibelle() != null && op.getLibelle().startsWith("[depuis C1]")));
    }

    // ====== gardes ======

    @Test
    void testGarde_compteDestinationClos_406_avantToutEcriture() {
        when(comptes.getCompteById("C2")).thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteInactif()));

        Throwable erreur = erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), virement("v", "x"), "C2", "userTest"));

        assertInstanceOf(CompteClosedException.class, erreur);
        assertEquals(406, new APIExceptionsHandler().toResponse((CompteClosedException) erreur).getStatus());
        assertAucuneEcriture(repository);
    }

    @Test
    void testGarde_virementVersLeMemeCompte_400_avantToutAcces() {
        Throwable erreur = erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), virement("v", "x"), "C1", "userTest"));

        assertInstanceOf(BadParametersException.class, erreur);
        assertEquals(400, new APIExceptionsHandler().toResponse((BadParametersException) erreur).getStatus());
        verify(comptes, never()).getCompteById(anyString());
        assertAucuneEcriture(repository);
    }

    @Test
    void testGarde_operationQuiNEstPasUnVirementInterne_400() {
        LigneOperation autre = MockDataOperations.getOperationPrelevement();
        autre.setEtat(OperationEtatEnum.PREVUE);

        Throwable erreur = erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), autre, "C2", "userTest"));

        assertInstanceOf(BadParametersException.class, erreur);
        assertAucuneEcriture(repository);
    }

    @Test
    void testGarde_ssCategorieAbsente_400() {
        LigneOperation sansSsCategorie = virement("v", "x");
        sansSsCategorie.setSsCategorie(null);

        assertInstanceOf(BadParametersException.class,
                erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), sansSsCategorie, "C2", "userTest")));
        assertAucuneEcriture(repository);
    }

    @Test
    void testGarde_compteDestinationNull_404_pasDeNpe() {
        when(comptes.getCompteById("C2")).thenReturn(Uni.createFrom().nullItem());

        Throwable erreur = erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), virement("v", "x"), "C2", "userTest"));

        assertInstanceOf(DataNotFoundException.class, erreur);
        assertEquals(404, new APIExceptionsHandler().toResponse((DataNotFoundException) erreur).getStatus());
        assertAucuneEcriture(repository);
    }

    @Test
    void testGarde_compteDestinationInconnuDuServiceComptes404_traduitEnDataNotFound() {
        when(comptes.getCompteById("C2")).thenReturn(Uni.createFrom().failure(new WebApplicationException(Response.status(404).build())));

        Throwable erreur = erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), virement("v", "x"), "C2", "userTest"));

        assertInstanceOf(DataNotFoundException.class, erreur);
        assertAucuneEcriture(repository);
    }

    @Test
    void testGarde_autreErreurDuServiceComptes_propageeInchangee() {
        WebApplicationException indisponible = new WebApplicationException(Response.status(503).build());
        when(comptes.getCompteById("C2")).thenReturn(Uni.createFrom().failure(indisponible));

        assertSame(indisponible, erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), virement("v", "x"), "C2", "userTest")));
        assertAucuneEcriture(repository);
    }

    // ====== F2a ======

    @Test
    void testF2a_dataNotFoundDeAddOrReplaceOperation_propagee_sansEcritureNiCompensation() throws Exception {
        DataNotFoundException introuvable = new DataNotFoundException("Opération introuvable");
        doThrow(introuvable).when(operationsService).addOrReplaceOperation(any(), any(), anyString(), any());

        assertSame(introuvable, erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), virement("v", "x"), "C2", "userTest")));

        assertAucuneEcriture(repository);
        verify(repository, never()).chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt());
    }

    // ====== échec partiel et compensation ======

    @Test
    void testEchecCible_creation_sourceCompenseeParRetrait_erreurDOrigineePropagee() {
        MongoTimeoutException timeout = new MongoTimeoutException("cible indisponible");
        cibleEchoueALEcritureDeLOperation(timeout);
        LigneOperation virement = virement("virement-creation", "Épargne");

        assertSame(timeout, erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), virement, "C2", "userTest")));

        // Création : retrait de l'opération (operationOriginale null, rang inconnu)
        ArgumentCaptor<BudgetMensuel> budgetCompense = ArgumentCaptor.forClass(BudgetMensuel.class);
        verify(repository, times(1)).compenseOperation(budgetCompense.capture(), eq("virement-creation"), isNull(), eq(-1));
        assertSame(budgetSource, budgetCompense.getValue());
        assertTrue(budgetSource.getListeOperations().stream().noneMatch(op -> "virement-creation".equals(op.getId())),
                "L'opération est retirée du budget source en mémoire avant le recalcul");
        assertNotNull(budgetSource.getDateMiseAJour());
        // Seule la source a été enregistrée en base (par save) : la compensation passe par compenseOperation, pas par un save complet
        verify(repository, times(1)).sauvegardeBudgetMensuel(budgetSource);
    }

    @Test
    void testEchecCible_modification_ligneDOrigineRestauree_aSonRang() {
        // L'opération existe déjà dans la source (2e position) : le virement la modifie
        LigneOperation avant = virement("virement-existant", "Libellé d'origine");
        avant.setValeur(50D);
        budgetSource.getListeOperations().add(avant);
        final int rangAttendu = budgetSource.getListeOperations().indexOf(avant);
        MongoTimeoutException timeout = new MongoTimeoutException("cible indisponible");
        cibleEchoueALEcritureDeLOperation(timeout);
        LigneOperation modification = virement("virement-existant", "Libellé modifié");
        modification.setValeur(75D);

        assertSame(timeout, erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), modification, "C2", "userTest")));

        ArgumentCaptor<LigneOperation> originale = ArgumentCaptor.forClass(LigneOperation.class);
        verify(repository, times(1)).compenseOperation(any(BudgetMensuel.class), eq("virement-existant"), originale.capture(), eq(rangAttendu));
        assertSame(avant, originale.getValue(), "La ligne d'origine capturée avant l'écriture source est restaurée");
        assertEquals("Libellé d'origine", originale.getValue().getLibelle());
        assertEquals(50D, originale.getValue().getValeur());
        // En mémoire aussi : la ligne d'origine est de retour à son rang
        assertSame(avant, budgetSource.getListeOperations().get(rangAttendu));
    }

    @Test
    void testEchecCible_sansIdDansLaRequete_unIdEstGenereEtUtilisePourLaCompensation() {
        MongoTimeoutException timeout = new MongoTimeoutException("cible indisponible");
        cibleEchoueALEcritureDeLOperation(timeout);
        LigneOperation virement = virement(null, "Sans id");

        erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), virement, "C2", "userTest"));

        assertNotNull(virement.getId(), "L'id est garanti avant la première écriture");
        verify(repository, times(1)).compenseOperation(any(BudgetMensuel.class), eq(virement.getId()), isNull(), anyInt());
    }

    @Test
    void testEchecCompensation_erreurDOrigineConservee_traceErrorAvecIdentifiants() {
        MongoTimeoutException origine = new MongoTimeoutException("cible indisponible");
        cibleEchoueALEcritureDeLOperation(origine);
        when(repository.compenseOperation(any(), any(), any(), anyInt()))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("compensation KO")));

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
        final java.util.logging.Logger loggerService = java.util.logging.Logger.getLogger(BudgetService.class.getName());
        loggerService.addHandler(capture);
        Throwable erreur;
        try {
            erreur = erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), virement("virement-ko", "x"), "C2", "userTest"));
        } finally {
            loggerService.removeHandler(capture);
        }

        // C'est l'erreur d'origine (écriture cible), jamais celle de la compensation, qui est propagée
        assertSame(origine, erreur);
        LogRecord echec = logs.stream()
                .filter(l -> l.getLevel().intValue() >= Level.SEVERE.intValue()
                        && l.getMessage() != null && l.getMessage().contains("ECHEC de la compensation du virement interne"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Trace ERROR « ECHEC de la compensation » attendue, logs : " + logs.stream().map(LogRecord::getMessage).toList()));
        String parametres = Arrays.toString(echec.getParameters());
        assertTrue(parametres.contains("virement-ko") && parametres.contains("C1") && parametres.contains("C2"),
                "Identifiants opération, source et destination attendus dans la trace : " + parametres);
    }

    @Test
    void testEchecSource_cibleJamaisEcrite_pasDeCompensation() {
        MongoTimeoutException timeout = new MongoTimeoutException("source indisponible");
        when(repository.sauvegardeBudgetMensuel(budgetSource)).thenReturn(Uni.createFrom().failure(timeout));

        assertSame(timeout, erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), virement("v", "x"), "C2", "userTest")));

        verify(repository, never()).chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt());
        verify(repository, never()).sauvegardeBudgetMensuel(budgetDestination);
        verify(repository, never()).compenseOperation(any(), any(), any(), anyInt());
    }

    @Test
    void testBudgetSourceAbsent_404_aucuneEcriture() {
        when(repository.chargeBudgetMensuel(budgetSource.getId()))
                .thenReturn(Uni.createFrom().failure(new io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException("absent")));

        Throwable erreur = erreur(budgetService.createOperationsVirementInterne(budgetSource.getId(), virement("v", "x"), "C2", "userTest"));

        assertInstanceOf(io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException.class, erreur);
        assertAucuneEcriture(repository);
    }
}
