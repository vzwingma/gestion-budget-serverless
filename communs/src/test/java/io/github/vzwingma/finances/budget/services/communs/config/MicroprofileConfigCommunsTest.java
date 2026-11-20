package io.github.vzwingma.finances.budget.services.communs.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * AP004 T2.2 (R4, R6) : contenu du fichier {@code META-INF/microprofile-config.properties} de communs, lu tel quel.
 * Garde-fou contre une clé « sans préfixe » qui désactiverait Dev Services MongoDB ou ferait appeler Google pendant les
 * tests, et contre la perte du masquage du jeton par {@code OidcProvider}.
 */
class MicroprofileConfigCommunsTest {

    private static Properties charger() throws IOException {
        Properties proprietes = new Properties();
        try (InputStream flux = MicroprofileConfigCommunsTest.class.getClassLoader().getResourceAsStream("META-INF/microprofile-config.properties")) {
            assertNotNull(flux, "META-INF/microprofile-config.properties introuvable dans communs");
            proprietes.load(new InputStreamReader(flux, StandardCharsets.UTF_8));
        }
        return proprietes;
    }

    @Test
    void cleSansEffetEnTest_toujoursPrefixeeDevProd() throws IOException {
        Properties p = charger();
        for (String cle : new String[]{"quarkus.mongodb.connection-string", "quarkus.oidc.auth-server-url",
                "quarkus.http.cors.origins", "quarkus.http.cors.access-control-allow-credentials"}) {
            assertNull(p.getProperty(cle), cle + " ne doit jamais être défini sans préfixe de profil (R4)");
            assertNotNull(p.getProperty("%dev,prod." + cle), "%dev,prod." + cle + " attendu");
        }
        assertEquals("https://accounts.google.com", p.getProperty("%dev,prod.quarkus.oidc.auth-server-url"));
    }

    @Test
    void masquageDuJetonParOidcProvider() throws IOException {
        assertEquals("ERROR", charger().getProperty("quarkus.log.category.\"io.quarkus.oidc.runtime.OidcProvider\".level"));
    }

    @Test
    void contournement55278_etTimeoutsMongo() throws IOException {
        Properties p = charger();
        assertEquals("false", p.getProperty("quarkus.rest.jackson.optimization.enable-reflection-free-serializers"));
        assertEquals("5S", p.getProperty("quarkus.mongodb.server-selection-timeout"));
        assertEquals("3S", p.getProperty("quarkus.mongodb.connect-timeout"));
        assertEquals("8S", p.getProperty("quarkus.mongodb.read-timeout"));
    }

    @Test
    void cleQuiDoitResterDansLesModules() throws IOException {
        Properties p = charger();
        for (String cle : new String[]{"quarkus.application.name", "quarkus.http.port", "quarkus.oidc.info-public.tenant-paths"}) {
            assertNull(p.getProperty(cle), cle + " est propre à chaque module");
        }
    }
}
