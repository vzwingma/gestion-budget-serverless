package io.github.vzwingma.finances.budget.services.communs.data.model;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de CompteBancaire
 */
class TestCompteBancaire {

    @Test
    void testConstructeurParDefaut() {
        CompteBancaire compte = new CompteBancaire();
        assertNull(compte.getId());
        assertNull(compte.getLibelle());
        // isActif() retourne true par défaut si actif est null
        assertTrue(compte.isActif());
    }

    @Test
    void testGettersSetters() {
        CompteBancaire compte = new CompteBancaire();
        compte.setId("id123");
        compte.setLibelle("Compte Courant");
        compte.setActif(true);
        compte.setItemIcon("icon.png");
        compte.setOrdre(1);

        assertEquals("id123", compte.getId());
        assertEquals("Compte Courant", compte.getLibelle());
        assertTrue(compte.isActif());
        assertEquals("icon.png", compte.getItemIcon());
        assertEquals(1, compte.getOrdre());
    }

    @Test
    void testIsActifNullReturnTrue() {
        CompteBancaire compte = new CompteBancaire();
        compte.setActif(null);
        assertTrue(compte.isActif());
    }

    @Test
    void testIsActifFalse() {
        CompteBancaire compte = new CompteBancaire();
        compte.setActif(false);
        assertFalse(compte.isActif());
    }

    @Test
    void testGetCompteInactif() {
        CompteBancaire compte = CompteBancaire.getCompteInactif();
        assertNotNull(compte);
        assertEquals("inactif", compte.getId());
        assertEquals("Compte inactif", compte.getLibelle());
        assertFalse(compte.isActif());
    }

    @Test
    void testToString() {
        CompteBancaire compte = new CompteBancaire();
        compte.setId("id123");
        compte.setLibelle("Compte Courant");
        compte.setActif(true);
        String str = compte.toString();
        assertTrue(str.contains("id123"));
        assertTrue(str.contains("Compte Courant"));
        assertTrue(str.contains("true"));
    }

    @Test
    void testEqualsEtHashCode() {
        CompteBancaire c1 = new CompteBancaire();
        c1.setId("id1");

        CompteBancaire c2 = new CompteBancaire();
        c2.setId("id1");

        CompteBancaire c3 = new CompteBancaire();
        c3.setId("id2");

        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertSame(c1, c1);
    }

    @Test
    void testEqualsNullId() {
        CompteBancaire c1 = new CompteBancaire();
        CompteBancaire c2 = new CompteBancaire();
        assertEquals(c1, c2); // les deux ont id null

        CompteBancaire c3 = new CompteBancaire();
        c3.setId("id3");
        assertNotEquals(c1, c3);
    }

    @Test
    void testEqualsNull() {
        CompteBancaire c1 = new CompteBancaire();
        c1.setId("id1");
        assertNotEquals(null, c1);
    }

    @Test
    void testEqualsAutreType() {
        CompteBancaire c1 = new CompteBancaire();
        c1.setId("id1");
        assertNotEquals("unString", c1);
    }

    @Test
    void testProprietaires() {
        CompteBancaire compte = new CompteBancaire();

        CompteBancaire.Proprietaire prop = new CompteBancaire.Proprietaire();
        prop.setId(new ObjectId());
        prop.setLogin("user@example.com");

        compte.setProprietaires(List.of(prop));
        assertNotNull(compte.getProprietaires());
        assertEquals(1, compte.getProprietaires().size());
        assertEquals("user@example.com", compte.getProprietaires().getFirst().getLogin());
    }
}

