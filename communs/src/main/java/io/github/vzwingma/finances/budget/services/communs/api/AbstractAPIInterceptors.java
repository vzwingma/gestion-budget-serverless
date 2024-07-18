package io.github.vzwingma.finances.budget.services.communs.api;

import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.api.security.IJwtSecurityContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interceptor for logging et sécurité des requests et des responses
 */
public abstract class AbstractAPIInterceptors {


    private static final Logger LOG = LoggerFactory.getLogger(AbstractAPIInterceptors.class);

    @Inject
    IJwtSecurityContext securityContext;

    private long startTime;
    /**
     * Logger requête
     *
     * @param requestContext context de la requête
     */
    public void preMatchingFilter(ContainerRequestContext requestContext) {
        // Replace pattern-breaking characters
        String path = requestContext.getUriInfo().getPath().replaceAll("[\n\r\t]", "_");
        String apiKey = requestContext.getHeaderString(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY);
        String jwt = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        LOG.debug("[HTTP] > [uri:{} {}]", requestContext.getMethod(), path);
        LOG.trace("[HTTP] > [api-key:{}][jwt:{}]", apiKey, jwt);

        startTime = System.currentTimeMillis();
    }

    /**
     * Logger réponse
     *
     * @param responseContext context de la réponse
     */
    public void postMatchingFilter(ContainerResponseContext responseContext) {

        BusinessTraceContext.getclear();
        LOG.debug("[HTTP] < [{} - {}][t:{} ms]", responseContext.getStatus(), responseContext.getStatusInfo().getReasonPhrase(), System.currentTimeMillis() - startTime);
    }

    /**
     * Retourne le nom de l'utilisateur authentifié
     * @return le nom de l'utilisateur authentifié
     */
    public String getAuthenticatedUser() {
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            return securityContext.getUserPrincipal().getName();
        } else {
            return "ANONYME";
        }
    }
}
