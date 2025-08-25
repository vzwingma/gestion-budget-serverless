package io.github.vzwingma.finances.budget.serverless.services.comptes.api;

import io.github.vzwingma.finances.budget.serverless.services.comptes.api.enums.ComptesAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ComptesService;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;

@QuarkusTest
class ComptesResourceTest {
    @Inject
    ComptesService comptesService;
    @BeforeAll
    static void init() {
        QuarkusMock.installMockForType(Mockito.mock(ComptesService.class), ComptesService.class);
    }

    @Test
    void testInfoEndpoint() {
        Mockito.when(comptesService.loadJwksSigningKeys()).thenReturn(Uni.createFrom().item(new HashMap<>()));
        given()
                .when().get(ComptesAPIEnum.COMPTES_BASE + "/_info")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase("comptes"));
    }

}
