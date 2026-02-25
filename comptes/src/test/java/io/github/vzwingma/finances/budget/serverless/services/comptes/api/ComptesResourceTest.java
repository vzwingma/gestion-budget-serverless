package io.github.vzwingma.finances.budget.serverless.services.comptes.api;

import io.github.vzwingma.finances.budget.serverless.services.comptes.api.enums.ComptesAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ComptesService;
import io.github.vzwingma.finances.budget.serverless.services.comptes.test.data.MockDataComptes;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;

@QuarkusTest
class ComptesResourceTest {


    static ComptesService mockService;

    @BeforeAll
    static void init() {
        mockService = Mockito.mock(ComptesService.class);
        QuarkusMock.installMockForType(mockService, ComptesService.class);
    }

    @BeforeEach
    void setupMocks() {
        Mockito.when(mockService.loadJwksSigningKeys()).thenReturn(Uni.createFrom().item(new HashMap<>()));
    }

    @Test
    void testInfoEndpoint() {
        given()
                .when().get(ComptesAPIEnum.COMPTES_BASE + "/_info")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase("comptes"));
    }

    @Test
    void testGetComptesUtilisateur() {
        Mockito.when(mockService.getComptesUtilisateur(Mockito.anyString()))
                .thenReturn(Uni.createFrom().item(MockDataComptes.getListeComptes()));

        given()
                .when().get(ComptesAPIEnum.COMPTES_BASE + ComptesAPIEnum.COMPTES_LIST)
                .then()
                .statusCode(200);
    }

    @Test
    void testGetComptesUtilisateurVide() {
        Mockito.when(mockService.getComptesUtilisateur(Mockito.anyString()))
                .thenReturn(Uni.createFrom().item(List.of()));

        given()
                .when().get(ComptesAPIEnum.COMPTES_BASE + ComptesAPIEnum.COMPTES_LIST)
                .then()
                .statusCode(200);
    }

    @Test
    void testGetCompteUtilisateur() {
        Mockito.when(mockService.getCompteById(Mockito.eq("C1"), Mockito.anyString()))
                .thenReturn(Uni.createFrom().item(MockDataComptes.getCompte1()));

        given()
                .when().get(ComptesAPIEnum.COMPTES_BASE + "/C1")
                .then()
                .statusCode(200);
    }

    @Test
    void testGetCompteUtilisateurIntrouvable() {
        Mockito.when(mockService.getCompteById(Mockito.eq("INCONNU"), Mockito.anyString()))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Compte non trouvé")));

        given()
                .when().get(ComptesAPIEnum.COMPTES_BASE + "/INCONNU")
                .then()
                .statusCode(404);
    }
}

