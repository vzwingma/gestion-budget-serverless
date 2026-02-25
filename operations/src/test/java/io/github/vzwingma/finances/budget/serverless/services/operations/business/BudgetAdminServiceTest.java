package io.github.vzwingma.finances.budget.serverless.services.operations.business;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.admin.LibelleAvantApres;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataBudgets;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@QuarkusTest
class BudgetAdminServiceTest {

    private BudgetAdminService budgetAdminService;
    private IOperationsRepository mockRepository;

    @BeforeEach
    void setup() {
        mockRepository = Mockito.mock(IOperationsRepository.class);
        budgetAdminService = new BudgetAdminService();
        budgetAdminService.setDataOperationsProvider(mockRepository);
    }

    @Test
    void testOverrideLibellesOperations() {
        BudgetMensuel budget = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        Mockito.when(mockRepository.overrideLibellesOperations(anyString(), anyList()))
                .thenReturn(Multi.createFrom().item(budget));

        LibelleAvantApres libelle = new LibelleAvantApres();
        libelle.setAvant("avant");
        libelle.setApres("après");

        List<String> result = budgetAdminService
                .overrideLibellesOperations("C1", List.of(libelle))
                .collect().asList().await().indefinitely();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(budget.getId(), result.getFirst());
    }

    @Test
    void testOverrideLibellesOperationsListeVide() {
        Mockito.when(mockRepository.overrideLibellesOperations(anyString(), anyList()))
                .thenReturn(Multi.createFrom().empty());

        List<String> result = budgetAdminService
                .overrideLibellesOperations("C1", List.of())
                .collect().asList().await().indefinitely();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

