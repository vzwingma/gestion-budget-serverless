package io.github.vzwingma.finances.budget.services.communs.api.security;

import com.sun.security.auth.UserPrincipal;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

/**
 * Implémentation personnalisée de {@link SecurityContext} pour gérer la sécurité basée sur les tokens JWT OIDC de Google.
 * Cette classe permet de définir le contexte de sécurité en fonction d'un token JWT.
 */
public class SecurityOverrideContext implements SecurityContext {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityOverrideContext.class);

    private final JWTAuthToken jwtValidatedToken;
    private final String rawBase64Token;

    /**
     * Constructeur de SecurityOverrideContext.
     *
     * @param jwtValidatedToken       Le token JWT utilisé pour l'authentification de l'utilisateur.
     * @param rawBase64Token Le token JWT sous forme de chaîne en Base64.
     */
    public SecurityOverrideContext(JWTAuthToken jwtValidatedToken, String rawBase64Token) {
        this.jwtValidatedToken = jwtValidatedToken;
        this.rawBase64Token = rawBase64Token;
    }

    /**
     * Récupère le principal de l'utilisateur à partir du token JWT.
     * Le principal est déterminé à partir du prénom et du nom de famille contenus dans le payload du token.
     *
     * @return Un {@link Principal} représentant l'utilisateur, ou null si le token est invalide.
     */
    @Override
    public Principal getUserPrincipal() {
        if (jwtValidatedToken != null) {
            JWTAuthPayload p = this.jwtValidatedToken.getPayload();
            if (p != null) {
                String g = p.getGiven_name() != null && !p.getGiven_name().isEmpty() ? p.getGiven_name().substring(0, 1).toLowerCase() : "";
                String f = p.getFamily_name() != null && !p.getFamily_name().isEmpty() ? p.getFamily_name().substring(0, Math.min(p.getFamily_name().length(), 7)).toLowerCase() : "";
                return new UserPrincipal(g + f);
            }
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
        if (jwtValidatedToken != null) {
            LOG.warn("L'utilisateur [{}] n'a pas de token JWT valide", getUserPrincipal().getName());
            return false;
        }
        return true;
    }

    /**
     * Indique si la connexion est sécurisée.
     *
     * @return l'état de la connexion
     */
    @Override
    public boolean isSecure() {
        return false;
    }

    /**
     * Récupère le schéma d'authentification utilisé.
     * Dans cette implémentation, retourne le token JWT en Base64.
     *
     * @return Le token JWT en Base64.
     */
    @Override
    public String getAuthenticationScheme() {
        return this.rawBase64Token;
    }
}
