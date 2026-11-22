package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.ServiceDistantIndisponibleException;

import java.io.Serial;

/**
 * Indisponibilité <b>rapide et transitoire</b> d'un service distant : connexion refusée ou réponse 429 / 502 / 503 / 504.
 * C'est la seule situation qui déclenche le {@code @Retry} des clients REST (ADR-009) : un timeout n'en fait pas partie
 * (un retry provoquerait un deuxième démarrage à froid de l'aval). Pour l'API, c'est un 503 comme sa classe mère.
 */
public class ServiceDistantTransitoireException extends ServiceDistantIndisponibleException {

    @Serial
    private static final long serialVersionUID = -7124466902851183059L;

    /**
     * @param libelleErreur libellé de l'erreur (sans donnée sensible)
     */
    public ServiceDistantTransitoireException(String libelleErreur) {
        super(libelleErreur);
    }
}
