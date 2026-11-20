package io.github.vzwingma.finances.budget.services.communs.api.metrics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Instrumentation des requêtes entrantes (AP004 T2.0) : une ligne INFO par requête, exploitable en
 * CloudWatch Logs Insights.
 * <pre>[HTTP-METRIC] op=BudgetsResource.getBudget status=200 durationMs=42 cold=false</pre>
 * <ul>
 *   <li>{@code op} : classe et méthode JAX-RS, jamais l'URI (donc aucun identifiant). {@code unmatched} si
 *   aucune ressource n'a été sélectionnée (requête rejetée avant le routage).</li>
 *   <li>{@code durationMs} : durée mesurée avec {@link System#nanoTime()} (horloge monotone, adaptée à une durée ;
 *   la {@link java.time.Clock} injectée de l'ADR-004 sert à obtenir un instant, pas à mesurer un intervalle).
 *   {@code -1} si l'heure de début est inconnue (requête avortée par un filtre exécuté avant le nôtre).</li>
 *   <li>{@code cold} : {@code true} uniquement pour la première requête traitée par l'instance.</li>
 * </ul>
 * Aucun identifiant, jeton, corps, email ni clé API n'est tracé.
 */
@ApplicationScoped
public class HttpMetricsRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(HttpMetricsRecorder.class);

    /** Propriété de requête portant l'état de début */
    static final String START_PROPERTY = HttpMetricsRecorder.class.getName() + ".start";
    static final String OP_UNMATCHED = "unmatched";
    static final long DURATION_UNKNOWN = -1L;

    /** Passe à false dès la première requête de l'instance */
    private final AtomicBoolean firstRequest = new AtomicBoolean(true);

    /** Contexte de la requête courante (proxy CDI résolu à l'appel) */
    @Inject
    ContainerRequestContext requestContext;

    /** Ressource de la requête courante (proxy CDI résolu à l'appel) */
    @Inject
    ResourceInfo resourceInfo;

    /** Début de requête : mémorise l'heure de début et l'état « démarrage à froid ». */
    record Start(long nanos, boolean cold) {
    }

    /**
     * Mémorise le début de la requête (pre-matching).
     *
     * @param request contexte de la requête
     */
    public void start(ContainerRequestContext request) {
        if (request != null) {
            request.setProperty(START_PROPERTY, new Start(System.nanoTime(), firstRequest.getAndSet(false)));
        }
    }

    /**
     * Journalise la métrique de la requête courante (post-matching).
     *
     * @param status code HTTP de la réponse
     * @return la ligne journalisée
     */
    public String record(int status) {
        return record(requestContext, resourceInfo, status);
    }

    /**
     * Journalise la métrique d'une requête.
     *
     * @param request contexte de la requête
     * @param info    ressource sélectionnée (peut être nulle)
     * @param status  code HTTP de la réponse
     * @return la ligne journalisée
     */
    String record(ContainerRequestContext request, ResourceInfo info, int status) {
        Object start = null;
        try {
            start = request != null ? request.getProperty(START_PROPERTY) : null;
        } catch (RuntimeException e) {
            LOG.debug("[HTTP-METRIC] contexte de requête indisponible", e);
        }
        long durationMs = DURATION_UNKNOWN;
        boolean cold = false;
        if (start instanceof Start s) {
            durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - s.nanos());
            cold = s.cold();
        }
        String line = format(operation(info), status, durationMs, cold);
        LOG.info(line);
        return line;
    }

    /**
     * Format de la ligne de métrique
     */
    static String format(String op, int status, long durationMs, boolean cold) {
        return "[HTTP-METRIC] op=" + op + " status=" + status + " durationMs=" + durationMs + " cold=" + cold;
    }

    /**
     * Nom {@code Ressource.méthode} de l'opération JAX-RS
     */
    static String operation(ResourceInfo info) {
        try {
            Class<?> resourceClass = info != null ? info.getResourceClass() : null;
            Method method = info != null ? info.getResourceMethod() : null;
            if (resourceClass != null && method != null) {
                return resourceClass.getSimpleName() + "." + method.getName();
            }
        } catch (RuntimeException e) {
            LOG.debug("[HTTP-METRIC] ressource indisponible", e);
        }
        return OP_UNMATCHED;
    }
}
