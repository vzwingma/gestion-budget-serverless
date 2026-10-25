package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
 * renvoie bien {@code Boolean} / {@code String} (jetons acceptés, 200). Le cas {@code JsonValue.TRUE} reste théorique mais est refusé
 * par le code actuel : test désactivé et signalé (voir {@code @Disabled}).
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
    @Disabled("BUG AP004 T0.5 (non bloquant en pratique, à arbitrer par DEVon) — JwtRolesAugmentor.isEmailVerified refuse JsonValue.TRUE "
            + "(n'accepte que Boolean.TRUE ou CharSequence 'true'). Le type réel renvoyé par quarkus-oidc est Boolean (prouvé par l'e2e comptes), "
            + "donc aucun impact constaté. Correctif suggéré : accepter aussi JsonValue.TRUE (ou comparer JsonValue.ValueType.TRUE).")
    void emailVerifiedJsonValueTrue_devraitEtreAccepte() {
        SecurityIdentity result = augment("a@b.com", JsonValue.TRUE);
        assertTrue(result.getRoles().contains("USER_COMPTES"));
    }

    @Test
    @Disabled("BUG AP004 T0.5 (même cause que ci-dessus) — JsonString 'true' n'est pas un CharSequence : refusé alors que la valeur sémantique est true.")
    void emailVerifiedJsonStringTrue_devraitEtreAccepte() {
        SecurityIdentity result = augment("a@b.com", Json.createValue("true"));
        assertTrue(result.getRoles().contains("USER_COMPTES"));
    }

    @Test
    void emailVerifiedJsonValueTrue_etJsonString_documentation_refusActuel() {
        // Caractérise le comportement actuel (fail-closed) : refus, sans exception autre que AuthenticationFailedException
        assertThrows(AuthenticationFailedException.class, () -> augment("a@b.com", JsonValue.TRUE));
        assertThrows(AuthenticationFailedException.class, () -> augment("a@b.com", Json.createValue("true")));
    }

    @Test
    void emailVerifiedJsonValueFalse_refuse() {
        assertThrows(AuthenticationFailedException.class, () -> augment("a@b.com", JsonValue.FALSE));
        assertThrows(AuthenticationFailedException.class, () -> augment("a@b.com", Json.createValue("false")));
    }

    @Test
    @Disabled("BUG AP004 T0.5 (théorique, même famille) — JwtRolesAugmentor teste email.toString().isBlank() : pour un JsonValue.NULL, toString() = 'null' "
            + "et pour un JsonString vide/blanc toString() = '\"   \"' (guillemets inclus) => jamais blanc => email vide ACCEPTÉ. "
            + "Aucun impact constaté : le type réel d'email est String (e2e comptes : email vide/blanc => 401). Correctif suggéré : extraire la valeur "
            + "(JsonString#getString) ou n'accepter que String.")
    void emailJsonValueNullOuBlanc_devraitEtreRefuse() {
        assertThrows(AuthenticationFailedException.class, () -> augment(JsonValue.NULL, true));
        assertThrows(AuthenticationFailedException.class, () -> augment(Json.createValue("   "), true));
    }
}
