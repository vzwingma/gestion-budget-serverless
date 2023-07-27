package io.github.vzwingma.finances.budget.services.operations.business;

import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.CompteClosedException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.github.vzwingma.finances.budget.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.services.operations.business.ports.IOperationsAppProvider;
import io.github.vzwingma.finances.budget.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.services.operations.spi.IComptesServiceProvider;
import io.github.vzwingma.finances.budget.services.operations.test.data.MockDataBudgets;
import io.github.vzwingma.finances.budget.services.operations.test.data.MockDataOperations;
import io.github.vzwingma.finances.budget.services.operations.utils.BudgetDataUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@QuarkusTest
class BudgetServiceTest {

    private IOperationsAppProvider operationsAppProvider;

    private BudgetService budgetAppProvider;
    private IOperationsRepository mockOperationDataProvider;
    private IComptesServiceProvider mockCompteServiceProvider;

    @BeforeEach
    public void setup() {
        mockOperationDataProvider = Mockito.mock(IOperationsRepository.class);
        mockCompteServiceProvider = Mockito.mock(IComptesServiceProvider.class);

        operationsAppProvider = Mockito.spy(new OperationsService());

        budgetAppProvider = Mockito.spy(new BudgetService());
        budgetAppProvider.setDataOperationsProvider(mockOperationDataProvider);
        budgetAppProvider.setComptesService(mockCompteServiceProvider);
        budgetAppProvider.setOperationsAppProvider(operationsAppProvider);

    }

