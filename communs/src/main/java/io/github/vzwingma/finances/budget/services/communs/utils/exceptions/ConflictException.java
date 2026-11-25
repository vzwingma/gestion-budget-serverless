package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import java.io.Serial;

/**
 * Conflit avec une ressource déjà existante.
 */
public class ConflictException extends AbstractBusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
