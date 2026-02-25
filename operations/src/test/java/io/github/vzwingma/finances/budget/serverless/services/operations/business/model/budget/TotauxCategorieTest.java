package io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class TotauxCategorieTest {

    @Test
    void testAjouterATotalAtMaintenant() {
        TotauxCategorie tc = new TotauxCategorie();
        tc.ajouterATotalAtMaintenant(100.5);
        tc.ajouterATotalAtMaintenant(50.25);
        assertEquals(150.75, tc.getTotalAtMaintenant());
    }

    @Test
    void testAjouterATotalAtFinMoisCourant() {
        TotauxCategorie tc = new TotauxCategorie();
        tc.ajouterATotalAtFinMoisCourant(200.0);
        tc.ajouterATotalAtFinMoisCourant(-50.5);
        assertEquals(149.5, tc.getTotalAtFinMoisCourant());
    }

    @Test
    void testToString() {
        TotauxCategorie tc = new TotauxCategorie();
        tc.setLibelleCategorie("Alimentation");
        tc.ajouterATotalAtMaintenant(100.0);
        tc.ajouterATotalAtFinMoisCourant(200.0);
        String str = tc.toString();
        assertTrue(str.contains("Alimentation"));
        assertTrue(str.contains("100.0"));
        assertTrue(str.contains("200.0"));
    }

    @Test
    void testValeursParDefaut() {
        TotauxCategorie tc = new TotauxCategorie();
        assertEquals(0D, tc.getTotalAtMaintenant());
        assertEquals(0D, tc.getTotalAtFinMoisCourant());
        assertNull(tc.getLibelleCategorie());
    }
}

