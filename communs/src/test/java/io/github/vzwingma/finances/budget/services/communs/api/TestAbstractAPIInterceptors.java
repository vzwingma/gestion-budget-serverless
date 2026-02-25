package io.github.vzwingma.finances.budget.services.communs.api;

import io.github.vzwingma.finances.budget.services.communs.api.security.IJwtSecurityContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de AbstractAPIInterceptors
 */
class TestAbstractAPIInterceptors {

    private ConcreteAPIInterceptors interceptors;
    private ContainerRequestContext requestContext;
    private ContainerResponseContext responseContext;
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception {
        interceptors = new ConcreteAPIInterceptors();
        requestContext = mock(ContainerRequestContext.class);
        responseContext = mock(ContainerResponseContext.class);
        uriInfo = mock(UriInfo.class);

        when(uriInfo.getPath()).thenReturn("/api/v1/test");
        when(uriInfo.getRequestUri()).thenReturn(new URI("/api/v1/test"));
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(requestContext.getMethod()).thenReturn("GET");
        MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        when(requestContext.getHeaders()).thenReturn(headers);
        when(requestContext.getHeaderString(anyString())).thenReturn(null);

        when(responseContext.getStatus()).thenReturn(200);
        when(responseContext.getStatusInfo()).thenReturn(mock(jakarta.ws.rs.core.Response.StatusType.class));
    }

    @Test
    void testPreMatchingFilter() {
        // Ne doit pas lancer d'exception
        assertDoesNotThrow(() -> interceptors.preMatchingFilter(requestContext));
    }

    @Test
    void testPreMatchingFilterAvecApiKeyEtJwt() {
        when(requestContext.getHeaderString("X-Api-Key")).thenReturn("api-key-value");
        when(requestContext.getHeaderString("Authorization")).thenReturn("Bearer someToken");
        assertDoesNotThrow(() -> interceptors.preMatchingFilter(requestContext));
    }

    @Test
    void testPreMatchingFilterAvecCaracteresSpeciaux() {
        when(uriInfo.getPath()).thenReturn("/api/v1\ninjection\rtest\tpath");
        assertDoesNotThrow(() -> interceptors.preMatchingFilter(requestContext));
    }

    @Test
    void testPostMatchingFilter() {
        when(responseContext.getStatusInfo().getReasonPhrase()).thenReturn("OK");
        assertDoesNotThrow(() -> interceptors.postMatchingFilter(responseContext));
    }

    @Test
    void testGetAuthenticatedUserSansSecurityContext() {
        // securityContext est null => retourne "ANONYME"
        assertEquals("ANONYME", interceptors.getAuthenticatedUser());
    }

    @Test
    void testGetAuthenticatedUserAvecSecurityContextEtPrincipal() {
        IJwtSecurityContext mockCtx = mock(IJwtSecurityContext.class);
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("user@example.com");
        when(mockCtx.getUserPrincipal()).thenReturn(mockPrincipal);

        interceptors.setSecurityContext(mockCtx);
        assertEquals("user@example.com", interceptors.getAuthenticatedUser());
    }

    @Test
    void testGetAuthenticatedUserAvecSecurityContextSansPrincipal() {
        IJwtSecurityContext mockCtx = mock(IJwtSecurityContext.class);
        when(mockCtx.getUserPrincipal()).thenReturn(null);

        interceptors.setSecurityContext(mockCtx);
        assertEquals("ANONYME", interceptors.getAuthenticatedUser());
    }

    // Implémentation concrète testable (sans CDI)
    private static class ConcreteAPIInterceptors extends AbstractAPIInterceptors {
        // Permet d'injecter manuellement le securityContext pour les tests
        public void setSecurityContext(IJwtSecurityContext ctx) {
            this.securityContext = ctx;
        }
    }
}

