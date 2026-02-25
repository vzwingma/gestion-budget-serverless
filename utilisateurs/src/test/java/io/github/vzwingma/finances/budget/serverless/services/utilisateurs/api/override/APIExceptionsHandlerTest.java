package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api.override;

import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.*;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests unitaires de APIExceptionsHandler (module utilisateurs)
 */
class APIExceptionsHandlerTest {

    private APIExceptionsHandler handler;

    @BeforeEach
    void setup() {
        handler = new APIExceptionsHandler();
    }

    @Test
    void testBadParametersException() {
        try (Response r = handler.toResponse(new BadParametersException("bad"))) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), r.getStatus());
        }
    }

    @Test
    void testDataNotFoundException() {
        try (Response r = handler.toResponse(new DataNotFoundException("not found"))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), r.getStatus());
        }
    }

    @Test
    void testBudgetNotFoundException() {
        try (Response r = handler.toResponse(new BudgetNotFoundException("budget"))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), r.getStatus());
        }
    }

    @Test
    void testCompteClosedException() {
        try (Response r = handler.toResponse(new CompteClosedException("fermé"))) {
            assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), r.getStatus());
        }
    }

    @Test
    void testUserNotAuthorizedException() {
        try (Response r = handler.toResponse(new UserNotAuthorizedException("non autorisé"))) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), r.getStatus());
        }
    }

    @Test
    void testUserAccessForbiddenException() {
        try (Response r = handler.toResponse(new UserAccessForbiddenException("interdit"))) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), r.getStatus());
        }
    }

    @Test
    void testAutreException() {
        AbstractBusinessException e = new AbstractBusinessException("autre") {};
        try (Response r = handler.toResponse(e)) {
            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), r.getStatus());
        }
    }
}

