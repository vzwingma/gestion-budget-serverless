package io.github.vzwingma.finances.budget.services.communs.api;


import io.github.vzwingma.finances.budget.services.communs.data.model.Info;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Resource de base pour les API REST de l'application
 */

public abstract class AbstractAPIResource {

    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    @ConfigProperty(name = "quarkus.application.version")
    String applicationVersion;

    @PermitAll
    public Uni<Info> info() {
        return Uni.createFrom().item(new Info(applicationName, applicationVersion));
    }
}
