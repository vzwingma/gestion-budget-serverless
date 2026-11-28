package io.github.vzwingma.finances.budget.services.communs.api.security;

import java.security.Principal;

/**
 * Contexte de sécurité applicatif, mince (AP004 T4.0-b, J1) : expose ce que les services ont besoin de connaître de la
 * requête courante. L'authentification et les rôles relèvent exclusivement de {@code quarkus-oidc} ({@code SecurityIdentity},
 * {@code @RolesAllowed}) : ce contexte n'étend donc plus {@link jakarta.ws.rs.core.SecurityContext}.
 */
public interface IJwtSecurityContext {

    /**
     * @return le principal (email) de l'identité authentifiée par {@code quarkus-oidc}, ou {@code null} si la requête est anonyme
     */
    Principal getUserPrincipal();

    /**
     * @return le jeton brut de l'identité authentifiée, pour le propager aux services aval ; {@code null} s'il est indisponible
     */
    String getRawToken();

    /**
     * @return la clé API de la requête, ou {@code null} si absente
     */
    String getApiKey();
}
