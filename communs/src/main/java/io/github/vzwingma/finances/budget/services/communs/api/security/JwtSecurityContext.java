package io.github.vzwingma.finances.budget.services.communs.api.security;

import com.sun.security.auth.UserPrincipal;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtValidationParams;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.ArrayList;

/**
 * Implémentation personnalisée de {@link SecurityContext} pour gérer la sécurité basée sur les tokens JWT OIDC de Google.
 * Cette classe permet de définir le contexte de sécurité en fonction d'un token JWT.
 */
@RequestScoped
@Setter
@Getter
public class JwtSecurityContext implements IJwtSecurityContext {

    private static final Logger LOG = LoggerFactory.getLogger(JwtSecurityContext.class);

    /**
     * Le token JWT validé.
     */
    private JWTAuthToken jwtValidatedToken;
    /**
     * L'API Key.
     */
    private String apiKey;
    /**
     * Paramètres de validation JWT
     */
    private JwtValidationParams jwtValidationParams;

    @ConfigProperty(name = "oidc.jwt.id.appusercontent")
    Instance<String> idAppUserContent; // Identifiant de l'application utilisateur, injecté depuis la configuration.


    @Inject
    public JwtSecurityContext() {

    }

    /**
     * Récupère le principal de l'utilisateur à partir du token JWT.
     * Le principal est déterminé à partir du prénom et du nom de famille contenus dans le payload du token.
     *
     * @return Un {@link Principal} représentant l'utilisateur, ou null si le token est invalide.
     */
    @Override
    public Principal getUserPrincipal() {
        if (jwtValidatedToken != null && jwtValidatedToken.getPayload() != null) {
            JWTAuthPayload p = this.jwtValidatedToken.getPayload();
            return new UserPrincipal(p.getEmail());
        }
        else{
            LOG.warn("L'utilisateur n'a pas de token JWT valide");
        }
        return null;
    }

    /**
     * Vérifie si l'utilisateur est dans un rôle spécifique.
     * Cette implémentation retourne toujours false et logge un avertissement si le token JWT n'est pas valide.
     *
     * @param role Le rôle à vérifier.
     * @return toujours false dans cette implémentation.
     */
    @Override
    public boolean isUserInRole(String role) {
        if (jwtValidatedToken == null) {
            LOG.warn("L'utilisateur [{}] n'a pas de token JWT valide", getUserPrincipal() != null ? getUserPrincipal().getName() : null);
            return false;
        }
        else {
            LOG.warn("L'utilisateur [{}] n'a pas le rôle [{}]",  getUserPrincipal() != null ? getUserPrincipal().getName() : null, role);
        }
        return true;
    }

    /**
     * @return l'indicateur de sécurité
     */
    @Override
    public boolean isSecure() {
        return true;
    }

    /**
     * @return le schéma d'authentification
     */
    @Override
    public String getAuthenticationScheme() {
        return jwtValidatedToken != null ? jwtValidatedToken.getHeader().toString() : null;
    }


    /**
     * Initialisation des clés de signature JWT
     */
    public JwtValidationParams getJwtValidationParams() {
        if(jwtValidationParams == null) {
            jwtValidationParams = new JwtValidationParams();
            jwtValidationParams.setIdAppUserContent(this.idAppUserContent.get());
            jwtValidationParams.setJwksAuthKeys(new ArrayList<>());
        }
        return jwtValidationParams;
    }
}
