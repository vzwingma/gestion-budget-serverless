package io.github.vzwingma.finances.budget.serverless.services.comptes.api;

import io.github.vzwingma.finances.budget.serverless.services.comptes.api.enums.ComptesAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ComptesService;
import io.github.vzwingma.finances.budget.serverless.services.comptes.test.data.MockDataComptes;
import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de sécurité e2e (AP004 T0.5) : chaîne réelle quarkus-oidc (clé publique de test) -> {@code JwtRolesAugmentor} -> {@code @RolesAllowed}.
 * Endpoint cible : {@code GET /comptes/v2/tous} ({@code @RolesAllowed USER_COMPTES}). Les jetons refusés doivent donner 401.
 * Le service métier est mocké (aucune base) ; seule la sécurité est réelle.
 */
@QuarkusTest
class OidcSecurityTest {

    private static final String URL = ComptesAPIEnum.COMPTES_BASE + ComptesAPIEnum.COMPTES_LIST;
    private static ComptesService comptesServiceMock;

    @BeforeAll
    static void init() {
        comptesServiceMock = mock(ComptesService.class);
        QuarkusMock.installMockForType(comptesServiceMock, ComptesService.class);
    }

    @BeforeEach
    void setup() {
        reset(comptesServiceMock);
        when(comptesServiceMock.getComptesUtilisateur(anyString())).thenReturn(Uni.createFrom().item(MockDataComptes.getListeComptes()));
    }

    private int statusWith(String authorization) {
        var req = given().header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "123");
        if (authorization != null) {
            req = req.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return req.when().get(URL).then().extract().statusCode();
    }

    private void assert401(String authorization, String message) {
        assertEquals(401, statusWith(authorization), message);
    }

    private static String bearerOf(Map<String, Object> claims) {
        return OidcTokenForge.bearer(OidcTokenForge.valid(claims));
    }

    // ---------- nominal e2e ----------

    @Test
    void jetonSigneValide_200_etIdentiteTransmise() {
        assertEquals(200, statusWith(bearerOf(OidcTokenForge.validClaims())),
                "chaîne réelle OIDC -> augmentor -> @RolesAllowed doit accepter un jeton valide");
        // L'email (principal-claim) doit arriver jusqu'au service métier
        verify(comptesServiceMock).getComptesUtilisateur(eq(OidcTokenForge.VALID_EMAIL));
    }

    @ParameterizedTest
    @ValueSource(strings = {"accounts.google.com", "https://accounts.google.com"})
    void issuerGoogleDesDeuxFormes_200(String iss) {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("iss", iss);
        assertEquals(200, statusWith(bearerOf(claims)), "iss=" + iss);
    }

    @Test
    void emailVerifiedEnChaineTrue_200_typeReelOidc() {
        // Type réel renvoyé par getClaim("email_verified") quand le jeton porte la chaîne "true"
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("email_verified", "true");
        assertEquals(200, statusWith(bearerOf(claims)));
    }

    // ---------- signature ----------

    @Test
    void jetonNonSigne_401() {
        assert401(OidcTokenForge.bearer(OidcTokenForge.unsigned(OidcTokenForge.rs256Header(), OidcTokenForge.validClaims())), "RS256 sans signature");
    }

    @ParameterizedTest
    @ValueSource(strings = {"none", "None", "NONE", "nOnE"})
    void algNone_401(String alg) {
        Map<String, Object> header = OidcTokenForge.rs256Header();
        header.put("alg", alg);
        String sansSignature = OidcTokenForge.unsigned(header, OidcTokenForge.validClaims());
        assert401(OidcTokenForge.bearer(sansSignature), "alg=" + alg + " sans signature");
        assert401(OidcTokenForge.bearer(sansSignature + "c2lnbmF0dXJl"), "alg=" + alg + " avec fausse signature");
    }

