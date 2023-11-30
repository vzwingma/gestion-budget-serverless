package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import java.io.Serial;

/**
 * Utilisateur non autorisé
 *
 * @author vzwingma
 */
public class UserNotAuthorizedException extends AbstractBusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Message d'erreur
     *
     * @param libelleErreur message d'erreur
     */
    public UserNotAuthorizedException(String libelleErreur) {
        super(libelleErreur);
    }
}
