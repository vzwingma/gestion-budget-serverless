package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.api.metrics.MetricsLog;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import java.util.concurrent.TimeUnit;

/**
 * Instrumentation des appels sortants vers les autres microservices (AP004 T2.0) :
 * <pre>[REST-METRIC] client=comptes method=GET status=200 durationMs=35</pre>
 * <ul>
 *   <li>Ni chemin, ni identifiant, ni jeton, ni en-tête n'est tracé : seuls le nom logique du client, la méthode
 *   HTTP, le statut et la durée le sont.</li>
 *   <li>Réponse en erreur (4xx / 5xx) : le filtre de réponse s'exécute, la ligne est tracée avec le statut reçu.</li>
 *   <li>Exception avant réponse (timeout, connexion refusée) : le filtre de réponse n'est <b>pas</b> exécuté ; la ligne
 *   est alors émise par {@link TraductionErreursServiceDistantInterceptor} avec {@code status=timeout} ou
 *   {@code status=connect-error}.</li>
 * </ul>
 * La durée est mesurée avec {@link System#nanoTime()} (horloge monotone).
 */
public abstract class AbstractRestClientMetricsFilter implements ClientRequestFilter, ClientResponseFilter {

    /** Propriété de requête portant l'heure de début (nanosecondes) */
    static final String START_PROPERTY = AbstractRestClientMetricsFilter.class.getName() + ".startNanos";

    private final String clientName;

    protected AbstractRestClientMetricsFilter(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        requestContext.setProperty(START_PROPERTY, System.nanoTime());
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        Object start = requestContext.getProperty(START_PROPERTY);
        long durationMs = start instanceof Long nanos ? TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanos) : -1L;
        MetricsLog.info(format(clientName, requestContext.getMethod(), responseContext.getStatus(), durationMs));
    }

    /**
     * Format de la ligne de métrique
     */
    static String format(String client, String method, int status, long durationMs) {
        return format(client, method, String.valueOf(status), durationMs);
    }

    /**
     * Format de la ligne de métrique, avec un statut textuel pour les échecs sans réponse HTTP
     * ({@code timeout}, {@code connect-error}), émis par {@link TraductionErreursServiceDistantInterceptor}.
     */
    static String format(String client, String method, String status, long durationMs) {
        return "[REST-METRIC] client=" + client + " method=" + method + " status=" + status + " durationMs=" + durationMs;
    }
}
