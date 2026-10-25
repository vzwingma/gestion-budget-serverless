package io.github.vzwingma.finances.budget.serverless.services.comptes.api;

import io.github.vzwingma.finances.budget.serverless.services.comptes.api.enums.ComptesAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ComptesService;
import io.github.vzwingma.finances.budget.serverless.services.comptes.test.data.MockDataComptes;
import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.quarkus.test.junit.QuarkusMock;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Outils communs aux tests de démarrage à froid OIDC (AP004 T0.5 d). */
final class OidcColdStartSupport {

    static final String URL = ComptesAPIEnum.COMPTES_BASE + ComptesAPIEnum.COMPTES_LIST;

    private OidcColdStartSupport() { }

    static Map<String, String> overrides(LocalOidcServer server) {
        return Map.of(
                "quarkus.oidc.auth-server-url", server.baseUrl(),
                "quarkus.oidc.discovery-enabled", "true",
                // l'absence de clé publique locale force la discovery + le JWKS
                "quarkus.oidc.public-key", "",
                "quarkus.oidc.connection-timeout", "3S");
    }

    static void mockService() {
        ComptesService mock = mock(ComptesService.class);
        when(mock.getComptesUtilisateur(anyString())).thenReturn(Uni.createFrom().item(MockDataComptes.getListeComptes()));
        QuarkusMock.installMockForType(mock, ComptesService.class);
    }

    /** Compteurs d'appels du faux serveur OIDC (lus en HTTP : classloaders distincts entre profil et test). */
    static int hits(String key) {
        String base = ConfigProvider.getConfig().getValue("quarkus.oidc.auth-server-url", String.class);
        return given().when().get(base + "/_stats").then().statusCode(200).extract().path(key);
    }

    static int statusWithValidToken() {
        Map<String, Object> header = OidcTokenForge.rs256Header();
        header.put("kid", LocalOidcServer.KID);
        String token = OidcTokenForge.sign(header, OidcTokenForge.validClaims());
        return given().header(HttpHeaders.AUTHORIZATION, OidcTokenForge.bearer(token)).header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123")
                .when().get(URL).then().extract().statusCode();
    }
}
