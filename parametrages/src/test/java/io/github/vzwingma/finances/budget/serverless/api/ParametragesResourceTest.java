package io.github.vzwingma.finances.budget.serverless.api;

import io.github.vzwingma.finances.budget.serverless.data.MockDataCategoriesOperations;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.api.enums.ParametragesAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ParametragesService;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import jakarta.ws.rs.core.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;

@QuarkusTest
class ParametragesResourceTest {

    private static ParametragesService parametragesServiceMock;

    @BeforeAll
    static void init() {
        parametragesServiceMock = mock(ParametragesService.class);
        QuarkusMock.installMockForType(parametragesServiceMock, ParametragesService.class);
    }

    @BeforeEach
    void setup() {
        reset(parametragesServiceMock);
    }

    @Test
    void testInfoEndpoint() {

        given()
                .when().get(ParametragesAPIEnum.PARAMS_BASE + "/_info")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase("param"));
    }

    @Test
    void testGetCategories() {

        // Init des données
        when(parametragesServiceMock.getCategories()).thenReturn(Uni.createFrom().item(MockDataCategoriesOperations.getListeTestCategories()));
        // Test
        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .when().get(ParametragesAPIEnum.PARAMS_BASE + ParametragesAPIEnum.PARAMS_CATEGORIES)
                .then()
                .statusCode(200)
                .body(Matchers.containsString(MockDataCategoriesOperations.getListeTestCategories().getFirst().getLibelle()));
    }

    @Test
    void testGetCategorieById() {
        // Init des données
        when(parametragesServiceMock.getCategorieById("8f1614c9-503c-4e7d-8cb5-0c9a9218b84a"))
                .thenReturn(Uni.createFrom().item(MockDataCategoriesOperations.getListeTestCategories().getFirst()));
        // Test
        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .when().get(ParametragesAPIEnum.PARAMS_BASE + ParametragesAPIEnum.PARAMS_CATEGORIES + "/8f1614c9-503c-4e7d-8cb5-0c9a9218b84a")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("Alimentation"));
    }

    @Test
    void testGetCategorieByIdIntrouvable() {
        // Init des données
        when(parametragesServiceMock.getCategorieById("unknown"))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Catégorie non trouvée")));
        // Test
        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .when().get(ParametragesAPIEnum.PARAMS_BASE + ParametragesAPIEnum.PARAMS_CATEGORIES + "/unknown")
                .then()
                .statusCode(404);
    }


    private String getTestJWTAuthHeader() {
        return JwtTestSupport.signedBearer("Test.test@world.com");
    }
}
