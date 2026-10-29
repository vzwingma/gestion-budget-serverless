package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import java.io.Serial;

/**
 * Donnée stockée non conforme (champ obligatoire absent, par exemple le mois d'un budget).
 * Ce n'est pas une indisponibilité du service mais une donnée corrompue : statut HTTP 500.
 *
 * @author vzwingma
 */
public class DonneeCorrompueException extends AbstractBusinessException {

    @Serial
    private static final long serialVersionUID = 3167425836492784123L;

    /**
     * Exception Donnée corrompue
     *
     * @param libelleErreur libellé de l'erreur
     */
    public DonneeCorrompueException(String libelleErreur) {
        super(libelleErreur);
    }
}
