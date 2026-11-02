package io.github.vzwingma.finances.budget.serverless.services.comptes.api;

import io.github.vzwingma.finances.budget.serverless.services.comptes.api.enums.ComptesAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ComptesService;
import io.github.vzwingma.finances.budget.serverless.services.comptes.test.data.MockDataComptes;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Non-régression ADR-008 (AP005 Gate #2) : le tenant OIDC {@code info-public} ne doit être sélectionné que pour {@code /_info}.
 * Une ressource dont l'identifiant vaut {@code info-public} (dernier segment du chemin) doit rester authentifiée par le tenant par défaut.
 * Route {@code GET /comptes/v2/{idCompte}} ({@code @RolesAllowed}) avec {@code idCompte=info-public}.
 */
@QuarkusTest
class InfoPublicTenantNonRegressionTest {

    private static final String URL = ComptesAPIEnum.COMPTES_BASE + "/info-public";
    private static ComptesService comptesServiceMock;

    @BeforeAll
    static void init() {
        comptesServiceMock = mock(ComptesService.class);
        QuarkusMock.installMockForType(comptesServiceMock, ComptesService.class);
    }

    @BeforeEach
    void setup() {
        reset(comptesServiceMock);
        when(comptesServiceMock.getCompteById(anyString(), anyString())).thenReturn(Uni.createFrom().item(MockDataComptes.getListeComptes().get(0)));
    }

    @Test
    void idInfoPublic_jetonValide_authentifie_pasAnonyme() {
        given().header(HttpHeaders.AUTHORIZATION, OidcTokenForge.bearer(OidcTokenForge.valid(OidcTokenForge.validClaims())))
                .when().get(URL).then().statusCode(200);
        // L'identité (email) est arrivée au service : preuve que le jeton a été validé par le tenant par défaut
        verify(comptesServiceMock).getCompteById("info-public", OidcTokenForge.VALID_EMAIL);
    }

    @Test
    void idInfoPublic_jetonInvalide_401() {
        assertEquals(401, given().header(HttpHeaders.AUTHORIZATION, "Bearer a.b.c").when().get(URL).then().extract().statusCode());
        assertEquals(401, given().header(HttpHeaders.AUTHORIZATION, "Bearer null").when().get(URL).then().extract().statusCode());
        verify(comptesServiceMock, never()).getCompteById(anyString(), anyString());
    }

    @Test
    void idInfoPublic_sansJeton_401() {
        assertEquals(401, given().when().get(URL).then().extract().statusCode());
    }
}
