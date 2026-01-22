package io.github.vzwingma.finances.budget.serverless.services.operations.utils;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationPeriodiciteEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationStatutEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataCategories;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataOperations;
import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractCategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.data.model.SsCategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
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

        assertEquals(Month.OCTOBER, cd.getMonth());
    }


    @Test
    void getBudgetId() {
        CompteBancaire c1 = new CompteBancaire();
        c1.setId("ING");
        assertEquals("ING_2018_01", BudgetMensuel.getBudgetId(c1.getId(), Month.JANUARY, 2018));
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
        catAlimentation.setLibelle("Alimentation");


        SsCategorieOperations ssCatCourse = new SsCategorieOperations();
        ssCatCourse.setActif(true);
        ssCatCourse.setId("467496e4-9059-4b9b-8773-21f230c8c5c6");
        ssCatCourse.setLibelle("Courses");
        ssCatCourse.setCategorieParente(new SsCategorieOperations.CategorieParente(catAlimentation.getId(), catAlimentation.getLibelle()));
        catAlimentation.setListeSSCategories(new HashSet<>());
        catAlimentation.getListeSSCategories().add(ssCatCourse);
        categoriesFromDB.add(catAlimentation);


        AbstractCategorieOperations cat = MockDataCategories.getCategorieById("8f1614c9-503c-4e7d-8cb5-0c9a9218b84a", categoriesFromDB);
        assertNotNull(cat);

        AbstractCategorieOperations ssCat = MockDataCategories.getCategorieById("467496e4-9059-4b9b-8773-21f230c8c5c6", categoriesFromDB);
        assertNotNull(ssCat);
        assertInstanceOf(SsCategorieOperations.class, ssCat);
        assertNotNull(((SsCategorieOperations)ssCat).getCategorieParente());
        assertEquals("8f1614c9-503c-4e7d-8cb5-0c9a9218b84a", ((SsCategorieOperations)ssCat).getCategorieParente().getId());
    }


    @Test
    void testDeleteTagFromString() {
        assertNull(BudgetDataUtils.deleteTagFromString(null));

        assertEquals("Libellé Opération", BudgetDataUtils.deleteTagFromString("Libellé Opération"));


        assertEquals("Libellé Opération", BudgetDataUtils.deleteTagFromString("[Virement depuis @utre Compte] Libellé Opération"));
        assertEquals("Libellé Opération", BudgetDataUtils.deleteTagFromString("[En Retard] Libellé Opération"));
        assertEquals("Libellé Opération", BudgetDataUtils.deleteTagFromString("[En Retard][Virement depuis @utre Compte] Libellé Opération"));
    }

    @Test
    void testGetCategorieById() {

        List<CategorieOperations> categoriesFromDB = new ArrayList<>();

        for (int i = 0; i < 9; i++) {

            CategorieOperations cat = new CategorieOperations();
            cat.setId("ID" + i);
            cat.setActif(true);
            cat.setLibelle("CAT" + i);

            for (int j = 0; j < 9; j++) {
                SsCategorieOperations ssCat = new SsCategorieOperations();
                ssCat.setActif(true);
                ssCat.setId("ID" + i + j);
                ssCat.setLibelle("SSCAT" + j);
                ssCat.setCategorieParente(new SsCategorieOperations.CategorieParente(cat.getId(), cat.getLibelle()));
                cat.setListeSSCategories(new HashSet<>());
                cat.getListeSSCategories().add(ssCat);


            }
            categoriesFromDB.add(cat);
        }

        AbstractCategorieOperations cat = MockDataCategories.getCategorieById("ID8", categoriesFromDB);
        assertNotNull(cat);

        AbstractCategorieOperations ssCat = MockDataCategories.getCategorieById("ID88", categoriesFromDB);
        assertNotNull(ssCat);
        assertInstanceOf(SsCategorieOperations.class, ssCat);

        assertNotNull(((SsCategorieOperations)ssCat).getCategorieParente());
        assertEquals("ID8", ((SsCategorieOperations)ssCat).getCategorieParente().getId());
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
        List<LigneOperation> clones = BudgetDataUtils.cloneOperationPeriodiqueToMoisSuivant(MockDataOperations.getOperationPrelevement(), Month.JANUARY, 2010);
        assertNotNull(clones);
        assertEquals(1, clones.size());

        LigneOperation clone = clones.getFirst();
        assertNotNull(clone);
        assertNotNull(clone.getAutresInfos());
        assertNull(clone.getMensualite());
    }


    @Test
    void testClonePeriodiqueLigneOperationPeriodiqueReportee() {

        LigneOperation operationMensuelle = MockDataOperations.getOperationMensuelleRealisee();
        operationMensuelle.getMensualite().setProchaineEcheance(1);
        operationMensuelle.setEtat(OperationEtatEnum.REPORTEE);

        List<LigneOperation> clones = BudgetDataUtils.cloneOperationPeriodiqueToMoisSuivant(operationMensuelle, Month.JANUARY, 2010);
        assertNotNull(clones);
        assertEquals(2, clones.size());

        LigneOperation opPrec = clones.getFirst();
        assertNotNull(opPrec);
        assertEquals(OperationEtatEnum.PREVUE, opPrec.getEtat());
        assertNotNull(opPrec.getAutresInfos());
        assertEquals(OperationPeriodiciteEnum.PONCTUELLE, opPrec.getMensualite().getPeriode());
        assertEquals(-1, opPrec.getMensualite().getProchaineEcheance());
        assertNull(opPrec.getMensualite().getDateFin());

        LigneOperation clone = clones.get(1);
        assertNotNull(clone);
        assertEquals(OperationEtatEnum.PREVUE, clone.getEtat());
        assertNotNull(clone.getAutresInfos());
        assertNotNull(clone.getMensualite());
    }


    @Test
    void testClonePeriodiqueLigneOperationPeriodiqueEnRetard() {

        LigneOperation operationMensuelle = MockDataOperations.getOperationMensuelleRealisee();
        operationMensuelle.getMensualite().setProchaineEcheance(1);
        operationMensuelle.setEtat(OperationEtatEnum.REPORTEE);

        List<LigneOperation> clones = BudgetDataUtils.cloneOperationPeriodiqueToMoisSuivant(operationMensuelle, Month.JANUARY, 2010);
        assertNotNull(clones);
        assertEquals(2, clones.size());

        LigneOperation opPrec = clones.getFirst();
        assertNotNull(opPrec);
        assertEquals(OperationEtatEnum.PREVUE, opPrec.getEtat());
        assertNotNull(opPrec.getAutresInfos());
        assertEquals(OperationPeriodiciteEnum.PONCTUELLE, opPrec.getMensualite().getPeriode());
        assertEquals(-1, opPrec.getMensualite().getProchaineEcheance());
        assertNull(opPrec.getMensualite().getDateFin());
        assertEquals(OperationStatutEnum.EN_RETARD, opPrec.getStatuts().getFirst());
    }


    @Test
    void testClonePeriodiqueLigneOperationPeriodiqueEnRetardAEcheance() {

        LigneOperation operationMensuelle = MockDataOperations.getOperationMensuelleRealisee();
        operationMensuelle.getMensualite().setProchaineEcheance(1);
        LocalDate dateFinMinusOneMonth = operationMensuelle.getMensualite().getDateFin().minusMonths(1);
        operationMensuelle.getMensualite().setDateFin(dateFinMinusOneMonth);
        operationMensuelle.setEtat(OperationEtatEnum.REPORTEE);

        List<LigneOperation> clones = BudgetDataUtils.cloneOperationPeriodiqueToMoisSuivant(operationMensuelle, Month.JANUARY, 2010);
        assertNotNull(clones);
        assertEquals(2, clones.size());

        LigneOperation opPrec = clones.getFirst();
        assertNotNull(opPrec);
        assertEquals(OperationEtatEnum.PREVUE, opPrec.getEtat());
        assertNotNull(opPrec.getAutresInfos());
        assertEquals(OperationPeriodiciteEnum.PONCTUELLE, opPrec.getMensualite().getPeriode());
        assertEquals(-1, opPrec.getMensualite().getProchaineEcheance());
        assertNull(opPrec.getMensualite().getDateFin());
        assertEquals(OperationStatutEnum.EN_RETARD, opPrec.getStatuts().getFirst());
    }



    @Test
    void testClonePeriodiqueLigneOperationPeriodiqueRealisee() {

        LigneOperation operationMensuelle = MockDataOperations.getOperationMensuelleRealisee();
        operationMensuelle.getMensualite().setProchaineEcheance(0);
        operationMensuelle.setEtat(OperationEtatEnum.REALISEE);

        List<LigneOperation> clones = BudgetDataUtils.cloneOperationPeriodiqueToMoisSuivant(operationMensuelle, Month.JANUARY, 2010);
        assertNotNull(clones);
        assertEquals(1, clones.size());

        LigneOperation clone = clones.getFirst();
        assertNotNull(clone);
        assertEquals(OperationEtatEnum.PREVUE, clone.getEtat());
        assertNotNull(clone.getAutresInfos());
        assertNotNull(clone.getMensualite());
    }



    @Test
    void testClonePeriodiqueLigneOperationPeriodiqueDerniereEcheance() {

        LigneOperation operationMensuelle = MockDataOperations.getOperationMensuelleAEcheance();
        operationMensuelle.getMensualite().setProchaineEcheance(0);
        operationMensuelle.setEtat(OperationEtatEnum.REALISEE);

        LocalDate dateFin = LocalDate.now();
        List<LigneOperation> clones = BudgetDataUtils.cloneOperationPeriodiqueToMoisSuivant(operationMensuelle, dateFin.getMonth(), dateFin.getYear());
        assertNotNull(clones);
        assertEquals(1, clones.size());

        LigneOperation clone = clones.getFirst();
        assertNotNull(clone);
        assertEquals(OperationEtatEnum.PREVUE, clone.getEtat());
        assertNotNull(clone.getAutresInfos());
        assertNotNull(clone.getMensualite());
        assertEquals(OperationStatutEnum.DERNIERE_ECHEANCE, clone.getStatuts().getFirst());
    }




    @Test
    void testClonePeriodiqueLigneOperationPeriodiqueEcheanceDepassee() {

        LigneOperation operationMensuelle = MockDataOperations.getOperationMensuelleAEcheance();
        operationMensuelle.getMensualite().setProchaineEcheance(0);
        LocalDate dateFinMinusOneMonth = operationMensuelle.getMensualite().getDateFin().minusMonths(1);
        operationMensuelle.getMensualite().setDateFin(dateFinMinusOneMonth);
        operationMensuelle.setEtat(OperationEtatEnum.REALISEE);

        LocalDate dateFin = LocalDate.now();
        List<LigneOperation> clones = BudgetDataUtils.cloneOperationPeriodiqueToMoisSuivant(operationMensuelle, dateFin.getMonth(), dateFin.getYear());
        assertNotNull(clones);
        assertEquals(0, clones.size());
    }

}
