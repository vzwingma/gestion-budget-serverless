package io.github.vzwingma.finances.budget.serverless.services.operations.business;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.IdsCategoriesEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationPeriodiciteEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IBudgetAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IParametragesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OperationsServiceTest {

    private OperationsService operationsAppProvider;

    private IOperationsRepository mockOperationDataProvider;

    @BeforeEach
    public void setup() {
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

        // When
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        // Opération à ajouter
        LigneOperation operation = MockDataOperations.getOperationPrelevement();
        operation.setEtat(OperationEtatEnum.REALISEE);
        // Test
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest", null);
        assertEquals(1, listeOperations.size());
        assertEquals(OperationEtatEnum.REALISEE, listeOperations.getFirst().getEtat());
        assertNotNull(listeOperations.getFirst().getAutresInfos().getDateOperation());
    }


    @Test
    void testUpdateOperationPeriodique() throws DataNotFoundException {

        // When
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationMensuelleRealisee());
        // Opération à ajouter
        LigneOperation operation = MockDataOperations.getOperationMensuelleRealisee();
        operation.setEtat(OperationEtatEnum.REALISEE);
        // Test
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest", null);
        assertEquals(1, listeOperations.size());
        assertEquals(OperationEtatEnum.REALISEE, listeOperations.getFirst().getEtat());
        assertEquals(OperationPeriodiciteEnum.MENSUELLE, listeOperations.getFirst().getMensualite().getPeriode());
        assertEquals(1, listeOperations.getFirst().getMensualite().getProchaineEcheance());


        // Changement de période
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

        // When
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        // Opération à ajouter
        LigneOperation operation = MockDataOperations.getOperationPrelevement();
        operation.setId("Test2");
        operation.setEtat(OperationEtatEnum.REALISEE);
        // Test
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest", null);
        assertEquals(2, listeOperations.size());
    }


    @Test
    void testAddOperationVirementInterne() {

        // When
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        // Opération à ajouter
        LigneOperation operation = MockDataOperations.getOperationIntercompte();
        operation.setEtat(OperationEtatEnum.REALISEE);
        // Test
        operationsAppProvider.addOperationVirementInterne(listeOperations, operation, "vers " + operation.getLibelle(), "userTest");
        assertEquals(2, listeOperations.size());
        assertEquals(OperationEtatEnum.PREVUE, listeOperations.get(1).getEtat());
        assertNull(listeOperations.get(1).getMensualite());
    }


    @Test
    void testAddOperationVirementInterneMensuel() {

        // When
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        // Opération à ajouter
        LigneOperation operation = MockDataOperations.getOperationIntercompte();
        operation.setEtat(OperationEtatEnum.REALISEE);
        operation.setMensualite(new LigneOperation.Mensualite());
        operation.getMensualite().setPeriode(OperationPeriodiciteEnum.MENSUELLE);
        operation.getMensualite().setProchaineEcheance(1);
        // Test
        operationsAppProvider.addOperationVirementInterne(listeOperations, operation, "vers " + operation.getLibelle(), "userTest");
        assertEquals(2, listeOperations.size());
        assertEquals(OperationEtatEnum.PREVUE, listeOperations.get(1).getEtat());
        assertEquals(OperationPeriodiciteEnum.MENSUELLE, listeOperations.get(1).getMensualite().getPeriode());
        assertEquals(1, listeOperations.get(1).getMensualite().getProchaineEcheance());
    }

    @Test
    void testAddOperationVirementInterneReportee() {

        // When
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        // Opération à ajouter
        LigneOperation operation = MockDataOperations.getOperationIntercompte();
        operation.setEtat(OperationEtatEnum.REPORTEE);
        // Test
        operationsAppProvider.addOperationVirementInterne(listeOperations, operation, "vers " + operation.getLibelle(), "userTest");
        assertEquals(2, listeOperations.size());
        assertEquals(OperationEtatEnum.REPORTEE, listeOperations.get(1).getEtat());
    }

    @Test
    void testAddOperationRemboursementCatFailure() {

        // When

        // Test
        Assertions.assertThrows(DataNotFoundException.class, () -> operationsAppProvider.addOrReplaceOperation(new ArrayList<>(), MockDataOperations.getOperationRemboursement(), "userTest", null));
        Mockito.verify(mockOperationDataProvider, Mockito.never()).sauvegardeBudgetMensuel(Mockito.any());

    }

    @Test
    void testAddOperationRemboursement() throws DataNotFoundException {

        // When
        CategorieOperations dep = new CategorieOperations(IdsCategoriesEnum.SS_CAT_FRAIS_REMBOURSABLE_SANTE_PHARMACIE.getId());
        dep.setLibelle(IdsCategoriesEnum.SS_CAT_FRAIS_REMBOURSABLE_SANTE_PHARMACIE.getLibelle());
        CategorieOperations.CategorieParente cat = new CategorieOperations.CategorieParente(IdsCategoriesEnum.CAT_FRAIS_REMBOURSABLE_SANTE.getId(), IdsCategoriesEnum.CAT_FRAIS_REMBOURSABLE_SANTE.getLibelle());
        dep.setCategorieParente(cat);
        // Test
        List<LigneOperation> operations = new ArrayList<>();
        operationsAppProvider.addOrReplaceOperation(operations, MockDataOperations.getOperationRemboursement(), "userTest", dep);

        assertNotNull(operations.get(0));
        assertNotNull(operations.get(1));
        // Anomalie #208
        assertEquals("TestRemboursement", operations.get(0).getLibelle());
        assertEquals("TestRemboursement", operations.get(1).getLibelle());
    }

}
