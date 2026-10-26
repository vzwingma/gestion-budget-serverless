package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AP004 T0.5 — {@link JwtRolesAugmentor} face aux claims typés {@code jakarta.json} ({@link JsonValue}, {@code JsonString}) :
 * selon l'implémentation MP JWT, {@code getClaim} peut renvoyer ces types plutôt que {@code Boolean} / {@code String}.
 * <p>
 * Constat : le test e2e du module comptes ({@code OidcSecurityTest}) prouve que, avec {@code quarkus-oidc} 3.40, {@code getClaim}
 * renvoie bien {@code Boolean} / {@code String} (jetons acceptés, 200). L'augmentor est durci pour accepter aussi les formes
 * {@code JsonValue.TRUE} / {@code JsonString "true"} et refuser {@code email} nul, {@code JsonValue.NULL}, vide ou blanc.
 */
class TestJwtRolesAugmentorJsonValue {

    private JwtRolesAugmentor augmentor;

    @BeforeEach
    void setUp() {
        augmentor = new JwtRolesAugmentor();
        augmentor.roles = Set.of("USER_COMPTES");
    }

    private SecurityIdentity augment(Object email, Object emailVerified) {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getIssuer()).thenReturn("https://accounts.google.com");
        when(jwt.getClaim("email")).thenReturn(email);
        when(jwt.getClaim("email_verified")).thenReturn(emailVerified);
        SecurityIdentity identity = QuarkusSecurityIdentity.builder().setPrincipal(jwt).build();
        return augmentor.augment(identity, null).await().indefinitely();
    }

    @Test
    void emailJsonString_accepte() {
        SecurityIdentity result = augment(Json.createValue("a@b.com"), true);
        assertTrue(result.getRoles().contains("USER_COMPTES"));
    }

    @Test
    void emailVerifiedBooleanEtEmailChaine_accepte() {
        assertTrue(augment("a@b.com", Boolean.TRUE).getRoles().contains("USER_COMPTES"));
    }

    @Test
    void emailVerifiedJsonValueTrue_accepte() {
        assertTrue(augment("a@b.com", JsonValue.TRUE).getRoles().contains("USER_COMPTES"));
    }

    @Test
    void emailVerifiedJsonStringTrue_accepte() {
        assertTrue(augment("a@b.com", Json.createValue("true")).getRoles().contains("USER_COMPTES"));
    }

    @Test
    void emailVerifiedChaineTrue_accepte() {
        assertTrue(augment("a@b.com", "true").getRoles().contains("USER_COMPTES"));
    }

    @Test
    void emailVerifiedJsonValueFalse_refuse() {
        assertThrows(AuthenticationFailedException.class, () -> augment("a@b.com", JsonValue.FALSE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "TRUE", "True", "1", "yes", "", " true"})
    void emailVerifiedChaineNonTrue_refusee(String value) {
        assertThrows(AuthenticationFailedException.class, () -> augment("a@b.com", value), "String " + value);
        assertThrows(AuthenticationFailedException.class, () -> augment("a@b.com", Json.createValue(value)), "JsonString " + value);
    }

    @Test
    void emailVerifiedAutresTypes_refuse() {
        assertThrows(AuthenticationFailedException.class, () -> augment("a@b.com", null));
        assertThrows(AuthenticationFailedException.class, () -> augment("a@b.com", JsonValue.NULL));
        assertThrows(AuthenticationFailedException.class, () -> augment("a@b.com", Boolean.FALSE));
        assertThrows(AuthenticationFailedException.class, () -> augment("a@b.com", 1));
    }

    @Test
    void emailNullOuJsonNull_refuse() {
        assertThrows(AuthenticationFailedException.class, () -> augment(null, true));
        assertThrows(AuthenticationFailedException.class, () -> augment(JsonValue.NULL, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "	"})
    void emailVideOuBlanc_refuse(String value) {
        assertThrows(AuthenticationFailedException.class, () -> augment(value, true), "String [" + value + "]");
        assertThrows(AuthenticationFailedException.class, () -> augment(Json.createValue(value), true), "JsonString [" + value + "]");
    }
}
