package io.github.vzwingma.finances.budget.serverless.services.comptes.api;

import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ComptesService;
import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ports.IComptesAppProvider;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;

@QuarkusTest
class ComptesResourceTest {


    @Test
    void testInfoEndpoint() {
        given()
          .when().get("/_info")
          .then()
             .statusCode(200)
                .body(containsStringIgnoringCase("comptes"));
    }

    @Inject
    IComptesAppProvider comptesService;

    @BeforeAll
    public static void init() {
        QuarkusMock.installMockForType(Mockito.mock(ComptesService.class), ComptesService.class);
    }

}
