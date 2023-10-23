package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api.enums.UtilisateursAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.UtilisateurDroitsEnum;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.UtilisateurPrefsAPIObject;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.UtilisateurPrefsEnum;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.ports.IUtilisateursAppProvider;
import io.github.vzwingma.finances.budget.services.communs.api.AbstractAPIInterceptors;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContextKeyEnum;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.UserAccessForbiddenException;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controleur REST -
 * Adapteur du port
 * @author vzwingma
 *
 */
@Path(UtilisateursAPIEnum.USERS_BASE)
public class UtilisateursResource extends AbstractAPIInterceptors {


    private static final Logger LOG = LoggerFactory.getLogger(UtilisateursResource.class);

    @Inject
    IUtilisateursAppProvider service;


    /**
     * Date de dernier accès utilisateur
     * @return date de dernier accès
     */
    @Operation(description = "Fournit la date de dernier accès d'un utilisateur", summary="Date de denier accès")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Opération réussie",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UtilisateurPrefsAPIObject.class)) }),
            @APIResponse(responseCode = "401", description = "L'utilisateur doit être identifié"),
            @APIResponse(responseCode = "403", description = "L'opération n'est pas autorisée"),
            @APIResponse(responseCode = "404", description = "Session introuvable")
    })
    @GET
    @RolesAllowed({ UtilisateursAPIEnum.UTILISATEURS_ROLE })
    @Path(UtilisateursAPIEnum.USERS_ACCESS_DATE)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<UtilisateurPrefsAPIObject> getLastAccessDateUtilisateur() throws UserAccessForbiddenException {
        String idProprietaire = super.getAuthenticatedUser();
        BusinessTraceContext.get().clear().put(BusinessTraceContextKeyEnum.USER, idProprietaire);
        if(idProprietaire != null) {
            return service.getLastAccessDate(idProprietaire)
                    .onItem().transform(lastAccess -> {
                        LOG.info("LastAccessTime : {}", lastAccess);
                        UtilisateurPrefsAPIObject prefs = new UtilisateurPrefsAPIObject();
                        prefs.setIdUtilisateur(idProprietaire);
                        prefs.setLastAccessTime(BudgetDateTimeUtils.getSecondsFromLocalDateTime(lastAccess));
                        return prefs;
                    });
        }
        else {
            return Uni.createFrom().failure(new UserAccessForbiddenException("Propriétaire introuvable"));
        }
    }



    /**
     * Préférences d'un utilisateur
     * @return préférences
     */
    @Operation(description = "Fournir les préférences d'affichage d'un utilisateur", summary="Préférences d'un utilisateur")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Opération réussie",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UtilisateurPrefsAPIObject.class))),
            @APIResponse(responseCode = "401", description = "L'utilisateur doit être identifié"),
            @APIResponse(responseCode = "403", description = "L'opération n'est pas autorisée"),
            @APIResponse(responseCode = "404", description = "Session introuvable")
    })
    @GET
    @RolesAllowed({ UtilisateursAPIEnum.UTILISATEURS_ROLE })
    @Path(UtilisateursAPIEnum.USERS_PREFS)
    public Uni<UtilisateurPrefsAPIObject> getPreferencesUtilisateur() {
        String idProprietaire = super.getAuthenticatedUser();
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.USER, idProprietaire);
        if(idProprietaire != null){
            return service.getUtilisateur(idProprietaire)
                    .map(utilisateur -> {
                        UtilisateurPrefsAPIObject prefs = new UtilisateurPrefsAPIObject();
                        prefs.setIdUtilisateur(idProprietaire);

                        if(utilisateur != null) {
                            Map<UtilisateurPrefsEnum, String> prefsUtilisateur = utilisateur.getPrefsUtilisateur();
                            Map<UtilisateurDroitsEnum, Boolean> droitsUtilisateur = utilisateur.getDroits();
                            LOG.info("Preferences Utilisateur : {} | {}", prefsUtilisateur, droitsUtilisateur);
                            prefs.setPreferences(prefsUtilisateur);
                            prefs.setDroits(droitsUtilisateur);
                        }
                        return prefs;
                    });
        }
        else {
            return Uni.createFrom().failure(new UserAccessForbiddenException("Propriétaire introuvable"));
        }
    }


    @Override
    @ServerRequestFilter(preMatching = true)
    public void preMatchingFilter(ContainerRequestContext requestContext) {
        super.preMatchingFilter(requestContext);
    }
    @Override
    @ServerResponseFilter
    public void postMatchingFilter(ContainerResponseContext responseContext) { super.postMatchingFilter(responseContext); }

}
