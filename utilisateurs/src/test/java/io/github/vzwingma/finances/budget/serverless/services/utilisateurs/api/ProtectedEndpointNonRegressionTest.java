package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api.enums.UtilisateursAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.UtilisateursService;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.spi.JwsSigningKeysDatabaseAdaptor;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Non-régression ADR-008 (AP005 T2.2), module utilisateurs : le tenant OIDC par défaut reste actif hors {@code /_info}.
 * Endpoint {@code GET /utilisateurs/v2/lastaccessdate} ({@code @RolesAllowed USER_UTILISATEURS}) : 401 sans jeton / jeton invalide,
 * 200 avec jeton valide.
 */
@QuarkusTest
class ProtectedEndpointNonRegressionTest {

    private static final String URL = UtilisateursAPIEnum.USERS_BASE + UtilisateursAPIEnum.USERS_ACCESS_DATE;
    private static UtilisateursService serviceMock;

    @BeforeAll
    static void init() {
        serviceMock = mock(UtilisateursService.class);
        QuarkusMock.installMockForType(serviceMock, UtilisateursService.class);
        QuarkusMock.installMockForType(mock(JwsSigningKeysDatabaseAdaptor.class), JwsSigningKeysDatabaseAdaptor.class);
    }

    @BeforeEach
    void setup() {
        reset(serviceMock);
        when(serviceMock.getLastAccessDate(anyString())).thenReturn(Uni.createFrom().item(LocalDateTime.of(2026, 1, 10, 8, 0)));
    }

    @Test
    void sansJeton_401() {
        assertEquals(401, given().when().get(URL).then().extract().statusCode());
        verify(serviceMock, never()).getLastAccessDate(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer null", "Bearer a.b.c"})
    void mauvaisJeton_401(String authorization) {
        assertEquals(401, given().header(HttpHeaders.AUTHORIZATION, authorization).when().get(URL).then().extract().statusCode(), authorization);
        verify(serviceMock, never()).getLastAccessDate(anyString());
    }

    @Test
    void jetonValide_200_tenantParDefautActif() {
        assertEquals(200, given().header(HttpHeaders.AUTHORIZATION, OidcTokenForge.bearer(OidcTokenForge.valid(OidcTokenForge.validClaims())))
                .when().get(URL).then().extract().statusCode());
        verify(serviceMock).getLastAccessDate(OidcTokenForge.VALID_EMAIL);
    }
}
