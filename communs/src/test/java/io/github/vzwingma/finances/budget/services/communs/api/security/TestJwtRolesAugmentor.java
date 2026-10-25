package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests de {@link JwtRolesAugmentor} : issuer, email, email_verified, rôles.
 */
class TestJwtRolesAugmentor {

    private JwtRolesAugmentor augmentor;

    @BeforeEach
    void setUp() {
        augmentor = new JwtRolesAugmentor();
        augmentor.roles = Set.of("USER_COMPTES", "USER_OPERATIONS");
    }

    private JsonWebToken jwt(String issuer, Object email, Object emailVerified) {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getIssuer()).thenReturn(issuer);
        when(jwt.getClaim("email")).thenReturn(email);
        when(jwt.getClaim("email_verified")).thenReturn(emailVerified);
        return jwt;
    }

    private SecurityIdentity identity(JsonWebToken jwt) {
        return QuarkusSecurityIdentity.builder().setPrincipal(jwt).build();
    }

    private SecurityIdentity augment(SecurityIdentity identity) {
        return augmentor.augment(identity, null).await().indefinitely();
    }

    @Test
    void nominal_ajouteLesRoles() {
        for (String issuer : new String[]{"accounts.google.com", "https://accounts.google.com"}) {
            SecurityIdentity result = augment(identity(jwt(issuer, "a@b.com", true)));
            assertTrue(result.getRoles().containsAll(Set.of("USER_COMPTES", "USER_OPERATIONS")));
        }
    }

    @Test
    void emailVerifiedEnChaineTrue_accepte() {
        SecurityIdentity result = augment(identity(jwt("https://accounts.google.com", "a@b.com", "true")));
        assertTrue(result.getRoles().contains("USER_COMPTES"));
    }

    @Test
    void emailAbsentOuVide_refuse() {
        for (Object email : new Object[]{null, "", "   "}) {
            SecurityIdentity id = identity(jwt("https://accounts.google.com", email, true));
            assertThrows(AuthenticationFailedException.class, () -> augment(id));
        }
    }

    @Test
    void emailNonVerifie_refuse() {
        for (Object verified : new Object[]{false, "false", null, "TRUE", 1}) {
            SecurityIdentity id = identity(jwt("https://accounts.google.com", "a@b.com", verified));
            assertThrows(AuthenticationFailedException.class, () -> augment(id));
        }
    }

    @Test
    void issuerInvalide_refuse() {
        for (String issuer : new String[]{null, "evilaccounts.google.com", "accounts.google.com.evil.com", "https://evil.com"}) {
            SecurityIdentity id = identity(jwt(issuer, "a@b.com", true));
            assertThrows(AuthenticationFailedException.class, () -> augment(id));
        }
    }

    @Test
    void identiteAnonyme_inchangee() {
        SecurityIdentity anonymous = QuarkusSecurityIdentity.builder().setAnonymous(true).build();
        assertSame(anonymous, augment(anonymous));
    }
}
