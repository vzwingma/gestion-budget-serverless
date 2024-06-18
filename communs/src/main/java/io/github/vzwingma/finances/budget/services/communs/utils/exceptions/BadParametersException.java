package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import java.io.Serial;

/**
 * Erreur sur les paramètres en entrée
 *
 * @author vzwingma
 */
public class BadParametersException extends AbstractBusinessException {

    @Serial
    private static final long serialVersionUID = -3217099790763455363L;

    /**
     * @param message d'erreur
     */
    public BadParametersException(String message) {
        super(message);
    }
}
