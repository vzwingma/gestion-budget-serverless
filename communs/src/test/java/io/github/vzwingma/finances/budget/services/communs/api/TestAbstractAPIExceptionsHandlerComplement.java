package io.github.vzwingma.finances.budget.services.communs.api;

import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.AbstractBusinessException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BadParametersException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DonneeCorrompueException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests complémentaires de AbstractAPIExceptionsHandler
 * couvrant le cas fallback (503)
 */
class TestAbstractAPIExceptionsHandlerComplement {

    private final ConcreteHandler handler = new ConcreteHandler();

    @Test
    void testFallback503() {
        // Exception non reconnue → 503 SERVICE_UNAVAILABLE
        AbstractBusinessException unknown = new AbstractBusinessException("erreur inconnue") {
            // sous-classe anonyme qui n'est pas l'un des types gérés
        };
        assertEquals(503, handler.toResponse(unknown).getStatus());
    }

    @Test
    void testDonneeCorrompue500() {
        // Donnée corrompue → 500 INTERNAL_SERVER_ERROR (et non 503)
        jakarta.ws.rs.core.Response response = handler.toResponse(new DonneeCorrompueException("mois absent"));
        assertEquals(500, response.getStatus());
        assertEquals("mois absent", response.getEntity());
    }

    @Test
    void testLibelleInResponse() {
        BadParametersException ex = new BadParametersException("champ manquant");
        jakarta.ws.rs.core.Response response = handler.toResponse(ex);
        assertEquals(400, response.getStatus());
        assertEquals("champ manquant", response.getEntity());
    }

    private static class ConcreteHandler extends AbstractAPIExceptionsHandler {
    }
}

