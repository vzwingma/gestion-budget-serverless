package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;

/**
 * Exeption métier
 * @author vzwingma
 *
 */
@Getter
public class AbstractBusinessException extends IOException {
	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = -8869692972880299979L;

	private final String libelle;
	/**
	 * Exception métier
	 * @param libelleErreur libellé de l'erreur
	 */
    public AbstractBusinessException(String libelleErreur){
		this.libelle = libelleErreur;
        logErreur(libelleErreur, null);
	}

	/**
	 * Exception métier
	 * @param libelleErreur libellé Erreur
	 * @param e exception
	 */
	public AbstractBusinessException(String libelleErreur, Throwable e){
		this.libelle = libelleErreur;
		logErreur(libelleErreur, e);
	}

	/**
	 * @param libelleErreur libellé Erreur
	 * @param ex exception
	 */
	private void logErreur(String libelleErreur, Throwable ex){
		Logger logger = LoggerFactory.getLogger(this.getClass());
		if(ex != null){
			logger.error("{}", libelleErreur);
		}
		else{
			logger.error("{}", libelleErreur, ex);
		}
	}

}
