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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Non-régression ADR-008 (AP005 T2.2), module parametrages : le tenant OIDC par défaut reste actif hors {@code /_info}.
 * {@code GET /parametres/v2/categories} ({@code @PermitAll}) : 200 sans jeton, 401 avec jeton invalide (auth proactive), 200 avec jeton valide.
 * Cas limite : {@code GET /parametres/v2/categories/_info} (identifiant de catégorie égal à {@code _info}) se termine par {@code /_info}.
 */
@QuarkusTest
class ProtectedEndpointNonRegressionTest {

    private static final String CATEGORIES = ParametragesAPIEnum.PARAMS_BASE + ParametragesAPIEnum.PARAMS_CATEGORIES;
    private static ParametragesService serviceMock;

    @BeforeAll
    static void init() {
        serviceMock = mock(ParametragesService.class);
        QuarkusMock.installMockForType(serviceMock, ParametragesService.class);
    }

    @BeforeEach
    void setup() {
        reset(serviceMock);
        when(serviceMock.getCategories()).thenReturn(Uni.createFrom().item(MockDataCategoriesOperations.getListeTestCategories()));
        when(serviceMock.getCategorieById(anyString())).thenReturn(Uni.createFrom().item(MockDataCategoriesOperations.getListeTestCategories().get(0)));
    }

    @Test
    void categories_sansJeton_200() {
        assertEquals(200, given().when().get(CATEGORIES).then().extract().statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer null", "Bearer a.b.c"})
    void categories_mauvaisJeton_401(String authorization) {
        assertEquals(401, given().header(HttpHeaders.AUTHORIZATION, authorization).when().get(CATEGORIES).then().extract().statusCode(), authorization);
        verify(serviceMock, never()).getCategories();
    }

    @Test
    void categories_jetonValide_200_tenantParDefautActif() {
        assertEquals(200, given().header(HttpHeaders.AUTHORIZATION, OidcTokenForge.bearer(OidcTokenForge.valid(OidcTokenForge.validClaims())))
                .when().get(CATEGORIES).then().extract().statusCode());
        verify(serviceMock).getCategories();
    }

    @Test
    void categoriesIdEgalInfo_jetonInvalide_200_anonyme_documente() {
        // Comportement réel figé (revue Gate #2) : le chemin /categories/_info se termine par /_info => tenant info-public (anonyme),
        // jeton invalide ignoré, route @PermitAll => 200. Sans impact de sécurité : même donnée publique que /categories (@PermitAll).
        // Un identifiant de catégorie réel n'est jamais "_info".
        assertEquals(200, given().header(HttpHeaders.AUTHORIZATION, "Bearer a.b.c").when().get(CATEGORIES + "/_info").then().extract().statusCode());
        assertEquals(200, given().when().get(CATEGORIES + "/_info").then().extract().statusCode());
        verify(serviceMock, org.mockito.Mockito.times(2)).getCategorieById("_info");
    }

    @Test
    void categoriesIdEgalInfo_jetonValide_200() {
        assertEquals(200, given().header(HttpHeaders.AUTHORIZATION, OidcTokenForge.bearer(OidcTokenForge.valid(OidcTokenForge.validClaims())))
                .when().get(CATEGORIES + "/_info").then().extract().statusCode());
    }
}
