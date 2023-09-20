package io.github.vzwingma.finances.budget.serverless.services.operations.business;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.IdsCategoriesEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IParametragesServiceProvider;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationPeriodiciteEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IBudgetAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataBudgets;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataOperations;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
class OperationsServiceTest {

    private OperationsService operationsAppProvider;

    private IParametragesServiceProvider parametragesServiceProvider;
    private IOperationsRepository mockOperationDataProvider;

    @BeforeEach
    public void setup() {
        mockOperationDataProvider = Mockito.mock(IOperationsRepository.class);
        operationsAppProvider = Mockito.spy(new OperationsService());
        IBudgetAppProvider budgetAppProvider = Mockito.mock(BudgetService.class);
        operationsAppProvider.setDataOperationsProvider(mockOperationDataProvider);
        operationsAppProvider.setBudgetService(budgetAppProvider);
        parametragesServiceProvider = Mockito.mock(IParametragesServiceProvider.class);
        operationsAppProvider.setParametragesService(parametragesServiceProvider);
    }



    @Test
    void testUpdateOperation(){

        // When
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        // Opération à ajouter
        LigneOperation operation = MockDataOperations.getOperationPrelevement();
        operation.setEtat(OperationEtatEnum.REALISEE);
        // Test
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest");
        assertEquals(1, listeOperations.size());
        assertEquals(OperationEtatEnum.REALISEE, listeOperations.get(0).getEtat());
        assertNotNull(listeOperations.get(0).getAutresInfos().getDateOperation());
    }



    @Test
    void testUpdateOperationPeriodique(){

        // When
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationMensuelleRealisee());
        // Opération à ajouter
        LigneOperation operation = MockDataOperations.getOperationMensuelleRealisee();
        operation.setEtat(OperationEtatEnum.REALISEE);
        // Test
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest");
        assertEquals(1, listeOperations.size());
        assertEquals(OperationEtatEnum.REALISEE, listeOperations.get(0).getEtat());
        assertEquals(OperationPeriodiciteEnum.MENSUELLE, listeOperations.get(0).getMensualite().getPeriode());
        assertEquals(1, listeOperations.get(0).getMensualite().getProchaineEcheance());


        // Changement de période
        operation.getMensualite().setPeriode(OperationPeriodiciteEnum.PONCTUELLE);
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest");
        assertEquals(OperationPeriodiciteEnum.PONCTUELLE, listeOperations.get(0).getMensualite().getPeriode());
        assertEquals(-1, listeOperations.get(0).getMensualite().getProchaineEcheance());
        operation.getMensualite().setPeriode(OperationPeriodiciteEnum.TRIMESTRIELLE);
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest");
        assertEquals(OperationPeriodiciteEnum.TRIMESTRIELLE, listeOperations.get(0).getMensualite().getPeriode());
        assertEquals(3, listeOperations.get(0).getMensualite().getProchaineEcheance());
    }


    @Test
    void testAddOperation(){

        // When
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        // Opération à ajouter
        LigneOperation operation = MockDataOperations.getOperationPrelevement();
        operation.setId("Test2");
        operation.setEtat(OperationEtatEnum.REALISEE);
        // Test
        operationsAppProvider.addOrReplaceOperation(listeOperations, operation, "userTest");
        assertEquals(2, listeOperations.size());
    }


    @Test
    void testAddOperationIntercompte(){

        // When
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        // Opération à ajouter
        LigneOperation operation = MockDataOperations.getOperationIntercompte();
        operation.setEtat(OperationEtatEnum.REALISEE);
        // Test
        operationsAppProvider.addOperationIntercompte(listeOperations, operation, "vers " + operation.getLibelle(), "userTest");
        assertEquals(2, listeOperations.size());
        assertEquals(OperationEtatEnum.PREVUE, listeOperations.get(1).getEtat());
        assertNull(listeOperations.get(1).getMensualite());
    }


    @Test
    void testAddOperationIntercompteMensuel(){

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
        operationsAppProvider.addOperationIntercompte(listeOperations, operation, "vers " + operation.getLibelle(), "userTest");
        assertEquals(2, listeOperations.size());
        assertEquals(OperationEtatEnum.PREVUE, listeOperations.get(1).getEtat());
        assertEquals(OperationPeriodiciteEnum.MENSUELLE, listeOperations.get(1).getMensualite().getPeriode());
        assertEquals(1, listeOperations.get(1).getMensualite().getProchaineEcheance());
    }

    @Test
    void testAddOperationIntercompteReportee(){

        // When
        List<LigneOperation> listeOperations = new ArrayList<>();
        listeOperations.add(MockDataOperations.getOperationPrelevement());
        // Opération à ajouter
        LigneOperation operation = MockDataOperations.getOperationIntercompte();
        operation.setEtat(OperationEtatEnum.REPORTEE);
        // Test
        operationsAppProvider.addOperationIntercompte(listeOperations, operation, "vers " + operation.getLibelle(), "userTest");
        assertEquals(2, listeOperations.size());
        assertEquals(OperationEtatEnum.REPORTEE, listeOperations.get(1).getEtat());
    }

    @Test
    void testAddOperationRemboursementCatFailure(){

        // When
        Mockito.when(parametragesServiceProvider.getCategorieParId(Mockito.anyString()))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Impossible de trouver la catégorie")));

        // Test
        CompletionException thrown = Assertions.assertThrows(CompletionException.class, () -> operationsAppProvider.createOperationRemboursement(MockDataOperations.getOperationRemboursement(), "userTest").await().indefinitely());
        assertEquals("io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException", thrown.getMessage());
        Mockito.verify(mockOperationDataProvider, Mockito.never()).sauvegardeBudgetMensuel(Mockito.any());

    }

    @Test
    void testAddOperationRemboursementCatUnkown(){

        // When
        Mockito.when(parametragesServiceProvider.getCategorieParId(Mockito.anyString())).thenReturn(Uni.createFrom().nullItem());

        // Test
        CompletionException thrown = Assertions.assertThrows(CompletionException.class, () -> operationsAppProvider.createOperationRemboursement(MockDataOperations.getOperationRemboursement(), "userTest").await().indefinitely());
        assertEquals("io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException", thrown.getMessage());
        Mockito.verify(mockOperationDataProvider, Mockito.never()).sauvegardeBudgetMensuel(Mockito.any());

    }

    @Test
    void testAddOperationRemboursement(){

        // When
        CategorieOperations dep = new CategorieOperations(IdsCategoriesEnum.FRAIS_REMBOURSABLES.getId());
        CategorieOperations.CategorieParente cat = new CategorieOperations.CategorieParente(IdsCategoriesEnum.FRAIS_REMBOURSABLES.getId(), "Frais");
        dep.setCategorieParente(cat);
        Mockito.when(parametragesServiceProvider.getCategorieParId(Mockito.anyString())).thenReturn(Uni.createFrom().item(dep));
        // Test
        LigneOperation operationRemb = operationsAppProvider.createOperationRemboursement(MockDataOperations.getOperationRemboursement(), "userTest").await().indefinitely();

        assertNotNull(operationRemb);
        // Anomalie #208
        assertEquals("TestRemboursement", operationRemb.getLibelle());
    }
}
