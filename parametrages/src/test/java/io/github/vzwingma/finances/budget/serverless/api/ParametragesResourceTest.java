package io.github.vzwingma.finances.budget.serverless.api;

import io.github.vzwingma.finances.budget.serverless.data.MockDataCategoriesOperations;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.api.enums.ParametragesAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ParametragesService;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.spi.JwsSigningKeysDatabaseAdaptor;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtAuthHeader;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.security.JWTUtils;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;

@QuarkusTest
class ParametragesResourceTest {

    @Inject
    ParametragesService parametragesService;

    @Inject
    IJwtSigningKeyReadRepository jwtSigningKeyReadRepository;

    @BeforeAll
    static void init() {
        QuarkusMock.installMockForType(Mockito.mock(ParametragesService.class), ParametragesService.class);
        QuarkusMock.installMockForType(Mockito.mock(JwsSigningKeysDatabaseAdaptor.class), IJwtSigningKeyReadRepository.class);

    }

    @BeforeEach
    void setup() {
        Mockito.when(jwtSigningKeyReadRepository.getJwksSigningAuthKeys()).thenReturn(Multi.createFrom().item(jwksAuthKey()));

    }

    public static JwksAuthKey jwksAuthKey() {
        JwksAuthKey jwksAuthKey = new JwksAuthKey();
        jwksAuthKey.setKid("test");
        jwksAuthKey.setKty("RSA");
        jwksAuthKey.setAlg("RS256");
        jwksAuthKey.setUse("sig");
        jwksAuthKey.setN("test");
        jwksAuthKey.setE("test");
        return jwksAuthKey;
    }

    @Test
    void testInfoEndpoint() {

        Mockito.when(parametragesService.refreshJwksSigningKeys()).thenReturn(Uni.createFrom().voidItem());
        Mockito.when(parametragesService.loadJwksSigningKeys()).thenReturn(Uni.createFrom().item(new HashMap<>()));
        given()
                .when().get(ParametragesAPIEnum.PARAMS_BASE + "/_info")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase("param"));
    }

    @Test
    void testGetCategories() {

        // Init des données
        Mockito.when(parametragesService.getCategories()).thenReturn(Uni.createFrom().item(MockDataCategoriesOperations.getListeTestCategories()));
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
        Mockito.when(parametragesService.getCategorieById(Mockito.eq("8f1614c9-503c-4e7d-8cb5-0c9a9218b84a")))
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
        Mockito.when(parametragesService.getCategorieById(Mockito.eq("unknown")))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Catégorie non trouvée")));
        // Test
        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .when().get(ParametragesAPIEnum.PARAMS_BASE + ParametragesAPIEnum.PARAMS_CATEGORIES + "/unknown")
                .then()
                .statusCode(404);
    }

    @Test
    void testRefreshJwksSigningKeys() {
        Mockito.when(parametragesService.refreshJwksSigningKeys()).thenReturn(Uni.createFrom().voidItem());
        Mockito.when(parametragesService.loadJwksSigningKeys()).thenReturn(Uni.createFrom().item(new HashMap<>()));
        given()
                .when().get(ParametragesAPIEnum.PARAMS_BASE + "/_info")
                .then()
                .statusCode(200);
    }


    private String getTestJWTAuthHeader() {
        JwtAuthHeader h = new JwtAuthHeader();
        JWTAuthPayload p = new JWTAuthPayload();
        p.setName("Test");
        p.setEmail("Test.test@world.com");
        p.setFamily_name("Test");
        p.setGiven_name("Test");
        p.setIat(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now()));
        p.setExp(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now().plusHours(1)));
        p.setIss("https://accounts.google.com");
        p.setAud("test.apps.googleusercontent.com");
        return "Bearer " + JWTUtils.encodeJWT(new JWTAuthToken(h, p));
    }
}
