package io.github.vzwingma.finances.budget.serverless.api;

import io.github.vzwingma.finances.budget.services.communs.api.AbstractAPIInterceptors;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Ressource de test (AP004 T2.1) : expose {@code getAuthenticatedUser()}, que les ressources de production du module
 * parametrages n'envoient à aucun service. Absente du jar de production (sources de test).
 */
@Path(AuthenticatedUserTestResource.PATH)
public class AuthenticatedUserTestResource extends AbstractAPIInterceptors {

    static final String PATH = "/test-support/authenticated-user";

    @GET
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    public String utilisateur() {
        return super.getAuthenticatedUser();
    }
}
