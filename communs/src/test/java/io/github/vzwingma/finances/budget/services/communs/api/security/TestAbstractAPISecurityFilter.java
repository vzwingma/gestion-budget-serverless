package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AP004 T0.5 — {@link AbstractAPISecurityFilter} : exposition de l'identité validée par quarkus-oidc au contexte applicatif
 * (email, jeton brut pour la propagation inter-services) et de la clé API. Test unitaire (pas de contexte Quarkus).
 */
class TestAbstractAPISecurityFilter {

    private AbstractAPISecurityFilter filter;
    private JwtSecurityContext securityContext;
    private ContainerRequestContext request;
    private MultivaluedMap<String, String> headers;

    @BeforeEach
    void setUp() {
        filter = new AbstractAPISecurityFilter() { };
        securityContext = new JwtSecurityContext();
        filter.securityContext = securityContext;
        headers = new MultivaluedHashMap<>();
        request = mock(ContainerRequestContext.class);
        when(request.getHeaders()).thenReturn(headers);
    }

    private static SecurityIdentity identity(String email, String rawToken) {
        Principal principal = () -> email;
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder().setPrincipal(principal);
        if (rawToken != null) {
            builder.addCredential(new AccessTokenCredential(rawToken));
        }
        return builder.build();
    }

    @Test
    void identiteAuthentifiee_exposeEmailEtJetonBrut() {
        filter.identity = identity("a@b.com", "raw.jwt.token");
        filter.filter(request);

        assertNotNull(securityContext.getJwtValidatedToken());
        assertEquals("a@b.com", securityContext.getJwtValidatedToken().getPayload().getEmail());
        assertEquals("raw.jwt.token", securityContext.getJwtValidatedToken().getRawContent(), "jeton brut propagé (Bearer inter-services)");
        assertEquals("a@b.com", securityContext.getUserPrincipal().getName());
    }

    @Test
    void identiteSansCredentialJeton_rawContentNull() {
        filter.identity = identity("a@b.com", null);
        filter.filter(request);

        assertNotNull(securityContext.getJwtValidatedToken());
        assertNull(securityContext.getJwtValidatedToken().getRawContent());
    }

    @Test
    void identiteAnonyme_aucunJetonExpose() {
        filter.identity = QuarkusSecurityIdentity.builder().setAnonymous(true).build();
        assertTrue(filter.identity.isAnonymous());
        filter.filter(request);

        assertNull(securityContext.getJwtValidatedToken(), "aucune identité exposée pour un anonyme");
    }

    @Test
    void identiteNull_aucunJetonExpose() {
        filter.identity = null;
        filter.filter(request);

        assertNull(securityContext.getJwtValidatedToken());
    }

    @Test
    void cleApiPresente_exposee() {
        filter.identity = identity("a@b.com", "t");
        headers.add(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "KEY-123");
        filter.filter(request);

        assertEquals("KEY-123", securityContext.getApiKey());
    }

    @Test
    void cleApiAbsenteOuVide_nonExposee() {
        filter.identity = identity("a@b.com", "t");
        filter.filter(request);
        assertNull(securityContext.getApiKey());

        headers.add(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY, "");
        filter.filter(request);
        assertNull(securityContext.getApiKey());
    }

    // ---------- niveau de log selon le chemin (ADR-008) ----------

    private List<LogRecord> logsPour(String path) {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn(path);
        when(request.getUriInfo()).thenReturn(uriInfo);
        filter.identity = QuarkusSecurityIdentity.builder().setAnonymous(true).build();

        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord r) {
                records.add(r);
            }

            @Override
            public void flush() {
                // rien à vider
            }

            @Override
            public void close() {
                // rien à fermer
            }
        };
        java.util.logging.Logger jul = java.util.logging.Logger.getLogger(AbstractAPISecurityFilter.class.getName());
        Level previous = jul.getLevel();
        jul.setLevel(Level.ALL);
        jul.addHandler(handler);
        try {
            filter.filter(request);
        } finally {
            jul.removeHandler(handler);
            jul.setLevel(previous);
        }
        return records;
    }

    private static Level levelOf(List<LogRecord> records, String fragment) {
        return records.stream().filter(r -> r.getMessage().contains(fragment)).map(LogRecord::getLevel).findFirst().orElse(null);
    }

    @Test
    void cheminInfo_anonymeEtCleApiAbsente_debug() {
        List<LogRecord> records = logsPour("comptes/v2/_info");
        assertEquals(Level.FINE, levelOf(records, "Accès anonyme"));
        assertEquals(Level.FINE, levelOf(records, "Clé API non trouvée"));
    }

    @Test
    void autreChemin_anonymeEtCleApiAbsente_warn() {
        List<LogRecord> records = logsPour("comptes/v2/tous");
        assertEquals(Level.WARNING, levelOf(records, "Accès anonyme"));
        assertEquals(Level.WARNING, levelOf(records, "Clé API non trouvée"));
    }

    @Test
    void cheminFinissantParInfoPublic_anonyme_warn() {
        assertEquals(Level.WARNING, levelOf(logsPour("parametres/v2/categories/info-public"), "Accès anonyme"));
    }
}