    @Test
    void algHs256ConfusionDeCle_401() {
        // Attaque de confusion : HS256 au lieu de RS256
        Map<String, Object> header = OidcTokenForge.rs256Header();
        header.put("alg", "HS256");
        String unsigned = OidcTokenForge.b64Json(header) + "." + OidcTokenForge.b64Json(OidcTokenForge.validClaims());
        assert401(OidcTokenForge.bearer(unsigned + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32])), "HS256");
    }

    @Test
    void signatureAutreCle_401() {
        String token = OidcTokenForge.sign(OidcTokenForge.rs256Header(), OidcTokenForge.validClaims(), OidcTokenForge.otherPrivateKey());
        assert401(OidcTokenForge.bearer(token), "signé par une autre clé");
    }

    @Test
    void payloadModifie_signatureConservee_401() {
        String token = OidcTokenForge.valid(OidcTokenForge.validClaims());
        Map<String, Object> forged = OidcTokenForge.validClaims();
        forged.put("email", "victime@world.com");
        assert401(OidcTokenForge.bearer(OidcTokenForge.withReplacedPayload(token, forged)), "payload modifié");
    }

    @Test
    void signatureTronquee_401() {
        String token = OidcTokenForge.valid(OidcTokenForge.validClaims());
        assert401(OidcTokenForge.bearer(token.substring(0, token.length() - 10)), "signature tronquée");
    }

    @Test
    void kidInconnu_avecClePubliqueLocale_documente() {
        // Avec quarkus.oidc.public-key (clé unique, pas de JWKS), le kid de l'en-tête ne sélectionne pas la clé :
        // un jeton correctement signé avec un kid inconnu est accepté. Le refus d'un kid inconnu n'a de sens qu'avec JWKS réel (à vérifier en QUA).
        Map<String, Object> header = OidcTokenForge.rs256Header();
        header.put("kid", "kid-inconnu");
        assertEquals(200, statusWith(OidcTokenForge.bearer(OidcTokenForge.sign(header, OidcTokenForge.validClaims()))),
                "comportement constaté avec public-key : kid ignoré");
    }

    // ---------- issuer ----------

    @ParameterizedTest
    @ValueSource(strings = {"evilaccounts.google.com", "accounts.google.com.evil.com", "https://evil.com", "http://accounts.google.com",
            "https://accounts.google.com/", "https://accounts.google.com.evil.com", "ACCOUNTS.GOOGLE.COM", ""})
    void issuerInvalide_401(String iss) {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("iss", iss);
        assert401(bearerOf(claims), "iss=" + iss);
    }

    @Test
    void issuerAbsent_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.remove("iss");
        assert401(bearerOf(claims), "iss absent");
    }

    // ---------- exp / aud ----------

    @Test
    void expDepasse_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("exp", JwtTestSupport.epochSecondsIn(Duration.ofHours(-1)));
        assert401(bearerOf(claims), "exp -1h");
    }

    @Test
    void expDepasseDe2Minutes_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("exp", JwtTestSupport.epochSecondsIn(Duration.ofMinutes(-2)));
        assert401(bearerOf(claims), "exp -2 min");
    }

    @Test
    void expAbsent_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.remove("exp");
        assert401(bearerOf(claims), "exp absent");
    }

    @Test
    void audInconnu_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("aud", "autre-app.apps.googleusercontent.com");
        assert401(bearerOf(claims), "aud inconnu");
    }

    @Test
    void audAbsent_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.remove("aud");
        assert401(bearerOf(claims), "aud absent");
    }

    // ---------- en-tête Authorization ----------

    static Stream<String> authorizationsMalFormes() {
        return Stream.of("Bearer null", "Bearer undefined", "Bearer ", "Bearer", "Bearer a.b", "Bearer a.b.c", "Bearer abc",
                "Basic dXNlcjpwYXNz", "Token xyz", "", "   ", "Bearer Bearer");
    }

    @ParameterizedTest
    @MethodSource("authorizationsMalFormes")
    void authorizationMalForme_401(String authorization) {
        assert401(authorization, "Authorization=[" + authorization + "]");
    }

    @Test
    void authorizationAbsent_401() {
        assert401(null, "sans en-tête Authorization");
    }

    @Test
    void jetonSansPrefixeBearer_401() {
        assert401(OidcTokenForge.valid(OidcTokenForge.validClaims()), "jeton valide sans préfixe Bearer");
    }

    // ---------- email / email_verified ----------

    @Test
    void emailAbsent_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.remove("email");
        assert401(bearerOf(claims), "email absent");
    }

    @Test
    void emailBlanc_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("email", "  ");
        assert401(bearerOf(claims), "email blanc");
    }

    @Test
    void emailVerifiedFalse_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("email_verified", false);
        assert401(bearerOf(claims), "email_verified=false");
    }

    @Test
    void emailVerifiedAbsent_401() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.remove("email_verified");
        assert401(bearerOf(claims), "email_verified absent");
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "TRUE", "1", "yes"})
    void emailVerifiedChaineNonTrue_401(String value) {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("email_verified", value);
        assert401(bearerOf(claims), "email_verified=" + value);
    }

    // ---------- logs : aucun jeton / clé API dans les logs ----------

    /** Envoie le jeton avec une clé API secrète et renvoie tout ce qui a été loggé pendant l'appel. */
    private String logsPendantAppel(String token, String apiKey) {
        List<String> messages = Collections.synchronizedList(new ArrayList<>());
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord r) {
                messages.add(r.getMessage() + " " + (r.getParameters() != null ? Arrays.toString(r.getParameters()) : "")
                        + (r.getThrown() != null ? " " + r.getThrown() : ""));
            }

            @Override
            public void flush() {
                // rien à vider
            }

            @Override
            public void close() {
                // rien à fermer
            }
        };
        java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
        root.addHandler(handler);
        try {
            given().header(HttpHeaders.AUTHORIZATION, OidcTokenForge.bearer(token)).header(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, apiKey)
                    .when().get(URL).then().extract().statusCode();
        } finally {
            root.removeHandler(handler);
        }
        assertFalse(messages.isEmpty(), "des logs doivent avoir été capturés (sinon le test ne prouve rien)");
        return String.join("\n", messages);
    }

    private static void assertSansFuite(String logs, String token, String apiKey) {
        String[] parts = token.split("[.]");
        assertFalse(logs.contains(token), "jeton complet présent dans les logs");
        assertFalse(logs.contains(parts[1]), "payload du jeton présent dans les logs");
        if (parts.length > 2) {
            assertFalse(logs.contains(parts[2]), "signature du jeton présente dans les logs");
        }
        assertFalse(logs.contains(apiKey), "clé API présente dans les logs");
    }

    @Test
    void logs_jetonValide_aucuneFuite() {
        String token = OidcTokenForge.valid(OidcTokenForge.validClaims());
        assertSansFuite(logsPendantAppel(token, "SECRET-API-KEY-987"), token, "SECRET-API-KEY-987");
    }

    @Test
    void logs_issuerInvalide_aucuneFuite() {
        Map<String, Object> bad = OidcTokenForge.validClaims();
        bad.put("iss", "https://evil.com");
        String token = OidcTokenForge.valid(bad);
        assertSansFuite(logsPendantAppel(token, "SECRET-API-KEY-987"), token, "SECRET-API-KEY-987");
    }

    @Test
    void logs_expDepasse_aucuneFuite() {
        Map<String, Object> claims = OidcTokenForge.validClaims();
        claims.put("exp", JwtTestSupport.epochSecondsIn(Duration.ofHours(-1)));
        String token = OidcTokenForge.valid(claims);
        assertSansFuite(logsPendantAppel(token, "SECRET-API-KEY-987"), token, "SECRET-API-KEY-987");
    }

    @Test
    void logs_cleApi_jamaisLogguee_surSignatureInvalide() {
        String token = OidcTokenForge.sign(OidcTokenForge.rs256Header(), OidcTokenForge.validClaims(), OidcTokenForge.otherPrivateKey());
        assertFalse(logsPendantAppel(token, "SECRET-API-KEY-987").contains("SECRET-API-KEY-987"), "clé API présente dans les logs");
    }

    @Test
    @Disabled("BUG AP004 T0.5 — quarkus-oidc (io.quarkus.oidc.runtime.OidcProvider:294, WARN) logue le jeton COMPLET (en-tête, payload, signature) "
            + "quand la signature est invalide (message jose4j 'Invalid JWS Signature: JsonWebSignature{...}->jeton'). "
            + "Correctif attendu (DEVon) : relever le niveau de la catégorie io.quarkus.oidc.runtime.OidcProvider (ex. ERROR) dans les application.properties, "
            + "ou filtrer. Retirer ce @Disabled une fois corrigé.")
    void logs_signatureInvalide_aucuneFuite() {
        String token = OidcTokenForge.sign(OidcTokenForge.rs256Header(), OidcTokenForge.validClaims(), OidcTokenForge.otherPrivateKey());
        assertSansFuite(logsPendantAppel(token, "SECRET-API-KEY-987"), token, "SECRET-API-KEY-987");
    }
}
