package io.github.vzwingma.finances.budget.serverless.services.operations.test.data;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.IdsCategoriesEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationPeriodiciteEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationTypeEnum;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;

import java.util.ArrayList;
import java.util.List;

/**
 * Jeu de données Opérations
 */
public class MockDataOperations {

    public static List<LigneOperation> get3LignesOperations(CompteBancaire compte) {

        List<LigneOperation> listeOperations = new ArrayList<>(3);
        listeOperations.add(getOperationRealisee(compte, 1));

        listeOperations.add(getOperationRealisee(compte, 2));

        LigneOperation lo3 = getOperationRealisee(compte, 3);
        lo3.setLibelle("[Virement depuis Autre Compte] Opération 3");
        listeOperations.add(lo3);

        return listeOperations;

    }

    public static LigneOperation getOperationRealisee(CompteBancaire compte, int numero) {

        LigneOperation lo = new LigneOperation();
        lo.setId(compte.getId() + "B2_L" + numero);
        lo.setEtat(OperationEtatEnum.REALISEE);
        lo.setLibelle("Opération " + numero);
        return lo;

    }

    public static LigneOperation getOperationIntercompte() {
        CategorieOperations dep = new CategorieOperations(IdsCategoriesEnum.TRANSFERT_INTERCOMPTE.getId());
        CategorieOperations cat = new CategorieOperations(IdsCategoriesEnum.TRANSFERT_INTERCOMPTE.getId());
        dep.setCategorieParente(new CategorieOperations.CategorieParente(cat.getId(), cat.getLibelle()));
        LigneOperation test1 = new LigneOperation(dep, "TestIntercompte", OperationTypeEnum.CREDIT, 123D, OperationEtatEnum.PREVUE);
        test1.setId("TestIntercompte");
        return test1;
    }

    public static LigneOperation getOperationPrelevement() {
        CategorieOperations dep = new CategorieOperations(IdsCategoriesEnum.PRELEVEMENTS_MENSUELS.getId());
        CategorieOperations cat = new CategorieOperations(IdsCategoriesEnum.PRELEVEMENTS_MENSUELS.getId());
        dep.setCategorieParente(new CategorieOperations.CategorieParente(cat.getId(), cat.getLibelle()));
        LigneOperation test1 = new LigneOperation(dep, "TEST1", OperationTypeEnum.CREDIT, 123D, OperationEtatEnum.PREVUE);
        test1.setId("TEST1");
        return test1;
    }

    public static LigneOperation getOperationRemboursement() {
        CategorieOperations dep = new CategorieOperations(IdsCategoriesEnum.FRAIS_REMBOURSABLES.getId());
        CategorieOperations cat = new CategorieOperations(IdsCategoriesEnum.FRAIS_REMBOURSABLES.getId());
        dep.setCategorieParente(new CategorieOperations.CategorieParente(cat.getId(), cat.getLibelle()));
        LigneOperation remboursement = new LigneOperation(dep, "TestRemboursement", OperationTypeEnum.DEPENSE, 123D, OperationEtatEnum.REALISEE);
        remboursement.setId("TestRemboursement");
        return remboursement;
    }


    public static LigneOperation getOperationMensuelleRealisee() {
        LigneOperation lo = new LigneOperation();
        lo.setId("C1_B2_L4");
        lo.setEtat(OperationEtatEnum.REALISEE);
        lo.setValeur(200D);
        lo.setLibelle("Opération 4");
        lo.setMensualite(new LigneOperation.Mensualite());
        lo.getMensualite().setPeriode(OperationPeriodiciteEnum.MENSUELLE);
        lo.getMensualite().setProchaineEcheance(-1);
        return lo;

    }
}
