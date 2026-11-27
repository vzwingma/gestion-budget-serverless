package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.config;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AP004 T2.3 : l'unique {@code application.properties} du module utilisateurs (profils Quarkus {@code %dev.} / {@code %prod.})
 * combiné à la configuration commune de communs donne, pour chaque profil, les valeurs des anciens fichiers
 * {@code dev/} et {@code prod/} (anciens profils Maven de ressources).
 * <p>
 * Test sans démarrage de Quarkus : la configuration est construite avec SmallRye Config (même mécanisme de profils que
 * Quarkus) à partir du seul {@code application.properties} de {@code target/classes} (pas celui des tests) et de
 * {@code META-INF/microprofile-config.properties}. Valeurs brutes : indépendantes des variables d'environnement.
 */
class ProfilsConfigurationTest {

    private static SmallRyeConfig config(String profil) throws IOException {
        ClassLoader cl = ProfilsConfigurationTest.class.getClassLoader();
        URL applicationDuModule = null;
        for (URL url : Collections.list(cl.getResources("application.properties"))) {
            if (!url.toString().contains("test-classes")) {
                applicationDuModule = url;
            }
        }
        assertNotNull(applicationDuModule, "application.properties du module introuvable dans target/classes");
        URL communs = cl.getResource("META-INF/microprofile-config.properties");
        assertNotNull(communs, "microprofile-config.properties de communs introuvable");
        return new SmallRyeConfigBuilder().addDefaultInterceptors().withProfile(profil)
                .withSources(new PropertiesConfigSource(applicationDuModule, 250), new PropertiesConfigSource(communs, 100))
                .build();
    }

    private static String brut(SmallRyeConfig config, String cle) {
        return config.getConfigValue(cle).getRawValue();
    }

    @Test
    void profilProd_valeursDeLAncienProfilNative() throws IOException {
        SmallRyeConfig c = config("prod");
        assertEquals("${QUARKUS_MONGODB_CONNECTION_STRING:mongodb://localhost:27017}", brut(c, "quarkus.mongodb.connection-string"));
        assertEquals("${QUARKUS_MONGODB_DATABASE:database}", brut(c, "quarkus.mongodb.database"));
        assertEquals("true", brut(c, "quarkus.mongodb.metrics.enabled"));
        assertEquals("API Utilisateurs", brut(c, "quarkus.application.name"));
        assertEquals("https://accounts.google.com", brut(c, "quarkus.oidc.auth-server-url"));
        assertEquals("${APP_CONFIG_URL_IHM:http://localhost:3000}", brut(c, "quarkus.http.cors.origins"));
        assertEquals("Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token", brut(c, "quarkus.http.cors.headers"));
        assertEquals("true", brut(c, "quarkus.http.cors.access-control-allow-credentials"));
        assertEquals("${QUARKUS_LOG_LEVEL:INFO}", brut(c, "quarkus.log.level"));
        assertEquals("${MONGODB_LOG_LEVEL:WARN}", brut(c, "quarkus.log.category.\"org.mongodb.driver\".level"));
        assertEquals("${QUARKUS_LOG_LEVEL:INFO}", brut(c, "quarkus.log.category.\"io.github.vzwingma.finances.budget.serverless.services.utilisateurs\".level"));
        assertFalse(brut(c, "quarkus.log.console.format").contains("%L"), "pas de numéro de ligne en production");
        assertEquals("ERROR", brut(c, "quarkus.log.category.\"io.quarkus.oidc.runtime.OidcProvider\".level"));
        assertEquals("/utilisateurs/v2/_info", brut(c, "quarkus.oidc.info-public.tenant-paths"));
        assertNull(brut(c, "quarkus.http.port"), "pas de port fixe en production");
        assertNull(brut(c, "quarkus.lambda.mock-event-server.dev-port"));
    }

