package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.api.metrics.MetricsLog;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AP004 T2.0 — {@code @RegisterProvider} : le filtre est bien appliqué au client REST réel (réponse 404 : le filtre de
 * réponse s'exécute avant la levée de l'exception).
 */
@QuarkusTest
class RestClientMetricsQuarkusTest {

    @Test
    void clientComptes_ligneRestMetric() throws Exception {
        List<String> lines = new CopyOnWriteArrayList<>();
        Logger jul = Logger.getLogger(MetricsLog.CATEGORIE);
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
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        jul.addHandler(handler);
        try {
            IComptesServiceProvider client = RestClientBuilder.newBuilder()
                    .baseUri(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
                    .build(IComptesServiceProvider.class);
            try {
                client.getCompteById("compte-secret-id").await().indefinitely();
            } catch (RuntimeException e) {
                // 404 attendu
            }
        } finally {
            jul.removeHandler(handler);
            server.stop(0);
        }
        System.out.println("EXEMPLE-LOG " + lines);
        assertFalse(lines.isEmpty(), "aucune ligne REST-METRIC");
        assertTrue(lines.get(0).matches("^\\[REST-METRIC] client=comptes method=GET status=404 durationMs=\\d+$"), lines.get(0));
        assertFalse(lines.get(0).contains("compte-secret-id"));
    }
}
