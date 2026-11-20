package io.github.vzwingma.finances.budget.serverless.services.operations.api;

import io.github.vzwingma.finances.budget.serverless.services.operations.api.enums.OperationsAPIEnum;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AP004 T2.0 — une ligne {@code [HTTP-METRIC]} INFO est émise par requête réelle, sans donnée sensible.
 */
@QuarkusTest
class HttpMetricsLogTest {

    @Test
    void requete_ligneHttpMetric() {
        List<String> lines = new CopyOnWriteArrayList<>();
        Logger jul = Logger.getLogger("io.github.vzwingma.finances.budget.services.communs.api.metrics.HttpMetricsRecorder");
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                lines.add(record.getMessage());
            }

            @Override
            public void flush() {
                // sans objet
            }

            @Override
            public void close() {
                // sans objet
            }
        };
        jul.addHandler(handler);
        try {
            given().header("Authorization", "Bearer eyJfaux.jeton.secret")
                    .when().get(OperationsAPIEnum.BUDGET_BASE + "/_info").then().statusCode(200);
        } finally {
            jul.removeHandler(handler);
        }
        System.out.println("EXEMPLE-LOG " + lines);
        assertFalse(lines.isEmpty());
        String line = lines.get(lines.size() - 1);
        assertTrue(line.matches("^\\[HTTP-METRIC] op=\\w+\\.\\w+ status=200 durationMs=\\d+ cold=(true|false)$"), line);
        assertFalse(line.contains("eyJ"));
    }
}
