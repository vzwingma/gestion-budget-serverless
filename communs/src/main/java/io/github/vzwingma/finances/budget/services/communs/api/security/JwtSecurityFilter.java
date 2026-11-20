package io.github.vzwingma.finances.budget.services.communs.api.security;

import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

/**
 * Filtre de sécurité des requêtes entrantes, mutualisé pour les 4 services (AP004 T2.1, ADR-009) : expose au contexte
 * applicatif l'identité validée par {@code quarkus-oidc} (ADR-005) et la clé API.
 */
@Provider
@PreMatching
public class JwtSecurityFilter extends AbstractAPISecurityFilter { }
