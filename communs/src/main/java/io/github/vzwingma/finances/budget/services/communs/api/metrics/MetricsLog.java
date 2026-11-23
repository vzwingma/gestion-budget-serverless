package io.github.vzwingma.finances.budget.services.communs.api.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Journal des métriques {@code [HTTP-METRIC]} et {@code [REST-METRIC]} (AP004 T2.0).
 * <p>
 * Catégorie de log <b>dédiée</b>, fixée à {@code INFO} dans la configuration commune ({@code META-INF/microprofile-config.properties}),
 * pour tous les profils : les métriques restent émises quel que soit {@code QUARKUS_LOG_LEVEL}
 * (qui règle la verbosité applicative des catégories des services et de communs).
 */
public final class MetricsLog {

    /** Nom de la catégorie de log des métriques (clé de configuration {@code quarkus.log.category."..."}) */
    public static final String CATEGORIE = "io.github.vzwingma.finances.budget.metrics";

    private static final Logger LOG = LoggerFactory.getLogger(CATEGORIE);

    private MetricsLog() {
        // utilitaire
    }

    /**
     * Émet une ligne de métrique (niveau INFO)
     *
     * @param ligne ligne formatée
     */
    public static void info(String ligne) {
        LOG.info(ligne);
    }
}
