package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtAuthHeader;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Filtre HTTP des API : expose au contexte applicatif l'identité validée par {@code quarkus-oidc} (ADR-005) et la clé API.
 * La validation du jeton (signature, iss, aud, exp, rotation JWKS) est entièrement déléguée à l'extension.
 */
public abstract class AbstractAPISecurityFilter implements ContainerRequestFilter {


    public static final String HTTP_HEADER_API_KEY = "X-Api-Key";
    private final Logger logger = LoggerFactory.getLogger(AbstractAPISecurityFilter.class);

    @Inject
    JwtSecurityContext securityContext;


    /**
     * Identité authentifiée par {@code quarkus-oidc} (signature, iss, aud, exp validés par l'extension).
     */
    @Inject
    SecurityIdentity identity;

    /**
     * Filtre de sécurité sur JWT
     *
     * @param requestContext requête
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String apiKey = requestContext.getHeaders().getFirst(HTTP_HEADER_API_KEY);

        if (identity != null && !identity.isAnonymous()) {
            // Jeton déjà validé par quarkus-oidc : simple exposition au contexte applicatif (propagation inter-services)
            AccessTokenCredential credential = identity.getCredential(AccessTokenCredential.class);
            JWTAuthPayload payload = new JWTAuthPayload();
            payload.setEmail(identity.getPrincipal().getName());
            securityContext.setJwtValidatedToken(new JWTAuthToken(new JwtAuthHeader(), payload, true, credential != null ? credential.getToken() : null));
        }
        else {
            logger.warn("Aucune identité authentifiée. Accès anonyme.");
        }
        if(apiKey == null || apiKey.isEmpty()){
            logger.warn("Clé API non trouvée");
        }
        else {
            securityContext.setApiKey(apiKey);
        }
        // Le SecurityContext JAX-RS reste celui de Quarkus (SecurityIdentity, @RolesAllowed) : ne pas le surcharger
    }
}
