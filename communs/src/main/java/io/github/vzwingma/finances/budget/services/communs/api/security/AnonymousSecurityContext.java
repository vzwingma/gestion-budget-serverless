package io.github.vzwingma.finances.budget.services.communs.api.security;

import com.sun.security.auth.UserPrincipal;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;

/**
 * SecurityContext issus du token JWT OIDC Google
 */
public class AnonymousSecurityContext implements SecurityContext {

    @Override
    public Principal getUserPrincipal() {
        return new UserPrincipal("ANONYME");
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }
}
