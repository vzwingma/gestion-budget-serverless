package io.github.vzwingma.finances.budget.services.communs.config;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AP004 T2.2 : la ligne de configuration effective journalisée au démarrage ne contient que des valeurs non sensibles.
 */
class EffectiveConfigLoggerTest {

    private static SmallRyeConfig config(Map<String, String> proprietes) {
        return new SmallRyeConfigBuilder().addDefaultInterceptors().withSources(new PropertiesConfigSource(proprietes, "test", 100)).build();
    }

    @Test
    void decrire_valeursNonSensibles() {
        SmallRyeConfig config = config(Map.of(
                "quarkus.mongodb.read-timeout", "8S",
                "quarkus.mongodb.max-pool-size", "10",
                "quarkus.log.category.\"io.quarkus.oidc.runtime.OidcProvider\".level", "ERROR",
                "quarkus.rest-client.comptes-service.read-timeout", "4000",
                "quarkus.rest-client.comptes-service.connect-timeout", "1500"));

        String ligne = EffectiveConfigLogger.decrire(config, List.of("prod"));

        assertTrue(ligne.startsWith("[CONFIG] valeurs effectives : profils=[prod]"), ligne);
        assertTrue(ligne.contains("quarkus.mongodb.read-timeout=8S"), ligne);
        assertTrue(ligne.contains("quarkus.mongodb.max-pool-size=10"), ligne);
        assertTrue(ligne.contains("quarkus.log.category.\"io.quarkus.oidc.runtime.OidcProvider\".level=ERROR"), ligne);
        assertTrue(ligne.contains("quarkus.rest-client.comptes-service.read-timeout=4000"), ligne);
        assertTrue(ligne.contains("quarkus.rest-client.comptes-service.connect-timeout=1500"), ligne);
        assertTrue(ligne.contains("quarkus.mongodb.connect-timeout=<absente>"), ligne);
    }

    @Test
    void decrire_neJournalisePasLesSecretsNiLesUrls() {
        SmallRyeConfig config = config(Map.of(
                "quarkus.mongodb.connection-string", "mongodb+srv://utilisateur:motdepasse@cluster.mongodb.net/base",
                "quarkus.oidc.public-key", "MIIBIjANBgkq-secret",
                "quarkus.rest-client.comptes-service.url", "https://api.exemple.org/comptes",
                "quarkus.mongodb.database", "mongodb://login:secret@hote/base"));

        String ligne = EffectiveConfigLogger.decrire(config, List.of("test"));

        assertFalse(ligne.contains("motdepasse"), ligne);
        assertFalse(ligne.contains("secret"), ligne);
        assertFalse(ligne.contains("MIIBIjAN"), ligne);
        assertFalse(ligne.contains("api.exemple.org"), ligne);
        assertFalse(ligne.contains("connection-string"), ligne);
        // Une valeur qui ressemble à une URL avec identifiants est masquée, même sur une clé de la liste blanche
        assertTrue(ligne.contains("quarkus.mongodb.database=" + EffectiveConfigLogger.VALEUR_MASQUEE), ligne);
    }

    @Test
    void decrire_expressionNonResolue_neLevePasDException() {
        SmallRyeConfig config = config(Map.of("quarkus.mongodb.read-timeout", "${VARIABLE_INCONNUE}"));

        String ligne = EffectiveConfigLogger.decrire(config, List.of("prod"));

        // Selon la version de SmallRye, une expression sans valeur est « absente » ou « non résolue » : jamais d'exception
        assertTrue(ligne.contains("quarkus.mongodb.read-timeout=<absente>")
                || ligne.contains("quarkus.mongodb.read-timeout=" + EffectiveConfigLogger.VALEUR_NON_RESOLUE), ligne);
        assertFalse(ligne.contains("${"), ligne);
    }

    @Test
    void onStart_journaliseSansException() {
        // Observateur de démarrage : lit la configuration réelle et les profils actifs, sans lever d'exception
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> new EffectiveConfigLogger().onStart(null));
    }
}
