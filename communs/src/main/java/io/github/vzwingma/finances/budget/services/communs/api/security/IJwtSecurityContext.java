package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;

/**
 * Interface pour le contexte de sécurité basé sur les tokens JWT.
 */
public interface IJwtSecurityContext extends SecurityContext {

    /**
     * Le token JWT validé.
     */
    JWTAuthToken getJwtValidatedToken();
    /**
     * L'API Key.
     */
    String getApiKey();

    Principal getUserPrincipal();

    /**
     * Vérifie si l'utilisateur est dans un rôle spécifique.
     * @param role le rôle à vérifier
     * @return true si l'utilisateur est dans le rôle spécifié, false sinon
     */
    boolean isUserInRole(String role);

}

