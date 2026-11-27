package io.github.vzwingma.finances.budget.services.communs.api;

import io.github.vzwingma.finances.budget.services.communs.api.metrics.HttpMetricsRecorder;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AP004 T2.8 : les filtres de trace mutualisés ({@link APITraceFilters}) sont bien déclarés (pre-matching + réponse) et délèguent
 * à {@code AbstractAPIInterceptors} (heure de début mémorisée à l'entrée, ligne de métrique à la sortie).
 */
class APITraceFiltersTest {

    public static class FakeResource {
        public void getBudget() {
            // cible uniquement
        }
    }

    private final APITraceFilters filtres = new APITraceFilters();
    private final Map<String, Object> proprietes = new HashMap<>();
    private ContainerRequestContext requete;
    private ContainerResponseContext reponse;
    private SimpleResourceInfo info;

    @BeforeEach
    void setUp() throws Exception {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/budgets/v2/test");
        when(uriInfo.getRequestUri()).thenReturn(new URI("/budgets/v2/test"));
        requete = mock(ContainerRequestContext.class);
        when(requete.getUriInfo()).thenReturn(uriInfo);
        when(requete.getMethod()).thenReturn("GET");
        when(requete.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        when(requete.getProperty(anyString())).thenAnswer(i -> proprietes.get(i.<String>getArgument(0)));
        doAnswer(i -> proprietes.put(i.getArgument(0), i.getArgument(1))).when(requete).setProperty(anyString(), any());
        reponse = mock(ContainerResponseContext.class);
        when(reponse.getStatus()).thenReturn(200);
        when(reponse.getStatusInfo()).thenReturn(mock(jakarta.ws.rs.core.Response.StatusType.class));
        // Injection CDI (@Inject, package-private) remplacée à la main en test unitaire
        filtres.httpMetrics = new HttpMetricsRecorder();
        info = mock(SimpleResourceInfo.class);
        when(info.getResourceClass()).thenAnswer(i -> FakeResource.class);
        when(info.getMethodName()).thenReturn("getBudget");
    }

    @Test
    void declarationDesFiltres_preMatchingEtReponse() throws NoSuchMethodException {
        ServerRequestFilter entree = APITraceFilters.class.getMethod("preMatchingFilter", ContainerRequestContext.class)
                .getAnnotation(ServerRequestFilter.class);
        assertNotNull(entree, "@ServerRequestFilter attendu sur preMatchingFilter");
        assertTrue(entree.preMatching(), "le filtre d'entrée doit s'exécuter au pre-matching (heure de début avant tout autre filtre)");
        assertNotNull(APITraceFilters.class.getMethod("postMatchingFilter", ContainerResponseContext.class, SimpleResourceInfo.class)
                .getAnnotation(ServerResponseFilter.class), "@ServerResponseFilter attendu sur postMatchingFilter");
    }

    @Test
    void preMatching_memoriseLHeureDeDebut() {
        assertDoesNotThrow(() -> filtres.preMatchingFilter(requete));
        verify(requete).setProperty(anyString(), any());
        assertFalse(proprietes.isEmpty(), "l'heure de début doit être mémorisée sur la requête");
    }

    @Test
    void reponse_neLevePasDException_etLitLeStatut() {
        filtres.preMatchingFilter(requete);
        assertDoesNotThrow(() -> filtres.postMatchingFilter(reponse, info));
        verify(reponse, org.mockito.Mockito.atLeastOnce()).getStatus();
    }
}
