package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.override.SecurityOverrideFilter;
import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.data.model.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.utils.security.JWTUtils;
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
    @Context
    SecurityContext securityContext;
    @Inject
    SecurityOverrideFilter securityOverrideFilter;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        String rawAuthJWT = getValidJWTToken(securityContext.getAuthenticationScheme());
        if (rawAuthJWT != null) {
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + rawAuthJWT);
        } else {
            LOG.warn("L'appel n'est pas authentifié : JWT Token est null");
        }

        // Ajout de l'API Key
        if (securityOverrideFilter != null) {
            String apiKey = securityOverrideFilter.getApiKey();
            if (apiKey != null) {
                headers.add(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, apiKey);
            } else {
                LOG.warn("L'appel n'est pas authentifié pour l'API Gateway : l'API Key est nulle");
            }
        } else {
            LOG.warn("L'appel n'est pas authentifié pour l'API Gateway :securityOverrideFilter est null");
        }
        LOG.trace("Injection des headers : {}", headers.keySet());
        return headers;
    }


    /**
     * Recherche d'un token valide dans le cache
     *
     * @param rawAuthJWT rawtJwt
     */
    private String getValidJWTToken(String rawAuthJWT) {

        // Revalidation de la validité du token
        if (rawAuthJWT != null) {
            JWTAuthToken jwToken = JWTUtils.decodeJWT(rawAuthJWT);
            if (!jwToken.isValid()) {
                return rawAuthJWT;
            }
        }
        return null;
    }
}
