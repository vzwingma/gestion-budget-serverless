package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.AbstractBusinessException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.ServiceDistantIndisponibleException;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.HttpMethod;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Traduction centralisée, pour tous les clients REST inter-services, des échecs <b>sans réponse HTTP</b>
 * (AP004 T2.4, ADR-009) :
 * <ul>
 *   <li>timeout (connexion ou lecture) → {@link ServiceDistantIndisponibleException} (503), {@code status=timeout} ;</li>
 *   <li>connexion impossible (refusée, hôte inconnu, connexion coupée) → {@link ServiceDistantTransitoireException}
 *   (503, seul cas éligible au {@code @Retry}), {@code status=connect-error}.</li>
 * </ul>
 * Les exceptions métier (levées par {@link ServicesDistantsResponseExceptionMapper} sur une réponse HTTP) et les
 * erreurs non reconnues traversent inchangées.
 * <p>
 * <b>Pourquoi un intercepteur</b> : les {@code ClientResponseFilter} et les {@code ResponseExceptionMapper} ne voient que
 * les réponses HTTP, jamais les échecs de transport. L'intercepteur est <i>interne</i> à l'intercepteur de fault tolerance
 * (priorité numérique plus élevée) : le {@code @Retry} voit donc déjà l'exception typée et peut distinguer connexion
 * refusée (retry) et timeout (pas de retry). Une seule classe pour les 2 clients, sans changer les types injectés.
 * <p>
 * Ligne tracée : {@code [REST-METRIC] client=comptes method=GET status=timeout durationMs=4003} — ni chemin, ni id, ni jeton.
 */
@TraductionErreursServiceDistant
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER + 500)
public class TraductionErreursServiceDistantInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(TraductionErreursServiceDistantInterceptor.class);

    static final String STATUT_TIMEOUT = "timeout";
    static final String STATUT_CONNECT_ERROR = "connect-error";

    /** Nature d'un échec sans réponse HTTP */
    enum Echec { TIMEOUT, CONNEXION, AUTRE }

    @AroundInvoke
    Object traduire(InvocationContext ctx) throws Exception {
        final long debut = System.nanoTime();
        final Class<?> contrat = contrat(ctx);
        traceCacheMiss(contrat, ctx.getMethod());
        Object resultat = ctx.proceed();
        if (resultat instanceof Uni<?> uni) {
            return uni.onFailure().transform(erreur ->
                    traduireErreur(contrat, ctx.getMethod(), erreur, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - debut)));
        }
        return resultat;
    }

    /**
     * Cet intercepteur est <i>interne</i> à l'intercepteur de cache : s'il s'exécute pour une méthode {@code @CacheResult},
     * c'est que le cache n'a pas de valeur (miss) et que le service distant va être appelé. Aucun paramètre (donc aucun
     * identifiant) n'est tracé. Un retry rejoue l'appel distant et donc cette trace.
     */
    private static void traceCacheMiss(Class<?> contrat, Method methode) {
        if (!LOG.isDebugEnabled()) {
            return;
        }
        try {
            CacheResult cache = contrat.getMethod(methode.getName(), methode.getParameterTypes()).getAnnotation(CacheResult.class);
            if (cache != null) {
                LOG.debug("[CACHE] miss cache={} : appel du service distant {}.{}", cache.cacheName(), nomClient(contrat), methode.getName());
            }
        } catch (NoSuchMethodException e) {
            // méthode non portée par le contrat : rien à tracer
        }
    }

    private static Throwable traduireErreur(Class<?> contrat, Method methode, Throwable erreur, long dureeMs) {
        if (erreur instanceof AbstractBusinessException) {
            return erreur;
        }
        Echec echec = classer(erreur);
        if (echec == Echec.AUTRE) {
            return erreur;
        }
        String statut = echec == Echec.TIMEOUT ? STATUT_TIMEOUT : STATUT_CONNECT_ERROR;
        LOG.info(AbstractRestClientMetricsFilter.format(nomClient(contrat), methodeHttp(contrat, methode), statut, dureeMs));
        return echec == Echec.TIMEOUT
                ? new ServiceDistantIndisponibleException("Service distant indisponible (délai dépassé)")
                : new ServiceDistantTransitoireException("Service distant indisponible (connexion impossible)");
    }

    /**
     * Classe un échec en parcourant sa chaîne de causes. Le timeout est testé <b>avant</b> la connexion :
     * un timeout de connexion Netty ({@code ConnectTimeoutException}) est une {@link ConnectException}, mais ne doit pas
     * être rejoué.
     */
    static Echec classer(Throwable erreur) {
        boolean connexion = false;
        Throwable courante = erreur;
        for (int profondeur = 0; courante != null && profondeur < 10; profondeur++) {
            if (courante instanceof AbstractBusinessException) {
                return Echec.AUTRE;
            }
            if (courante instanceof TimeoutException || courante instanceof SocketTimeoutException
                    || courante.getClass().getSimpleName().endsWith("TimeoutException")) {
                return Echec.TIMEOUT;
            }
            if (courante instanceof ConnectException || courante instanceof UnknownHostException
                    || courante instanceof NoRouteToHostException || courante instanceof ClosedChannelException
                    || "HttpClosedException".equals(courante.getClass().getSimpleName())) {
                connexion = true;
            }
            courante = courante.getCause() == courante ? null : courante.getCause();
        }
        return connexion ? Echec.CONNEXION : Echec.AUTRE;
    }

    /**
     * Interface du client REST (annotée {@link RegisterRestClient}) : l'intercepteur s'exécute sur le bean généré par
     * Quarkus, qui implémente cette interface sans en recopier les annotations.
     */
    private static Class<?> contrat(InvocationContext ctx) {
        Class<?> cible = ctx.getTarget() != null ? ctx.getTarget().getClass() : ctx.getMethod().getDeclaringClass();
        for (Class<?> c = cible; c != null; c = c.getSuperclass()) {
            if (c.isAnnotationPresent(RegisterRestClient.class)) {
                return c;
            }
            for (Class<?> i : c.getInterfaces()) {
                if (i.isAnnotationPresent(RegisterRestClient.class)) {
                    return i;
                }
            }
        }
        return ctx.getMethod().getDeclaringClass();
    }

    /** Nom logique du client : clé de configuration sans le suffixe {@code -service} (ex. {@code comptes-service} -> {@code comptes}) */
    private static String nomClient(Class<?> contrat) {
        RegisterRestClient client = contrat.getAnnotation(RegisterRestClient.class);
        if (client == null || client.configKey().isBlank()) {
            return contrat.getSimpleName();
        }
        return client.configKey().replaceAll("-service$", "");
    }

    /** Méthode HTTP déduite de l'annotation JAX-RS de la méthode du contrat ({@code GET} par défaut) */
    private static String methodeHttp(Class<?> contrat, Method methode) {
        try {
            Method methodeContrat = contrat.getMethod(methode.getName(), methode.getParameterTypes());
            return Arrays.stream(methodeContrat.getAnnotations())
                    .map(a -> a.annotationType().getAnnotation(HttpMethod.class))
                    .filter(Objects::nonNull)
                    .map(HttpMethod::value)
                    .findFirst()
                    .orElse("GET");
        } catch (NoSuchMethodException e) {
            return "GET";
        }
    }
}
