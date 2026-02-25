package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import com.mongodb.MongoClientException;
import com.mongodb.MongoConfigurationException;
import com.mongodb.MongoServerUnavailableException;
import com.mongodb.MongoTimeoutException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests unitaires de {@link SPIExceptionsHandler}
 */
class SPIExceptionsHandlerTest {

    private SPIExceptionsHandler handler;

    @BeforeEach
    void setup() {
        handler = new SPIExceptionsHandler();
    }

    @Test
    void testToResponseMongoConfigurationException() {
        MongoConfigurationException e = new MongoConfigurationException("Config incorrecte");
        try (Response response = handler.toResponse(e)) {
            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testToResponseMongoTimeoutException() {
        MongoTimeoutException e = new MongoTimeoutException("Timeout");
        try (Response response = handler.toResponse(e)) {
            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testToResponseMongoServerUnavailableException() {
        MongoServerUnavailableException e = new MongoServerUnavailableException("Serveur indisponible");
        try (Response response = handler.toResponse(e)) {
            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void testToResponseMongoClientExceptionGenerique() {
        MongoClientException e = new MongoClientException("Erreur générique") {};
        try (Response response = handler.toResponse(e)) {
            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        }
    }
}

