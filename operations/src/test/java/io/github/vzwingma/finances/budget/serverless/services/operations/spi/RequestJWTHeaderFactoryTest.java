package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.api.security.IJwtSecurityContext;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthPayload;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JWTAuthToken;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwtAuthHeader;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AP004 T0.5 — {@link RequestJWTHeaderFactory} : propagation du jeton Bearer validé et de la clé API vers les autres services.
 */
class RequestJWTHeaderFactoryTest {

    private RequestJWTHeaderFactory factory;
    private IJwtSecurityContext securityContext;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        factory = new RequestJWTHeaderFactory();
        securityContext = mock(IJwtSecurityContext.class);
        inject(securityContext);
    }

    private void inject(IJwtSecurityContext context) throws ReflectiveOperationException {
        Field f = RequestJWTHeaderFactory.class.getDeclaredField("securityContext");
        f.setAccessible(true);
        f.set(factory, context);
    }

    private MultivaluedMap<String, String> update() {
        return factory.update(new MultivaluedHashMap<>(), new MultivaluedHashMap<>());
    }

    @Test
    void jetonEtCleApi_propagesDansLesHeaders() {
        when(securityContext.getJwtValidatedToken()).thenReturn(new JWTAuthToken(new JwtAuthHeader(), new JWTAuthPayload(), true, "raw.jwt.token"));
        when(securityContext.getApiKey()).thenReturn("KEY-123");

        MultivaluedMap<String, String> headers = update();

        assertEquals("Bearer raw.jwt.token", headers.getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals("KEY-123", headers.getFirst(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY));
    }

    @Test
    void sansJeton_pasDeHeaderAuthorization() {
        when(securityContext.getJwtValidatedToken()).thenReturn(null);
        when(securityContext.getApiKey()).thenReturn("KEY-123");

        MultivaluedMap<String, String> headers = update();

        assertNull(headers.getFirst(HttpHeaders.AUTHORIZATION), "aucun Bearer ne doit être inventé");
        assertEquals("KEY-123", headers.getFirst(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY));
    }

    @Test
    void jetonSansContenuBrut_pasDeHeaderAuthorization() {
        when(securityContext.getJwtValidatedToken()).thenReturn(new JWTAuthToken(new JwtAuthHeader(), new JWTAuthPayload(), true, null));

        assertNull(update().getFirst(HttpHeaders.AUTHORIZATION), "pas de 'Bearer null'");
    }

    @Test
    void sansCleApi_pasDeHeaderCleApi() {
        when(securityContext.getJwtValidatedToken()).thenReturn(new JWTAuthToken(new JwtAuthHeader(), new JWTAuthPayload(), true, "t"));
        when(securityContext.getApiKey()).thenReturn(null);

        MultivaluedMap<String, String> headers = update();

        assertEquals("Bearer t", headers.getFirst(HttpHeaders.AUTHORIZATION));
        assertNull(headers.getFirst(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY));
    }

    @Test
    void contexteDeSecuriteNull_headersVides() throws ReflectiveOperationException {
        inject(null);

        assertTrue(update().isEmpty());
    }
}
