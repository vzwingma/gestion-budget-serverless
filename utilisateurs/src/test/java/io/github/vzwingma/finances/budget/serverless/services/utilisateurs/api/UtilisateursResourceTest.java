package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api.enums.UtilisateursAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.UtilisateursService;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.data.MockDataUtilisateur;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsStringIgnoringCase;

@QuarkusTest
class UtilisateursResourceTest {

    @Inject
    UtilisateursService utilisateurService;

    @BeforeAll
    static void init() {
        QuarkusMock.installMockForType(mock(UtilisateursService.class), UtilisateursService.class);

    }

    @Test
    void testInfoEndpoint() {
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
        when(utilisateurService.getLastAccessDate(anyString()))
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
        when(utilisateurService.getUtilisateur(anyString()))
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
        when(utilisateurService.getLastAccessDate(anyString()))
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

    @Test
    void testGetPreferencesUtilisateurVide() {
        // Utilisateur sans préférences ni droits
        when(utilisateurService.getUtilisateur(anyString()))
                .thenReturn(Uni.createFrom().nullItem());
        given()
                .header(HttpHeaders.AUTHORIZATION, getTestJWTAuthHeader())
                .when()
                .get(UtilisateursAPIEnum.USERS_BASE + UtilisateursAPIEnum.USERS_PREFS)
                .then()
                .statusCode(200);
    }

    @Test
    void testGetLastAccessDateSansAuth() {
        // Sans header Authorization → 401 (quarkus-oidc ; 403 avec le filtre maison, cf. AP004 T0.6)
        given()
                .when()
                .get(UtilisateursAPIEnum.USERS_BASE + UtilisateursAPIEnum.USERS_ACCESS_DATE)
                .then()
                .statusCode(401);
    }


    private String getTestJWTAuthHeader() {
        return JwtTestSupport.signedBearer("test.test@world.com");
    }


}
