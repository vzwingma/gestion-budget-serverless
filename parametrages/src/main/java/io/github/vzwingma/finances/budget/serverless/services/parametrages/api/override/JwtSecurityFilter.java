package io.github.vzwingma.finances.budget.serverless.services.parametrages.api.override;

import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import lombok.Getter;


/**
 * Filtre de sécurité personnalisé pour les requêtes entrantes.
 * Ce filtre étend AbstractAPISecurityFilter pour fournir une implémentation spécifique
 * de la validation des tokens JWT dans le contexte de cette application.
 */
@Getter
// AP004 T2.1 (R10) : @Provider et @PreMatching retirés. Le filtre de sécurité vit désormais dans communs
// (api.security.JwtSecurityFilter). Cette coquille, devenue inerte, est à supprimer par le développeur (ADR-009).
public class JwtSecurityFilter extends AbstractAPISecurityFilter { }
