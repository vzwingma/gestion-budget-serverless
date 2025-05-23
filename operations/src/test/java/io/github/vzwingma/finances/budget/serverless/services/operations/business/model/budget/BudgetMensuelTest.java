package io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationTypeEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.utils.BudgetDataUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BudgetMensuelTest {

    @Test
    void testResultat() {

        BudgetMensuel b = new BudgetMensuel();
        b.setSoldes(new BudgetMensuel.Soldes());
        b.getSoldes().setSoldeAtFinMoisPrecedent(100D);
        BudgetDataUtils.razCalculs(b);
        LigneOperation o = new LigneOperation();
        o.setSsCategorie(new LigneOperation.Categorie());
        o.getSsCategorie().setId("d005de34-f768-4e96-8ccd-70399792c48f");
        o.setValeur(123D);
        o.setTypeOperation(OperationTypeEnum.CREDIT);
        b.getListeOperations().add(o);

        LigneOperation o2 = new LigneOperation();
        o2.setValeur(123D);
        o2.setTypeOperation(OperationTypeEnum.CREDIT);
        b.getListeOperations().add(o2);

        assertEquals(100D, b.getSoldes().getSoldeAtFinMoisCourant());

    }
}
