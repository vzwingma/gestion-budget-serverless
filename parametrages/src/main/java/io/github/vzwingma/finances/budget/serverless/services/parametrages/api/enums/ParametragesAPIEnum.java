package io.github.vzwingma.finances.budget.serverless.services.parametrages.api.enums;

/**
 * Enum des URL d'API
 * @author vzwingma
 *
 */
public class ParametragesAPIEnum {


	private ParametragesAPIEnum(){
		// Constructeur privé pour une classe enum
	}
	public static final String PARAM_ID_CATEGORIE = "idCategorie";
	/**
	 * Paramétrages
	 */
	public static final String PARAMS_BASE = "/parametres/v1";
	public static final String PARAMS_CATEGORIES = "/categories";

	public static final String PARAMS_CATEGORIE_ID = "/{"+PARAM_ID_CATEGORIE + "}";
}
