package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import java.io.Serial;

/**
 * Budget non trouvé
 *
 * @author vzwingma
 */
public class BudgetNotFoundException extends AbstractBusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Exception Budget introuvable
     *
     * @param libelleErreur libellé de l'erreur
     */
    public BudgetNotFoundException(String libelleErreur) {
        super(libelleErreur);
    }

}
