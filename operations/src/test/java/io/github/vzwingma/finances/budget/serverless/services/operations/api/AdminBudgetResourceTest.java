package io.github.vzwingma.finances.budget.serverless.services.operations.api;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.enums.OperationsAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.BudgetAdminService;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.BudgetService;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.OperationsService;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.JwsSigningKeysDatabaseAdaptor;
import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtAuthHeader;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.github.vzwingma.finances.budget.services.communs.utils.security.JWTUtils;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
class AdminBudgetResourceTest {

    // Mock injecté par CDI — à utiliser pour les stubs Mockito (pattern identique à BudgetsResourceTest)
    @Inject
    BudgetAdminService budgetAdminService;

    @Inject
    IJwtSigningKeyReadRepository jwtSigningKeyReadRepository;

    @BeforeAll
    static void init() {
        QuarkusMock.installMockForType(Mockito.mock(BudgetAdminService.class), BudgetAdminService.class);
        QuarkusMock.installMockForType(Mockito.mock(BudgetService.class), BudgetService.class);
        QuarkusMock.installMockForType(Mockito.mock(OperationsService.class), OperationsService.class);
        QuarkusMock.installMockForType(Mockito.mock(JwsSigningKeysDatabaseAdaptor.class), JwsSigningKeysDatabaseAdaptor.class);
    }

    @BeforeEach
    void setup() {
        Mockito.when(jwtSigningKeyReadRepository.getJwksSigningAuthKeys())
                .thenReturn(Multi.createFrom().item(BudgetsResourceTest.jwksAuthKey()));
    }

    @Test
    void testConsolidateLibellesOperations() {
        Mockito.when(budgetAdminService.overrideLibellesOperations(anyString(), anyList()))
                .thenReturn(io.smallrye.mutiny.Multi.createFrom().items("C1_2022_01", "C1_2022_02"));

        String url = OperationsAPIEnum.BUDGET_ADMIN_BASE
                + OperationsAPIEnum.OPERATIONS_LIBELLES_OVERRIDE.replace(OperationsAPIEnum.PARAM_ID_COMPTE, "C1");

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.ACCEPT, "application/json")
                .body("[{\"avant\":\"avant\",\"apres\":\"apres\"}]")
                .when().post(url)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("C1_2022_01"));
    }

    @Test
    void testConsolidateLibellesOperationsListeVide() {
        String url = OperationsAPIEnum.BUDGET_ADMIN_BASE
                + OperationsAPIEnum.OPERATIONS_LIBELLES_OVERRIDE.replace(OperationsAPIEnum.PARAM_ID_COMPTE, "C1");

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.ACCEPT, "application/json")
                .body("[]")
                .when().post(url)
                .then()
                .statusCode(200);
    }

    private String getTestJWTAuthHeader() {
        JwtAuthHeader h = new JwtAuthHeader();
        JWTAuthPayload p = new JWTAuthPayload();
        p.setName("Test");
        p.setEmail("toto.toto@world.com");
        p.setFamily_name("Test");
        p.setGiven_name("Test");
        p.setIat(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now()));
        p.setExp(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now().plusHours(1)));
        p.setIss("https://accounts.google.com");
        p.setAud("test.apps.googleusercontent.com");
        return "Bearer " + JWTUtils.encodeJWT(new JWTAuthToken(h, p));
    }
}
