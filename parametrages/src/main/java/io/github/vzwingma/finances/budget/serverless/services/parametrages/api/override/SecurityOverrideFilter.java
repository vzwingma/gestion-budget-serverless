package io.github.vzwingma.finances.budget.serverless.services.parametrages.api.override;

import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtValidationParams;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.util.Optional;

@Getter
@Provider
@PreMatching
public class SecurityOverrideFilter extends AbstractAPISecurityFilter implements ContainerRequestFilter {


    @ConfigProperty(name = "oidc.jwt.id.appusercontent")
    Instance<Optional<String>> idAppUserContent;


    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        super.filter(requestContext);
    }
}
