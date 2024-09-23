package io.github.vzwingma.finances.budget.serverless.services.operations.api;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.enums.OperationsAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LibelleAvantApres;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IBudgetAdminAppProvider;
import io.github.vzwingma.finances.budget.services.communs.api.AbstractAPIInterceptors;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContextKeyEnum;
import io.smallrye.mutiny.Multi;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.RestPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@Path(OperationsAPIEnum.BUDGET_ADMIN_BASE)
public class AdminBudgetResource extends AbstractAPIInterceptors {


    private static final Logger LOG = LoggerFactory.getLogger(AdminBudgetResource.class);

    @Inject
    IBudgetAdminAppProvider budgetAdminService;



    /**
     * Mise à jour des libellés des budgets d'un compte
     *
     * @param idCompte id du compte
     * @return statut de la mise à jour
     */
    @Operation(description = "Mise à jour des libellés des budgets d'un compte")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Opération réussie",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = BudgetMensuel.class))}),
            @APIResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @APIResponse(responseCode = "403", description = "Opération non autorisée"),
            @APIResponse(responseCode = "404", description = "Données introuvables"),
            @APIResponse(responseCode = "500", description = "Opération en échec")
    })
    @POST
    @RolesAllowed({OperationsAPIEnum.OPERATIONS_ROLE})
    @Path(value = OperationsAPIEnum.OPERATIONS_LIBELLES_OVERRIDE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<String> consolidateLibellesOperations(@RestPath("idCompte") String idCompte, List<LibelleAvantApres> libelles) {

        BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.COMPTE, idCompte).put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());
        LOG.trace("[idCompte={}] Override libellés : {}", idCompte, libelles != null ? libelles.size() : 0);
        if(libelles == null || libelles.isEmpty()) {
            return Multi.createFrom().empty();
        }
        else{
            return budgetAdminService.overrideLibellesOperations(idCompte, libelles);
        }
    }
}
