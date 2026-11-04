package io.github.vzwingma.finances.budget.serverless.services.operations.api;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.enums.OperationsAPIEnum;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
     * Lecture directe des fichiers : configs déployées (dev/prod, non chargées par le profil de test) et config de test.
     * Le fichier de test est lu explicitement car la config runtime est aussi alimentée par l'application.properties
     * de main (copié dans target/classes), ce qui masquerait la suppression de la ligne côté test.
     */
    @ParameterizedTest(name = "src/{0}/application.properties")
    @ValueSource(strings = {"main/resources/dev", "main/resources/prod", "test/resources"})
    void configFichier_tenantInfoPublic(String dossier) throws IOException {
        Path fichier = Path.of("src").resolve(dossier).resolve("application.properties");
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        assertEquals("false", props.getProperty(ENABLED, "").trim(), ENABLED + " dans " + fichier);
        assertEquals(INFO, props.getProperty(PATHS, "").trim(), PATHS + " dans " + fichier);
    }
}
