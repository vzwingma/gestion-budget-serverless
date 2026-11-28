package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de JwtSecurityContext (contexte mince alimenté par la SecurityIdentity, AP004 T4.0-b)
 */
class TestJwtSecurityContext {

    private JwtSecurityContext securityContext;

    @BeforeEach
    void setUp() {
        securityContext = new JwtSecurityContext();
    }

    private static SecurityIdentity identity(String email, String rawToken) {
        Principal principal = () -> email;
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder().setPrincipal(principal);
        if (rawToken != null) {
            builder.addCredential(new AccessTokenCredential(rawToken));
        }
        return builder.build();
    }

    @Test
    void testGetUserPrincipalSansIdentite() {
        securityContext.setIdentity(null);
        assertNull(securityContext.getUserPrincipal());
        assertNull(securityContext.getRawToken());
    }

    @Test
    void testGetUserPrincipalIdentiteAnonyme() {
        securityContext.setIdentity(QuarkusSecurityIdentity.builder().setAnonymous(true).build());
        assertNull(securityContext.getUserPrincipal());
        assertNull(securityContext.getRawToken());
    }

    @Test
    void testGetUserPrincipalAvecIdentite() {
        securityContext.setIdentity(identity("user@test.com", "raw.jwt.token"));
        assertNotNull(securityContext.getUserPrincipal());
        assertEquals("user@test.com", securityContext.getUserPrincipal().getName());
        assertEquals("raw.jwt.token", securityContext.getRawToken());
    }

    @Test
    void testIdentiteSansCredentialJeton() {
        securityContext.setIdentity(identity("user@test.com", null));
        assertEquals("user@test.com", securityContext.getUserPrincipal().getName());
        assertNull(securityContext.getRawToken());
    }

    @Test
    void testIdentiteAnonymeEffaceLIdentitePrecedente() {
        securityContext.setIdentity(identity("user@test.com", "t"));
        securityContext.setIdentity(null);
        assertNull(securityContext.getUserPrincipal());
        assertNull(securityContext.getRawToken());
    }

    @Test
    void testGetterSetterApiKey() {
        securityContext.setApiKey("my-api-key-123");
        assertEquals("my-api-key-123", securityContext.getApiKey());
    }

    @Test
    void testContexteNeSurchargePasLeSecurityContextJaxRs() {
        // J1 : plus de isUserInRole toujours vrai ; rôles et authentification relèvent de SecurityIdentity / @RolesAllowed
        assertFalse(SecurityContext.class.isAssignableFrom(IJwtSecurityContext.class));
        assertFalse(SecurityContext.class.isAssignableFrom(JwtSecurityContext.class));
    }
}
