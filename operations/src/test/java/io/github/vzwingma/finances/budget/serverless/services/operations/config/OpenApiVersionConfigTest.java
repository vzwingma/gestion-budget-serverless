package io.github.vzwingma.finances.budget.serverless.services.operations.config;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * AP004 T2.7 : la version du schéma OpenAPI est celle de l'application (configuration commune de {@code communs}),
 * et non plus une valeur codée en dur (ancien « 21.1.0 »).
 */
@QuarkusTest
class OpenApiVersionConfigTest {

    @Test
    void versionOpenApi_alignee_surLaVersionDeLApplication() {
        Config config = ConfigProvider.getConfig();
        String versionApplication = config.getValue("quarkus.application.version", String.class);
        assertNotNull(versionApplication);
        assertEquals(versionApplication, config.getValue("quarkus.smallrye-openapi.info-version", String.class));
        assertNotEquals("21.1.0", config.getValue("quarkus.smallrye-openapi.info-version", String.class));
    }
}
