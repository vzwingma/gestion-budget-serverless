package io.github.vzwingma.finances.budget.services.communs.data.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de CategorieOperations et SsCategorieOperations
 */
class TestCategorieOperations {

    // ====== CategorieOperations ======

    @Test
    void testCategorieOperationsConstructeurParDefaut() {
        CategorieOperations cat = new CategorieOperations();
        assertNotNull(cat.getId()); // UUID généré
        assertNull(cat.getLibelle());
    }

    @Test
    void testCategorieOperationsConstructeurAvecGuid() {
        CategorieOperations cat = new CategorieOperations("guid-123");
        assertEquals("guid-123", cat.getId());
    }

    @Test
    void testCategorieOperationsGettersSetters() {
        CategorieOperations cat = new CategorieOperations("id1");
        cat.setLibelle("Alimentation");
        cat.setActif(true);

        SsCategorieOperations ss1 = new SsCategorieOperations("ss1");
        ss1.setLibelle("Courses");
        Set<SsCategorieOperations> ssSet = new HashSet<>();
        ssSet.add(ss1);
        cat.setListeSSCategories(ssSet);

        assertEquals("id1", cat.getId());
        assertEquals("Alimentation", cat.getLibelle());
        assertTrue(cat.isActif());
        assertEquals(1, cat.getListeSSCategories().size());
    }

    @Test
    void testCategorieOperationsCompareTo() {
        CategorieOperations catA = new CategorieOperations("1");
        catA.setLibelle("Alimentation");

        CategorieOperations catL = new CategorieOperations("2");
        catL.setLibelle("Loisirs");

        assertTrue(catA.compareTo(catL) < 0);
        assertTrue(catL.compareTo(catA) > 0);

        CategorieOperations catA2 = new CategorieOperations("99");
        catA2.setLibelle("Alimentation");
        assertEquals(0, catA.compareTo(catA2));
    }

    @Test
    void testCategorieOperationsEqualsHashCode() {
        CategorieOperations c1 = new CategorieOperations("1");
        c1.setLibelle("Alimentation");

        CategorieOperations c2 = new CategorieOperations("2");
        c2.setLibelle("Alimentation");

        CategorieOperations c3 = new CategorieOperations("3");
        c3.setLibelle("Loisirs");

        assertEquals(c1, c2);  // même libelle
        assertNotEquals(c1, c3);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    // ====== SsCategorieOperations ======

    @Test
    void testSsCategorieOperationsConstructeurParDefaut() {
        SsCategorieOperations ss = new SsCategorieOperations();
        assertNull(ss.getId());
    }

    @Test
    void testSsCategorieOperationsConstructeurAvecGuid() {
        SsCategorieOperations ss = new SsCategorieOperations("guid-ss-456");
        assertEquals("guid-ss-456", ss.getId());
    }

    @Test
    void testSsCategorieOperationsGettersSetters() {
        SsCategorieOperations ss = new SsCategorieOperations("ss1");
        ss.setLibelle("Courses");
        ss.setActif(true);
        ss.setType(CategorieOperationTypeEnum.ESSENTIEL);

        assertEquals("ss1", ss.getId());
        assertEquals("Courses", ss.getLibelle());
        assertTrue(ss.isActif());
        assertEquals(CategorieOperationTypeEnum.ESSENTIEL, ss.getType());
    }

    @Test
    void testSsCategorieOperationsToStringAvecParente() {
        SsCategorieOperations ss = new SsCategorieOperations("ss1");
        ss.setLibelle("Courses");

        SsCategorieOperations.CategorieParente parente = new SsCategorieOperations.CategorieParente("idCat", "Alimentation");
        ss.setCategorieParente(parente);

        String str = ss.toString();
        assertTrue(str.contains("Alimentation"));
        assertTrue(str.contains("Courses"));
    }

    @Test
    void testSsCategorieOperationsToStringSansParente() {
        SsCategorieOperations ss = new SsCategorieOperations("ss1");
        ss.setLibelle("Courses");

        String str = ss.toString();
        assertTrue(str.contains("-"));
        assertTrue(str.contains("Courses"));
    }

    @Test
    void testSsCategorieOperationsCompareTo() {
        SsCategorieOperations ssA = new SsCategorieOperations("1");
        ssA.setLibelle("Alimentation");

        SsCategorieOperations ssL = new SsCategorieOperations("2");
        ssL.setLibelle("Loisirs");

        assertTrue(ssA.compareTo(ssL) < 0);
        assertTrue(ssL.compareTo(ssA) > 0);

        SsCategorieOperations ssA2 = new SsCategorieOperations("99");
        ssA2.setLibelle("Alimentation");
        assertEquals(0, ssA.compareTo(ssA2));
    }

    @Test
    void testSsCategorieOperationsEqualsHashCode() {
        SsCategorieOperations ss1 = new SsCategorieOperations("1");
        ss1.setLibelle("Courses");

        SsCategorieOperations ss2 = new SsCategorieOperations("2");
        ss2.setLibelle("Courses");

        SsCategorieOperations ss3 = new SsCategorieOperations("3");
        ss3.setLibelle("Restaurant");

        assertEquals(ss1, ss2); // même libelle
        assertNotEquals(ss1, ss3);
    }

    // ====== CategorieParente (classe interne) ======

    @Test
    void testCategorieParenteConstructeurParDefaut() {
        SsCategorieOperations.CategorieParente parente = new SsCategorieOperations.CategorieParente();
        assertNull(parente.getId());
        assertNull(parente.getLibelle());
    }

    @Test
    void testCategorieParenteConstructeurAvecParams() {
        SsCategorieOperations.CategorieParente parente = new SsCategorieOperations.CategorieParente("idP", "Alimentation");
        assertEquals("idP", parente.getId());
        assertEquals("Alimentation", parente.getLibelle());
        assertEquals("Alimentation", parente.toString());
    }

    @Test
    void testCategorieParenteGettersSetters() {
        SsCategorieOperations.CategorieParente parente = new SsCategorieOperations.CategorieParente();
        parente.setId("idP2");
        parente.setLibelle("Loisirs");
        assertEquals("idP2", parente.getId());
        assertEquals("Loisirs", parente.getLibelle());
    }

    // ====== CategorieOperationTypeEnum ======

    @Test
    void testCategorieOperationTypeEnum() {
        assertEquals(6, CategorieOperationTypeEnum.values().length);
        assertNotNull(CategorieOperationTypeEnum.valueOf("REVENUS"));
        assertNotNull(CategorieOperationTypeEnum.valueOf("ESSENTIEL"));
        assertNotNull(CategorieOperationTypeEnum.valueOf("PLAISIR"));
        assertNotNull(CategorieOperationTypeEnum.valueOf("ECONOMIES"));
        assertNotNull(CategorieOperationTypeEnum.valueOf("IMPREVUS"));
        assertNotNull(CategorieOperationTypeEnum.valueOf("EXTRAS"));
    }
}

