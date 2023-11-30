package io.github.vzwingma.finances.budget.services.communs.api.security;

import com.sun.security.auth.UserPrincipal;
import io.github.vzwingma.finances.budget.services.communs.data.model.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.JWTAuthToken;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

/**
 * SecurityContext issus du token JWT OIDC Google
 */
public class SecurityOverrideContext implements SecurityContext {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityOverrideContext.class);

    private final JWTAuthToken idToken;

    private final String rawBase64Token;


    public SecurityOverrideContext(JWTAuthToken idToken, String rawBase64Token) {
        this.idToken = idToken;
        this.rawBase64Token = rawBase64Token;
    }

    @Override
    public Principal getUserPrincipal() {
        if (idToken != null) {
            JWTAuthPayload p = this.idToken.getPayload();
            if (p != null) {
                String g = p.getGiven_name() != null && !p.getGiven_name().isEmpty() ? p.getGiven_name().substring(0, 1).toLowerCase() : "";
                String f = p.getFamily_name() != null && !p.getFamily_name().isEmpty() ? p.getFamily_name().substring(0, Math.min(p.getFamily_name().length(), 7)).toLowerCase() : "";
                return new UserPrincipal(g + f);
            }
        }
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        if (idToken.isExpired()) {
            LOG.warn("L'utilisateur [{}] n'a pas de token JWT valide", getUserPrincipal().getName());
            return false;
        }
        return true;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return this.rawBase64Token;
    }
}
