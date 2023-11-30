package io.github.vzwingma.finances.budget.services.communs.spi;

import com.mongodb.MongoClientException;
import com.mongodb.MongoConfigurationException;
import com.mongodb.MongoServerUnavailableException;
import com.mongodb.MongoTimeoutException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for exceptions pour les API REST
 */
public abstract class AbstractBDDExceptionsHandler implements ExceptionMapper<MongoClientException> {


    /*
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBDDExceptionsHandler.class);

    @Override
    public Response toResponse(MongoClientException e) {
        if (e instanceof MongoConfigurationException) {
            LOGGER.error("Statut HTTP : [500] Database Client access error : {}", e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Configuration incorrecte").build();
        } else if (e instanceof MongoTimeoutException) {
            LOGGER.error("Statut HTTP : [500] Database Timeout access error : {}", e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Timeout lors de la connexion").build();
        } else if (e instanceof MongoServerUnavailableException) {
            LOGGER.error("Statut HTTP : [500] Database unavailable access error : {}", e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Base de données indisponible").build();
        }
        LOGGER.error("Statut HTTP : [500] Database access error : {}", e.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
}
