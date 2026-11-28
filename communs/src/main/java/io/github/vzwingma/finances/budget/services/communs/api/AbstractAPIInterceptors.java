package io.github.vzwingma.finances.budget.services.communs.api;

import io.github.vzwingma.finances.budget.services.communs.api.metrics.HttpMetricsRecorder;
import io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter;
import io.github.vzwingma.finances.budget.services.communs.api.security.IJwtSecurityContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import io.github.vzwingma.finances.budget.services.communs.utils.security.SecurityUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.HttpHeaders;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.regex.Pattern;

/**
 * Interceptor for logging et sécurité des requests et des responses
 */
public abstract class AbstractAPIInterceptors {


    private static final Logger LOG = LoggerFactory.getLogger(AbstractAPIInterceptors.class);

    /** En-tête posé par l'API Gateway / ALB, propagé aux appels inter-services (AP004 T3.2b, ADR-010). */
    public static final String HTTP_HEADER_AMZN_TRACE_ID = "X-Amzn-Trace-Id";
    /** Clé MDC de l'identifiant de trace AWS (affichée par le format de log : {@code %X{awsTraceId}}). */
    public static final String MDC_AWS_TRACE_ID = "awsTraceId";
    /** Longueur maximale acceptée pour l'en-tête entrant : au-delà, il est ignoré. */
    static final int AWS_TRACE_HEADER_MAX_LENGTH = 256;
    private static final String AWS_TRACE_ROOT_PREFIX = "Root=";
    /** Format strict du {@code Root} AWS X-Ray : version 1, epoch hexadécimal sur 8 caractères, identifiant sur 24 caractères. */
    private static final Pattern AWS_TRACE_ROOT_PATTERN = Pattern.compile("^1-[0-9a-f]{8}-[0-9a-f]{24}$");

    @Inject
    IJwtSecurityContext securityContext;

    /** Instrumentation des requêtes (AP004 T2.0). Peut être nul hors CDI (tests unitaires). */
    @Inject
    HttpMetricsRecorder httpMetrics;

    /**
     * Logger requête
     *
     * @param requestContext context de la requête
     */
    public void preMatchingFilter(ContainerRequestContext requestContext) {
        // Corrélation inter-Lambdas : seul un Root strictement valide entre dans le MDC (jamais l'en-tête brut)
        String awsTraceRoot = extractAwsTraceRoot(requestContext.getHeaderString(HTTP_HEADER_AMZN_TRACE_ID));
        if (awsTraceRoot != null) {
            MDC.put(MDC_AWS_TRACE_ID, awsTraceRoot);
        } else {
            MDC.remove(MDC_AWS_TRACE_ID);
        }
        if (httpMetrics != null) {
            httpMetrics.start(requestContext);
        }
        // Replace pattern-breaking characters
        String path = requestContext.getUriInfo().getPath().replaceAll(SecurityUtils.ESCAPE_INPUT_REGEX, "_");
        String apiKey = requestContext.getHeaderString(AbstractAPISecurityFilter.HTTP_HEADER_API_KEY);
        LOG.debug("[HTTP] > [uri:{} {}]", requestContext.getMethod(), path);
        // Ni la clé API ni le jeton JWT ne doivent être tracés : seule leur présence est journalisée
        LOG.debug("[HTTP] > [api-key:{}][jwt:{}]", apiKey != null && !apiKey.isEmpty() ? "présente" : "absente",
                requestContext.getHeaderString(HttpHeaders.AUTHORIZATION) != null ? "présent" : "absent");
    }

    /**
     * Logger réponse, sans information sur la ressource ({@code op=unmatched})
     *
     * @param responseContext context de la réponse
     */
    public void postMatchingFilter(ContainerResponseContext responseContext) {
        postMatchingFilter(responseContext, null);
    }

    /**
     * Logger réponse
     *
     * @param responseContext context de la réponse
     * @param resourceInfo    ressource sélectionnée (peut être nulle)
     */
    public void postMatchingFilter(ContainerResponseContext responseContext, SimpleResourceInfo resourceInfo) {
        try {
            if (httpMetrics != null) {
                httpMetrics.record(resourceInfo, responseContext.getStatus());
            }
            BusinessTraceContext.getclear();
            LOG.debug("[HTTP] < [{} - {}]", responseContext.getStatus(), responseContext.getStatusInfo().getReasonPhrase());
        } finally {
            // Un conteneur Lambda est réutilisé : aucune fuite de l'identifiant de trace d'une invocation à l'autre
            MDC.remove(MDC_AWS_TRACE_ID);
        }
    }

    /**
     * Extrait le {@code Root=} de l'en-tête {@code X-Amzn-Trace-Id} ({@code Root=1-...;Parent=...;Sampled=1}).
     *
     * @param header valeur brute de l'en-tête (non fiable)
     * @return le {@code Root} s'il respecte le format {@code 1-xxxxxxxx-xxxxxxxxxxxxxxxxxxxxxxxx} (hexadécimal minuscule),
     * sinon {@code null} (en-tête absent, trop long ou invalide, y compris avec CR/LF). La valeur brute n'est jamais journalisée.
     */
    public static String extractAwsTraceRoot(String header) {
        if (header == null || header.length() > AWS_TRACE_HEADER_MAX_LENGTH) {
            return null;
        }
        for (String part : header.split(";")) {
            String element = part.strip();
            if (element.startsWith(AWS_TRACE_ROOT_PREFIX)) {
                String root = element.substring(AWS_TRACE_ROOT_PREFIX.length());
                // matches() exige la correspondance complète : un saut de ligne final est refusé
                return AWS_TRACE_ROOT_PATTERN.matcher(root).matches() ? root : null;
            }
        }
        return null;
    }

    /**
     * Retourne le nom de l'utilisateur authentifié
     * @return le nom de l'utilisateur authentifié
     */
    public String getAuthenticatedUser() {
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            return securityContext.getUserPrincipal().getName();
        } else {
            return "ANONYME";
        }
    }
}
