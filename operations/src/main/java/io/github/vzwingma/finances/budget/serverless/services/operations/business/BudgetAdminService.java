package io.github.vzwingma.finances.budget.serverless.services.operations.business;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.admin.LibelleAvantApres;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IBudgetAdminAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Service fournissant les opérations d'administration des budgets
 *
 * @author vzwingma
 */
@ApplicationScoped
@NoArgsConstructor
@Setter
public class BudgetAdminService implements IBudgetAdminAppProvider {

    /**
     * Service Provider Interface des données
     */
    @Inject
    IOperationsRepository dataOperationsProvider;

    /**
     * Mise à jour des libellés des budgets d'un compte en harmonisant les libellés
     * @param idCompte        id du compte
     * @param libellesToOverride liste des libellés à consolider
     * @return liste des budgets modifiés
     */
    @Override
    public Multi<String> overrideLibellesOperations(String idCompte, List<LibelleAvantApres> libellesToOverride) {
        return this.dataOperationsProvider.overrideLibellesOperations(idCompte, libellesToOverride)
                .onItem().transform(BudgetMensuel::getId);

    }
}
