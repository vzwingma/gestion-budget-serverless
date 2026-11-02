package io.github.vzwingma.finances.budget.serverless.api;

import io.github.vzwingma.finances.budget.serverless.services.parametrages.api.enums.ParametragesAPIEnum;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AP005 T2.1 / ADR-008 : {@code /_info} est public (200 + JSON {@code nom}/{@code version}) quel que soit l'en-tête Authorization,
 * y compris un jeton invalide ou valide (tenant OIDC {@code info-public} désactivé, sélectionné par {@code InfoPublicTenantResolver}).
 * Les variantes de chemin ne se terminant pas exactement par {@code /_info} retombent sur le tenant par défaut (échec fermé).
 */
@QuarkusTest
class InfoEndpointPublicTest {

    private static final String INFO = ParametragesAPIEnum.PARAMS_BASE + "/_info";

    private static String bearerOf(Map<String, Object> claims) {
        return OidcTokenForge.bearer(OidcTokenForge.valid(claims));
    }

    /** @return les valeurs d'en-tête Authorization (invalides ou valide) pour lesquelles /_info doit répondre 200 */
    static Stream<Arguments> authorizations() {
        Map<String, Object> expire = OidcTokenForge.validClaims();
        expire.put("exp", JwtTestSupport.epochSecondsIn(Duration.ofHours(-1)));
        Map<String, Object> emailNonVerifie = OidcTokenForge.validClaims();
        emailNonVerifie.put("email_verified", false);
        Map<String, Object> issuerInvalide = OidcTokenForge.validClaims();
        issuerInvalide.put("iss", "accounts.google.com.evil.com");
        Map<String, Object> algNone = OidcTokenForge.rs256Header();
        algNone.put("alg", "none");
        return Stream.of(
                Arguments.of("Bearer null", "Bearer null"),
                Arguments.of("Bearer a.b.c", "Bearer a.b.c"),
                Arguments.of("jeton valide sans préfixe Bearer", OidcTokenForge.valid(OidcTokenForge.validClaims())),
                Arguments.of("jeton expiré", bearerOf(expire)),
                Arguments.of("signé par une autre clé", OidcTokenForge.bearer(OidcTokenForge.sign(OidcTokenForge.rs256Header(),
                        OidcTokenForge.validClaims(), OidcTokenForge.otherPrivateKey()))),
                Arguments.of("non signé", OidcTokenForge.bearer(OidcTokenForge.unsigned(OidcTokenForge.rs256Header(), OidcTokenForge.validClaims()))),
                Arguments.of("alg=none", OidcTokenForge.bearer(OidcTokenForge.unsigned(algNone, OidcTokenForge.validClaims()))),
                Arguments.of("email_verified=false", bearerOf(emailNonVerifie)),
                Arguments.of("issuer invalide", bearerOf(issuerInvalide)),
                Arguments.of("jeton valide", bearerOf(OidcTokenForge.validClaims()))
        );
    }

    @Test
    void info_sansEnTete_200_jsonNomVersion() {
        given().when().get(INFO).then().statusCode(200)
                .body("nom", notNullValue())
                .body("version", notNullValue());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("authorizations")
    void info_quelQueSoitLeJeton_200_jsonNomVersion(String cas, String authorization) {
        given().header(HttpHeaders.AUTHORIZATION, authorization)
                .when().get(INFO)
                .then().statusCode(200)
                .body("nom", notNullValue())
                .body("version", notNullValue());
    }

    // ---------- variantes de chemin (documentation du comportement réel, échec fermé) ----------

    @ParameterizedTest
    @ValueSource(strings = {"/_info/", "/_info;x", "/_INFO"})
    void variantesChemin_jetonInvalide_pasPublic_401(String suffix) {
        // Ne se terminent pas par "/_info" exactement : tenant par défaut => jeton invalide refusé (aucune fuite de l'anonymat)
        assertEquals(401, given().header(HttpHeaders.AUTHORIZATION, "Bearer a.b.c").when().get(ParametragesAPIEnum.PARAMS_BASE + suffix)
                .then().extract().statusCode(), "jeton invalide sur " + suffix);
    }

    @Test
    void variantesChemin_sansJeton_resultatDocumente() {
        // Comportement réel figé (AP005 T2.1) :
        // - "/_info/" : le routage JAX-RS tolère le "/" final => 200 sans jeton (@PermitAll) ; avec jeton invalide => 401 (test ci-dessus) : échec fermé.
        // - "/_info;x" et "/_INFO" : 404 sans jeton dans ce module (aucune route paramétrée {id} ne les capte ; aucune ouverture de l'accès).
        assertEquals(200, given().when().get(INFO + "/").then().extract().statusCode(), "/_info/");
        assertEquals(404, given().when().get(INFO + ";x").then().extract().statusCode(), "/_info;x");
        assertEquals(404, given().when().get(ParametragesAPIEnum.PARAMS_BASE + "/_INFO").then().extract().statusCode(), "/_INFO");
    }
}
