package io.github.vzwingma.finances.budget.serverless.api;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * AP004 T2.7 : le schéma OpenAPI réellement servi par {@code /q/openapi} porte la version de l'application, et jamais
 * le littéral non résolu {@code ${quarkus.application.version}}.
 */
@QuarkusTest
class OpenApiSchemaVersionTest {

    @Test
    void schemaOpenApi_infoVersion_estLaVersionDeLApplication() {
        String versionApplication = ConfigProvider.getConfig().getValue("quarkus.application.version", String.class);
        assertNotNull(versionApplication);
        assertFalse(versionApplication.contains("${"), versionApplication);

        given().accept("application/json")
                .when().get("/q/openapi")
                .then().statusCode(200)
                .body("info.version", equalTo(versionApplication))
                .body("info.version", not(containsString("${")))
                .body("info.version", not(equalTo("${quarkus.application.version}")));
    }
}
