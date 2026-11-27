package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import io.github.vzwingma.finances.budget.services.communs.api.APIExceptionsHandler;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * AP004 T2.8 : {@link ServiceDistantIndisponibleException} est une exception métier au libellé conservé, traduite en 503
 * (jamais en 401/403 : l'IHM déconnecterait l'utilisateur) par le repli de {@code AbstractAPIExceptionsHandler}.
 */
class ServiceDistantIndisponibleExceptionTest {

    @Test
    void libelleConserve_etExceptionMetier() {
        ServiceDistantIndisponibleException e = new ServiceDistantIndisponibleException("aval indisponible");
        assertInstanceOf(AbstractBusinessException.class, e);
        assertEquals("aval indisponible", e.getLibelle());
    }

    @Test
    void traduiteEn503_parLeHandlerDesApi() {
        try (Response response = new APIExceptionsHandler().toResponse(new ServiceDistantIndisponibleException("aval indisponible"))) {
            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
            assertEquals("aval indisponible", response.getEntity());
        }
    }
}
