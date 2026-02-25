package io.github.vzwingma.finances.budget.serverless.services.operations.api.override;

import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.*;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests unitaires de {@link APIExceptionsHandler}
 */
class APIExceptionsHandlerTest {

    private APIExceptionsHandler handler;

    @BeforeEach
    void setup() {
        handler = new APIExceptionsHandler();
    }

    @Test
    void testToResponseBadParametersException() {
        try (Response response = handler.toResponse(new BadParametersException("Mauvais paramètre"))) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testToResponseDataNotFoundException() {
        try (Response response = handler.toResponse(new DataNotFoundException("Donnée introuvable"))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testToResponseBudgetNotFoundException() {
        try (Response response = handler.toResponse(new BudgetNotFoundException("Budget introuvable"))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testToResponseCompteClosedException() {
        try (Response response = handler.toResponse(new CompteClosedException("Compte fermé"))) {
            assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testToResponseUserNotAuthorizedException() {
        try (Response response = handler.toResponse(new UserNotAuthorizedException("Non autorisé"))) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testToResponseUserAccessForbiddenException() {
        try (Response response = handler.toResponse(new UserAccessForbiddenException("Accès interdit"))) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testToResponseAutreException() {
        AbstractBusinessException e = new AbstractBusinessException("Erreur inconnue") {};
        try (Response response = handler.toResponse(e)) {
            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        }
    }
}

