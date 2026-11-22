package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.enums.ParametragesApiUrlEnum;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.SsCategorieOperations;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import io.quarkus.cache.CacheResult;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * Service Provider Interface de {@link }
 */
@Produces("application/json")
@Path(ParametragesApiUrlEnum.PARAMS_BASE)
@RegisterRestClient(configKey = "parametrages-service")
@RegisterClientHeaders(RequestJWTHeaderFactory.class)
@RegisterProvider(ParametragesRestClientMetricsFilter.class)
@RegisterProvider(ServicesDistantsResponseExceptionMapper.class)
public interface IParametragesServiceProvider {

    /** Cache des catégories (TTL 1 h : quarkus.cache.caffeine."categories".expire-after-write). Les erreurs ne sont jamais mises en cache. */
    String CATEGORIES_CACHE = "categories";

    /**
     * Recherche d'une catégorie ou d'une sous catégorie d'opérations à partir de son id
     *
     * @param idCategorie de la catégorie ou de la sous catégorie
     * @return catégorie correspondante. Null sinon
     */
    @GET
    @TraductionErreursServiceDistant(client = "parametrages", method = "GET")
    @Path(ParametragesApiUrlEnum.PARAMS_CATEGORIES + ParametragesApiUrlEnum.PARAMS_CATEGORIE_ID)
    @CacheResult(cacheName = CATEGORIES_CACHE)
    @Retry(maxRetries = 1, delay = 200, jitter = 100, retryOn = ServiceDistantTransitoireException.class)
    Uni<SsCategorieOperations> getSsCategorieParId(String idCategorie);

    /**
     * Liste des catégories
     *
     * @return liste de catégories
     */
    @GET
    @TraductionErreursServiceDistant(client = "parametrages", method = "GET")
    @Path(ParametragesApiUrlEnum.PARAMS_CATEGORIES)
    @CacheResult(cacheName = CATEGORIES_CACHE)
    @Retry(maxRetries = 1, delay = 200, jitter = 100, retryOn = ServiceDistantTransitoireException.class)
    Uni<List<CategorieOperations>> getCategories();
}
