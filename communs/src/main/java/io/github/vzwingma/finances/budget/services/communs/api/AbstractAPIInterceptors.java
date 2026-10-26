package io.github.vzwingma.finances.budget.services.communs.api;

import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.api.security.IJwtSecurityContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import io.github.vzwingma.finances.budget.services.communs.utils.security.SecurityUtils;
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

    /**
     * Logger requête
     *
     * @param requestContext context de la requête
     */
    public void preMatchingFilter(ContainerRequestContext requestContext) {
        // Replace pattern-breaking characters
        String path = requestContext.getUriInfo().getPath().replaceAll(SecurityUtils.ESCAPE_INPUT_REGEX, "_");
        String apiKey = requestContext.getHeaderString(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY);
        LOG.debug("[HTTP] > [uri:{} {}]", requestContext.getMethod(), path);
        // Ni la clé API ni le jeton JWT ne doivent être tracés : seule leur présence est journalisée
        LOG.debug("[HTTP] > [api-key:{}][jwt:{}]", apiKey != null && !apiKey.isEmpty() ? "présente" : "absente",
                requestContext.getHeaderString(HttpHeaders.AUTHORIZATION) != null ? "présent" : "absent");
    }

    /**
     * Logger réponse
     *
     * @param responseContext context de la réponse
     */
    public void postMatchingFilter(ContainerResponseContext responseContext) {

        BusinessTraceContext.getclear();
        LOG.debug("[HTTP] < [{} - {}]", responseContext.getStatus(), responseContext.getStatusInfo().getReasonPhrase());
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
