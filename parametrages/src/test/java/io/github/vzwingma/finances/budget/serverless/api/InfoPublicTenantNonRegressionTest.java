package io.github.vzwingma.finances.budget.serverless.api;

import io.github.vzwingma.finances.budget.serverless.data.MockDataCategoriesOperations;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.api.enums.ParametragesAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ParametragesService;
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
 * Non-régression ADR-008 (AP005 Gate #2) : {@code /parametres/v2/categories/info-public} (dernier segment = {@code info-public}) ne doit PAS
 * être traité par le tenant OIDC {@code info-public} : un jeton invalide reste refusé (401), même sur une route {@code @PermitAll}.
 */
@QuarkusTest
class InfoPublicTenantNonRegressionTest {

    private static final String URL = ParametragesAPIEnum.PARAMS_BASE + ParametragesAPIEnum.PARAMS_CATEGORIES + "/info-public";
    private static ParametragesService serviceMock;

    @BeforeAll
    static void init() {
        serviceMock = mock(ParametragesService.class);
        QuarkusMock.installMockForType(serviceMock, ParametragesService.class);
    }

    @BeforeEach
    void setup() {
        reset(serviceMock);
        when(serviceMock.getCategorieById(anyString())).thenReturn(Uni.createFrom().item(MockDataCategoriesOperations.getListeTestCategories().get(0)));
    }

    @Test
    void idInfoPublic_jetonInvalide_401() {
        assertEquals(401, given().header(HttpHeaders.AUTHORIZATION, "Bearer a.b.c").when().get(URL).then().extract().statusCode());
        assertEquals(401, given().header(HttpHeaders.AUTHORIZATION, "Bearer null").when().get(URL).then().extract().statusCode());
        verify(serviceMock, never()).getCategorieById(anyString());
    }

    @Test
    void idInfoPublic_sansJeton_permitAll_200() {
        assertEquals(200, given().when().get(URL).then().extract().statusCode());
        verify(serviceMock).getCategorieById("info-public");
    }

    @Test
    void idInfoPublic_jetonValide_200() {
        assertEquals(200, given().header(HttpHeaders.AUTHORIZATION, OidcTokenForge.bearer(OidcTokenForge.valid(OidcTokenForge.validClaims())))
                .when().get(URL).then().extract().statusCode());
    }
}
