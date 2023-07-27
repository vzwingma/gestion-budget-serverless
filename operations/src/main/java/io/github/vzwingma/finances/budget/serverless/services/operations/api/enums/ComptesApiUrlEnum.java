package io.github.vzwingma.finances.budget.serverless.services.operations.api.enums;

/**
 * Enum des URL d'API
 * @author vzwingma
 *
 */
public class ComptesApiUrlEnum {


	private ComptesApiUrlEnum(){
		// Constructeur privé pour une classe enum
	}
	public static final String PARAM_ID_COMPTE = "idCompte";

	/**
	 * Comptes
	 */
	public static final String COMPTES_BASE = "/comptes/v2";
	public static final String COMPTES_ID = "/{"+PARAM_ID_COMPTE + "}";
}
