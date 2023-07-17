package io.github.vzwingma.finances.budget.services.communs.api;

import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * Handler for exceptions pour les API REST
 */
public abstract  class AbstractAPIExceptionsHandler implements ExceptionMapper<AbstractBusinessException> {


    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAPIExceptionsHandler.class);
    @Override
    public Response toResponse(AbstractBusinessException e) {
        if(e instanceof BadParametersException) {
            LOGGER.error("Statut HTTP : [400] Bad parameters : {}", e.getLibelle());
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getLibelle()).build();
        }
        else if(e instanceof DataNotFoundException) {
            LOGGER.error("Statut HTTP : [404] Data not found : {}", e.getLibelle());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getLibelle()).build();
        }
        else if(e instanceof BudgetNotFoundException) {
            LOGGER.error("Statut HTTP : [404] Budget not found : {}", e.getLibelle());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getLibelle()).build();
        }
        else if(e instanceof CompteClosedException) {
            LOGGER.error("Statut HTTP : [406] Compte closed : {}", e.getLibelle());
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(e.getLibelle()).build();
        }
        else if(e instanceof UserNotAuthorizedException) {
            LOGGER.error("Statut HTTP : [401] User not authorized : {}", e.getLibelle());
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getLibelle()).build();
        }
        else if(e instanceof UserAccessForbiddenException) {
            LOGGER.error("Statut HTTP : [403] Access forbidden : {}", e.getLibelle());
            return Response.status(Response.Status.FORBIDDEN).entity(e.getLibelle()).build();
        }
        LOGGER.error("Statut HTTP : [500] Internal Error : {}", e.getLibelle());
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(e.getLibelle()).build();
    }
}
