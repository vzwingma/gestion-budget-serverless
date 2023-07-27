package io.github.vzwingma.finances.budget.serverless.services.comptes.spi;

import com.mongodb.MongoClientException;
import io.github.vzwingma.finances.budget.services.communs.spi.AbstractBDDExceptionsHandler;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Handler for exceptions pour les SPI
 */
@Provider
public class SPIExceptionsHandler extends AbstractBDDExceptionsHandler {

    @Override
    public Response toResponse(MongoClientException e) {
        return super.toResponse(e);
    }
}
