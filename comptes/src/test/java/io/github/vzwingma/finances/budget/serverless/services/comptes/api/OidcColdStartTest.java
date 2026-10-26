package io.github.vzwingma.finances.budget.serverless.services.comptes.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AP004 T0.5 (d) — démarrage à froid RÉEL de quarkus-oidc (discovery + JWKS, sans clé publique locale) face à un faux serveur OIDC local
 * ({@link LocalOidcServer}, aucun réseau externe). Le {@link TestProfile} force un redémarrage de l'application : la première requête
 * authentifiée est donc bien la première après démarrage. Attendu : 200, discovery et JWKS chargés.
 * <p>
 * Non couvert (à vérifier en QUA) : appel réel à Google, latence réseau réelle, effet de {@code connection-timeout}/{@code connection-delay}
 * face à une discovery injoignable au démarrage.
 */
@QuarkusTest
@TestProfile(OidcColdStartTest.Profile.class)
class OidcColdStartTest {

    public static class Profile implements QuarkusTestProfile {
        static final LocalOidcServer SERVER = new LocalOidcServer(false);

        @Override
        public Map<String, String> getConfigOverrides() {
            return OidcColdStartSupport.overrides(SERVER);
        }
    }

    @BeforeAll
    static void init() {
        OidcColdStartSupport.mockService();
    }

    @Test
    void premiereRequeteAuthentifieeApresDemarrage_200() {
        assertEquals(200, assertTimeoutPreemptively(Duration.ofSeconds(20), OidcColdStartSupport::statusWithValidToken),
                "la première requête authentifiée après démarrage doit passer");
        assertTrue(OidcColdStartSupport.hits("discovery") >= 1, "discovery OIDC non appelée : la config de test n'est pas effective");
        assertTrue(OidcColdStartSupport.hits("jwks") >= 1, "JWKS non récupéré : la config de test n'est pas effective");
    }
}
