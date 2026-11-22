package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.AbstractBusinessException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.ServiceDistantIndisponibleException;
import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
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

    /** Libellé par défaut si la liaison est illisible (ne casse jamais l'appel) */
    static final String LIBELLE_INCONNU = "inconnu";

    /** Client et méthode HTTP tracés pour un appel */
    record Cible(String client, String methode) {
        static final Cible INCONNUE = new Cible(LIBELLE_INCONNU, LIBELLE_INCONNU);
    }

    @AroundInvoke
    Object traduire(InvocationContext ctx) throws Exception {
        final long debut = System.nanoTime();
        final Cible cible = cible(ctx);
        traceCacheMiss(ctx, cible);
        Object resultat = ctx.proceed();
        if (resultat instanceof Uni<?> uni) {
            return uni.onFailure().transform(erreur ->
                    traduireErreur(cible, erreur, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - debut)));
        }
        return resultat;
    }

    /**
     * Lit client et méthode HTTP dans la liaison d'intercepteur (aucune réflexion). Toute défaillance, y compris une
     * {@link Error} de réflexion/liaison (ex. {@code LinkageError}, {@code MissingReflectionRegistrationError} en natif),
     * donne un libellé par défaut : la traduction des erreurs et l'appel ne sont jamais cassés par une trace.
     */
    static Cible cible(InvocationContext ctx) {
        try {
            if (ctx instanceof ArcInvocationContext arc) {
                TraductionErreursServiceDistant liaison = arc.getInterceptorBinding(TraductionErreursServiceDistant.class);
                if (liaison != null) {
                    return new Cible(liaison.client(), liaison.method());
                }
            }
        } catch (RuntimeException | Error e) {
            rethrowSiFatale(e);
            LOG.debug("[REST-METRIC] liaison d'intercepteur illisible : libellé par défaut", e);
        }
        return Cible.INCONNUE;
    }

    /** Une erreur fatale de la JVM n'est jamais absorbée */
    private static void rethrowSiFatale(Throwable e) {
        if (e instanceof VirtualMachineError fatale) {
            throw fatale;
        }
    }

    /**
     * Cet intercepteur est <i>interne</i> à l'intercepteur de cache : s'il s'exécute pour une méthode {@code @CacheResult},
     * c'est que le cache n'a pas de valeur (miss) et que le service distant va être appelé. Aucun paramètre (donc aucun
     * identifiant) n'est tracé. Un retry rejoue l'appel distant et donc cette trace.
     */
    private static void traceCacheMiss(InvocationContext ctx, Cible cible) {
        if (!LOG.isDebugEnabled()) {
            return;
        }
        try {
            if (ctx instanceof ArcInvocationContext arc) {
                CacheResult cache = arc.getInterceptorBinding(CacheResult.class);
                if (cache != null) {
                    LOG.debug("[CACHE] miss cache={} : appel du service distant {}", cache.cacheName(), cible.client());
                }
            }
        } catch (RuntimeException | Error e) {
            rethrowSiFatale(e);
        }
    }

    private static Throwable traduireErreur(Cible cible, Throwable erreur, long dureeMs) {
        if (erreur instanceof AbstractBusinessException) {
            return erreur;
        }
        Echec echec = classer(erreur);
        if (echec == Echec.AUTRE) {
            return erreur;
        }
        String statut = echec == Echec.TIMEOUT ? STATUT_TIMEOUT : STATUT_CONNECT_ERROR;
        LOG.info(AbstractRestClientMetricsFilter.format(cible.client(), cible.methode(), statut, dureeMs));
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
}
