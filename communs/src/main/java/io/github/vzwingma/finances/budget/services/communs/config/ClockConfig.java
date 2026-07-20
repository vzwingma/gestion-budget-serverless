package io.github.vzwingma.finances.budget.services.communs.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Clock;

/**
 * Producteur CDI de l'horloge applicative, source unique de vérité pour toute obtention de
 * l'instant courant ("now") dans le code métier — remplace les appels directs
 * {@code LocalDateTime.now()} / {@code Instant.now()} (sans zone explicite) par une injection de
 * {@link Clock}, testable via {@link Clock#fixed(java.time.Instant, java.time.ZoneId)}.
 * <p>
 * Convention projet : UTC systématique, indépendante de l'environnement d'exécution (Lambda
 * région, poste dev, CI). Voir ADR-004 (docs/adr/004-clock-injection-convention.md).
 *
 * @author DEVon
 */
@ApplicationScoped
public class ClockConfig {

    /**
     * Horloge applicative UTC, injectable par constructeur dans les classes CDI-managées.
     *
     * @return {@link Clock#systemUTC()}
     */
    @Produces
    @ApplicationScoped
    public Clock clock() {
        return Clock.systemUTC();
    }
}
