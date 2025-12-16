package io.github.vzwingma.finances.budget.services.communs.api;


import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyService;
import io.github.vzwingma.finances.budget.services.communs.data.model.Info;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Resource de base pour les API REST de l'application
 */

public abstract class AbstractAPIResource {

    @ConfigProperty(name = "quarkus.application.name", defaultValue = "µService Budget")
    String applicationName;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "0.0.0")
    String applicationVersion;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    IJwtSigningKeyService jwtSigningKeyService;

    @PermitAll
    public Uni<Info> info() {
        return Uni.combine()
                .all()
                .unis(Uni.createFrom().item(new Info(applicationName, applicationVersion)), jwtSigningKeyService.loadJwksSigningKeys())
                .asTuple()
                .map(Tuple2::getItem1);
    }
}
