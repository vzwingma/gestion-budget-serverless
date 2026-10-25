package io.github.vzwingma.finances.budget.serverless.api;

import io.github.vzwingma.finances.budget.serverless.data.MockDataCategoriesOperations;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.api.enums.ParametragesAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ParametragesService;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Tests de sécurité des endpoints {@code @PermitAll} (AP004 T0.5), module parametrages.
 * Comportement accepté (décision 👤 Gate #2) : sans jeton => accessible (200) ; avec jeton invalide => 401
 * (authentification proactive de quarkus-oidc, y compris sur {@code @PermitAll}) ; avec jeton valide => 200.
 * Endpoints : {@code GET /params/v2/_info} et {@code GET /params/v2/categories} (tous deux {@code @PermitAll}).
 */
@QuarkusTest
class OidcSecurityPermitAllTest {

    private static final String INFO = ParametragesAPIEnum.PARAMS_BASE + "/_info";
    private static final String CATEGORIES = ParametragesAPIEnum.PARAMS_BASE + ParametragesAPIEnum.PARAMS_CATEGORIES;
    private static ParametragesService serviceMock;

    @BeforeAll
    static void init() {
        serviceMock = mock(ParametragesService.class);
        QuarkusMock.installMockForType(serviceMock, ParametragesService.class);
    }

    @BeforeEach
    void setup() {
        reset(serviceMock);
        when(serviceMock.getCategories()).thenReturn(Uni.createFrom().item(MockDataCategoriesOperations.getListeTestCategories()));
    }

    private static int status(String url, String authorization) {
        var req = given();
        if (authorization != null) {
            req = req.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return req.when().get(url).then().extract().statusCode();
    }

    @ParameterizedTest
    @ValueSource(strings = {INFO, CATEGORIES})
    void permitAll_sansJeton_200(String url) {
        assertEquals(200, status(url, null), "@PermitAll sans jeton doit rester accessible : " + url);
    }

    @ParameterizedTest
    @ValueSource(strings = {INFO, CATEGORIES})
    void permitAll_jetonValide_200(String url) {
        assertEquals(200, status(url, OidcTokenForge.bearer(OidcTokenForge.valid(OidcTokenForge.validClaims()))), url);
    }

    @ParameterizedTest
    @ValueSource(strings = {INFO, CATEGORIES})
    void permitAll_jetonInvalide_401_authProactive(String url) {
        // Comportement accepté (décision 👤) : un jeton invalide est refusé même sur @PermitAll
        assertEquals(401, status(url, "Bearer null"), "Bearer null : " + url);
        assertEquals(401, status(url, "Bearer a.b.c"), "mal formé : " + url);
        assertEquals(401, status(url, OidcTokenForge.bearer(OidcTokenForge.sign(OidcTokenForge.rs256Header(), OidcTokenForge.validClaims(),
                OidcTokenForge.otherPrivateKey()))), "autre clé : " + url);
        assertEquals(401, status(url, OidcTokenForge.bearer(OidcTokenForge.unsigned(OidcTokenForge.rs256Header(), OidcTokenForge.validClaims()))),
                "non signé : " + url);
    }

    @Test
    void permitAll_jetonExpire_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("exp", JwtTestSupport.epochSecondsIn(Duration.ofHours(-1)));
        assertEquals(401, status(CATEGORIES, OidcTokenForge.bearer(OidcTokenForge.valid(claims))));
    }

    @Test
    void permitAll_issuerInvalide_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("iss", "accounts.google.com.evil.com");
        assertEquals(401, status(CATEGORIES, OidcTokenForge.bearer(OidcTokenForge.valid(claims))));
    }

    @Test
    void permitAll_emailVerifiedFalse_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("email_verified", false);
        assertEquals(401, status(INFO, OidcTokenForge.bearer(OidcTokenForge.valid(claims))));
    }
}
