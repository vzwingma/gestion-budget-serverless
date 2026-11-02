package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ADR-008 : {@link InfoPublicTenantResolver} ne renvoie {@code info-public} que pour les chemins finissant par {@code /_info} ;
 * tout autre chemin reçoit explicitement le tenant par défaut (renvoyer {@code null} laisserait la résolution statique de quarkus-oidc
 * sélectionner {@code info-public} sur un segment quelconque du chemin).
 */
class TestInfoPublicTenantResolver {

    private final InfoPublicTenantResolver resolver = new InfoPublicTenantResolver();

    private String resolve(String path) {
        RoutingContext context = mock(RoutingContext.class);
        when(context.normalizedPath()).thenReturn(path);
        return resolver.resolve(context);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/x/v2/_info", "/comptes/v2/_info", "/budgets/v2/_info", "/_info"})
    void cheminInfo_tenantInfoPublic(String path) {
        assertEquals("info-public", resolve(path), path);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/x/v2/info-public", "/x/v2/_info/", "/x/v2/categories/info-public", "/x/v2/_info/abc",
            "/x/v2/info-public/tous", "/x/v2/_information", "/x/v2/my_info", "/x/v2/tous", "/"})
    void autreChemin_tenantParDefaut(String path) {
        assertEquals("Default", resolve(path), path);
    }

    @Test
    void cheminNull_tenantParDefaut() {
        assertEquals("Default", resolve(null));
    }
}
