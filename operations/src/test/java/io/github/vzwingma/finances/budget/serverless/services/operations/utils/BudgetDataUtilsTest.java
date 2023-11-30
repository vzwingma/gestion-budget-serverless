package io.github.vzwingma.finances.budget.serverless.services.operations.utils;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationPeriodiciteEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataCategories;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BudgetDataUtilsTest {

    @Test
    void testDoubleFromString() {
        assertNull(BudgetDataUtils.getValueFromString(null));
        assertNull(BudgetDataUtils.getValueFromString("123/0"));
        assertEquals(123.3D, BudgetDataUtils.getValueFromString("123.3"));
        assertEquals(123.3D, BudgetDataUtils.getValueFromString("123,3"));
        assertEquals(-123.3D, BudgetDataUtils.getValueFromString("-123,3"));
    }


    @Test
    void testMaxDateOperations() {

        LocalDate c = LocalDate.now();
        LigneOperation depense1 = new LigneOperation();
        depense1.setId("Op1");
        depense1.setAutresInfos(new LigneOperation.AddInfos());
        depense1.getAutresInfos().setDateOperation(c);

        LigneOperation depense2 = new LigneOperation();
        depense2.setId("Op2");
        c = c.withDayOfMonth(28);
        depense2.setAutresInfos(new LigneOperation.AddInfos());
        depense2.getAutresInfos().setDateOperation(c);

        LigneOperation depense3 = new LigneOperation();
        depense3.setId("Op3");
        LocalDate c3 = LocalDate.now().withDayOfMonth(12).withMonth(10).withYear(2050);
        depense3.setAutresInfos(new LigneOperation.AddInfos());
        depense3.getAutresInfos().setDateOperation(c3);

        List<LigneOperation> depenses = new ArrayList<>(Arrays.asList(depense1, depense2, depense3));
        LocalDate cd = BudgetDataUtils.getMaxDateListeOperations(depenses);

        assertEquals(Month.OCTOBER.getValue(), cd.get(ChronoField.MONTH_OF_YEAR));
    }


    @Test
    void getBudgetId() {
        CompteBancaire c1 = new CompteBancaire();
        c1.setId("ING");
        assertEquals("ING_2018_01", BudgetDataUtils.getBudgetId(c1.getId(), Month.JANUARY, 2018));
    }


    @Test
    void getAnneeFromBudgetId() throws BudgetNotFoundException {
        String id1 = "ING_2018_01";

        assertEquals(Integer.valueOf(2018), BudgetDataUtils.getAnneeFromBudgetId(id1));
        assertEquals(Month.JANUARY, BudgetDataUtils.getMoisFromBudgetId(id1));
        assertEquals("ING", BudgetDataUtils.getCompteFromBudgetId(id1));

        String id2 = "ingdirectV_2018_08";

        assertEquals(Integer.valueOf(2018), BudgetDataUtils.getAnneeFromBudgetId(id2));
        assertEquals(Month.AUGUST, BudgetDataUtils.getMoisFromBudgetId(id2));
        assertEquals("ingdirectV", BudgetDataUtils.getCompteFromBudgetId(id2));
    }


    @Test
    void testGetCategorie() {


        List<CategorieOperations> categoriesFromDB = new ArrayList<>();
        CategorieOperations catAlimentation = new CategorieOperations();
        catAlimentation.setId("8f1614c9-503c-4e7d-8cb5-0c9a9218b84a");
        catAlimentation.setActif(true);
        catAlimentation.setCategorie(true);
        catAlimentation.setLibelle("Alimentation");


        CategorieOperations ssCatCourse = new CategorieOperations();
        ssCatCourse.setActif(true);
        ssCatCourse.setCategorie(false);
        ssCatCourse.setId("467496e4-9059-4b9b-8773-21f230c8c5c6");
        ssCatCourse.setLibelle("Courses");
        ssCatCourse.setCategorieParente(new CategorieOperations.CategorieParente(catAlimentation.getId(), catAlimentation.getLibelle()));
        catAlimentation.setListeSSCategories(new HashSet<>());
        catAlimentation.getListeSSCategories().add(ssCatCourse);
        categoriesFromDB.add(catAlimentation);


        CategorieOperations cat = MockDataCategories.getCategorieById("8f1614c9-503c-4e7d-8cb5-0c9a9218b84a", categoriesFromDB);
        assertNotNull(cat);

        CategorieOperations ssCat = MockDataCategories.getCategorieById("467496e4-9059-4b9b-8773-21f230c8c5c6", categoriesFromDB);
        assertNotNull(ssCat);
        assertNotNull(ssCat.getCategorieParente());
        assertEquals("8f1614c9-503c-4e7d-8cb5-0c9a9218b84a", ssCat.getCategorieParente().getId());
    }


    @Test
    void testDeleteTagFromString() {
        assertNull(BudgetDataUtils.deleteTagFromString(null));

        assertEquals("Libellé Opération", BudgetDataUtils.deleteTagFromString("Libellé Opération"));


        assertEquals("Libellé Opération", BudgetDataUtils.deleteTagFromString("[Virement depuis @utre Compte] Libellé Opération"));
    }

    @Test
    void testGetCategorieById() {

        List<CategorieOperations> categoriesFromDB = new ArrayList<>();

        for (int i = 0; i < 9; i++) {

            CategorieOperations cat = new CategorieOperations();
            cat.setId("ID" + i);
            cat.setActif(true);
            cat.setCategorie(true);
            cat.setLibelle("CAT" + i);

            for (int j = 0; j < 9; j++) {
                CategorieOperations ssCat = new CategorieOperations();
                ssCat.setActif(true);
                ssCat.setCategorie(false);
                ssCat.setId("ID" + i + j);
                ssCat.setLibelle("SSCAT" + j);
                ssCat.setCategorieParente(new CategorieOperations.CategorieParente(cat.getId(), cat.getLibelle()));
                cat.setListeSSCategories(new HashSet<>());
                cat.getListeSSCategories().add(ssCat);


            }
            categoriesFromDB.add(cat);
        }

        CategorieOperations cat = MockDataCategories.getCategorieById("ID8", categoriesFromDB);
        assertNotNull(cat);

        CategorieOperations ssCat = MockDataCategories.getCategorieById("ID88", categoriesFromDB);
        assertNotNull(ssCat);
        assertNotNull(ssCat.getCategorieParente());
        assertEquals("ID8", ssCat.getCategorieParente().getId());
    }


    @Test
    void testCloneLigneOperation() {
        LigneOperation clone = BudgetDataUtils.cloneOperationToMoisSuivant(MockDataOperations.getOperationPrelevement());
        assertNotNull(clone);
        assertNotEquals(MockDataOperations.getOperationPrelevement().getId(), clone.getId());
        assertNotNull(clone.getAutresInfos());
        assertNull(clone.getMensualite());
    }


    @Test
    void testClonePeriodiqueLigneOperationNonPeriodique() {
        List<LigneOperation> clones = BudgetDataUtils.cloneOperationPeriodiqueToMoisSuivant(MockDataOperations.getOperationPrelevement());
        assertNotNull(clones);
        assertEquals(1, clones.size());

        LigneOperation clone = clones.get(0);
        assertNotNull(clone);
        assertNotNull(clone.getAutresInfos());
        assertNull(clone.getMensualite());
    }


    @Test
    void testClonePeriodiqueLigneOperationPeriodiqueReportee() {

        LigneOperation operationMensuelle = MockDataOperations.getOperationMensuelleRealisee();
        operationMensuelle.getMensualite().setProchaineEcheance(1);
        operationMensuelle.setEtat(OperationEtatEnum.REPORTEE);

        List<LigneOperation> clones = BudgetDataUtils.cloneOperationPeriodiqueToMoisSuivant(operationMensuelle);
        assertNotNull(clones);
        assertEquals(2, clones.size());

        LigneOperation opPrec = clones.get(0);
        assertNotNull(opPrec);
        assertEquals(OperationEtatEnum.PREVUE, opPrec.getEtat());
        assertNotNull(opPrec.getAutresInfos());
        assertEquals(OperationPeriodiciteEnum.PONCTUELLE, opPrec.getMensualite().getPeriode());
        assertEquals(-1, opPrec.getMensualite().getProchaineEcheance());


        LigneOperation clone = clones.get(1);
        assertNotNull(clone);
        assertEquals(OperationEtatEnum.PREVUE, clone.getEtat());
        assertNotNull(clone.getAutresInfos());
        assertNotNull(clone.getMensualite());
    }


    @Test
    void testClonePeriodiqueLigneOperationPeriodiqueRealisee() {

        LigneOperation operationMensuelle = MockDataOperations.getOperationMensuelleRealisee();
        operationMensuelle.getMensualite().setProchaineEcheance(0);
        operationMensuelle.setEtat(OperationEtatEnum.REALISEE);

        List<LigneOperation> clones = BudgetDataUtils.cloneOperationPeriodiqueToMoisSuivant(operationMensuelle);
        assertNotNull(clones);
        assertEquals(1, clones.size());

        LigneOperation clone = clones.get(0);
        assertNotNull(clone);
        assertEquals(OperationEtatEnum.PREVUE, clone.getEtat());
        assertNotNull(clone.getAutresInfos());
        assertNotNull(clone.getMensualite());
    }


}
