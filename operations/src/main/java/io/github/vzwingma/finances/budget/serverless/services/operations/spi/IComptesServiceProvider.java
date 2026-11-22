package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.enums.ComptesApiUrlEnum;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Service Provider Interface de {@link }
 */
@Produces("application/json")
@Path(ComptesApiUrlEnum.COMPTES_BASE)
@RegisterRestClient(configKey = "comptes-service")
@RegisterClientHeaders(RequestJWTHeaderFactory.class)
@RegisterProvider(ComptesRestClientMetricsFilter.class)
@RegisterProvider(ServicesDistantsResponseExceptionMapper.class)
@TraductionErreursServiceDistant
public interface IComptesServiceProvider {

    /**
     * Recherche du compte
     *
     * @param idCompte id du Compte
     * @return compte correspondant. Échecs : 404 -> DataNotFoundException ; 401 -> UserNotAuthorizedException ; 403, 429, 5xx,
     * connexion, timeout -> ServiceDistantIndisponibleException (503). Un seul retry, sur connexion refusée et 429/502/503/504.
     */
    @GET
    @Path(ComptesApiUrlEnum.COMPTES_ID)
    @Retry(maxRetries = 1, delay = 200, jitter = 100, retryOn = ServiceDistantTransitoireException.class)
    Uni<CompteBancaire> getCompteById(String idCompte);
}
