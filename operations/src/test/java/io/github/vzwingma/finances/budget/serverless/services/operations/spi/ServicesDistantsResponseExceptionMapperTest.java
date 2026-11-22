package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.api.APIExceptionsHandler;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.AbstractBusinessException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.ServiceDistantIndisponibleException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.UserNotAuthorizedException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AP004 T2.4 : statut de l'aval, exception typée, statut renvoyé par l'API (via {@link APIExceptionsHandler}).
 */
class ServicesDistantsResponseExceptionMapperTest {

    private final ServicesDistantsResponseExceptionMapper mapper = new ServicesDistantsResponseExceptionMapper();

    private AbstractBusinessException exceptionPour(int statutAval) {
        return mapper.toThrowable(Response.status(statutAval).build());
    }

    private int statutApi(AbstractBusinessException e) {
        return new APIExceptionsHandler().toResponse(e).getStatus();
    }

    @Test
    void s404_dataNotFound_404() {
        AbstractBusinessException e = exceptionPour(404);
        assertInstanceOf(DataNotFoundException.class, e);
        assertEquals(404, statutApi(e));
    }

    @Test
    void s401_userNotAuthorized_401() {
        AbstractBusinessException e = exceptionPour(401);
        assertInstanceOf(UserNotAuthorizedException.class, e);
        assertEquals(401, statutApi(e));
    }

    @Test
    void s403_neProduitJamaisUn403_mais503_nonTransitoire() {
        AbstractBusinessException e = exceptionPour(403);
        assertInstanceOf(ServiceDistantIndisponibleException.class, e);
        assertFalse(e instanceof ServiceDistantTransitoireException);
        assertEquals(503, statutApi(e));
    }

    @Test
    void statutsTransitoires_429_502_503_504_donnent503_retryPossible() {
        for (int statut : new int[]{429, 502, 503, 504}) {
            AbstractBusinessException e = exceptionPour(statut);
            assertInstanceOf(ServiceDistantTransitoireException.class, e, "statut " + statut);
            assertEquals(503, statutApi(e), "statut " + statut);
        }
    }

    @Test
    void autresStatuts_donnent503_nonTransitoire() {
        for (int statut : new int[]{400, 406, 500, 501, 505}) {
            AbstractBusinessException e = exceptionPour(statut);
            assertInstanceOf(ServiceDistantIndisponibleException.class, e, "statut " + statut);
            assertFalse(e instanceof ServiceDistantTransitoireException, "statut " + statut);
            assertEquals(503, statutApi(e), "statut " + statut);
        }
    }

    @Test
    void gereLesStatutsEnErreurUniquement() {
        assertTrue(mapper.handles(404, null));
        assertTrue(mapper.handles(503, null));
        assertFalse(mapper.handles(200, null));
    }
}
