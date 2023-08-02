package io.github.vzwingma.finances.budget.serverless.services.operations.api.override;

import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;

import java.io.IOException;

/**
 * Filtre HTTP de sécurité des API, vérification du token JWT et des réservations
 */
@Getter
@Provider
@PreMatching
public class SecurityOverrideFilter extends AbstractAPISecurityFilter implements ContainerRequestFilter {

    private String apiKey;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        super.filter(requestContext);
        this.apiKey = requestContext.getHeaderString(HTTP_HEADER_API_KEY);
    }
}
