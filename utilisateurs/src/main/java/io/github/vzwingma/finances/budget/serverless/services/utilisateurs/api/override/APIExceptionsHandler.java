package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api.override;

import io.github.vzwingma.finances.budget.services.communs.api.AbstractAPIExceptionsHandler;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.AbstractBusinessException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Handler for exceptions pour les API REST
 */
@Provider
public class APIExceptionsHandler extends AbstractAPIExceptionsHandler {

    @Override
    public Response toResponse(AbstractBusinessException e) {
        return super.toResponse(e);
    }
}
