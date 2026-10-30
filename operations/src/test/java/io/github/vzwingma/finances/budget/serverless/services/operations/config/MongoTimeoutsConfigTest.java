package io.github.vzwingma.finances.budget.serverless.services.operations.config;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lecture au runtime des timeouts et du pool du client MongoDB (AP004 T1.2c) : garde-fou contre la disparition ou la
 * dérive des clés d'`application.properties`. Tous les timeouts doivent rester inférieurs au timeout Lambda (15 s),
 * et le {@code read-timeout} (8 s) supérieur à 7 s. Le {@code maxTime} de 7 s sur l'agrégation des libellés a été retiré
 * (commit 2fba365) : l'agrégation n'est plus bornée que par ce read-timeout client.
 * <p>
 * `wait-queue-timeout` n'existe pas dans quarkus-mongodb-client 3.40.1 (voir rapport de phase 1) et n'est donc pas contrôlé.
 *
 * @author QALvin
 */
@QuarkusTest
class MongoTimeoutsConfigTest {

    private static final Duration TIMEOUT_LAMBDA = Duration.ofSeconds(15);

    private static <T> T valeur(String cle, Class<T> type) {
        return ConfigProvider.getConfig().getValue(cle, type);
    }

    @Test
    void testTimeoutsEtPoolDuClientMongo() {
        assertEquals(Duration.ofSeconds(5), valeur("quarkus.mongodb.server-selection-timeout", Duration.class));
        assertEquals(Duration.ofSeconds(3), valeur("quarkus.mongodb.connect-timeout", Duration.class));
        assertEquals(Duration.ofSeconds(8), valeur("quarkus.mongodb.read-timeout", Duration.class));
        assertEquals(10, valeur("quarkus.mongodb.max-pool-size", Integer.class));
        assertEquals(0, valeur("quarkus.mongodb.min-pool-size", Integer.class));
        assertEquals(Duration.ofSeconds(60), valeur("quarkus.mongodb.max-connection-idle-time", Duration.class));
    }

    @Test
    void testTimeoutsInferieursAuTimeoutLambda() {
        for (String cle : new String[]{"quarkus.mongodb.server-selection-timeout", "quarkus.mongodb.connect-timeout", "quarkus.mongodb.read-timeout"}) {
            Duration timeout = valeur(cle, Duration.class);
            assertTrue(timeout.compareTo(TIMEOUT_LAMBDA) < 0, cle + " (" + timeout + ") doit rester inférieur au timeout Lambda de 15 s");
        }
        assertTrue(valeur("quarkus.mongodb.read-timeout", Duration.class).compareTo(Duration.ofSeconds(7)) > 0,
                "read-timeout > 7 s (seule borne client de l'agrégation des libellés, plus de maxTime)");
    }
}
