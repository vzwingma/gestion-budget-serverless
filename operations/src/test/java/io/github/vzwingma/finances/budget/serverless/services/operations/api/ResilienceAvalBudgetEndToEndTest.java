package io.github.vzwingma.finances.budget.serverless.services.operations.api;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.enums.OperationsAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.aval.FauxAval;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.aval.ProfilFauxAvalTimeoutsReels;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AP004 T2.8 — de bout en bout : requête HTTP de l'IHM sur {@code GET /budgets/v2/query} (BudgetsResource, BudgetService et
 * client REST comptes <b>réels</b>, jeton OIDC valide) face à un faux service comptes. Vérifie le code HTTP réellement renvoyé à
 * l'IHM (plan IHM 011) : panne ou timeout de l'aval = 503 ; compte inconnu = 404 ; 401 / 403 de l'aval jamais transmis tels quels
 * (l'IHM déconnecterait l'utilisateur).
 * <p>
 * L'appel à comptes précède toute lecture MongoDB : ces cas n'ont pas besoin de base.
 */
@QuarkusTest
@TestProfile(ProfilFauxAvalTimeoutsReels.class)
class ResilienceAvalBudgetEndToEndTest {

    private static final int PORT_COMPTES = FauxAval.port(FauxAval.PROP_PORT_COMPTES);
    private static final String URL = OperationsAPIEnum.BUDGET_BASE + OperationsAPIEnum.BUDGET_QUERY + "?idCompte=C1&mois=1&annee=2026";

    private FauxAval avalComptes;

    @AfterEach
    void teardown() {
        if (avalComptes != null) {
            avalComptes.close();
        }
    }

    private static int statutIhm() {
        return given().header(HttpHeaders.AUTHORIZATION, OidcTokenForge.bearer(OidcTokenForge.valid(OidcTokenForge.validClaims())))
                .when().get(URL).then().extract().statusCode();
    }

    @Test
    void compteInconnu_404() throws IOException {
        avalComptes = FauxAval.demarrer(PORT_COMPTES, n -> 404, "", 0);
        assertEquals(404, statutIhm());
        assertEquals(1, avalComptes.appels());
    }

    @ParameterizedTest(name = "comptes répond {0} -> l'IHM reçoit 503")
    @ValueSource(ints = {401, 403, 500})
    void panneDeComptes_503_jamaisUn401Ou403(int statutAval) throws IOException {
        avalComptes = FauxAval.demarrer(PORT_COMPTES, n -> statutAval, "", 0);
        assertEquals(503, statutIhm(), "statut aval " + statutAval);
        assertEquals(1, avalComptes.appels());
    }

    @Test
    void avalIndisponible502_unRetryPuis503() throws IOException {
        avalComptes = FauxAval.demarrer(PORT_COMPTES, n -> 502, "", 0);
        assertEquals(503, statutIhm());
        assertEquals(2, avalComptes.appels());
    }

    @Test
    void avalInjoignable_connexionRefusee_503() {
        assertEquals(503, statutIhm());
    }

    @Test
    void avalLent_timeout_503_enMoinsDe6Secondes() throws IOException {
        avalComptes = FauxAval.demarrer(PORT_COMPTES, n -> 200, "{\"id\":\"C1\",\"libelle\":\"Compte\",\"actif\":true}", 6500);
        long debut = System.nanoTime();
        int statut = statutIhm();
        long dureeMs = (System.nanoTime() - debut) / 1_000_000;
        assertEquals(503, statut);
        assertTrue(dureeMs < 6000, "durée = " + dureeMs + " ms");
        assertEquals(1, avalComptes.appels(), "aucun retry sur timeout");
    }
}
