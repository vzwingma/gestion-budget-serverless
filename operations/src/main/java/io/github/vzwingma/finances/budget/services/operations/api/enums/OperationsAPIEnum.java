package io.github.vzwingma.finances.budget.services.operations.api.enums;

/**
 * Enum des URL d'API
 * @author vzwingma
 *
 */
public class OperationsAPIEnum {


	private OperationsAPIEnum(){
		// Constructeur privé pour une classe enum
	}
	
	public static final String PARAM_ID_BUDGET = "{idBudget}";
	public static final String PARAM_ID_COMPTE = "{idCompte}";
	public static final String PARAM_ID_OPERATION = "{idOperation}";

	
	/**
	 * Budget
	 */
	public static final String BUDGET_BASE = "/budgets/v1";
	public static final String BUDGET_ID = "/"+ PARAM_ID_BUDGET;

	// Avec en paramètre
	// - idCompte & mois & annee
	public static final String BUDGET_QUERY = "/query"; 

	// Avec en paramètres : 
	// - actif
	public static final String BUDGET_ETAT = "/"+PARAM_ID_BUDGET+"/etat";
	public static final String BUDGET_COMPTE_INTERVALLES = "/"+PARAM_ID_COMPTE+"/intervalles";
	public static final String BUDGET_COMPTE_OPERATIONS_LIBELLES = "/"+PARAM_ID_COMPTE+"/operations/libelles";

	/**
	 * Operations
	 */
	public static final String BUDGET_OPERATION = BUDGET_ID + "/operations" ;
	public static final String BUDGET_OPERATION_BY_ID = BUDGET_OPERATION + "/"+PARAM_ID_OPERATION ;
	public static final String BUDGET_OPERATION_DERNIERE = BUDGET_OPERATION_BY_ID + "/derniereOperation";
	public static final String BUDGET_OPERATION_INTERCOMPTE = BUDGET_OPERATION + "/versCompte/"+PARAM_ID_COMPTE ;

	/**
	 * Roles
	 */
	public static final String OPERATIONS_ROLE = "USER_OPERATIONS";
}
