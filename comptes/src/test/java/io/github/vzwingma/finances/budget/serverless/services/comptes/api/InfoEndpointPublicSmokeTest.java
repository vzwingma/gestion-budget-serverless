package io.github.vzwingma.finances.budget.serverless.services.comptes.api;

import io.github.vzwingma.finances.budget.serverless.services.comptes.api.enums.ComptesAPIEnum;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Vérification minimale (AP005 T1) : {@code /_info} est public via le tenant OIDC {@code info-public} désactivé (ADR-008),
 * même avec un jeton invalide. Tests complets : AP005 phase 2.
 */
@QuarkusTest
class InfoEndpointPublicSmokeTest {

    private static final String URL = ComptesAPIEnum.COMPTES_BASE + "/_info";

    @Test
    void info_bearerNull_200() {
        given().header(HttpHeaders.AUTHORIZATION, "Bearer null")
                .when().get(URL)
                .then().statusCode(200);
    }

    @Test
    void info_sansJeton_200() {
        given().when().get(URL).then().statusCode(200);
    }
}
