package io.github.vzwingma.finances.budget.serverless.services.operations.business;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.IdsCategoriesEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.TotauxCategorie;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationPeriodiciteEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IBudgetAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IParametragesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataBudgets;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataOperations;
import io.github.vzwingma.finances.budget.serverless.services.operations.utils.BudgetDataUtils;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperationTypeEnum;
import io.github.vzwingma.finances.budget.services.communs.data.model.SsCategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OperationsServiceTest {

    private OperationsService operationsAppProvider;
    private IOperationsRepository mockOperationDataProvider;

    @BeforeEach
    void setup() {
        mockOperationDataProvider = Mockito.mock(IOperationsRepository.class);
        operationsAppProvider = Mockito.spy(new OperationsService());
        IBudgetAppProvider budgetAppProvider = Mockito.mock(BudgetService.class);
        operationsAppProvider.setDataOperationsProvider(mockOperationDataProvider);
        operationsAppProvider.setBudgetService(budgetAppProvider);
        IParametragesServiceProvider mockParam = Mockito.mock(IParametragesServiceProvider.class);
        operationsAppProvider.setParametragesService(mockParam);
    }

    @Test
    void testUpdateOperation() throws DataNotFoundException {
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        LigneOperation operation = MockDataOperations.getOperationPrelevement();
        operation.setEtat(OperationEtatEnum.REALISEE);
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest", null);
        assertEquals(1, listeOperations.size());
        assertEquals(OperationEtatEnum.REALISEE, listeOperations.getFirst().getEtat());
        assertNotNull(listeOperations.getFirst().getAutresInfos().getDateOperation());
    }

    @Test
    void testUpdateOperationPeriodique() throws DataNotFoundException {
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationMensuelleRealisee());
        LigneOperation operation = MockDataOperations.getOperationMensuelleRealisee();
        operation.setEtat(OperationEtatEnum.REALISEE);
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest", null);
        assertEquals(1, listeOperations.size());
        assertEquals(OperationEtatEnum.REALISEE, listeOperations.getFirst().getEtat());
        assertEquals(OperationPeriodiciteEnum.MENSUELLE, listeOperations.getFirst().getMensualite().getPeriode());
        assertEquals(1, listeOperations.getFirst().getMensualite().getProchaineEcheance());

        operation.getMensualite().setPeriode(OperationPeriodiciteEnum.PONCTUELLE);
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest", null);
        assertEquals(OperationPeriodiciteEnum.PONCTUELLE, listeOperations.getFirst().getMensualite().getPeriode());
        assertEquals(-1, listeOperations.getFirst().getMensualite().getProchaineEcheance());
        operation.getMensualite().setPeriode(OperationPeriodiciteEnum.TRIMESTRIELLE);
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest", null);
        assertEquals(OperationPeriodiciteEnum.TRIMESTRIELLE, listeOperations.getFirst().getMensualite().getPeriode());
        assertEquals(3, listeOperations.getFirst().getMensualite().getProchaineEcheance());
    }

    @Test
    void testAddOperation() throws DataNotFoundException {
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        LigneOperation operation = MockDataOperations.getOperationPrelevement();
        operation.setId("Test2");
        operation.setEtat(OperationEtatEnum.REALISEE);
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest", null);
        assertEquals(2, listeOperations.size());
    }

    @Test
    void testAddOperationEtatNull() throws DataNotFoundException {
        // Opération avec état null : suppression (pas d'ajout)
        List<LigneOperation> listeOperations = new ArrayList<>();
        LigneOperation operation = MockDataOperations.getOperationPrelevement();
        operation.setId("TestNull");
        operation.setEtat(null);
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest", null);
        assertEquals(0, listeOperations.size());
    }

    @Test
    void testAddOperationVirementInterne() {
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        LigneOperation operation = MockDataOperations.getOperationIntercompte();
        operation.setEtat(OperationEtatEnum.REALISEE);
        operationsAppProvider.addOperationVirementInterne(listeOperations, operation, "vers " + operation.getLibelle(), "userTest");
        assertEquals(2, listeOperations.size());
        assertEquals(OperationEtatEnum.PREVUE, listeOperations.get(1).getEtat());
        assertNull(listeOperations.get(1).getMensualite());
    }

    @Test
    void testAddOperationVirementInterneMensuel() {
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        LigneOperation operation = MockDataOperations.getOperationIntercompte();
        operation.setEtat(OperationEtatEnum.REALISEE);
        operation.setMensualite(new LigneOperation.Mensualite());
        operation.getMensualite().setPeriode(OperationPeriodiciteEnum.MENSUELLE);
        operation.getMensualite().setProchaineEcheance(1);
        operationsAppProvider.addOperationVirementInterne(listeOperations, operation, "vers " + operation.getLibelle(), "userTest");
        assertEquals(2, listeOperations.size());
        assertEquals(OperationEtatEnum.PREVUE, listeOperations.get(1).getEtat());
        assertEquals(OperationPeriodiciteEnum.MENSUELLE, listeOperations.get(1).getMensualite().getPeriode());
        assertEquals(1, listeOperations.get(1).getMensualite().getProchaineEcheance());
    }

    @Test
    void testAddOperationVirementInterneReportee() {
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        LigneOperation operation = MockDataOperations.getOperationIntercompte();
        operation.setEtat(OperationEtatEnum.REPORTEE);
        operationsAppProvider.addOperationVirementInterne(listeOperations, operation, "vers " + operation.getLibelle(), "userTest");
        assertEquals(2, listeOperations.size());
        assertEquals(OperationEtatEnum.REPORTEE, listeOperations.get(1).getEtat());
    }

    @Test
    void testAddOperationVirementInterneAnnulee() {
        List<LigneOperation> listeOperations = new ArrayList<>();
        LigneOperation operation = MockDataOperations.getOperationIntercompte();
        operation.setEtat(OperationEtatEnum.ANNULEE);
        operationsAppProvider.addOperationVirementInterne(listeOperations, operation, "vers test", "userTest");
        assertEquals(1, listeOperations.size());
        assertEquals(OperationEtatEnum.ANNULEE, listeOperations.getFirst().getEtat());
    }

    @Test
    void testAddOperationRemboursementCatFailure() {
        Assertions.assertThrows(DataNotFoundException.class,
                () -> operationsAppProvider.addOrReplaceOperation(new ArrayList<>(), MockDataOperations.getOperationRemboursement(), "userTest", null));
        Mockito.verify(mockOperationDataProvider, Mockito.never()).sauvegardeBudgetMensuel(Mockito.any());
    }

    @Test
    void testAddOperationRemboursement() throws DataNotFoundException {
        SsCategorieOperations dep = new SsCategorieOperations(IdsCategoriesEnum.SS_CAT_FRAIS_REMBOURSABLE_SANTE_PHARMACIE.getId());
        dep.setLibelle(IdsCategoriesEnum.SS_CAT_FRAIS_REMBOURSABLE_SANTE_PHARMACIE.getLibelle());
        SsCategorieOperations.CategorieParente cat = new SsCategorieOperations.CategorieParente(IdsCategoriesEnum.CAT_FRAIS_REMBOURSABLE_SANTE.getId(), IdsCategoriesEnum.CAT_FRAIS_REMBOURSABLE_SANTE.getLibelle());
        dep.setCategorieParente(cat);
        List<LigneOperation> operations = new ArrayList<>();
        operationsAppProvider.addOrReplaceOperation(operations, MockDataOperations.getOperationRemboursement(), "userTest", dep);
        assertNotNull(operations.get(0));
        assertNotNull(operations.get(1));
        assertEquals("TestRemboursement", operations.get(0).getLibelle());
        assertEquals("TestRemboursement", operations.get(1).getLibelle());
    }

    @Test
    void testDeleteOperationExistante() {
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        operationsAppProvider.deleteOperation(listeOperations, "TEST1");
        assertTrue(listeOperations.isEmpty());
    }

    @Test
    void testDeleteOperationInexistante() {
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        operationsAppProvider.deleteOperation(listeOperations, "INCONNU");
        assertEquals(1, listeOperations.size());
    }

    @Test
    void testDeleteOperationListeVide() {
        List<LigneOperation> listeOperations = new ArrayList<>();
        operationsAppProvider.deleteOperation(listeOperations, "TEST1");
        assertTrue(listeOperations.isEmpty());
    }

    @Test
    void testGetIntervalleBudgets() {
        Instant[] intervalle = {Instant.now().minusSeconds(3600), Instant.now()};
        Mockito.when(mockOperationDataProvider.chargeIntervalleBudgets("C1"))
                .thenReturn(Uni.createFrom().item(intervalle));
        Instant[] result = operationsAppProvider.getIntervalleBudgets("C1").await().indefinitely();
        assertNotNull(result);
        assertEquals(2, result.length);
    }

    @Test
    void testCalculSoldesOperationRealisee() {
        BudgetMensuel budget = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        BudgetDataUtils.razCalculs(budget);

        LigneOperation op = MockDataOperations.getOperationPrelevement();
        op.setEtat(OperationEtatEnum.REALISEE);
        op.setValeur(50D);

        Map<String, TotauxCategorie> totCat = new HashMap<>();
        Map<String, TotauxCategorie> totSsCat = new HashMap<>();
        Map<String, TotauxCategorie> totTypes = new HashMap<>();
        operationsAppProvider.calculSoldes(List.of(op), budget.getSoldes(), totCat, totSsCat, totTypes);

        assertEquals(50D, budget.getSoldes().getSoldeAtMaintenant());
        assertEquals(50D, budget.getSoldes().getSoldeAtFinMoisCourant());
        assertFalse(totCat.isEmpty());
        assertFalse(totSsCat.isEmpty());
    }

    @Test
    void testCalculSoldesOperationPrevue() {
        BudgetMensuel budget = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        BudgetDataUtils.razCalculs(budget);

        LigneOperation op = MockDataOperations.getOperationPrelevement();
        op.setEtat(OperationEtatEnum.PREVUE);
        op.setValeur(75D);

        Map<String, TotauxCategorie> totCat = new HashMap<>();
        Map<String, TotauxCategorie> totSsCat = new HashMap<>();
        Map<String, TotauxCategorie> totTypes = new HashMap<>();
        operationsAppProvider.calculSoldes(List.of(op), budget.getSoldes(), totCat, totSsCat, totTypes);

        assertEquals(0D, budget.getSoldes().getSoldeAtMaintenant());
        assertEquals(75D, budget.getSoldes().getSoldeAtFinMoisCourant());
        // totaux catégorie fin de mois aussi mis à jour
        assertFalse(totCat.isEmpty());
    }

    @Test
    void testCalculSoldesOperationSansCategorie() {
        BudgetMensuel budget = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        BudgetDataUtils.razCalculs(budget);

        LigneOperation op = new LigneOperation();
        op.setId("NO_CAT");
        op.setEtat(OperationEtatEnum.REALISEE);
        op.setValeur(10D);

        Map<String, TotauxCategorie> totCat = new HashMap<>();
        Map<String, TotauxCategorie> totSsCat = new HashMap<>();
        Map<String, TotauxCategorie> totTypes = new HashMap<>();
        assertDoesNotThrow(() ->
                operationsAppProvider.calculSoldes(List.of(op), budget.getSoldes(), totCat, totSsCat, totTypes));
    }

    @Test
    void testCalculSoldesAvecSsCategorieTypeNull() {
        BudgetMensuel budget = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        BudgetDataUtils.razCalculs(budget);

        LigneOperation op = MockDataOperations.getOperationPrelevement();
        op.setEtat(OperationEtatEnum.REALISEE);
        op.setValeur(30D);
        op.getSsCategorie().setType(null); // doit être initialisé à ESSENTIEL

        Map<String, TotauxCategorie> totCat = new HashMap<>();
        Map<String, TotauxCategorie> totSsCat = new HashMap<>();
        Map<String, TotauxCategorie> totTypes = new HashMap<>();
        operationsAppProvider.calculSoldes(List.of(op), budget.getSoldes(), totCat, totSsCat, totTypes);

        assertFalse(totTypes.isEmpty());
        assertTrue(totTypes.containsKey(CategorieOperationTypeEnum.ESSENTIEL.name()));
    }

    @Test
    void testCalculSoldesPlusieursOperations() {
        BudgetMensuel budget = MockDataBudgets.getBudgetActifCompteC1et3operationsRealisees();
        BudgetDataUtils.razCalculs(budget);
        // S'assurer que toutes les opérations ont une valeur non nulle
        budget.getListeOperations().forEach(op -> {
            if (op.getValeur() == null) op.setValeur(0D);
        });

        Map<String, TotauxCategorie> totCat = new HashMap<>();
        Map<String, TotauxCategorie> totSsCat = new HashMap<>();
        Map<String, TotauxCategorie> totTypes = new HashMap<>();
        operationsAppProvider.calculSoldes(budget.getListeOperations(), budget.getSoldes(), totCat, totSsCat, totTypes);
        assertNotNull(budget.getSoldes());
    }
}
