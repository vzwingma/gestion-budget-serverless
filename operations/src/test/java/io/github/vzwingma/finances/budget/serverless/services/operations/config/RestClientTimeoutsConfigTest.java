package io.github.vzwingma.finances.budget.serverless.services.operations.config;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Garde-fou (AP004 T2.4 / T2.6, ADR-009) : timeouts des 2 clients REST inter-services (connexion 1,5 s, lecture 4 s,
 * tous deux sous le budget Lambda de 15 s) et TTL du cache des catégories (1 h).
 */
@QuarkusTest
class RestClientTimeoutsConfigTest {

    private static <T> T valeur(String cle, Class<T> type) {
        return ConfigProvider.getConfig().getValue(cle, type);
    }

    @Test
    void timeoutsDesClientsRest() {
        for (String client : new String[]{"comptes-service", "parametrages-service"}) {
            assertEquals(1500L, valeur("quarkus.rest-client." + client + ".connect-timeout", Long.class), client);
            assertEquals(4000L, valeur("quarkus.rest-client." + client + ".read-timeout", Long.class), client);
        }
    }

    @Test
    void ttlDuCacheDesCategories() {
        assertEquals(Duration.ofHours(1), valeur("quarkus.cache.caffeine.\"categories\".expire-after-write", Duration.class));
    }
}
