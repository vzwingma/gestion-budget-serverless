package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;

import java.security.Principal;

/**
 * Implémentation du contexte de sécurité applicatif, alimentée par la {@link SecurityIdentity} validée par {@code quarkus-oidc}
 * (ADR-005) et par la clé API de la requête. Portée requête : aucune donnée n'est conservée d'une requête à l'autre.
 */
@RequestScoped
public class JwtSecurityContext implements IJwtSecurityContext {

    private Principal userPrincipal;
    private String rawToken;
    private String apiKey;

    /**
     * Alimente le contexte depuis l'identité authentifiée. Une identité nulle ou anonyme laisse le contexte sans utilisateur.
     *
     * @param identity identité issue de {@code quarkus-oidc}
     */
    public void setIdentity(SecurityIdentity identity) {
        if (identity == null || identity.isAnonymous()) {
            this.userPrincipal = null;
            this.rawToken = null;
            return;
        }
        this.userPrincipal = identity.getPrincipal();
        AccessTokenCredential credential = identity.getCredential(AccessTokenCredential.class);
        this.rawToken = credential != null ? credential.getToken() : null;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    @Override
    public String getRawToken() {
        return rawToken;
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
