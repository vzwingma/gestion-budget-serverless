package io.github.vzwingma.finances.budget.serverless.spi;

import com.mongodb.MongoClientException;
import com.mongodb.MongoConfigurationException;
import com.mongodb.MongoServerUnavailableException;
import com.mongodb.MongoTimeoutException;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.spi.SPIExceptionsHandler;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests unitaires de SPIExceptionsHandler (module parametrages)
 */
class SPIExceptionsHandlerTest {

    private SPIExceptionsHandler handler;

    @BeforeEach
    void setup() {
        handler = new SPIExceptionsHandler();
    }

    @Test
    void testMongoConfigurationException() {
        try (Response r = handler.toResponse(new MongoConfigurationException("config"))) {
            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), r.getStatus());
        }
    }

    @Test
    void testMongoTimeoutException() {
        try (Response r = handler.toResponse(new MongoTimeoutException("timeout"))) {
            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), r.getStatus());
        }
    }

    @Test
    void testMongoServerUnavailableException() {
        try (Response r = handler.toResponse(new MongoServerUnavailableException("unavailable"))) {
            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), r.getStatus());
        }
    }

    @Test
    void testMongoClientExceptionGenerique() {
        MongoClientException e = new MongoClientException("generic") {};
        try (Response r = handler.toResponse(e)) {
            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
        }
    }
}

