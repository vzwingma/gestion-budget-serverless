package io.github.vzwingma.finances.budget.services.communs.utils.security;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cache thread-safe des clés publiques de signature JWT (JWKS).
 * <p>
 * Le contenu (clés + instant de chargement) est remplacé de manière atomique par un instantané immuable :
 * les clés retirées de la source sont donc purgées, et les lecteurs concurrents ne voient jamais d'état partiel.
 * L'instant de chargement provient d'une {@link Clock} fournie par l'appelant (ADR-004, UTC).
 */
public final class JwksKeyCache {

    /**
     * Délai mini avant nouvelle tentative de chargement quand le cache est vide (évite de solliciter la source à chaque requête).
     */
    static final Duration EMPTY_RETRY_DELAY = Duration.ofSeconds(30);

    private record Snapshot(Map<String, JwksAuthKey> keys, Instant loadedAt) {
    }

    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(new Snapshot(Map.of(), null));

    /**
     * @return les clés courantes (vue immuable)
     */
    public Map<String, JwksAuthKey> keys() {
        return snapshot.get().keys();
    }

    /**
     * Remplace atomiquement l'ensemble des clés (les clés absentes de {@code newKeys} sont purgées).
     *
     * @param newKeys  nouvelles clés
     * @param loadedAt instant de chargement
     */
    public void replace(Map<String, JwksAuthKey> newKeys, Instant loadedAt) {
        snapshot.set(new Snapshot(newKeys == null ? Map.of() : Map.copyOf(newKeys), loadedAt));
    }

    /**
     * @param clock horloge applicative
     * @param ttl   durée de validité du cache
     * @return true si le cache n'a jamais été chargé, ou est vide depuis plus de {@link #EMPTY_RETRY_DELAY}, ou est plus ancien que le TTL
     */
    public boolean isExpired(Clock clock, Duration ttl) {
        Snapshot current = snapshot.get();
        if (current.loadedAt() == null) {
            return true;
        }
        Duration age = Duration.between(current.loadedAt(), clock.instant());
        Duration effectiveTtl = current.keys().isEmpty() && EMPTY_RETRY_DELAY.compareTo(ttl) < 0 ? EMPTY_RETRY_DELAY : ttl;
        return age.compareTo(effectiveTtl) >= 0;
    }
}
