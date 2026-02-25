package io.github.vzwingma.finances.budget.serverless.services.operations.api.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de {@link OperationsAPIEnum}
 */
class OperationsAPIEnumTest {

    @Test
    void testConstantesBudgetBase() {
        assertEquals("/budgets/v2", OperationsAPIEnum.BUDGET_BASE);
        assertEquals("/budgets/v2/admin", OperationsAPIEnum.BUDGET_ADMIN_BASE);
    }

    @Test
    void testConstantesOperations() {
        assertEquals("/query", OperationsAPIEnum.BUDGET_QUERY);
        assertEquals("/soldes", OperationsAPIEnum.BUDGET_SOLDES);
        assertEquals("/intervalles", OperationsAPIEnum.BUDGET_INTERVALLES);
        assertEquals("USER_OPERATIONS", OperationsAPIEnum.OPERATIONS_ROLE);
    }

    @Test
    void testConstantesParams() {
        assertEquals("{idBudget}", OperationsAPIEnum.PARAM_ID_BUDGET);
        assertEquals("{idCompte}", OperationsAPIEnum.PARAM_ID_COMPTE);
        assertEquals("{idOperation}", OperationsAPIEnum.PARAM_ID_OPERATION);
    }

    @Test
    void testConstantesOperationsPaths() {
        assertNotNull(OperationsAPIEnum.BUDGET_OPERATION);
        assertNotNull(OperationsAPIEnum.BUDGET_OPERATION_BY_ID);
        assertNotNull(OperationsAPIEnum.BUDGET_OPERATION_INTERCOMPTE);
        assertNotNull(OperationsAPIEnum.BUDGET_ETAT);
        assertNotNull(OperationsAPIEnum.OPERATIONS_LIBELLES);
        assertNotNull(OperationsAPIEnum.OPERATIONS_LIBELLES_OVERRIDE);
    }
}

