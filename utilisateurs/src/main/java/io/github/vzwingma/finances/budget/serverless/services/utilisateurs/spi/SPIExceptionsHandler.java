package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.spi;

import com.mongodb.MongoException;
import io.github.vzwingma.finances.budget.services.communs.spi.AbstractBDDExceptionsHandler;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Handler for exceptions pour les SPI
 */
@Provider
public class SPIExceptionsHandler extends AbstractBDDExceptionsHandler {

    @Override
    public Response toResponse(MongoException e) {
        return super.toResponse(e);
    }
}
