package io.github.vzwingma.finances.budget.serverless.services.operations.business.ports;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LibelleAvantApres;
import io.smallrye.mutiny.Multi;

import java.util.List;

/**
 * Interface pour les services d'administration des budgets
 */
public interface IBudgetAdminAppProvider {

    /**
     * Surcharge des libellés des opérations en harmonisant les libellés
     * @param idCompte id du compte
     * @param libellesToOverride liste des libellés à consolider
     * @return liste des ids des budgets modifiés
     */
    Multi<String> overrideLibellesOperations(String idCompte, List<LibelleAvantApres> libellesToOverride);
}

