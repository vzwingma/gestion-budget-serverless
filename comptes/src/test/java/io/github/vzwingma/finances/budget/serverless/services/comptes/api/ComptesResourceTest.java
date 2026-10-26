package io.github.vzwingma.finances.budget.serverless.services.comptes.api;

import io.github.vzwingma.finances.budget.serverless.services.comptes.api.enums.ComptesAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ComptesService;
import io.github.vzwingma.finances.budget.serverless.services.comptes.spi.JwsSigningKeysDatabaseAdaptor;
import io.github.vzwingma.finances.budget.serverless.services.comptes.test.data.MockDataComptes;
import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtAuthHeader;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;

@QuarkusTest
class ComptesResourceTest {

    // Mock injecté par CDI — à utiliser pour les stubs Mockito (pattern identique à BudgetsResourceTest)
    @Inject
    ComptesService comptesService;

    @Inject
    IJwtSigningKeyReadRepository jwtSigningKeyReadRepository;

    @BeforeAll
    static void init() {
        QuarkusMock.installMockForType(mock(ComptesService.class), ComptesService.class);
        QuarkusMock.installMockForType(mock(JwsSigningKeysDatabaseAdaptor.class), JwsSigningKeysDatabaseAdaptor.class);
    }

    
    @Test
    void testInfoEndpoint() {
        when(comptesService.loadJwksSigningKeys()).thenReturn(Uni.createFrom().item(new HashMap<>()));
        given()
                .when().get(ComptesAPIEnum.COMPTES_BASE + "/_info")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase("comptes"));
    }

    @Test
    void testGetComptesUtilisateur() {
        when(comptesService.getComptesUtilisateur(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataComptes.getListeComptes()));

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .when().get(ComptesAPIEnum.COMPTES_BASE + ComptesAPIEnum.COMPTES_LIST)
                .then()
                .statusCode(200);
    }

    @Test
    void testGetComptesUtilisateurVide() {
        when(comptesService.getComptesUtilisateur(anyString()))
                .thenReturn(Uni.createFrom().item(List.of()));

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .when().get(ComptesAPIEnum.COMPTES_BASE + ComptesAPIEnum.COMPTES_LIST)
                .then()
                .statusCode(200);
    }

    @Test
    void testGetCompteUtilisateur() {
        when(comptesService.getCompteById(eq("C1"), anyString()))
                .thenReturn(Uni.createFrom().item(MockDataComptes.getCompte1()));

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .when().get(ComptesAPIEnum.COMPTES_BASE + "/C1")
                .then()
                .statusCode(200);
    }

    @Test
    void testGetCompteUtilisateurIntrouvable() {
        when(comptesService.getCompteById(eq("INCONNU"), anyString()))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Compte non trouvé")));

        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .when().get(ComptesAPIEnum.COMPTES_BASE + "/INCONNU")
                .then()
                .statusCode(404);
    }

    // ---- helpers ----

    private String getTestJWTAuthHeader() {
        JwtAuthHeader h = new JwtAuthHeader();
        JWTAuthPayload p = new JWTAuthPayload();
        p.setName("Test");
        p.setEmail("toto.toto@world.com");
        p.setFamily_name("Test");
        p.setGiven_name("Test");
        p.setIat(JwtTestSupport.epochSecondsIn(java.time.Duration.ZERO));
        p.setExp(JwtTestSupport.epochSecondsIn(java.time.Duration.ofHours(1)));
        p.setIss("https://accounts.google.com");
        p.setAud("test.apps.googleusercontent.com");
        return JwtTestSupport.signedBearer(new JWTAuthToken(h, p));
    }
}
