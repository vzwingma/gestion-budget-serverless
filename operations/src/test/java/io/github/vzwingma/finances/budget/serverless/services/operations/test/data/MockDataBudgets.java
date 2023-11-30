package io.github.vzwingma.finances.budget.serverless.services.operations.test.data;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.utils.BudgetDataUtils;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;

/**
 * Jeu de données Budgets
 */
public class MockDataBudgets {


    public static CompteBancaire getCompteC1() {
        return getCompteCx(1);
    }

    public static CompteBancaire getCompteC2() {
        return getCompteCx(2);
    }

    private static CompteBancaire getCompteCx(int noCompte) {
        CompteBancaire c1 = new CompteBancaire();
        c1.setActif(true);
        c1.setId("C" + noCompte);
        c1.setLibelle("Libelle" + noCompte);
        c1.setProprietaire(new CompteBancaire.Proprietaire());
        c1.getProprietaire().setLogin("test");
        c1.setOrdre(noCompte);
        return c1;
    }


    public static CompteBancaire getCompteInactif() {
        CompteBancaire c2 = getCompteCx(2);
        c2.setActif(false);
        return c2;
    }

    public static BudgetMensuel getBudgetInactifCompteC1() {
        // Budget
        BudgetMensuel bo = new BudgetMensuel();
        bo.setIdCompteBancaire(getCompteC1().getId());
        bo.setMois(Month.JANUARY);
        bo.setAnnee(2022);
        bo.setActif(false);
        bo.setId(getCompteC1().getId() + "_2022_1");

        bo.getSoldes().setSoldeAtFinMoisCourant(0D);
        bo.getSoldes().setSoldeAtMaintenant(1000D);
        bo.setDateMiseAJour(LocalDateTime.now());
        bo.getSoldes().setSoldeAtFinMoisPrecedent(0D);

        return bo;
    }

    public static BudgetMensuel getBudgetActifCompteC1et1operationPrevue() {

        BudgetMensuel budget = new BudgetMensuel();
        budget.setMois(Month.JANUARY);
        budget.setAnnee(2022);
        budget.setIdCompteBancaire(getCompteC1().getId());
        budget.setId(BudgetDataUtils.getBudgetId(budget.getIdCompteBancaire(), budget.getMois(), budget.getAnnee()));

        budget.setActif(true);
        budget.setDateMiseAJour(LocalDateTime.now().minusDays(1));
        // Soldes
        budget.getSoldes().setSoldeAtFinMoisPrecedent(0D);
        budget.setListeOperations(new ArrayList<>());
        BudgetDataUtils.razCalculs(budget);
        // Opération
        budget.getListeOperations().add(MockDataOperations.getOperationPrelevement());

        return budget;
    }


    public static BudgetMensuel getBudgetActifCompteC2et0operationPrevue() {

        BudgetMensuel budget = new BudgetMensuel();
        budget.setMois(Month.JANUARY);
        budget.setAnnee(2022);
        budget.setIdCompteBancaire(getCompteCx(2).getId());
        budget.setId(BudgetDataUtils.getBudgetId(budget.getIdCompteBancaire(), budget.getMois(), budget.getAnnee()));

        budget.setActif(true);
        budget.setDateMiseAJour(LocalDateTime.now().minusDays(1));
        // Soldes
        budget.getSoldes().setSoldeAtFinMoisPrecedent(0D);
        return budget;
    }


    public static BudgetMensuel getBudgetPrecedentCompteC1() {
        // Budget
        BudgetMensuel bo = new BudgetMensuel();
        bo.setIdCompteBancaire(getCompteC1().getId());
        bo.setMois(Month.DECEMBER);
        bo.setAnnee(2021);
        bo.setActif(false);
        bo.setIdCompteBancaire(getCompteC1().getId());
        bo.setId(BudgetDataUtils.getBudgetId(bo.getIdCompteBancaire(), bo.getMois(), bo.getAnnee()));

        bo.getSoldes().setSoldeAtFinMoisCourant(1000D);
        bo.getSoldes().setSoldeAtMaintenant(1000D);
        bo.setDateMiseAJour(LocalDateTime.now());
        bo.getSoldes().setSoldeAtFinMoisPrecedent(0D);

        bo.getListeOperations().add(MockDataOperations.getOperationRealisee(getCompteC1(), 1));
        bo.getListeOperations().add(MockDataOperations.getOperationRealisee(getCompteC1(), 2));
        return bo;

    }


    public static BudgetMensuel getBudgetActifCompteC1et3operationsRealisees() {

        BudgetMensuel budget = new BudgetMensuel();
        budget.setMois(Month.JANUARY);
        budget.setAnnee(2022);
        budget.setIdCompteBancaire(getCompteC1().getId());
        budget.setId(BudgetDataUtils.getBudgetId(budget.getIdCompteBancaire(), budget.getMois(), budget.getAnnee()));

        budget.setActif(true);
        budget.setDateMiseAJour(LocalDateTime.now().minusDays(1));
        // Soldes
        budget.getSoldes().setSoldeAtFinMoisPrecedent(0D);
        budget.setListeOperations(new ArrayList<>());
        BudgetDataUtils.razCalculs(budget);
        // Opération
        budget.getListeOperations().addAll(MockDataOperations.get3LignesOperations(MockDataBudgets.getCompteC1()));
        BudgetDataUtils.razCalculs(budget);
        return budget;
    }
}