    @Test
    void testGetBudgetWithNoCompte() {

        // Initialisation
        Mockito.when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Compte introuvable")));

        // Test
        CompletionException thrown = assertThrows(CompletionException.class, () -> budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2020)
                .await().indefinitely());
        assertEquals("io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException", thrown.getMessage());
    }


    /**
     * Test du chargement nominal d'un budget actif sur compte actif
     */
    @Test
    void testGetBudgetInactifSurCompteActif() {

        // Initialisation
        Mockito.when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.JANUARY), eq(2022)))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetInactifCompteC1()));

        // Test
        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2022)
                .await().indefinitely();

        // Assertion
        assertNotNull(budgetCharge);
        assertEquals("C1_2022_1", budgetCharge.getId());
        Mockito.verify(budgetAppProvider, Mockito.times(1)).getBudgetMensuel(eq("C1"), any(Month.class), anyInt());
    }


    /**
     * Test du chargement nominal d'un budget actif sur compte actif
     */
    @Test
    void testGetBudgetActifSurCompteActif() {

        // Initialisation
        Mockito.when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        BudgetMensuel b1 = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        BudgetMensuel b0 = MockDataBudgets.getBudgetPrecedentCompteC1();

        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.JANUARY), eq(2022)))
                .thenReturn(Uni.createFrom().item(b1));
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.DECEMBER), eq(2021)))
                .thenReturn(Uni.createFrom().item(b0));


        // Test
        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2022)
                .await().indefinitely();

        // Assertion
        assertNotNull(budgetCharge);
        assertEquals("C1_2022_01", budgetCharge.getId());
        // bien le recacul du solde de fin de budget précédent
        assertEquals(b0.getSoldes().getSoldeAtFinMoisCourant(), budgetCharge.getSoldes().getSoldeAtFinMoisPrecedent());
        // bien le recalcul global des soldes
        assertEquals(1000D, budgetCharge.getSoldes().getSoldeAtMaintenant());
        assertEquals(1123D, budgetCharge.getSoldes().getSoldeAtFinMoisCourant());

        Mockito.verify(mockOperationDataProvider, Mockito.times(2)).chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt());
        Mockito.verify(mockOperationDataProvider, Mockito.times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    @Test
    void testInitNewBudgetOnCompteActif() {

        // Initialisation
        Mockito.when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        BudgetMensuel bcree = MockDataBudgets.getBudgetPrecedentCompteC1();
        bcree.setId("C1_2022_04");
        bcree.setAnnee(2022);
        bcree.setMois(Month.APRIL);
        bcree.setActif(true);
        bcree.setNewBudget(true);
        bcree.getListeOperations().clear();
        BudgetDataUtils.razCalculs(bcree);

        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Budget introuvable")), Uni.createFrom().item(bcree));
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(bcree));


        Mockito.when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class))).thenReturn(Uni.createFrom().item(bcree));
        // Test
        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.MAY, 2022)
                .await().indefinitely();

        // Assertion
        assertNotNull(budgetCharge);
        assertEquals("C1_2022_05", budgetCharge.getId());
        Mockito.verify(budgetAppProvider, Mockito.times(1)).getBudgetMensuel("C1", Month.MAY, 2022);
        Mockito.verify(budgetAppProvider, Mockito.times(1)).getBudgetMensuel("C1_2022_04");
        Mockito.verify(mockOperationDataProvider, Mockito.times(2)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }


    @Test
    void testGetBudgetSurCompteInactif() {

        // Initialisation
        Mockito.when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteInactif()));
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt()))
                .thenReturn(
                        Uni.createFrom().nullItem(),
                        Uni.createFrom().item(MockDataBudgets.getBudgetInactifCompteC1()));
        // Test
        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2022)
                .await().indefinitely();

        // Assertion
        assertNotNull(budgetCharge);
        assertEquals("C1_2022_1", budgetCharge.getId());
        Mockito.verify(mockOperationDataProvider, Mockito.times(1)).chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.JANUARY), eq(2022));
        Mockito.verify(mockOperationDataProvider, Mockito.times(1)).chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.DECEMBER), eq(2021));
    }


    /**
     * Test #121
     */
    @Test
    void testCalculBudget() {
        assertNotNull(this.operationsAppProvider);
        BudgetMensuel budget = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        assertNotNull(budget);

        this.budgetAppProvider.recalculSoldes(budget);
        assertEquals(0, budget.getSoldes().getSoldeAtMaintenant().intValue());
        assertEquals(123, budget.getSoldes().getSoldeAtFinMoisCourant().intValue());

        budget.getSoldes().setSoldeAtFinMoisPrecedent(0D);
        this.budgetAppProvider.recalculSoldes(budget);
        assertEquals(0, budget.getSoldes().getSoldeAtMaintenant().intValue());
        assertEquals(123, budget.getSoldes().getSoldeAtFinMoisCourant().intValue());
    }

    @Test
    void testReinitBudget() {
        // When
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(
                        Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt()))
                .thenReturn(
                        Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));

        Mockito.when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        //Test
        BudgetMensuel budgetReinit = budgetAppProvider.reinitialiserBudgetMensuel(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue().getId()).await().indefinitely();
        // Assertion
        assertNotNull(budgetReinit);
        Mockito.verify(mockOperationDataProvider, Mockito.times(1)).sauvegardeBudgetMensuel(budgetReinit);
    }


    @Test
    void testReinitCompteClosed() {
        // When
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(
                        Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt()))
                .thenReturn(
                        Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        Mockito.when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteInactif()));
        //Test

        CompletionException exception = Assertions.assertThrows(CompletionException.class,
                () -> budgetAppProvider.reinitialiserBudgetMensuel(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue().getId()).await().indefinitely());
        assertEquals(CompteClosedException.class, exception.getCause().getClass());
    }


    @Test
    void testReinitCompteUnknown() {
        // When
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(
                        Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));

        Mockito.when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Compte introuvable")));
        //Test

        CompletionException exception = Assertions.assertThrows(CompletionException.class,
                () -> budgetAppProvider.reinitialiserBudgetMensuel(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue().getId()).await().indefinitely());
        assertEquals(DataNotFoundException.class, exception.getCause().getClass());
    }


    @Test
    void testIsBudgetActif(){
        // When
        Mockito.when(mockOperationDataProvider.isBudgetActif(anyString())).thenReturn(Uni.createFrom().item(true));

        // Test
        assertTrue(budgetAppProvider.isBudgetMensuelActif(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue().getId()).await().indefinitely());
    }


    @Test
    void testSetBudgetInactif(){
        // When
        BudgetMensuel budgetADesactiver = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(anyString())).thenReturn(Uni.createFrom().item(budgetADesactiver));

        // Test
        BudgetMensuel budgetDesactive = budgetAppProvider.setBudgetActif(budgetADesactiver.getId(), false).await().indefinitely();

        assertFalse(budgetDesactive.isActif());
        assertEquals(OperationEtatEnum.REPORTEE, budgetDesactive.getListeOperations().get(0).getEtat());

        Mockito.verify(mockOperationDataProvider, Mockito.times(1)).sauvegardeBudgetMensuel(budgetDesactive);
    }


    @Test
    void testUpdateBudgetCompteClos(){
        // When
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetInactifCompteC1()));

        Mockito.when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteInactif()));

        // Test
        CompletionException exception = Assertions.assertThrows(CompletionException.class,
                () -> budgetAppProvider.addOperationInBudget("idBudget", MockDataOperations.getOperationPrelevement(), "userTest").await().indefinitely());
        assertEquals(CompteClosedException.class, exception.getCause().getClass());

    }
    @Test
    void testUpdateBudget(){
        // When
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));

        Mockito.when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        // Test
        LigneOperation ligneOperation = MockDataOperations.getOperationPrelevement();
        BudgetMensuel budgetMensuelAJour = budgetAppProvider.addOperationInBudget("C1_2022_01", ligneOperation, "userTest").await().indefinitely();
        assertEquals(1, budgetMensuelAJour.getListeOperations().size());

        Mockito.verify(budgetAppProvider, Mockito.times(1)).recalculSoldes(any(BudgetMensuel.class));
        Mockito.verify(mockOperationDataProvider, Mockito.times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));

    }


    @Test
    void testCreateOperationIntercompte(){
        // When
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel("C1_2022_01"))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel("C2_2022_01"))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC2et0operationPrevue()));


        Mockito.when(mockCompteServiceProvider.getCompteById("C1"))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        Mockito.when(mockCompteServiceProvider.getCompteById("C2"))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC2()));

        // Test
        LigneOperation ligneOperation = MockDataOperations.getOperationIntercompte();
        BudgetMensuel budgetMensuelAJour = budgetAppProvider.createOperationsIntercomptes("C1_2022_01", ligneOperation, "C2" , "userTest").await().indefinitely();

        assertEquals("[vers Libelle2] TestIntercompte", budgetMensuelAJour.getListeOperations().get(1).getLibelle());

        assertEquals(2, budgetMensuelAJour.getListeOperations().size());

        Mockito.verify(budgetAppProvider, Mockito.times(2)).recalculSoldes(any(BudgetMensuel.class));
        Mockito.verify(mockOperationDataProvider, Mockito.times(2)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));

        Mockito.verify(budgetAppProvider, Mockito.times(1)).recalculSoldes(budgetMensuelAJour);
        Mockito.verify(mockOperationDataProvider, Mockito.times(1)).sauvegardeBudgetMensuel(budgetMensuelAJour);

    }

    @Test
    void testDeleteOperationInBudget(){
        // When
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));

        Mockito.when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        // Test
        LigneOperation ligneOperation = MockDataOperations.getOperationPrelevement();
        BudgetMensuel budgetMensuelAJour = budgetAppProvider.deleteOperationInBudget("C1_2022_01", "TEST1").await().indefinitely();
        assertEquals(0, budgetMensuelAJour.getListeOperations().size());

        Mockito.verify(budgetAppProvider, Mockito.times(1)).recalculSoldes(any(BudgetMensuel.class));
        Mockito.verify(mockOperationDataProvider, Mockito.times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));

    }


    @Test
    void testDeleteOperationUnkownInBudget(){
        // When
        Mockito.when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));

        Mockito.when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        // Test
        LigneOperation ligneOperation = MockDataOperations.getOperationPrelevement();
        BudgetMensuel budgetMensuelAJour = budgetAppProvider.deleteOperationInBudget("C1_2022_01", "noId").await().indefinitely();
        assertEquals(1, budgetMensuelAJour.getListeOperations().size());

        Mockito.verify(budgetAppProvider, Mockito.times(1)).recalculSoldes(any(BudgetMensuel.class));
        Mockito.verify(mockOperationDataProvider, Mockito.times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }
}
