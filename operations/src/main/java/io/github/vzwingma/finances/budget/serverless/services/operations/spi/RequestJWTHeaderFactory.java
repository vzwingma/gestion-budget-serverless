package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.api.security.IJwtSecurityContext;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory pour injecter le token JWT correspondant à l'utilisateur connecté. S'il existe, s'il n'est pas expiré
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
        // Ajout du JWT Token et Ajout de l'API Key
        if (securityContext != null) {
            JWTAuthToken jwToken = securityContext.getJwtValidatedToken();
            if (jwToken != null && jwToken.isNotExpired()) {
                headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + jwToken.getRawContent());
            } else {
                LOG.warn("L'appel n'est pas authentifié : JWT Token est null ou expiré");
            }

            String apiKey = securityContext.getApiKey();
            if (apiKey != null) {
                headers.add(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, apiKey);
            } else {
                LOG.warn("L'appel n'est pas authentifié pour l'API Gateway : l'API Key est nulle");
            }
        } else {
            LOG.warn("L'appel n'est pas valide pour l'API Gateway :securityOverrideContext est {}", securityContext);
        }
        return headers;
    }
}
