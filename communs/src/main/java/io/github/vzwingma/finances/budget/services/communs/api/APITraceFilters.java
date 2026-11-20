package io.github.vzwingma.finances.budget.services.communs.api;

import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

/**
 * Filtres de trace des requêtes et réponses (journal DEBUG + ligne {@code [HTTP-METRIC]}), mutualisés pour les
 * 4 services (AP004 T2.1, ADR-009). Ils étaient auparavant déclarés dans chaque ressource ; le comportement est
 * inchangé : délégation à {@link AbstractAPIInterceptors} (donc à {@code HttpMetricsRecorder}).
 * <p>
 * Ordre : priorité par défaut (USER), identique à celle de l'ancienne déclaration dans les ressources et à celle de
 * {@code JwtSecurityFilter}. Aucune dépendance fonctionnelle entre les deux : le jeton est validé en amont par
 * {@code quarkus-oidc} ; ce filtre ne fait que mémoriser l'heure de début puis journaliser.
 */
@Singleton
public class APITraceFilters extends AbstractAPIInterceptors {

    @Override
    @ServerRequestFilter(preMatching = true)
    public void preMatchingFilter(ContainerRequestContext requestContext) {
        super.preMatchingFilter(requestContext);
    }

    @Override
    @ServerResponseFilter
    public void postMatchingFilter(ContainerResponseContext responseContext) {
        super.postMatchingFilter(responseContext);
    }
}
