package io.github.vzwingma.finances.budget.services.communs.api;

import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

/**
 * Interceptor for logging et sécurité des requests et des responses
 */
public abstract class AbstractAPIInterceptors {


    private static final Logger LOG = LoggerFactory.getLogger(AbstractAPIInterceptors.class);
    @Context
    SecurityContext securityContext;
    /**
     * Logger requête
     * @param requestContext context de la requête
     */
    public void preMatchingFilter(ContainerRequestContext requestContext) {
        // Replace pattern-breaking characters
        String path = requestContext.getUriInfo().getPath().replaceAll("[\n\r\t]", "_");
        LOG.debug("[HTTP][uri:{} {}]", requestContext.getMethod(), path);
    }

    /**
     * Logger réponse
     * @param responseContext context de la réponse
     */
    public void postMatchingFilter(ContainerResponseContext responseContext) {

        BusinessTraceContext.getclear();
        LOG.debug("[HTTP][{}] {}", responseContext.getStatus(), responseContext.getStatusInfo().getReasonPhrase());
    }

    public String getAuthenticatedUser(){
        if(securityContext != null && securityContext.getUserPrincipal() != null){
            return securityContext.getUserPrincipal().getName();
        }
        else{
            return "unknown";
        }
    }
}
