package io.github.vzwingma.finances.budget.services.communs.api.metrics;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AP004 T2.0 — format de la ligne {@code [HTTP-METRIC]}, indicateur {@code cold} et absence de donnée sensible.
 */
class HttpMetricsRecorderTest {

    private static final Pattern LINE = Pattern.compile("^\\[HTTP-METRIC] op=\\S+ status=\\d{3} durationMs=-?\\d+ cold=(true|false)$");

    /** Ressource factice dont le nom de classe et de méthode apparaissent dans {@code op} */
    public static class FakeResource {
        public void getBudget() {
            // méthode cible uniquement
        }
    }

    private HttpMetricsRecorder recorder;
    private ContainerRequestContext request;
    private ResourceInfo info;

    @BeforeEach
    void setUp() throws Exception {
        recorder = new HttpMetricsRecorder();
        Map<String, Object> properties = new HashMap<>();
        request = mock(ContainerRequestContext.class);
        when(request.getProperty(anyString())).thenAnswer(i -> properties.get(i.<String>getArgument(0)));
        doAnswer(i -> properties.put(i.getArgument(0), i.getArgument(1))).when(request).setProperty(anyString(), any());
        info = mock(ResourceInfo.class);
        when(info.getResourceClass()).thenAnswer(i -> FakeResource.class);
        when(info.getResourceMethod()).thenReturn(FakeResource.class.getMethod("getBudget"));
    }

    @Test
    void format_ligneAttendue() {
        assertEquals("[HTTP-METRIC] op=A.b status=404 durationMs=12 cold=true", HttpMetricsRecorder.format("A.b", 404, 12, true));
    }

    @Test
    void record_formatOperationEtStatut() {
        recorder.start(request);
        String line = recorder.record(request, info, 200);
        assertTrue(LINE.matcher(line).matches(), line);
        assertTrue(line.contains("op=FakeResource.getBudget status=200 "), line);
    }

    @Test
    void record_coldVraiPuisFaux() {
        recorder.start(request);
        assertTrue(recorder.record(request, info, 200).endsWith("cold=true"));
        recorder.start(request);
        assertTrue(recorder.record(request, info, 200).endsWith("cold=false"));
    }

    @Test
    void record_dureePositive() throws Exception {
        recorder.start(request);
        Thread.sleep(15);
        String line = recorder.record(request, info, 200);
        long duration = Long.parseLong(line.replaceAll(".*durationMs=(-?\\d+).*", "$1"));
        assertTrue(duration >= 10, line);
    }

    @Test
    void record_sansDebut_dureeInconnue() {
        String line = recorder.record(request, info, 401);
        assertTrue(line.contains("durationMs=-1 cold=false"), line);
    }

    @Test
    void record_sansRessource_unmatched() {
        recorder.start(request);
        String line = recorder.record(request, null, 401);
        assertTrue(line.contains("op=unmatched status=401"), line);
        assertTrue(LINE.matcher(line).matches(), line);
    }

    @Test
    void record_ressourceIndisponible_unmatched() {
        when(info.getResourceClass()).thenThrow(new IllegalStateException("hors requête"));
        recorder.start(request);
        assertTrue(recorder.record(request, info, 500).contains("op=unmatched"));
    }

    @Test
    void record_aucunJetonNiUriNiCle() {
        recorder.start(request);
        String line = recorder.record(request, info, 200);
        assertFalse(line.contains("eyJ"), line);
        assertFalse(line.toLowerCase().contains("bearer"), line);
        assertFalse(line.contains("/"), line);
        assertFalse(line.contains("@"), line);
    }
}
