package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

/**
 * Métriques des appels vers le service paramétrages (AP004 T2.0)
 */
public class ParametragesRestClientMetricsFilter extends AbstractRestClientMetricsFilter {
    public ParametragesRestClientMetricsFilter() {
        super("parametrages");
    }
}
