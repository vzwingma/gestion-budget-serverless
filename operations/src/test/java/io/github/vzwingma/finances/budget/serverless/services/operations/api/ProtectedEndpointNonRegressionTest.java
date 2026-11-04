package io.github.vzwingma.finances.budget.serverless.services.operations.api;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.enums.OperationsAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.BudgetService;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.OperationsService;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.JwsSigningKeysDatabaseAdaptor;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataBudgets;
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
 * Non-régression ADR-008 (AP005 T2.2), module operations : le tenant OIDC par défaut reste actif hors {@code /_info}.
 * Endpoint {@code GET /budgets/v2/{idBudget}} ({@code @RolesAllowed USER_OPERATIONS}) : 401 sans jeton / jeton invalide, 200 avec jeton valide.
 */
@QuarkusTest
class ProtectedEndpointNonRegressionTest {

    private static final String URL = OperationsAPIEnum.BUDGET_BASE + "/budget-test";
    private static BudgetService budgetServiceMock;

    @BeforeAll
    static void init() {
        budgetServiceMock = mock(BudgetService.class);
        QuarkusMock.installMockForType(budgetServiceMock, BudgetService.class);
        QuarkusMock.installMockForType(mock(OperationsService.class), OperationsService.class);
        QuarkusMock.installMockForType(mock(JwsSigningKeysDatabaseAdaptor.class), JwsSigningKeysDatabaseAdaptor.class);
    }

    @BeforeEach
    void setup() {
        reset(budgetServiceMock);
        when(budgetServiceMock.getBudgetMensuel(anyString())).thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
    }

    @Test
    void sansJeton_401() {
        assertEquals(401, given().when().get(URL).then().extract().statusCode());
        verify(budgetServiceMock, never()).getBudgetMensuel(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer null", "Bearer a.b.c"})
    void mauvaisJeton_401(String authorization) {
        assertEquals(401, given().header(HttpHeaders.AUTHORIZATION, authorization).when().get(URL).then().extract().statusCode(), authorization);
        verify(budgetServiceMock, never()).getBudgetMensuel(anyString());
    }

    @Test
    void jetonValide_200_tenantParDefautActif() {
        assertEquals(200, given().header(HttpHeaders.AUTHORIZATION, OidcTokenForge.bearer(OidcTokenForge.valid(OidcTokenForge.validClaims())))
                .when().get(URL).then().extract().statusCode());
        verify(budgetServiceMock).getBudgetMensuel("budget-test");
    }

    @Test
    void idInfoPublic_jetonInvalide_401() {
        // Identifiant égal au nom du tenant : ne doit pas basculer en anonyme (ADR-008, segment quelconque)
        assertEquals(401, given().header(HttpHeaders.AUTHORIZATION, "Bearer a.b.c").when().get(OperationsAPIEnum.BUDGET_BASE + "/info-public")
                .then().extract().statusCode());
        verify(budgetServiceMock, never()).getBudgetMensuel(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Bearer a.b.c"})
    void idOperationInfo_ressourceProtegee_401_pasPublic(String authorization) {
        // Grep des ressources (AP005 T2.1/T2.2) : seule route @RolesAllowed se terminant par {param} sous /budgets/v2 = /{idBudget}/operations/{idOperation}.
        // Avec idOperation="_info" le chemin finit par /_info => tenant info-public (anonyme) : l'accès doit rester refusé par @RolesAllowed, jamais 200.
        var requete = given();
        if (!authorization.isEmpty()) {
            requete = requete.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        assertEquals(401, requete.when().delete(OperationsAPIEnum.BUDGET_BASE + "/budget-test/operations/_info").then().extract().statusCode(),
                "DELETE .../operations/_info ne doit pas devenir public");
        verify(budgetServiceMock, never()).getBudgetMensuel(anyString());
    }
}
