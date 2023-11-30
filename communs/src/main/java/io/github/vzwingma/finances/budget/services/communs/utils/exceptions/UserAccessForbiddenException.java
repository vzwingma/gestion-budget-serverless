package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import java.io.Serial;

/**
 * Utilisateur non authentifié
 *
 * @author vzwingma
 */
public class UserAccessForbiddenException extends AbstractBusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Message d'erreur
     *
     * @param libelleErreur libellé de l'erreur
     */
    public UserAccessForbiddenException(String libelleErreur) {
        super(libelleErreur);
    }
}
