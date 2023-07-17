package io.github.vzwingma.finances.budget.services.communs.api;

import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

/**
 * Interceptor for logging et sécurité des requests et des responses
 */
public abstract class AbstractAPIInterceptors {


    private final Logger LOG = LoggerFactory.getLogger(AbstractAPIInterceptors.class);

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
}
