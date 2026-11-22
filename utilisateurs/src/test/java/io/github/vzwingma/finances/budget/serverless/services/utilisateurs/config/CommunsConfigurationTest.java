package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.config;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AP004 T2.2 (R3, R4, R6) : la configuration commune de {@code communs} ({@code META-INF/microprofile-config.properties})
 * est bien lue par le module utilisateurs en profil test, et la configuration de test reste prioritaire.
 * Lecture au runtime par {@link ConfigProvider} : garde-fou contre la disparition ou la dérive d'une clé lors
 * d'un déplacement de configuration.
 */
@QuarkusTest
class CommunsConfigurationTest {

    private static Config config() {
        return ConfigProvider.getConfig();
    }

    private static <T> T valeur(String cle, Class<T> type) {
        return config().getValue(cle, type);
    }

    @Test
    void mongo_timeoutsEtPool() {
        assertEquals(Duration.ofSeconds(5), valeur("quarkus.mongodb.server-selection-timeout", Duration.class));
        assertEquals(Duration.ofSeconds(3), valeur("quarkus.mongodb.connect-timeout", Duration.class));
        assertEquals(Duration.ofSeconds(8), valeur("quarkus.mongodb.read-timeout", Duration.class));
        assertEquals(10, valeur("quarkus.mongodb.max-pool-size", Integer.class));
        assertEquals(0, valeur("quarkus.mongodb.min-pool-size", Integer.class));
        assertEquals(Duration.ofSeconds(60), valeur("quarkus.mongodb.max-connection-idle-time", Duration.class));
    }

    @Test
    void oidc_cleCommunesEtCleDuModule() {
        assertEquals("service", valeur("quarkus.oidc.application-type", String.class));
        assertEquals("any", valeur("quarkus.oidc.token.issuer", String.class));
        assertEquals("email", valeur("quarkus.oidc.token.principal-claim", String.class));
        assertEquals(Duration.ofSeconds(3), valeur("quarkus.oidc.connection-timeout", Duration.class));
        assertEquals(Duration.ofSeconds(4), valeur("quarkus.oidc.connection-delay", Duration.class));
        assertFalse(valeur("quarkus.oidc.info-public.tenant-enabled", Boolean.class));
        assertEquals("/utilisateurs/v2/_info", valeur("quarkus.oidc.info-public.tenant-paths", String.class));
    }

    @Test
    void cors_profilTest() {
        assertTrue(valeur("quarkus.http.cors.enabled", Boolean.class));
        assertEquals("DELETE,GET,HEAD,OPTIONS,POST,PUT", valeur("quarkus.http.cors.methods", String.class));
        assertEquals("Content-Type,Authorization,X-Api-Key", valeur("quarkus.http.cors.headers", String.class));
        assertTrue(valeur("quarkus.http.enable-compression", Boolean.class));
    }

    @Test
    void contournement55278_serializersReflectionFreeDesactives() {
        assertFalse(valeur("quarkus.rest.jackson.optimization.enable-reflection-free-serializers", Boolean.class));
    }

    @Test
    void masquageDuJetonParOidcProvider_niveauError() {
        assertEquals("ERROR", valeur("quarkus.log.category.\"io.quarkus.oidc.runtime.OidcProvider\".level", String.class));
        // Niveau effectivement appliqué au logger (pas seulement présent dans la configuration)
        Logger oidcProvider = Logger.getLogger("io.quarkus.oidc.runtime.OidcProvider");
        assertEquals(Level.SEVERE.intValue(), oidcProvider.getLevel().intValue(), "OidcProvider doit rester en ERROR (le WARN contient le jeton complet)");
    }

    @Test
    void profilTest_laConfigurationDeTestResteProritaire() {
        // src/test/resources/application.properties surcharge les valeurs de communs et du module
        assertEquals("test", valeur("oidc.jwt.id.appusercontent", String.class));
        assertEquals("test.apps.googleusercontent.com", valeur("quarkus.oidc.client-id", String.class));
        assertTrue(config().getOptionalValue("quarkus.oidc.public-key", String.class).isPresent());
        // Préfixe %dev,prod. : rien ne doit s'appliquer aux tests (pas d'appel à Google ; Dev Services MongoDB reste actif)
        assertTrue(config().getOptionalValue("quarkus.oidc.auth-server-url", String.class).isEmpty());
        assertTrue(config().getOptionalValue("quarkus.http.cors.origins", String.class).isEmpty());
        assertTrue(config().getOptionalValue("quarkus.http.cors.access-control-allow-credentials", String.class).isEmpty());
    }
}
