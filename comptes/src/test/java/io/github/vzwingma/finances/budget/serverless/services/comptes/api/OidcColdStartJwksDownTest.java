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
 * AP004 T0.5 (d) — récupération du JWKS en échec (500) lors de la première requête après démarrage : rejet fail-closed, sans blocage.
 * <p>
 * AP004 T4.0-e (décision 👤 Gate #1 Q8) : l'{@code OIDCException} de quarkus-oidc, auparavant transformée en <b>500</b> par QuarkusErrorHandler,
 * est désormais convertie en <b>503</b> par {@code OidcIndisponibleFailureHandler} (jamais 401 : le jeton n'est pas en cause). Aucune donnée
 * n'est servie ; côté IHM un 503 donne un toast d'erreur (pas de déconnexion).
 */
@QuarkusTest
@TestProfile(OidcColdStartJwksDownTest.Profile.class)
class OidcColdStartJwksDownTest {

    public static class Profile implements QuarkusTestProfile {
        static final LocalOidcServer SERVER = new LocalOidcServer(true);

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
    void recuperationJwksEnEchec_rejet_failClosed_sansBlocage() {
        int status = assertTimeoutPreemptively(Duration.ofSeconds(20), OidcColdStartSupport::statusWithValidToken);
        assertEquals(503, status, "JWKS en panne => 503 (T4.0-e), ni 401 (jeton non en cause) ni 500");
        assertTrue(OidcColdStartSupport.hits("jwks") >= 1, "JWKS non interrogé : la config de test n'est pas effective");
    }
}
