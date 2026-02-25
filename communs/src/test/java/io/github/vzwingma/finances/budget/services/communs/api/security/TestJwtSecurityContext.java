package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtAuthHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de JwtSecurityContext
 */
class TestJwtSecurityContext {

    private JwtSecurityContext securityContext;

    @BeforeEach
    void setUp() {
        securityContext = new JwtSecurityContext();
    }

    private JWTAuthToken buildToken(String email) {
        JWTAuthPayload payload = new JWTAuthPayload();
        payload.setEmail(email);
        payload.setGiven_name("John");
        payload.setFamily_name("Doe");

        JwtAuthHeader header = new JwtAuthHeader();
        header.setAlg("RS256");
        header.setKid("kid123");
        header.setTyp("JWT");

        return new JWTAuthToken(header, payload, false, null);
    }

    @Test
    void testGetUserPrincipalSansToken() {
        securityContext.setJwtValidatedToken(null);
        assertNull(securityContext.getUserPrincipal());
    }

    @Test
    void testGetUserPrincipalAvecToken() {
        securityContext.setJwtValidatedToken(buildToken("user@test.com"));
        assertNotNull(securityContext.getUserPrincipal());
        assertEquals("user@test.com", securityContext.getUserPrincipal().getName());
    }

    @Test
    void testGetUserPrincipalAvecPayloadNull() {
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), null, false, null);
        securityContext.setJwtValidatedToken(token);
        assertNull(securityContext.getUserPrincipal());
    }

    @Test
    void testIsUserInRoleSansToken() {
        securityContext.setJwtValidatedToken(null);
        assertFalse(securityContext.isUserInRole("ADMIN"));
    }

    @Test
    void testIsUserInRoleAvecToken() {
        securityContext.setJwtValidatedToken(buildToken("user@test.com"));
        // L'implémentation retourne toujours true quand le token est présent
        assertTrue(securityContext.isUserInRole("ADMIN"));
    }

    @Test
    void testIsSecure() {
        assertTrue(securityContext.isSecure());
    }

    @Test
    void testGetAuthenticationSchemeSansToken() {
        securityContext.setJwtValidatedToken(null);
        assertNull(securityContext.getAuthenticationScheme());
    }

    @Test
    void testGetAuthenticationSchemeAvecToken() {
        securityContext.setJwtValidatedToken(buildToken("user@test.com"));
        assertNotNull(securityContext.getAuthenticationScheme());
    }

    @Test
    void testGetterSetterApiKey() {
        securityContext.setApiKey("my-api-key-123");
        assertEquals("my-api-key-123", securityContext.getApiKey());
    }

    @Test
    void testGetterSetterToken() {
        JWTAuthToken token = buildToken("user@test.com");
        securityContext.setJwtValidatedToken(token);
        assertEquals(token, securityContext.getJwtValidatedToken());
    }

    @Test
    void testIsUserInRoleSansTokenAvecPrincipalNull() {
        securityContext.setJwtValidatedToken(null);
        // getUserPrincipal() retourne null mais ne doit pas provoquer de NPE
        assertFalse(securityContext.isUserInRole("USER"));
    }
}
