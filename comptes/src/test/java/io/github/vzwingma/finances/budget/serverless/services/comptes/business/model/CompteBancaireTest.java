package io.github.vzwingma.finances.budget.serverless.services.comptes.business.model;

import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du modèle {@link CompteBancaire}
 */
class CompteBancaireTest {

    @Test
    void testIsActifTrue() {
        CompteBancaire compte = new CompteBancaire();
        compte.setActif(true);
        assertTrue(compte.isActif());
    }

    @Test
    void testIsActifFalse() {
        CompteBancaire compte = new CompteBancaire();
        compte.setActif(false);
        assertFalse(compte.isActif());
    }

    @Test
    void testIsActifParDefautVraiSiNull() {
        // actif null => true par défaut
        CompteBancaire compte = new CompteBancaire();
        assertTrue(compte.isActif());
    }

    @Test
    void testGetCompteInactif() {
        CompteBancaire inactif = CompteBancaire.getCompteInactif();
        assertNotNull(inactif);
        assertEquals("inactif", inactif.getId());
        assertFalse(inactif.isActif());
    }

    @Test
    void testToString() {
        CompteBancaire compte = new CompteBancaire();
        compte.setId("C1");
        compte.setLibelle("TestLibelle");
        compte.setActif(true);
        String str = compte.toString();
        assertTrue(str.contains("C1"));
        assertTrue(str.contains("TestLibelle"));
    }

    @Test
    void testEqualsDeuxObjetsMemeId() {
        CompteBancaire c1 = new CompteBancaire();
        c1.setId("C1");
        CompteBancaire c2 = new CompteBancaire();
        c2.setId("C1");
        assertEquals(c1, c2);
    }

    @Test
    void testEqualsDifferentsIds() {
        CompteBancaire c1 = new CompteBancaire();
        c1.setId("C1");
        CompteBancaire c2 = new CompteBancaire();
        c2.setId("C2");
        assertNotEquals(c1, c2);
    }

    @Test
    void testEqualsIdNull() {
        CompteBancaire c1 = new CompteBancaire();
        CompteBancaire c2 = new CompteBancaire();
        assertEquals(c1, c2);
    }

    @Test
    void testEqualsNullObj() {
        CompteBancaire c1 = new CompteBancaire();
        c1.setId("C1");
        assertNotEquals(null, c1);
    }

    @Test
    void testEqualsAutreType() {
        CompteBancaire c1 = new CompteBancaire();
        c1.setId("C1");
        assertNotEquals("C1", c1);
    }

    @Test
    void testEqualsIdNullVsNonNull() {
        CompteBancaire c1 = new CompteBancaire();
        CompteBancaire c2 = new CompteBancaire();
        c2.setId("C2");
        assertNotEquals(c1, c2);
    }

    @Test
    void testHashCode() {
        CompteBancaire c1 = new CompteBancaire();
        c1.setId("C1");
        CompteBancaire c2 = new CompteBancaire();
        c2.setId("C1");
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testHashCodeIdNull() {
        CompteBancaire compte = new CompteBancaire();
        // ne doit pas lever d'exception
        assertDoesNotThrow(compte::hashCode);
    }

    @Test
    void testProprietaireGettersSetters() {
        CompteBancaire.Proprietaire proprietaire = new CompteBancaire.Proprietaire();
        ObjectId objectId = new ObjectId();
        proprietaire.setId(objectId);
        proprietaire.setLogin("testLogin");

        assertEquals(objectId, proprietaire.getId());
        assertEquals("testLogin", proprietaire.getLogin());
    }

    @Test
    void testCompteBancaireGettersSetters() {
        CompteBancaire compte = new CompteBancaire();
        compte.setId("C1");
        compte.setLibelle("Livret A");
        compte.setItemIcon("img/bank.png");
        compte.setOrdre(3);

        assertEquals("C1", compte.getId());
        assertEquals("Livret A", compte.getLibelle());
        assertEquals("img/bank.png", compte.getItemIcon());
        assertEquals(3, compte.getOrdre());
    }
}

