package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api.override;

import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Logger logger = LoggerFactory.getLogger(AbstractAPISecurityFilter.class);


    @ConfigProperty(name = "oidc.jwt.id.appusercontent")
    Instance<Optional<String>> idAppUserContent; // Identifiant de l'application utilisateur, injecté depuis la configuration.

    @Inject
    Instance<IJwtSigningKeyReadRepository> jwtSigningKeyRepository;

    /**
     * @return les clés de signature JWT
     */
    @Override
    public List<JwksAuthKey> getJwksAuthKeys() {
        return jwtSigningKeyRepository.get().getJwksSigningAuthKeys().toList();
    }
}
