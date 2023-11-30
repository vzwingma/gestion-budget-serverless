package io.github.vzwingma.finances.budget.serverless.services.comptes.api.enums;

/**
 * Enum des URL d'API
 *
 * @author vzwingma
 */
public class ComptesAPIEnum {


    public static final String PARAM_ID_COMPTE = "{idCompte}";
    /**
     * Comptes
     */
    public static final String COMPTES_BASE = "/comptes/v2";
    public static final String COMPTES_LIST = "/tous";
    public static final String COMPTES_ID = "/" + PARAM_ID_COMPTE;
    /**
     * Roles
     */
    public static final String COMPTES_ROLE = "USER_COMPTES";


    private ComptesAPIEnum() {
        // Constructeur privé pour une classe enum
    }

}
