package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import java.io.Serial;

/**
 * Erreur sur le chargement de données
 *
 * @author vzwingma
 */
public class DataNotFoundException extends AbstractBusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * @param message d'erreur
     */
    public DataNotFoundException(String message) {
        super(message);
    }
}
