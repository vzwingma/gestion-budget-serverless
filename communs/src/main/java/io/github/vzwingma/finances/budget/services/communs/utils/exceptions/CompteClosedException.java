package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import java.io.Serial;

/**
 * Budget non trouvé
 *
 * @author vzwingma
 */
public class CompteClosedException extends AbstractBusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Erreur compte clos
     *
     * @param libelleErreur libellé de l'erreur
     */
    public CompteClosedException(String libelleErreur) {
        super(libelleErreur);
    }
}
