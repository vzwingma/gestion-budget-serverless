package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKeys;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Service Provider Interface de {@link }
 */
@Produces("application/json")
@Path("oauth2/v3")
@RegisterRestClient(configKey = "jwt-signing-key-service")
public interface IJwtAuthSigningKeyServiceProvider {

    /**
     * Recherche des clés de signature JWT
     *
     * @return la liste des clés de signature
     */
    @GET
    @Path("/certs")
    Uni<JwksAuthKeys> getJwksAuthKeys();
}
