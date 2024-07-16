package io.github.vzwingma.finances.budget.serverless.services.comptes.api.override;

import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
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

    @ConfigProperty(name = "oidc.jwt.id.appusercontent")
    Instance<Optional<String>> idAppUserContent; // Identifiant de l'application utilisateur, injecté depuis la configuration.


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
