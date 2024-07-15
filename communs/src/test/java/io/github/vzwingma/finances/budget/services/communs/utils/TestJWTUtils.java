package io.github.vzwingma.finances.budget.services.communs.utils;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtValidationParams;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.github.vzwingma.finances.budget.services.communs.utils.security.JWTUtils;
import io.vertx.core.json.DecodeException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de test JWT
 */
class TestJWTUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TestJWTUtils.class);
    private static final String ID_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjhlMGFjZjg5MWUwOTAwOTFlZjFhNWU3ZTY0YmFiMjgwZmQxNDQ3ZmEiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI1NTA0MzE5MjgxMzgtZWRlc3RqMjhyazVhMGVtazU0NnA3aWkyOGRsNWJvYzUuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI1NTA0MzE5MjgxMzgtZWRlc3RqMjhyazVhMGVtazU0NnA3aWkyOGRsNWJvYzUuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDAxMDI1MjcyMjA5NTAwNzY2ODgiLCJlbWFpbCI6InZpbmNlbnQuendpbmdtYW5uQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhdF9oYXNoIjoiSzZBNjRfUlJyMm5KbVk1YWNBanVjdyIsIm5hbWUiOiJWaW5jZW50IFp3aW5nbWFubiIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BRWRGVHA0VjVITGx1dktDNWdJYW9GRFU4a1Q0emJmSk94dE5lRmNYTjM4NnA1bz1zOTYtYyIsImdpdmVuX25hbWUiOiJWaW5jZW50IiwiZmFtaWx5X25hbWUiOiJad2luZ21hbm4iLCJsb2NhbGUiOiJmciIsImlhdCI6MTY3MjY2MDAwMiwiZXhwIjoxNjcyNjYzNjAyfQ.afhBX1myxxHsqqhAB8aksbBQo0Si6v141rAIC-RGNE6zjoJXIkJsN9dPpHLjP9VXJzFNIZLa8O01qwLBZj6qF4vFOqHgrVKVIwGL0UNpbvdf8yfHd401EexFpxn1UwUccC2DnDANxA3s4DZXNAVKIraMBPC5AtKDmbguGdh5Gh1s4mQtPNPy_f9hxhAumOiHAwrAzxdFsrsQR003WNbnWNCknvu87vp3ZUWO5yMvPVtAo0_Eyyg4HoZMX6XeRs6vKf6OY4NLAOkH0z8BgCqgQ2yV68RROPQ2Ic3icbAQANa3GxD5cqQ5YTJQ_hcsIt1y2XD9r9rGETBk3nzEH9tnCQ";

    private static final String ID_TOKEN_2 = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjhlMGFjZjg5MWUwOTAwOTFlZjFhNWU3ZTY0YmFiMjgwZmQxNDQ3ZmEiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI1NTA0MzE5MjgxMzgtZWRlc3RqMjhyazVhMGVtazU0NnA3aWkyOGRsNWJvYzUuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI1NTA0MzE5MjgxMzgtZWRlc3RqMjhyazVhMGVtazU0NnA3aWkyOGRsNWJvYzUuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDAxMDI1MjcyMjA5NTAwNzY2ODgiLCJlbWFpbCI6InZpbmNlbnQuendpbmdtYW5uQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhdF9oYXNoIjoiZDUtbUJGU1RUaUNEaS1lYlFpZ3pyUSIsIm5hbWUiOiJWaW5jZW50IFp3aW5nbWFubiIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BRWRGVHA0VjVITGx1dktDNWdJYW9GRFU4a1Q0emJmSk94dE5lRmNYTjM4NnA1bz1zOTYtYyIsImdpdmVuX25hbWUiOiJWaW5jZW50IiwiZmFtaWx5X25hbWUiOiJad2luZ21hbm4iLCJsb2NhbGUiOiJmciIsImlhdCI6MTY3Mjk1NzE1MSwiZXhwIjoxNjcyOTYwNzUxfQ.qEVb34k3vQsKG0cJ7rYxEC8tlm_T4oOpu3hav4jTNK4R1Sp1yNljlpIgjP34PhaJf2Zzxn9Om1pn2la1gzpTqdEQpT-f9xHKOhEKf2J9GK72LeLYXdAVS-MfyigY1Vq91oUiCVNg58w4oqRC2kiobKaxrYakMhLdgte4iWTo1qP0PnaqiT_x9rh7pPu7qs_gq1ervT-qQpG504mO31CaMV8NxldBWOyRbFIy5_zUiKH0mcZ2GfCPKSeP3UpN_YBxzkBwd9CmhTawg3dBXgMwOpogtfeL7cn9DHevMNEsfX59ZdFN4rGkMcDAYNve7pGjDr3QPa0TobzFKd7HpKql7w";


    private static final String ID_TOKEN_BAD_ISS = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjhlMGFjZjg5MWUwOTAwOTFlZjFhNWU3ZTY0YmFiMjgwZmQxNDQ3ZmEiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL05PVC5nb29nbGUuY29tIiwiYXpwIjoiNTUwNDMxOTI4MTM4LWVkZXN0ajI4cms1YTBlbWs1NDZwN2lpMjhkbDVib2M1LmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwiYXVkIjoiNTUwNDMxOTI4MTM4LWVkZXN0ajI4cms1YTBlbWs1NDZwN2lpMjhkbDVib2M1LmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwic3ViIjoiMTAwMTAyNTI3MjIwOTUwMDc2Njg4IiwiZW1haWwiOiJ2aW5jZW50Lnp3aW5nbWFubkBnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiYXRfaGFzaCI6Iks2QTY0X1JScjJuSm1ZNWFjQWp1Y3ciLCJuYW1lIjoiVmluY2VudCBad2luZ21hbm4iLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUVkRlRwNFY1SExsdXZLQzVnSWFvRkRVOGtUNHpiZkpPeHROZUZjWE4zODZwNW89czk2LWMiLCJnaXZlbl9uYW1lIjoiVmluY2VudCIsImZhbWlseV9uYW1lIjoiWndpbmdtYW5uIiwibG9jYWxlIjoiZnIiLCJpYXQiOjE2NzI2NjAwMDIsImV4cCI6MTY3MjY2MzYwMn0=";

    private static final String ID_TOKEN_BAD_APP = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjhlMGFjZjg5MWUwOTAwOTFlZjFhNWU3ZTY0YmFiMjgwZmQxNDQ3ZmEiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI1NTA0MzE5MjgxMzgtZWRlc3RqMjhyazVhMGVtazU0NnA3aWkyOGRsNWJvYzUuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiJlZGVzdGoyOHJrNWEwZW1rNTQ2cDdpaTI4ZGw1Ym9jNS5hcHBzLk5PVC5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDAxMDI1MjcyMjA5NTAwNzY2ODgiLCJlbWFpbCI6InZpbmNlbnQuendpbmdtYW5uQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhdF9oYXNoIjoiSzZBNjRfUlJyMm5KbVk1YWNBanVjdyIsIm5hbWUiOiJWaW5jZW50IFp3aW5nbWFubiIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BRWRGVHA0VjVITGx1dktDNWdJYW9GRFU4a1Q0emJmSk94dE5lRmNYTjM4NnA1bz1zOTYtYyIsImdpdmVuX25hbWUiOiJWaW5jZW50IiwiZmFtaWx5X25hbWUiOiJad2luZ21hbm4iLCJsb2NhbGUiOiJmciIsImlhdCI6MTY3MjY2MDAwMiwiZXhwIjoxNjcyNjYzNjAyfQ";



    static String generateValidToken() {
        JWTAuthToken token = JWTUtils.decodeJWT(ID_TOKEN);

        token.getPayload().setIat(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now()));
        token.getPayload().setExp(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now().plusHours(1)));
        return JWTUtils.encodeJWT(token);
    }

    static JwtValidationParams generateValidParams(){
        JwtValidationParams params = new JwtValidationParams();
        params.setIdAppUserContent(JWTUtils.decodeJWT(ID_TOKEN).getPayload().getAud().replace(".apps.googleusercontent.com", ""));
        return params;
    }

    @Test
    void testDecode() {

        JWTAuthToken token = JWTUtils.decodeJWT(ID_TOKEN);
        assertNotNull(token);
        assertNotNull(token.getHeader());
        assertEquals("RS256", token.getHeader().getAlg());

        assertNotNull(token.getPayload());
        assertEquals("https://accounts.google.com", token.getPayload().getIss());
        assertNotNull(token.issuedAt());
        assertEquals("2023-01-02T13:46:42", token.issuedAt().toString());
        assertNotNull(token.expiredAt());
        assertEquals("2023-01-02T14:46:42", token.expiredAt().toString());

        LOG.info(LocalDateTime.now().toString());
        LOG.info(token.expiredAt().toString());
        assertFalse(token.isValid(null));

        JWTUtils.encodeJWT(token);

    }

    @Test
    void testDecodeToken2() {

        JWTAuthToken token = JWTUtils.decodeJWT(ID_TOKEN_2);
        assertNotNull(token);
        assertNotNull(token.getHeader());
        assertEquals("RS256", token.getHeader().getAlg());

        assertNotNull(token.getPayload());
    }

    @Test
    void testDecodeBadToken() {

        assertThrows(DecodeException.class, () -> JWTUtils.decodeJWT("BaDToken" + ID_TOKEN));
    }

    @Test
    void testEncode() {
        JWTAuthToken token = JWTUtils.decodeJWT(ID_TOKEN);
        String encode = JWTUtils.encodeJWT(token);
        assertNotNull(encode);
        assertEquals("eyJhbGciOiJSUzI1NiIsImtpZCI6IjhlMGFjZjg5MWUwOTAwOTFlZjFhNWU3ZTY0YmFiMjgwZmQxNDQ3ZmEiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI1NTA0MzE5MjgxMzgtZWRlc3RqMjhyazVhMGVtazU0NnA3aWkyOGRsNWJvYzUuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI1NTA0MzE5MjgxMzgtZWRlc3RqMjhyazVhMGVtazU0NnA3aWkyOGRsNWJvYzUuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDAxMDI1MjcyMjA5NTAwNzY2ODgiLCJlbWFpbCI6InZpbmNlbnQuendpbmdtYW5uQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhdF9oYXNoIjoiSzZBNjRfUlJyMm5KbVk1YWNBanVjdyIsIm5hbWUiOiJWaW5jZW50IFp3aW5nbWFubiIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BRWRGVHA0VjVITGx1dktDNWdJYW9GRFU4a1Q0emJmSk94dE5lRmNYTjM4NnA1bz1zOTYtYyIsImdpdmVuX25hbWUiOiJWaW5jZW50IiwiZmFtaWx5X25hbWUiOiJad2luZ21hbm4iLCJsb2NhbGUiOiJmciIsImlhdCI6MTY3MjY2MDAwMiwiZXhwIjoxNjcyNjYzNjAyfQ", encode);
    }


    @Test
    void testInvalidAppTokenApp() {
        String rawToken = ID_TOKEN_BAD_APP;
        assertNotNull(rawToken);
        JWTAuthToken token = JWTUtils.decodeJWT(rawToken);
        assertFalse(token.isValid(generateValidParams()));
    }


    @Test
    void testInvalidAppTokenIss() {
        String rawToken = ID_TOKEN_BAD_ISS;
        assertNotNull(rawToken);
        JWTAuthToken token = JWTUtils.decodeJWT(rawToken);

        assertFalse(token.isValid(generateValidParams()));
    }


    @Test
    void testValidToken() {
        String rawToken = generateValidToken();
        assertNotNull(rawToken);
        JWTAuthToken token = JWTUtils.decodeJWT(rawToken);

        assertTrue(token.isValid(generateValidParams()));
    }

}
