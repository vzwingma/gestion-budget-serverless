package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.api.AbstractAPIInterceptors;
import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.api.security.IJwtSecurityContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Factory pour injecter le token JWT correspondant à l'utilisateur connecté. S'il existe, s'il n'est pas expiré, pour l'appel à une autre API
 */
public class RequestJWTHeaderFactory implements ClientHeadersFactory {


    private static final Logger LOG = LoggerFactory.getLogger(RequestJWTHeaderFactory.class);
    @Inject
    IJwtSecurityContext securityContext;


    /**
     * Injection des headers
     * @param incomingHeaders incomingHeaders
     * @param clientOutgoingHeaders clientOutgoingHeaders
     * @return headers
     */
    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        // Corrélation des logs (ADR-010) : on repropage le Root validé du MDC, jamais l'en-tête entrant brut
        String awsTraceRoot = AbstractAPIInterceptors.extractAwsTraceRoot(
                "Root=" + MDC.get(AbstractAPIInterceptors.MDC_AWS_TRACE_ID));
        if (awsTraceRoot != null) {
            headers.add(AbstractAPIInterceptors.HTTP_HEADER_AMZN_TRACE_ID, "Root=" + awsTraceRoot);
        }
        // Ajout du JWT Token et Ajout de l'API Key
        if (securityContext != null) {
            String rawToken = securityContext.getRawToken();
            if (rawToken != null) {
                headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + rawToken);
            } else {
                LOG.warn("L'appel n'est pas authentifié : aucun JWT Token validé disponible");
            }

            String apiKey = securityContext.getApiKey();
            if (apiKey != null) {
                headers.add(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, apiKey);
            } else {
                LOG.warn("L'appel n'est pas authentifié pour l'API Gateway : l'API Key est nulle");
            }
        } else {
            LOG.warn("L'appel n'est pas valide pour l'API Gateway :securityOverrideContext est null");
        }
        return headers;
    }
}
