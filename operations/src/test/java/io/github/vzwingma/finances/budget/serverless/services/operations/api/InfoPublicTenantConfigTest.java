package io.github.vzwingma.finances.budget.serverless.services.operations.api;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.enums.OperationsAPIEnum;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * AP005 / ADR-008 : la config du tenant OIDC {@code info-public} (désactivé, lié au seul chemin {@code /_info}) est présente
 * et chargée dans le module. Redondant avec {@code InfoPublicTenantResolver} / {@code InfoEndpointPublicTest} : assumé,
 * une régression de config (ligne supprimée/commentée) est signalée ici explicitement.
 */
@QuarkusTest
class InfoPublicTenantConfigTest {

    private static final String ENABLED = "quarkus.oidc.info-public.tenant-enabled";
    private static final String PATHS = "quarkus.oidc.info-public.tenant-paths";
    private static final String INFO = OperationsAPIEnum.BUDGET_BASE + "/_info";

    @Test
    void configRuntime_tenantInfoPublicDesactive() {
        Config config = ConfigProvider.getConfig();
        assertEquals(false, config.getValue(ENABLED, Boolean.class), ENABLED);
    }

    @Test
    void configRuntime_tenantPathsEgalAuSeulChemin_info() {
        Config config = ConfigProvider.getConfig();
        assertEquals(INFO, config.getValue(PATHS, String.class), PATHS);
    }

    /**
     * Lecture directe des fichiers (AP004 T2.3 : plus de fichiers dev/ et prod/, un seul {@code application.properties} par module).
     * Le chemin {@code /_info} est propre au module ; la désactivation du tenant est commune (fichier de communs).
     */
    @Test
    void configFichierModule_tenantPathsEgalAuSeulChemin_info() throws IOException {
        Path fichier = Path.of("src/main/resources/application.properties");
        assertEquals(INFO, charger(fichier).getProperty(PATHS, "").trim(), PATHS + " dans " + fichier);
    }

    @Test
    void configFichierCommuns_tenantInfoPublicDesactive() throws IOException {
        Properties props = new Properties();
        try (InputStream flux = getClass().getClassLoader().getResourceAsStream("META-INF/microprofile-config.properties")) {
            assertNotNull(flux, "microprofile-config.properties de communs introuvable");
            props.load(new InputStreamReader(flux, StandardCharsets.UTF_8));
        }
        assertEquals("false", props.getProperty(ENABLED, "").trim(), ENABLED + " dans communs");
    }

    /**
     * AP004 T2.8 : le fichier de test ne redéfinit ni la désactivation du tenant (commune, dans communs) ni {@code tenant-paths}
     * (propre au module, dans son {@code application.properties} de main) : pas de doublon qui masquerait une dérive de la source réelle.
     * La provenance effective des valeurs est vérifiée par {@code CommunsConfigurationTest}.
     */
    @Test
    void configFichierTest_neRedefinitPasLeTenantInfoPublic() throws IOException {
        Path fichier = Path.of("src/test/resources/application.properties");
        Properties props = charger(fichier);
        assertFalse(props.containsKey(ENABLED), ENABLED + " ne doit pas être dupliqué dans " + fichier);
        assertFalse(props.containsKey(PATHS), PATHS + " ne doit pas être dupliqué dans " + fichier);
    }

    private static Properties charger(Path fichier) throws IOException {
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        return props;
    }
}
