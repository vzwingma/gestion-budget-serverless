package io.github.vzwingma.finances.budget.services.communs.data.trace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires complets de BusinessTraceContext (put, remove, clear, get)
 */
class TestBusinessTraceContextComplete {

    @BeforeEach
    void setUp() {
        BusinessTraceContext.getclear();
        MDC.clear();
    }

    @Test
    void testGetRetourneLInstance() {
        BusinessTraceContext ctx = BusinessTraceContext.get();
        assertNotNull(ctx);
        assertSame(ctx, BusinessTraceContext.get()); // Singleton
    }

    @Test
    void testGetclearRetourneLInstance() {
        BusinessTraceContext ctx = BusinessTraceContext.getclear();
        assertNotNull(ctx);
    }

    @Test
    void testPutAjouteUneCleMDC() {
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.USER, "userTest");
        assertEquals("userTest", MDC.get(BusinessTraceContextKeyEnum.USER.getKeyId()));
    }

    @Test
    void testPutPlusieursCles() {
        BusinessTraceContext.get()
                .put(BusinessTraceContextKeyEnum.USER, "userTest")
                .put(BusinessTraceContextKeyEnum.COMPTE, "compte123")
                .put(BusinessTraceContextKeyEnum.BUDGET, "budget456")
                .put(BusinessTraceContextKeyEnum.OPERATION, "op789");

        assertEquals("userTest", MDC.get(BusinessTraceContextKeyEnum.USER.getKeyId()));
        assertEquals("compte123", MDC.get(BusinessTraceContextKeyEnum.COMPTE.getKeyId()));
        assertEquals("budget456", MDC.get(BusinessTraceContextKeyEnum.BUDGET.getKeyId()));
        assertEquals("op789", MDC.get(BusinessTraceContextKeyEnum.OPERATION.getKeyId()));
    }

    @Test
    void testRemoveSupprimeCle() {
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.USER, "userTest");
        assertNotNull(MDC.get(BusinessTraceContextKeyEnum.USER.getKeyId()));

        BusinessTraceContext.get().remove(BusinessTraceContextKeyEnum.USER);
        assertNull(MDC.get(BusinessTraceContextKeyEnum.USER.getKeyId()));
    }

    @Test
    void testClearSupprimeToutesLesCles() {
        BusinessTraceContext.get()
                .put(BusinessTraceContextKeyEnum.USER, "userTest")
                .put(BusinessTraceContextKeyEnum.COMPTE, "compte123");

        BusinessTraceContext.getclear();

        assertNull(MDC.get(BusinessTraceContextKeyEnum.USER.getKeyId()));
        assertNull(MDC.get(BusinessTraceContextKeyEnum.COMPTE.getKeyId()));
    }

    @Test
    void testBusinessTraceContextKeyEnumValues() {
        assertEquals("idUser", BusinessTraceContextKeyEnum.USER.getKeyId());
        assertEquals("idCompte", BusinessTraceContextKeyEnum.COMPTE.getKeyId());
        assertEquals("idBudget", BusinessTraceContextKeyEnum.BUDGET.getKeyId());
        assertEquals("idOperation", BusinessTraceContextKeyEnum.OPERATION.getKeyId());
    }

    @Test
    void testBudgetContextMDCMisAJourApresMultiplePuts() {
        BusinessTraceContext.get()
                .put(BusinessTraceContextKeyEnum.USER, "user1")
                .put(BusinessTraceContextKeyEnum.COMPTE, "compte2");

        String budgetContext = MDC.get("budgetContext");
        assertNotNull(budgetContext);
        assertTrue(budgetContext.contains("idUser:user1"));
        assertTrue(budgetContext.contains("idCompte:compte2"));
    }
}

