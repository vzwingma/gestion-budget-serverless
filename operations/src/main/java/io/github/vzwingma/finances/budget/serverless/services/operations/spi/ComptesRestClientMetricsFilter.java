package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

/**
 * Métriques des appels vers le service comptes (AP004 T2.0)
 */
public class ComptesRestClientMetricsFilter extends AbstractRestClientMetricsFilter {
    public ComptesRestClientMetricsFilter() {
        super("comptes");
    }
}
