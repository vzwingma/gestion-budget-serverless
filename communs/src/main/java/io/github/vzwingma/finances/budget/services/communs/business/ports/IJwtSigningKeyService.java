package io.github.vzwingma.finances.budget.services.communs.business.ports;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.github.vzwingma.finances.budget.services.communs.utils.security.JwksKeyCache;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;


/**
  * Interface définissant les services pour la gestion des clés de signature JWT.
  * Cette interface fournit des méthodes pour accéder aux clés de signature JWT
  * et les charger depuis un dépôt de lecture dédié.
  */

public interface IJwtSigningKeyService {

    Logger JWKS_LOG = LoggerFactory.getLogger(IJwtSigningKeyService.class);

    /**
     * Durée max d'attente du chargement synchrone des clés lors de la validation d'un token.
     */
    Duration JWKS_LOAD_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Cache thread-safe des clés de signature JWT (un par JVM / microservice).
     */
    JwksKeyCache JWKS_KEY_CACHE = new JwksKeyCache();


    IJwtSigningKeyReadRepository getSigningKeyReadRepository();

    /**
     *
     * @return la liste courante des clés (vue immuable, sans déclencher de chargement)
     */
    default Map<String,JwksAuthKey> getJwksAuthKeyList() {
        return JWKS_KEY_CACHE.keys();
    }


    /**
     * Chargement des clés JWKS, horloge UTC par défaut.
     * @return chargement des clés JWKS
     */
    default Uni<Map<String, JwksAuthKey>> loadJwksSigningKeys() {
        return loadJwksSigningKeys(Clock.systemUTC());
    }

    /**
     * Chargement des clés JWKS : remplace atomiquement le cache (les clés retirées sont purgées).
     * @param clock horloge applicative (ADR-004)
     * @return clés chargées
     */
    default Uni<Map<String, JwksAuthKey>> loadJwksSigningKeys(Clock clock) {
        return getSigningKeyReadRepository().getJwksSigningAuthKeys()
                .collect().asMap(JwksAuthKey::getKid)
                .invoke(keys -> JWKS_KEY_CACHE.replace(keys, clock.instant()))
                .map(_ -> JWKS_KEY_CACHE.keys());
    }

    /**
     * Retourne les clés de validation des signatures, en garantissant leur chargement si le cache est vide ou expiré
     * (sans dépendre de l'appel préalable à {@code /_info}).
     * Appel potentiellement bloquant (hors event loop) ; en cas d'échec de chargement, les clés déjà en cache sont conservées
     * (cache vide => validation de signature en échec : fail-closed).
     *
     * @param clock horloge applicative (ADR-004)
     * @param ttl   durée de validité du cache
     * @return les clés disponibles
     */
    default Map<String, JwksAuthKey> getValidJwksAuthKeys(Clock clock, Duration ttl) {
        if (JWKS_KEY_CACHE.isExpired(clock, ttl)) {
            if (Context.isOnEventLoopThread()) {
                JWKS_LOG.error("Chargement des clés JWKS impossible sur l'event loop (appel bloquant)");
            } else {
                try {
                    loadJwksSigningKeys(clock).await().atMost(JWKS_LOAD_TIMEOUT);
                } catch (RuntimeException e) {
                    JWKS_LOG.error("Erreur lors du chargement des clés JWKS : {}", e.getMessage());
                }
            }
        }
        return JWKS_KEY_CACHE.keys();
    }
}
