package io.github.vzwingma.finances.budget.serverless.services.operations.api.override;

import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import lombok.Getter;


/**
 * Filtre de sécurité personnalisé pour les requêtes entrantes.
 * Ce filtre étend AbstractAPISecurityFilter pour fournir une implémentation spécifique
 * de la validation des tokens JWT dans le contexte de cette application.
 */
@Getter
@Provider
@PreMatching
public class JwtSecurityFilter extends AbstractAPISecurityFilter { }
