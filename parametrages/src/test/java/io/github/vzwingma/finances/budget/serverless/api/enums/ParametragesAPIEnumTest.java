package io.github.vzwingma.finances.budget.serverless.api.enums;

import io.github.vzwingma.finances.budget.serverless.services.parametrages.api.enums.ParametragesAPIEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de ParametragesAPIEnum
 */
class ParametragesAPIEnumTest {

    @Test
    void testConstantesBase() {
        assertEquals("/parametres/v2", ParametragesAPIEnum.PARAMS_BASE);
        assertEquals("/categories", ParametragesAPIEnum.PARAMS_CATEGORIES);
    }

    @Test
    void testConstantesCategorieId() {
        assertNotNull(ParametragesAPIEnum.PARAMS_CATEGORIE_ID);
        assertEquals("/{idCategorie}", ParametragesAPIEnum.PARAMS_CATEGORIE_ID);
    }
}

