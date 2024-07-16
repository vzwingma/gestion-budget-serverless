package io.github.vzwingma.finances.budget.serverless.services.operations.api.override;

import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
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
public class SecurityOverrideFilter extends AbstractAPISecurityFilter {

    String apiKey; // Clé API extraite de l'en-tête de la requête.

    @ConfigProperty(name = "oidc.jwt.id.appusercontent")
    Instance<Optional<String>> idAppUserContent; // Identifiant de l'application utilisateur, injecté depuis la configuration.

    @Inject
    Instance<IJwtSigningKeyReadRepository> jwtSigningKeyRepository;

    /**
     * @return les clés de signature JWT
     */
    @Override
    public List<JwksAuthKey> getJwksAuthKeys() {
        return jwtSigningKeyRepository.get().getJwksSigningAuthKeys().subscribe().asStream().toList();
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
        this.apiKey = requestContext.getHeaderString(HTTP_HEADER_API_KEY);
    }
}
