package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.quarkus.oidc.TenantResolver;
import jakarta.enterprise.context.ApplicationScoped;
import io.vertx.ext.web.RoutingContext;

/**
 * Résolution du tenant OIDC (ADR-008) : le tenant {@code info-public} (désactivé, requête anonyme) n'est sélectionné
 * que pour les chemins se terminant par {@code /_info}.
 * <p>
 * Renvoyer {@code null} ne suffit pas : {@code StaticTenantResolver} enchaîne alors sur le {@code tenant-paths} puis sur
 * {@code DefaultStaticTenantResolver}, qui sélectionne le tenant statique dont l'identifiant égale un SEGMENT quelconque du chemin
 * (ex. {@code /parametres/v2/categories/info-public} → {@code info-public}, jeton non validé). Pour tout autre chemin, ce resolver
 * renvoie donc explicitement {@link #DEFAULT_TENANT_ID}, valeur non nulle qui court-circuite ces résolutions.
 */
@ApplicationScoped
public class InfoPublicTenantResolver implements TenantResolver {

    /** Identifiant du tenant OIDC désactivé, anonyme (propriété {@code quarkus.oidc.info-public.*}) */
    public static final String INFO_PUBLIC_TENANT_ID = "info-public";
    /** Identifiant du tenant par défaut de quarkus-oidc ({@code io.quarkus.oidc.runtime.OidcUtils.DEFAULT_TENANT_ID}, non public) */
    public static final String DEFAULT_TENANT_ID = "Default";
    /** Suffixe du chemin de l'endpoint public d'information */
    public static final String INFO_PATH_SUFFIX = "/_info";

    @Override
    public String resolve(RoutingContext context) {
        String path = context.normalizedPath();
        return path != null && path.endsWith(INFO_PATH_SUFFIX) ? INFO_PUBLIC_TENANT_ID : DEFAULT_TENANT_ID;
    }
}
