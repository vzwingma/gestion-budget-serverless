package io.github.vzwingma.finances.budget.services.communs.data.abstrait;

import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Classe d'objets Sérialisable échangés en REST
 * @author vzwingma
 *
 */
@NoArgsConstructor
public abstract class AbstractAPIObjectModel implements Serializable {

	/**
	 *
	 */
	@Serial
	private static final long serialVersionUID = 7048018115641885137L;

}
