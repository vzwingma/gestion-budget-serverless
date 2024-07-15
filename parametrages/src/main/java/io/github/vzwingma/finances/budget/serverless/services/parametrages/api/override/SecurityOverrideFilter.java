package io.github.vzwingma.finances.budget.serverless.services.parametrages.api.override;

import io.github.vzwingma.finances.budget.serverless.services.parametrages.spi.IJwtAuthSigningKeyServiceProvider;
import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKeys;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


/**
 * Filtre de sécurité personnalisé pour les requêtes entrantes.
 * Ce filtre étend AbstractAPISecurityFilter pour fournir une implémentation spécifique
 * de la validation des tokens JWT dans le contexte de cette application.
 */
@Getter
@Provider
@PreMatching
public class SecurityOverrideFilter extends AbstractAPISecurityFilter implements ContainerRequestFilter {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractAPISecurityFilter.class);

    @ConfigProperty(name = "oidc.jwt.id.appusercontent")
    Instance<Optional<String>> idAppUserContent; // Identifiant de l'application utilisateur, injecté depuis la configuration.

    @Inject
    @RestClient
    @ApplicationScoped
    jakarta.inject.Provider<IJwtAuthSigningKeyServiceProvider> jwtAuthSigningKeyServiceProvider; // Service fournissant les clés de signature JWT.

    /**
     * Récupère le fournisseur de clés de signature JWT pour la validation des tokens.

     */

    public Uni<JwksAuthKeys> getJwtAuthSigningKeyServiceProvider() {
        return jwtAuthSigningKeyServiceProvider.get().getJwksAuthKeys();
    }


    /**
     * Filtre les requêtes entrantes pour appliquer la sécurité.
     * Cette méthode est appelée automatiquement pour chaque requête entrante.
     * @param requestContext Contexte de la requête, permettant d'accéder aux détails de la requête et de manipuler la réponse.
     * @throws IOException Peut être lancée lors de la manipulation du contexte de la requête.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        super.filter(requestContext); // Appelle la méthode de filtrage de la classe parente pour appliquer la logique de sécurité.
    }
}