    @Test
    void profilDev_valeursDeLAncienProfilDev() throws IOException {
        SmallRyeConfig c = config("dev");
        assertEquals("${QUARKUS_MONGODB_CONNECTION_STRING:mongodb://localhost:27017}", brut(c, "quarkus.mongodb.connection-string"));
        assertEquals("${QUARKUS_MONGODB_DATABASE:v12-app-dev}", brut(c, "quarkus.mongodb.database"));
        assertNull(brut(c, "quarkus.mongodb.metrics.enabled"));
        assertNull(brut(c, "quarkus.application.name"));
        assertEquals("8093", brut(c, "quarkus.http.port"));
        assertEquals("8093", brut(c, "quarkus.lambda.mock-event-server.dev-port"));
        assertEquals("https://accounts.google.com", brut(c, "quarkus.oidc.auth-server-url"));
        assertEquals("Content-Type,Authorization,X-Api-Key", brut(c, "quarkus.http.cors.headers"));
        assertEquals("INFO", brut(c, "quarkus.log.level"));
        assertEquals("DEBUG", brut(c, "quarkus.log.category.\"io.github.vzwingma.finances.budget.serverless.services.utilisateurs\".level"));
        assertTrue(brut(c, "quarkus.log.console.format").contains("%L"));
        assertEquals("/utilisateurs/v2/_info", brut(c, "quarkus.oidc.info-public.tenant-paths"));
    }

    @Test
    void profilTest_niChaineDeConnexionNiAuthServerUrl() throws IOException {
        SmallRyeConfig c = config("test");
        // Dev Services MongoDB reste actif et aucun appel à Google : ces clés sont réservées à dev et prod (R4)
        assertNull(brut(c, "quarkus.mongodb.connection-string"));
        assertNull(brut(c, "quarkus.oidc.auth-server-url"));
        assertNull(brut(c, "quarkus.http.cors.origins"));
        assertNull(brut(c, "quarkus.mongodb.metrics.enabled"));
        assertNull(brut(c, "quarkus.application.name"));
        // Ce que l'ancien profil Maven dev donnait aux tests JVM
        assertEquals("8093", brut(c, "quarkus.http.port"));
        assertEquals("Content-Type,Authorization,X-Api-Key", brut(c, "quarkus.http.cors.headers"));
        assertEquals("ERROR", brut(c, "quarkus.log.category.\"io.quarkus.oidc.runtime.OidcProvider\".level"));
    }

    /**
     * AP004 T2.8 : configuration effective commune aux 3 profils (dev, test, prod) : timeouts MongoDB, catégorie des métriques
     * {@code [HTTP-METRIC]} / {@code [REST-METRIC]} toujours en INFO (jamais sous {@code QUARKUS_LOG_LEVEL}), masquage du jeton
     * par {@code OidcProvider} en ERROR, version OpenAPI alignée sur l'application, tenant {@code /_info} public propre au module.
     */
    @Test
    void tousProfils_configurationEffectiveCommune() throws IOException {
        for (String profil : new String[]{"dev", "test", "prod"}) {
            SmallRyeConfig c = config(profil);
            assertEquals("5S", brut(c, "quarkus.mongodb.server-selection-timeout"), profil);
            assertEquals("3S", brut(c, "quarkus.mongodb.connect-timeout"), profil);
            assertEquals("8S", brut(c, "quarkus.mongodb.read-timeout"), profil);
            assertEquals("10", brut(c, "quarkus.mongodb.max-pool-size"), profil);
            assertEquals("INFO", brut(c, "quarkus.log.category.\"io.github.vzwingma.finances.budget.metrics\".level"), profil + " : catégorie des métriques en INFO");
            assertEquals("ERROR", brut(c, "quarkus.log.category.\"io.quarkus.oidc.runtime.OidcProvider\".level"), profil + " : masquage du jeton");
            assertEquals("${quarkus.application.version}", brut(c, "quarkus.smallrye-openapi.info-version"), profil);
            assertEquals("/utilisateurs/v2/_info", brut(c, "quarkus.oidc.info-public.tenant-paths"), profil);
            assertEquals("false", brut(c, "quarkus.oidc.info-public.tenant-enabled"), profil);
            assertEquals("any", brut(c, "quarkus.oidc.token.issuer"), profil);
            assertEquals("email", brut(c, "quarkus.oidc.token.principal-claim"), profil);
            assertEquals("service", brut(c, "quarkus.oidc.application-type"), profil);
        }
    }
}
