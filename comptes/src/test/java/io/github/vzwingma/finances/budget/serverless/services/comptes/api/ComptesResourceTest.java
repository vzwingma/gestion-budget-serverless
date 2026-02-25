package io.github.vzwingma.finances.budget.serverless.services.comptes.api;

import io.github.vzwingma.finances.budget.serverless.services.comptes.api.enums.ComptesAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ComptesService;
import io.github.vzwingma.finances.budget.serverless.services.comptes.spi.JwsSigningKeysDatabaseAdaptor;
import io.github.vzwingma.finances.budget.serverless.services.comptes.test.data.MockDataComptes;
import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

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
        QuarkusMock.installMockForType(Mockito.mock(ComptesService.class), ComptesService.class);
        QuarkusMock.installMockForType(Mockito.mock(JwsSigningKeysDatabaseAdaptor.class), JwsSigningKeysDatabaseAdaptor.class);
    }

    @BeforeEach
    void setupMocks() {
        // Peupler le repository JWKS avec une clé de test (sans signature RSA réelle),
        // ce qui déclenche le chargement dans la map jwksAuthKeyList via loadJwksSigningKeys().
        // Le token de test n'est pas signé (hasSignature=false) → hasValidSignature() retourne true.
        Mockito.when(jwtSigningKeyReadRepository.getJwksSigningAuthKeys())
                .thenReturn(Multi.createFrom().item(getJwksAuthKey()));
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

    @Test
    void testGetComptesUtilisateur() {
        Mockito.when(comptesService.getComptesUtilisateur(Mockito.anyString()))
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
        Mockito.when(comptesService.getComptesUtilisateur(Mockito.anyString()))
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
        Mockito.when(comptesService.getCompteById(Mockito.eq("C1"), Mockito.anyString()))
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
        Mockito.when(comptesService.getCompteById(Mockito.eq("INCONNU"), Mockito.anyString()))
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
        p.setIat(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now()));
        p.setExp(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now().plusHours(1)));
        p.setIss("https://accounts.google.com");
        p.setAud("test.apps.googleusercontent.com");
        return "Bearer " + JWTUtils.encodeJWT(new JWTAuthToken(h, p));
    }

    private static JwksAuthKey getJwksAuthKey() {
        JwksAuthKey key = new JwksAuthKey();
        key.setKid("test");
        key.setKty("RSA");
        key.setAlg("RS256");
        key.setUse("sig");
        key.setN("test");
        key.setE("test");
        return key;
    }
}
