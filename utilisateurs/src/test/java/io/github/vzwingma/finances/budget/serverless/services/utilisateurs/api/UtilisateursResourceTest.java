package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api.enums.UtilisateursAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.UtilisateursService;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.data.MockDataUtilisateur;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.spi.JwsSigningKeysDatabaseAdaptor;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtAuthHeader;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
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
class UtilisateursResourceTest {

    @Inject
    UtilisateursService utilisateurService;

    @Inject
    IJwtSigningKeyReadRepository jwtSigningKeyReadRepository;

    @BeforeAll
    static void init() {
        QuarkusMock.installMockForType(Mockito.mock(UtilisateursService.class), UtilisateursService.class);
        QuarkusMock.installMockForType(Mockito.mock(JwsSigningKeysDatabaseAdaptor.class), JwsSigningKeysDatabaseAdaptor.class);

    }

    @BeforeEach
    void setup() {
        Mockito.when(jwtSigningKeyReadRepository.getJwksSigningAuthKeys()).thenReturn(Multi.createFrom().item(jwksAuthKey()));
    }
    @Test
    void testInfoEndpoint() {
        Mockito.when(utilisateurService.loadJwksSigningKeys()).thenReturn(Uni.createFrom().item(new HashMap<>()));
        given()
                .when().get(UtilisateursAPIEnum.USERS_BASE + "/_info")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase("utilisateurs"));
    }

    @Test
    void testGetLastAccessDate() {
        // Init des données
        Utilisateur utilisateurExpected = MockDataUtilisateur.getTestUtilisateurWithDate();
        Mockito.when(utilisateurService.getLastAccessDate(Mockito.anyString()))
                .thenReturn(Uni.createFrom().item(utilisateurExpected.getDernierAcces()));

        // Test
        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .when()
                .get(UtilisateursAPIEnum.USERS_BASE + UtilisateursAPIEnum.USERS_ACCESS_DATE)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("lastAccessTime"));
    }


    @Test
    void testGetPreferences() {
        // Init des données
        Utilisateur utilisateurExpected = MockDataUtilisateur.getTestUtilisateurWithDate();
        Mockito.when(utilisateurService.getUtilisateur(Mockito.anyString()))
                .thenReturn(Uni.createFrom().item(utilisateurExpected));
        // Test
        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .when()
                .get(UtilisateursAPIEnum.USERS_BASE + UtilisateursAPIEnum.USERS_PREFS)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("\"preferences\":{\"PREFS_STATUT_NLLE_DEPENSE\":\"Nouvelle\"}"));
    }


    @Test
    void testForUtilisateurUnkown() {
        // Init des données
        Mockito.when(utilisateurService.getLastAccessDate(Mockito.anyString()))
                .thenReturn(Uni.createFrom().nullItem());

        // Test
        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .when()
                .get(UtilisateursAPIEnum.USERS_BASE + UtilisateursAPIEnum.USERS_ACCESS_DATE)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("\"lastAccessTime\":null"));
    }


    private String getTestJWTAuthHeader() {
        JwtAuthHeader h = new JwtAuthHeader();
        JWTAuthPayload p = new JWTAuthPayload();
        p.setName("Test");
        p.setEmail("test.test@world.com");
        p.setFamily_name("Test");
        p.setGiven_name("Test");
        p.setIat(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now()));
        p.setExp(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now().plusHours(1)));
        p.setIss("https://accounts.google.com");
        p.setAud("test.apps.googleusercontent.com");
        return "Bearer " + JWTUtils.encodeJWT(new JWTAuthToken(h, p));
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
}
