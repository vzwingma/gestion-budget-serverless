package io.github.vzwingma.finances.budget.services.communs.api.security;

import io.quarkus.oidc.OIDCException;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Indisponibilité du fournisseur OIDC (discovery ou JWKS Google injoignable, timeout) : <b>503</b> au lieu du 500 par défaut
 * (AP004 T4.0-e, décision 👤 Gate #1 Q8).
 * <p>
 * Mécanisme du 500 constaté : {@code quarkus-oidc} échoue l'authentification avec une {@link OIDCException} (ou une erreur de
 * transport) qui n'est pas une {@code AuthenticationFailedException} ; elle n'est donc pas convertie en 401 mais transmise
 * à la chaîne d'échec de Vert.x, où {@code QuarkusErrorHandler} répond 500. L'authentification s'exécute <i>avant</i> JAX-RS :
 * un {@code ExceptionMapper} ne la voit pas, d'où ce gestionnaire d'échec posé sur le routeur HTTP.
 * <p>
 * Un jeton invalide, forgé ou expiré reste en 401 (il lève une {@code AuthenticationFailedException}, non concernée). Le corps
 * de la réponse ne contient aucun détail technique ; seuls les noms des classes d'exception sont journalisés, jamais leur message
 * (qui peut contenir le corps de la réponse du serveur OIDC). Aucune réflexion : pas de contrainte pour l'image native.
 */
@ApplicationScoped
public class OidcIndisponibleFailureHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OidcIndisponibleFailureHandler.class);

    /** Corps de la réponse 503 : volontairement générique. */
    static final String MESSAGE_INDISPONIBLE = "Service d'authentification momentanément indisponible";

    private static final int PROFONDEUR_MAX_CAUSES = 10;

    /**
     * Enregistre le gestionnaire d'échec sur le routeur HTTP, avant le gestionnaire d'erreur par défaut de Quarkus.
     *
     * @param router routeur HTTP de l'application
     */
    void enregistre(@Observes Router router) {
        router.route().failureHandler(OidcIndisponibleFailureHandler::traite);
    }

    static void traite(RoutingContext ctx) {
        Throwable echec = ctx.failure();
        if (isOidcIndisponible(echec) && !ctx.response().ended() && !ctx.response().headWritten()) {
            LOG.warn("Fournisseur OIDC indisponible (discovery ou JWKS) : réponse 503 [{}]", classes(echec));
            ctx.response()
                    .setStatusCode(503)
                    .putHeader("Content-Type", "text/plain;charset=UTF-8")
                    .end(MESSAGE_INDISPONIBLE);
        } else {
            ctx.next();
        }
    }

    /**
     * @param echec échec transmis à la chaîne d'échec HTTP
     * @return vrai si l'échec (ou l'une de ses causes) est une indisponibilité du fournisseur OIDC : {@link OIDCException} ou
     * erreur de transport (connexion refusée, hôte inconnu, délai dépassé) levée depuis le code {@code quarkus-oidc}
     */
    static boolean isOidcIndisponible(Throwable echec) {
        Throwable courant = echec;
        for (int i = 0; courant != null && i < PROFONDEUR_MAX_CAUSES; i++) {
            if (courant instanceof OIDCException) {
                return true;
            }
            if (isErreurTransport(courant) && provientDeQuarkusOidc(courant)) {
                return true;
            }
            courant = courant.getCause() == courant ? null : courant.getCause();
        }
        return false;
    }

    private static boolean isErreurTransport(Throwable t) {
        return t instanceof ConnectException || t instanceof UnknownHostException || t instanceof SocketTimeoutException
                || t instanceof TimeoutException || t.getClass().getSimpleName().contains("Timeout");
    }

    private static boolean provientDeQuarkusOidc(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            if (e.getClassName().startsWith("io.quarkus.oidc.")) {
                return true;
            }
        }
        return false;
    }

    private static String classes(Throwable echec) {
        StringBuilder sb = new StringBuilder();
        Throwable courant = echec;
        for (int i = 0; courant != null && i < PROFONDEUR_MAX_CAUSES; i++) {
            sb.append(i == 0 ? "" : " <- ").append(courant.getClass().getName());
            courant = courant.getCause() == courant ? null : courant.getCause();
        }
        return sb.toString();
    }
}
