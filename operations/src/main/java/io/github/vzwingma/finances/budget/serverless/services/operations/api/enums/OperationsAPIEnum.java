package io.github.vzwingma.finances.budget.serverless.services.operations.api.enums;

/**
 * Enum des URL d'API
 *
 * @author vzwingma
 */
public class OperationsAPIEnum {


    public static final String PARAM_ID_BUDGET = "{idBudget}";
    public static final String PARAM_ID_COMPTE = "{idCompte}";
    public static final String PARAM_ID_OPERATION = "{idOperation}";

    public static final String ADMIN_SUFFIX = "/admin";

    /**
     * Budget
     */
    public static final String BUDGET_BASE = "/budgets/v2";
    public static final String BUDGET_ADMIN_BASE = BUDGET_BASE + ADMIN_SUFFIX;
    public static final String BUDGET_ID = "/" + PARAM_ID_BUDGET;
    /**
     * Operations
     */
    public static final String BUDGET_OPERATION = BUDGET_ID + "/operations";
    public static final String BUDGET_OPERATION_BY_ID = BUDGET_OPERATION + "/" + PARAM_ID_OPERATION;
    public static final String BUDGET_OPERATION_INTERCOMPTE = BUDGET_OPERATION + "/versCompte/" + PARAM_ID_COMPTE;
    // Avec en paramètre
    // - idCompte & mois & annee
    public static final String BUDGET_QUERY = "/query";
    public static final String BUDGET_SOLDES = "/soldes";
    // Avec en paramètres :
    // - actif
    public static final String BUDGET_ETAT = "/" + PARAM_ID_BUDGET + "/etat";
    public static final String OPERATIONS_LIBELLES = "/compte/" + PARAM_ID_COMPTE + "/operations/libelles";
    public static final String OPERATIONS_LIBELLES_OVERRIDE = OPERATIONS_LIBELLES + "/override";
    /**
     * Roles
     */
    public static final String OPERATIONS_ROLE = "USER_OPERATIONS";
    private OperationsAPIEnum() {
        // Constructeur privé pour une classe enum
    }
}
