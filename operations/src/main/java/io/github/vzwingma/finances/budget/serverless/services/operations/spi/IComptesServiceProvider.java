package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.enums.ComptesApiUrlEnum;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Service Provider Interface de {@link }
 */
@Produces("application/json")
@Path(ComptesApiUrlEnum.COMPTES_BASE)
@RegisterRestClient(configKey = "comptes-service")
@RegisterClientHeaders(RequestJWTHeaderFactory.class)
public interface IComptesServiceProvider {

    /**
     * Recherche du compte
     *
     * @param idCompte id du Compte
     * @return compte correspondant
     */
    @GET
    @Path(ComptesApiUrlEnum.COMPTES_ID)
    Uni<CompteBancaire> getCompteById(String idCompte);
}
