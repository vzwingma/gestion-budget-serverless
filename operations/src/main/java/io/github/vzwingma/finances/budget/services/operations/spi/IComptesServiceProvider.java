package io.github.vzwingma.finances.budget.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.operations.api.enums.ComptesApiUrlEnum;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Service Provider Interface de {@link }
 */
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
