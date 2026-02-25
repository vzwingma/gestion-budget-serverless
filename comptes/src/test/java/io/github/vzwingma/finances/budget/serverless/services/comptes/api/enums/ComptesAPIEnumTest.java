package io.github.vzwingma.finances.budget.serverless.services.comptes.api.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests unitaires de {@link ComptesAPIEnum}
 */
class ComptesAPIEnumTest {

    @Test
    void testConstantesAPI() {
        assertEquals("/comptes/v2", ComptesAPIEnum.COMPTES_BASE);
        assertEquals("/tous", ComptesAPIEnum.COMPTES_LIST);
        assertEquals("/{idCompte}", ComptesAPIEnum.COMPTES_ID);
        assertEquals("USER_COMPTES", ComptesAPIEnum.COMPTES_ROLE);
    }

    @Test
    void testParamIdCompte() {
        assertNotNull(ComptesAPIEnum.PARAM_ID_COMPTE);
        assertEquals("{idCompte}", ComptesAPIEnum.PARAM_ID_COMPTE);
    }
}

