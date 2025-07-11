package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyService;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.github.vzwingma.finances.budget.services.communs.utils.security.JWTUtils;
import io.smallrye.mutiny.Multi;
import io.vertx.core.json.DecodeException;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Filtre HTTP de sécurité des API, vérification du token JWT et des rôles
 */
public abstract class AbstractAPISecurityFilter implements ContainerRequestFilter {


    public static final String HTTP_HEADER_API_KEY = "X-Api-Key";
    private final Logger logger = LoggerFactory.getLogger(AbstractAPISecurityFilter.class);

    @Inject
    JwtSecurityContext securityContext;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    IJwtSigningKeyService jwtSigningKeyService;

    /**
     * Filtre de sécurité sur JWT
     *
     * @param requestContext requête
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        if(JwtSecurityContext.JWKS_AUTH_KEYS == null || JwtSecurityContext.JWKS_AUTH_KEYS.isEmpty()){
            loadJwksSigningAuthKeys();
        }

        String apiKey = requestContext.getHeaders().getFirst(HTTP_HEADER_API_KEY);
        String rawJWTToken = getAuthBearerFromHeaders(requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION.toLowerCase(Locale.ROOT)));

        if (rawJWTToken != null && !rawJWTToken.isEmpty() && !"null".equals(rawJWTToken)) {
            try {
                JWTAuthToken jwToken = JWTUtils.decodeJWT(rawJWTToken);
                if(JWTUtils.isValid(jwToken, String.valueOf(securityContext.getIdAppUserContent()))){
                    securityContext.setJwtValidatedToken(jwToken);
                }
                else {
                    logger.error("Token JWT invalide");
                }
            } catch (DecodeException e) {
                logger.error("Erreur lors du décodage du token JWT : {}", rawJWTToken);
            }
        }
        else {
            logger.warn("Token JWT non trouvé. Accès anonyme.");
        }
        if(apiKey == null || apiKey.isEmpty()){
            logger.warn("Clé API non trouvée");
        }
        else {
            securityContext.setApiKey(apiKey);
        }
        requestContext.setSecurityContext(securityContext);
    }


    /**
     * Récupération de l'Auth Bearer à partir des entêtes
     *
     * @param authBearer liste des entêtes
     * @return l'auth Bearer si elle existe
     */
    private String getAuthBearerFromHeaders(String authBearer) {
        if (authBearer != null && !authBearer.isEmpty()) {
            return authBearer.replace("Bearer ", "");
        } else {
            logger.trace("Auth is null");
            return null;
        }
    }

    /**
     * Chargement des clés de signature JWKS
     */
    private void loadJwksSigningAuthKeys() {
        Multi<JwksAuthKey> jwksAuthKeyMulti =
                jwtSigningKeyService.loadJwksSigningKeys();
        if(jwksAuthKeyMulti != null){
            jwksAuthKeyMulti.subscribe().with(jwksAuthKey -> JwtSecurityContext.JWKS_AUTH_KEYS.add(jwksAuthKey));
        }
    }
}
