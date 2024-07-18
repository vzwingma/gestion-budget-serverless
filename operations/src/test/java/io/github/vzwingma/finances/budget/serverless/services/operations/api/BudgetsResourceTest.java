package io.github.vzwingma.finances.budget.serverless.services.operations.api;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.enums.OperationsAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.BudgetService;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.OperationsService;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IBudgetAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.JwsSigningKeysDatabaseAdaptor;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataBudgets;
import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtAuthHeader;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.github.vzwingma.finances.budget.services.communs.utils.security.JWTUtils;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.time.Month;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;


@QuarkusTest
class BudgetsResourceTest {

    @Inject
    IBudgetAppProvider budgetService;


    @BeforeAll
    public static void init() {
        QuarkusMock.installMockForType(Mockito.mock(BudgetService.class), BudgetService.class);
        QuarkusMock.installMockForType(Mockito.mock(OperationsService.class), OperationsService.class);
        QuarkusMock.installMockForType(Mockito.mock(JwsSigningKeysDatabaseAdaptor.class), JwsSigningKeysDatabaseAdaptor.class);
    }


    @Test
    void testInfoEndpoint() {
        given()
                .when().get(OperationsAPIEnum.BUDGET_BASE + "/_info")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase("rations"));
    }

    /**
     * POST Set Actif OperationsApiUrlEnum.BUDGET_ETAT
     */
    @Test
    void testSetEtatActif() {
        // Init des données
        Mockito.when(budgetService.setBudgetActif(anyString(), anyBoolean()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        // Test
        String url = OperationsAPIEnum.BUDGET_BASE
                + OperationsAPIEnum.BUDGET_ETAT.replace(OperationsAPIEnum.PARAM_ID_BUDGET, "1") + "?actif=true";

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                    .header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .header(HttpHeaders.ACCEPT, "application/json")
                .body("{}")
                .when().post(url)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("true"));
    }

    /**
     * Test OperationsApiUrlEnum.BUDGET_ETAT
     */
    @Test
    void testIsActifOK() {
        // Init des données
        Mockito.when(budgetService.isBudgetMensuelActif(anyString()))
                .thenReturn(Uni.createFrom().item(Boolean.TRUE));
        // Test
        String url = OperationsAPIEnum.BUDGET_BASE
                + OperationsAPIEnum.BUDGET_ETAT.replace(OperationsAPIEnum.PARAM_ID_BUDGET, "1")
                + "?actif=true";

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("true"));
    }

    @Test
    void testIsActifNOK() {
        // Init des données
        Mockito.when(budgetService.isBudgetMensuelActif(anyString()))
                .thenReturn(Uni.createFrom().item(Boolean.FALSE));
        // Test
        String url = OperationsAPIEnum.BUDGET_BASE
                + OperationsAPIEnum.BUDGET_ETAT.replace(OperationsAPIEnum.PARAM_ID_BUDGET, "1")
                + "?actif=true";

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader()).header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("false"));
    }

    @Test
    void testGetBudgetsUtilisateurById() {
        // Init des données
        Mockito.when(budgetService.getBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        // Test
        String url = OperationsAPIEnum.BUDGET_BASE
                + OperationsAPIEnum.BUDGET_ID.replace(OperationsAPIEnum.PARAM_ID_BUDGET, "1");

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader()).header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .when()
                .get(url)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("TEST1"));
    }


    @Test
    void testGetBudgetsUtilisateurByParams() {
        // Init des données
        Mockito.when(budgetService.getBudgetMensuel(anyString(), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        // Test
        String url = OperationsAPIEnum.BUDGET_BASE
                + OperationsAPIEnum.BUDGET_QUERY + "?idCompte=1&mois=1&annee=2020";

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader()).header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .when().get(url)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("TEST1"));
    }


    @Test
    void testGetBudgetsUtilisateurByParamsKO() {
        // Test
        String url = OperationsAPIEnum.BUDGET_BASE + OperationsAPIEnum.BUDGET_QUERY;

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader()).header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .when().get(url)
                .then()
                .statusCode(500);
    }


    @Test
    void testReinitBudget() {
        // Init des données
        Mockito.when(budgetService.reinitialiserBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        // Test
        String url = OperationsAPIEnum.BUDGET_BASE
                + OperationsAPIEnum.BUDGET_ID.replace(OperationsAPIEnum.PARAM_ID_BUDGET, "1");

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .when().delete(url)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("TEST1"));
    }


    private String getTestJWTAuthHeader() {
        JwtAuthHeader h = new JwtAuthHeader();
        JWTAuthPayload p = new JWTAuthPayload();
        p.setName("Test");
        p.setFamily_name("Test");
        p.setGiven_name("Test");
        p.setIat(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now()));
        p.setExp(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now().plusHours(1)));
        p.setIss("https://accounts.google.com");
        p.setAud("test.apps.googleusercontent.com");
        return "Bearer " + JWTUtils.encodeJWT(new JWTAuthToken(h, p));
    }
}
