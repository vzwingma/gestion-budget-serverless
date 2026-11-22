package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.api.metrics.MetricsLog;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AP004 T2.0 — filtres {@code [REST-METRIC]} des clients REST comptes et paramétrages.
 */
class RestClientMetricsFilterTest {

    private static String logLine(AbstractRestClientMetricsFilter filter, String method, int status) {
        Map<String, Object> props = new HashMap<>();
        ClientRequestContext request = mock(ClientRequestContext.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getProperty(anyString())).thenAnswer(i -> props.get(i.<String>getArgument(0)));
        doAnswer(i -> props.put(i.getArgument(0), i.getArgument(1))).when(request).setProperty(anyString(), any());
        ClientResponseContext response = mock(ClientResponseContext.class);
        when(response.getStatus()).thenReturn(status);

        StringBuilder captured = new StringBuilder();
        Logger jul = Logger.getLogger(MetricsLog.CATEGORIE);
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                captured.append(record.getMessage());
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
            filter.filter(request);
            filter.filter(request, response);
        } finally {
            jul.removeHandler(handler);
        }
        return captured.toString();
    }

    @Test
    void format_ligneAttendue() {
        assertEquals("[REST-METRIC] client=comptes method=GET status=200 durationMs=5",
                AbstractRestClientMetricsFilter.format("comptes", "GET", 200, 5));
    }

    @Test
    void comptes_ligneLoggee_sansDonneeSensible() {
        String line = logLine(new ComptesRestClientMetricsFilter(), "GET", 200);
        assertTrue(line.matches("^\\[REST-METRIC] client=comptes method=GET status=200 durationMs=\\d+$"), line);
        assertFalse(line.contains("eyJ"));
        assertFalse(line.contains("/"));
    }

    @Test
    void parametrages_reponseEnErreur_statutTrace() {
        String line = logLine(new ParametragesRestClientMetricsFilter(), "GET", 503);
        assertTrue(line.matches("^\\[REST-METRIC] client=parametrages method=GET status=503 durationMs=\\d+$"), line);
    }

    @Test
    void sansDebut_pasDException() {
        ClientRequestContext request = mock(ClientRequestContext.class);
        when(request.getMethod()).thenReturn("GET");
        ClientResponseContext response = mock(ClientResponseContext.class);
        when(response.getStatus()).thenReturn(200);
        assertDoesNotThrow(() -> new ComptesRestClientMetricsFilter().filter(request, response));
    }
}
