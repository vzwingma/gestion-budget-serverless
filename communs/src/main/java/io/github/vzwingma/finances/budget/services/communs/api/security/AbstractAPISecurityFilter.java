package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKeys;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtValidationParams;
import io.github.vzwingma.finances.budget.services.communs.utils.security.JWTUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.DecodeException;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;


/**
 * Filtre HTTP de sécurité des API, vérification du token JWT et des rôles
 */
public abstract class AbstractAPISecurityFilter implements ContainerRequestFilter {


    public static final String HTTP_HEADER_API_KEY = "X-Api-Key";
    private final Logger logger = LoggerFactory.getLogger(AbstractAPISecurityFilter.class);

    private JwtValidationParams jwtValidationParams;

    public abstract Uni<JwksAuthKeys> getJwtAuthSigningKeyServiceProvider();

    /**
     * Initialisation des clés de signature JWT
     */
    public JwtValidationParams getJwtValidationParams() {
        if(jwtValidationParams == null) {
            jwtValidationParams = new JwtValidationParams();
            jwtValidationParams.setIdAppUserContent(getIdAppUserContent().get().isPresent() ? getIdAppUserContent().get().get() : null);
            List<JwksAuthKey> listJwksAuthKey = new ArrayList<>();
            getJwtAuthSigningKeyServiceProvider().subscribe().with(jwksAuthKeys -> {
                listJwksAuthKey.addAll(Arrays.asList(jwksAuthKeys.getKeys()));
            });
            jwtValidationParams.setJwksAuthKeys(listJwksAuthKey);
        }
        return jwtValidationParams;
    }


    public abstract Instance<Optional<String>> getIdAppUserContent();
    /**
     * Filtre de sécurité sur JWT
     *
     * @param requestContext requête
     * @throws DecodeException erreur de décodage
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {

        String auth = getAuthBearerFromHeaders(requestContext.getHeaders().get(HttpHeaders.AUTHORIZATION.toLowerCase(Locale.ROOT)));
        if (auth != null && !auth.isEmpty() && !"null".equals(auth)) {
            try {
                JWTAuthToken jwToken = JWTUtils.decodeJWT(auth);
                if(jwToken.isValid(getJwtValidationParams())){
                    requestContext.setSecurityContext(new SecurityOverrideContext(jwToken, auth));
                    return;
                }
                else {
                    logger.error("Token JWT invalide : {}", auth);
                }
            } catch (DecodeException e) {
                logger.error("Erreur lors du décodage du token JWT : {}", auth);
            }
        }
        requestContext.setSecurityContext(new AnonymousSecurityContext());
    }


    /**
     * Récupération de l'Auth Bearer à partir des entêtes
     *
     * @param authBearer liste des entêtes
     * @return l'auth Bearer si elle existe
     */
    protected String getAuthBearerFromHeaders(List<String> authBearer) {
        if (authBearer != null && !authBearer.isEmpty()) {
            Optional<String> accessToken = authBearer.stream()
                    .filter(a -> a.startsWith("Bearer "))
                    .map(a -> a.replace("Bearer ", ""))
                    .findFirst();
            return accessToken.orElse(null);
        } else {
            logger.trace("Auth is null");
            return null;
        }
    }
}
