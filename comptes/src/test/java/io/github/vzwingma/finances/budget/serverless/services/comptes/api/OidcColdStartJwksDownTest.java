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
 * Constat documenté : l'{@code OIDCException} de quarkus-oidc n'est pas convertie en 401 mais en <b>500</b> (QuarkusErrorHandler). Aucune donnée
 * n'est servie ; côté IHM un 500 donne un toast d'erreur (pas de déconnexion). À valider en QUA (panne Google : comportement acceptable ?).
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
        assertTrue(status == 401 || status == 500, "JWKS indisponible => rejet attendu (401 ou 500), reçu " + status);
        assertEquals(500, status, "comportement constaté (documenté) : JWKS en panne => 500, pas 401");
        assertTrue(OidcColdStartSupport.hits("jwks") >= 1, "JWKS non interrogé : la config de test n'est pas effective");
    }
}
