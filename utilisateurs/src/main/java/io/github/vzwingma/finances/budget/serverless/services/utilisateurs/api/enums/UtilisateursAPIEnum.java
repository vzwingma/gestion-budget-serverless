package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api.enums;

/**
 * Enum des URL d'API
 * @author vzwingma
 *
 */

public class UtilisateursAPIEnum {


	private UtilisateursAPIEnum(){
		// Constructeur privé pour une classe enum
	}

	/**
	 * Utilisateurs
	 */
	public static final String USERS_BASE = "/utilisateurs/v2";
	public static final String USERS_ACCESS_DATE = "/lastaccessdate";
	public static final String USERS_PREFS = "/preferences";

	/**
	 * Roles
	 */
	public static final String UTILISATEURS_ROLE = "USER_UTILISATEURS";
}
