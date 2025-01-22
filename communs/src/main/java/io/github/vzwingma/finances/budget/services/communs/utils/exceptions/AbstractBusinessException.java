package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;

/**
 * Exeption métier
 *
 * @author vzwingma
 */
@Getter
public class AbstractBusinessException extends IOException {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -8869692972880299979L;

    // Libellé de l'exception
    private final String libelle;
    /**
     * Exception métier
     *
     * @param libelleErreur libellé de l'erreur
     */
    public AbstractBusinessException(String libelleErreur) {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        libelleErreur = libelleErreur.replaceAll("[\n\r]", "_");
        this.libelle = libelleErreur;
        logger.error("{}", libelleErreur);
    }
}
