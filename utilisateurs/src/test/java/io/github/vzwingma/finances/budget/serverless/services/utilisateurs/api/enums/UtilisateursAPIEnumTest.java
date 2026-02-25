package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de UtilisateursAPIEnum
 */
class UtilisateursAPIEnumTest {

    @Test
    void testConstantesBase() {
        assertEquals("/utilisateurs/v2", UtilisateursAPIEnum.USERS_BASE);
    }

    @Test
    void testConstantesPaths() {
        assertEquals("/lastaccessdate", UtilisateursAPIEnum.USERS_ACCESS_DATE);
        assertEquals("/preferences", UtilisateursAPIEnum.USERS_PREFS);
    }

    @Test
    void testConstantesRoles() {
        assertEquals("USER_UTILISATEURS", UtilisateursAPIEnum.UTILISATEURS_ROLE);
    }
}

