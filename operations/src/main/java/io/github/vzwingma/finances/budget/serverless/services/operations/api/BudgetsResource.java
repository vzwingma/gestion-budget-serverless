package io.github.vzwingma.finances.budget.serverless.services.operations.api;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.enums.OperationsAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LibelleCategorieOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IBudgetAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.projections.ProjectionBudgetSoldes;
import io.github.vzwingma.finances.budget.services.communs.api.AbstractAPIInterceptors;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContextKeyEnum;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BadParametersException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Month;
import java.util.UUID;

/**
 * Controleur REST -
 * Adapteur du port {@link IOperationsAppProvider}
 *
 * @author vzwingma
 */
@Path(OperationsAPIEnum.BUDGET_BASE)
public class BudgetsResource extends AbstractAPIInterceptors {

    private static final Logger LOG = LoggerFactory.getLogger(BudgetsResource.class);


    @Inject
    IBudgetAppProvider budgetService;

    /**
     * Retour le budget d'un utilisateur
     *
     * @param idCompte id du compte
     * @param mois     mois du budget
     * @param annee    année du budget
     * @return budget
     */
    @Operation(description = "Recherche d'un budget mensuel pour un compte d'un utilisateur")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Opération réussie",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = BudgetMensuel.class))}),
            @APIResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @APIResponse(responseCode = "403", description = "Opération non autorisée"),
            @APIResponse(responseCode = "404", description = "Données introuvables")
    })
    @GET
    @RolesAllowed({OperationsAPIEnum.OPERATIONS_ROLE})
    @Path(value = OperationsAPIEnum.BUDGET_QUERY)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<BudgetMensuel> getBudgetUtilisateur(
            @RestQuery("idCompte") String idCompte,
            @RestQuery("mois") Integer mois,
            @RestQuery("annee") Integer annee) {

        BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.COMPTE, idCompte).put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());

        if (idCompte != null && mois != null && annee != null) {
            try {
                idCompte = idCompte.replaceAll("[\n\r]", "_");
                String idBudget = BudgetMensuel.getBudgetId(idCompte, Month.of(mois), annee);
                BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, idBudget);
                return budgetService.getBudgetMensuel(idCompte, Month.of(mois), annee);
            } catch (NumberFormatException e) {
                return Uni.createFrom().failure(new BadParametersException("Mois et année doivent être des entiers"));
            }
        }
        else {
            return Uni.createFrom().failure(new BadParametersException("IdCompte et Mois et année doivent être renseignés"));
        }
    }


    /**
     * Retour le budget d'un utilisateur
     *
     * @param idCompte id du compte
     * @param mois     mois du budget
     * @param annee    année du budget
     * @return budget
     */
    @Operation(description = "Recherche d'un solde d'un budget mensuel pour un compte d'un utilisateur")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Opération réussie",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = BudgetMensuel.class))}),
            @APIResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @APIResponse(responseCode = "403", description = "Opération non autorisée"),
            @APIResponse(responseCode = "404", description = "Données introuvables")
    })
    @GET
    @RolesAllowed({OperationsAPIEnum.OPERATIONS_ROLE})
    @Path(value = OperationsAPIEnum.BUDGET_SOLDES)
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<ProjectionBudgetSoldes> getBudgetSoldes(
            @RestQuery("idCompte") String idCompte,
            @RestQuery("mois") Integer mois,
            @RestQuery("annee") Integer annee) {

        if(idCompte == null) {
            return Multi.createFrom().failure(new BadParametersException("IdCompte doit être renseigné"));
        }
        idCompte = idCompte.replaceAll("[\n\r]", "_");
        BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.COMPTE, idCompte).put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());

        if (mois != null && annee != null) {
            try {
                String idBudget = BudgetMensuel.getBudgetId(idCompte, Month.of(mois), annee);
                BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, idBudget);
                return budgetService.getSoldesBudgetMensuel(idCompte, Month.of(mois), annee);
            } catch (NumberFormatException e) {
                return Multi.createFrom().failure(new BadParametersException("Mois et année doivent être des entiers"));
            }
        } else if (annee != null) {
            return budgetService.getSoldesBudgetMensuel(idCompte, null, annee);
        }
        return Multi.createFrom().failure(new BadParametersException("Mois et année doivent être renseignés"));
    }


    /**
     * Mise à jour du budget
     *
     * @param idBudget id du budget
     * @return budget mis à jour
     */
    @Operation(description = "Chargement d'un budget")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Budget chargé",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = BudgetMensuel.class))}),
            @APIResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @APIResponse(responseCode = "403", description = "Opération non autorisée"),
            @APIResponse(responseCode = "404", description = "Données introuvables")
    })
    @GET
    @RolesAllowed({OperationsAPIEnum.OPERATIONS_ROLE})
    @Path(value = OperationsAPIEnum.BUDGET_ID)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<BudgetMensuel> getBudgetsUtilisateur(@RestPath("idBudget") String idBudget) {

        BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.BUDGET, idBudget).put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());
        LOG.trace("chargeBudget");
        if (idBudget != null) {
            return budgetService.getBudgetMensuel(idBudget);
        } else {
            return Uni.createFrom().failure(new BadParametersException("L'id du budget doit être renseigné"));
        }
    }


    /**
     * Mise à jour du budget
     *
     * @param idBudget id du budget
     * @return budget mis à jour
     */
    @Operation(description = "Réinitialisation d'un budget")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Budget réinitialisé",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = BudgetMensuel.class))}),
            @APIResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @APIResponse(responseCode = "403", description = "Opération non autorisée"),
            @APIResponse(responseCode = "404", description = "Données introuvables"),
            @APIResponse(responseCode = "405", description = "Compte clos. Impossible de réinitialiser le budget")
    })
    @DELETE
    @RolesAllowed({OperationsAPIEnum.OPERATIONS_ROLE})
    @Path(value = OperationsAPIEnum.BUDGET_ID)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<BudgetMensuel> reinitializeBudget(@RestPath("idBudget") String idBudget) {

        BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.BUDGET, idBudget).put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());
        LOG.trace("Réinitialisation du budget");
        if (idBudget != null) {
            idBudget = idBudget.replaceAll("[\n\r]", "_");
            return budgetService.reinitialiserBudgetMensuel(idBudget);
        } else {
            return Uni.createFrom().failure(new BadParametersException("L'id du budget doit être renseigné"));
        }
    }

    /**
     * Retourne le statut du budget
     *
     * @param idBudget id du compte
     * @return statut du budget
     */
    @Operation(description = "Retourne l'état d'un budget mensuel : {etat}; {etat} : indique si le budget est ouvert ou cloturé.")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Budget actif"),
            @APIResponse(responseCode = "423", description = "Budget inactif"),
            @APIResponse(responseCode = "400", description = "Paramètres incorrects"),
            @APIResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @APIResponse(responseCode = "403", description = "Opération non autorisée"),
            @APIResponse(responseCode = "404", description = "Données introuvables")
    })
    @GET
    @RolesAllowed({OperationsAPIEnum.OPERATIONS_ROLE})
    @Path(value = OperationsAPIEnum.BUDGET_ETAT)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Boolean> isBudgetActif(
            @RestPath("idBudget") String idBudget,
            @RestQuery(value = "actif") Boolean actif) {

        if(idBudget == null){
            return Uni.createFrom().failure(new BadParametersException("Le paramètre {idBudget} est obligatoire"));
        }
        idBudget = idBudget.replaceAll("[\n\r]", "_");
        BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.BUDGET, idBudget).put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());

        if (Boolean.TRUE.equals(actif)) {
            return budgetService.isBudgetMensuelActif(idBudget);
        }
        return Uni.createFrom().failure(new BadParametersException("Les paramètres {idBudget}=" + idBudget + " et {actif}=" + actif + " ne sont pas valides"));
    }


    /**
     * Met à jour le statut du budget
     *
     * @param idBudget id du compte
     * @return statut du budget
     */
    @Operation(description = "Mise à jour de l'{état} d'un budget mensuel (ouvert/cloturé) ; {etat} : indique si le budget est ouvert ou cloturé.")
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
    @Path(value = OperationsAPIEnum.BUDGET_ETAT)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<BudgetMensuel> setBudgetActif(
            @RestPath("idBudget") String idBudget,
            @RestQuery(value = "actif") Boolean actif) {
        idBudget = idBudget.replaceAll("[\n\r]", "_");
        BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.BUDGET, idBudget).put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());
        return budgetService.setBudgetActif(idBudget, actif);
    }


    /* ********************************************************
     *                      OPERATIONS
     *********************************************************/


    /**
     * Création d'une opération
     *
     * @param idBudget  id du budget
     * @param operation opération à mettre à jour
     * @return budget mis à jour
     */
    @Operation(description = "Création d'une opération")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Opération mise à jour",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = BudgetMensuel.class))}),
            @APIResponse(responseCode = "400", description = "Paramètres incorrects"),
            @APIResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @APIResponse(responseCode = "403", description = "Opération non autorisée"),
            @APIResponse(responseCode = "404", description = "Données introuvables"),
            @APIResponse(responseCode = "423", description = "Compte clos")
    })
    @POST
    @RolesAllowed({OperationsAPIEnum.OPERATIONS_ROLE})
    @Path(value = OperationsAPIEnum.BUDGET_OPERATION)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<BudgetMensuel> createOperation(@RestPath("idBudget") String idBudget, LigneOperation operation) {

        BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.BUDGET, idBudget).put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());
        LOG.trace("createOperation");
        if (operation != null && idBudget != null) {
            operation.setId(UUID.randomUUID().toString());
            return budgetService.addOrUpdateOperationInBudget(idBudget, operation, super.getAuthenticatedUser());
        } else {
            return Uni.createFrom().failure(new BadParametersException("Les paramètres idBudget et operation sont obligatoires"));
        }
    }


    /**
     * Création d'une opération inter comptes
     *
     * @param idBudget id du budget
     * @param idCompte id du compte à mettre à jour
     * @return budget mis à jour
     */
    @Operation(description = "Création d'une opération Intercomptes")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Opération mise à jour",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = BudgetMensuel.class))}),
            @APIResponse(responseCode = "400", description = "Paramètres incorrects"),
            @APIResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @APIResponse(responseCode = "403", description = "Opération non autorisée"),
            @APIResponse(responseCode = "404", description = "Données introuvables"),
            @APIResponse(responseCode = "423", description = "Compte clos")
    })
    @POST
    @RolesAllowed({OperationsAPIEnum.OPERATIONS_ROLE})
    @Path(value = OperationsAPIEnum.BUDGET_OPERATION_INTERCOMPTE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<BudgetMensuel> createOperationIntercomptes(
            @RestPath("idBudget") String idBudget,
            @RestPath("idCompte") String idCompte,
            LigneOperation operation) {

        String uuidOperation = UUID.randomUUID().toString();
        BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.BUDGET, idBudget).put(BusinessTraceContextKeyEnum.OPERATION, uuidOperation).put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());
        if (operation != null && idBudget != null) {
            idBudget = idBudget.replaceAll("[\n\r]", "_");
            idCompte = idCompte.replaceAll("[\n\r]", "_");
            LOG.trace("Create Operation InterCompte [->{}]", idCompte);
            operation.setId(uuidOperation);
            return budgetService.createOperationsIntercomptes(idBudget, operation, idCompte, super.getAuthenticatedUser());
        } else {
            return Uni.createFrom().failure(new BadParametersException("Les paramètres idBudget, idOperation et idCompte sont obligatoires"));
        }
    }


    /**
     * Mise à jour d'une opération
     *
     * @param idBudget  id du budget
     * @param operation opération à mettre à jour
     * @return budget mis à jour
     */
    @Operation(description = "Mise à jour d'une opération")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Opération mise à jour",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = BudgetMensuel.class))}),
            @APIResponse(responseCode = "400", description = "Paramètres incorrects"),
            @APIResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @APIResponse(responseCode = "403", description = "Opération non autorisée"),
            @APIResponse(responseCode = "404", description = "Données introuvables"),
            @APIResponse(responseCode = "423", description = "Compte clos")
    })
    @POST
    @Path(value = OperationsAPIEnum.BUDGET_OPERATION_BY_ID)
    @RolesAllowed({OperationsAPIEnum.OPERATIONS_ROLE})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<BudgetMensuel> updateOperation(
            @RestPath("idBudget") String idBudget,
            @RestPath("idOperation") String idOperation,
            LigneOperation operation) {


        BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.BUDGET, idBudget).put(BusinessTraceContextKeyEnum.OPERATION, idOperation).put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());
        LOG.trace("UpdateOperation");

        if (operation != null && idBudget != null) {
            operation.setId(idOperation);
            return budgetService.addOrUpdateOperationInBudget(idBudget, operation, super.getAuthenticatedUser());
        } else {
            return Uni.createFrom().failure(new BadParametersException("Les paramètres idBudget et idOperation sont obligatoires"));
        }
    }


    /**
     * Suppression d'une opération
     *
     * @param idBudget    id du budget
     * @param idOperation opération à mettre à jour
     * @return budget mis à jour
     */
    @Operation(description = "Suppression d'une opération")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Opération supprimée",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = BudgetMensuel.class))}),
            @APIResponse(responseCode = "204", description = "Opération supprimée"),
            @APIResponse(responseCode = "400", description = "Paramètres incorrects"),
            @APIResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @APIResponse(responseCode = "403", description = "Opération non autorisée"),
            @APIResponse(responseCode = "404", description = "Données introuvables"),
            @APIResponse(responseCode = "405", description = "Compte clos")
    })
    @DELETE
    @RolesAllowed({OperationsAPIEnum.OPERATIONS_ROLE})
    @Path(value = OperationsAPIEnum.BUDGET_OPERATION_BY_ID)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<BudgetMensuel> deleteOperation(
            @RestPath("idBudget") String idBudget,
            @RestPath("idOperation") String idOperation) {
        BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.BUDGET, idBudget).put(BusinessTraceContextKeyEnum.OPERATION, idOperation).put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());
        if (idOperation != null && idBudget != null) {
            LOG.trace("Delete Operation");
            return budgetService.deleteOperationInBudget(idBudget, idOperation);
        } else {
            return Uni.createFrom().failure(new BadParametersException("Les paramètres idBudget et idOperation sont obligatoires"));
        }
    }


    /**
     * Liste des libellés des opérations
     *
     * @param idCompte id du compte
     * @return liste des libellés
     */
    @Operation(description = "Liste des libellés des opérations")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Liste des opérations",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = BudgetMensuel.class))}),
            @APIResponse(responseCode = "400", description = "Paramètres incorrects"),
            @APIResponse(responseCode = "401", description = "Utilisateur non authentifié"),
            @APIResponse(responseCode = "403", description = "Opération non autorisée"),
            @APIResponse(responseCode = "404", description = "Données introuvables"),
            @APIResponse(responseCode = "423", description = "Compte clos")
    })
    @GET
    @Path(value = OperationsAPIEnum.OPERATIONS_LIBELLES)
    @RolesAllowed({OperationsAPIEnum.OPERATIONS_ROLE})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<LibelleCategorieOperation> libellesOperationsCompte(@RestPath("idCompte") String idCompte) {

        if (idCompte != null) {
            idCompte = idCompte.replaceAll("[\n\r]", "_");
            LOG.info("Libelles des opérations du Compte [{}]", idCompte);
            BusinessTraceContext.getclear().put(BusinessTraceContextKeyEnum.COMPTE, idCompte).put(BusinessTraceContextKeyEnum.USER, super.getAuthenticatedUser());
            return budgetService.getLibellesOperations(idCompte, super.getAuthenticatedUser());
        } else {
            return Multi.createFrom().failure(new BadParametersException("Le paramètre idCompte est obligatoire"));
        }
    }


    @Override
    @ServerRequestFilter(preMatching = true)
    public void preMatchingFilter(ContainerRequestContext requestContext) {
        super.preMatchingFilter(requestContext);
    }

    @Override
    @ServerResponseFilter
    public void postMatchingFilter(ContainerResponseContext responseContext) {
        super.postMatchingFilter(responseContext);
    }

}
