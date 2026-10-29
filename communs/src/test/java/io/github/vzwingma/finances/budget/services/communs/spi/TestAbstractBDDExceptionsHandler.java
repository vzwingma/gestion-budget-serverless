package io.github.vzwingma.finances.budget.services.communs.spi;

import com.mongodb.MongoClientException;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoConfigurationException;
import com.mongodb.MongoSocketReadException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;
import jakarta.ws.rs.core.Response;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de {@link AbstractBDDExceptionsHandler} : toute {@link com.mongodb.MongoException} est mappée en 503 (R10)
 */
class TestAbstractBDDExceptionsHandler {

    private final ConcreteHandler handler = new ConcreteHandler();

    @Test
    void testMongoSocketReadException503() {
        try (Response response = handler.toResponse(new MongoSocketReadException("socket", new ServerAddress("localhost", 27017)))) {
            assertEquals(503, response.getStatus());
        }
    }

    @Test
    void testMongoCommandException503() {
        try (Response response = handler.toResponse(new MongoCommandException(BsonDocument.parse("{\"ok\": 0, \"errmsg\": \"erreur\", \"code\": 13}"), new ServerAddress("localhost", 27017)))) {
            assertEquals(503, response.getStatus());
        }
    }

    @Test
    void testMongoClientExceptionGenerique503() {
        try (Response response = handler.toResponse(new MongoClientException("générique"))) {
            assertEquals(503, response.getStatus());
        }
    }

    @Test
    void testMongoConfigurationEtTimeout503() {
        try (Response configuration = handler.toResponse(new MongoConfigurationException("config"));
             Response timeout = handler.toResponse(new MongoTimeoutException("timeout"))) {
            assertEquals(503, configuration.getStatus());
            assertEquals(503, timeout.getStatus());
        }
    }

    private static class ConcreteHandler extends AbstractBDDExceptionsHandler {
    }
}
