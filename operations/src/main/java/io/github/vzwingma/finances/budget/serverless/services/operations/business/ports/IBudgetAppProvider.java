package io.github.vzwingma.finances.budget.serverless.services.operations.business.ports;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LibelleCategorieOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.projections.ProjectionBudgetSoldes;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.time.Month;

/**
 * Application Provider Interface de Budgets
 */
public interface IBudgetAppProvider {


    /**
     * Chargement du budget du mois courant
     *
     * @param idCompte compte
     * @param mois     mois
     * @param annee    année
     * @return budget mensuel chargé et initialisé à partir des données précédentes
     */
    Uni<BudgetMensuel> getBudgetMensuel(String idCompte, Month mois, int annee);

    /**
     * Retourne le solde et les totaux par catégorie pour un budget mensuel (ou la liste des budgets mensuels) pour un compte et une année donnée
     * @param idCompte identifiant du compte
     * @param mois mois (facultatif)
     * @param annee année
     * @return liste des soldes et totaux par catégorie
     */
    Multi<ProjectionBudgetSoldes> getSoldesBudgetMensuel(String idCompte, Month mois, Integer annee);
    /**
     * Charger budget
     *
     * @param idBudget id du budget
     * @return budget correspondant aux paramètres
     */
    Uni<BudgetMensuel> getBudgetMensuel(String idBudget);


    /**
     * Mise à jour d'une ligne de dépense
     *
     * @param idBudget       identifiant de budget
     * @param ligneOperation ligne de dépense
     */
    Uni<BudgetMensuel> addOrUpdateOperationInBudget(String idBudget, final LigneOperation ligneOperation, String auteur);

    /**
     * Création d'une opération de virements internes
     *
     * @param idBudget            id du budget source
     * @param ligneOperation      opération virement interne source
     * @param idCompteDestination id du compte destination
     * @return budget mensuel source modifié
     */
    Uni<BudgetMensuel> createOperationsVirementInterne(String idBudget, LigneOperation ligneOperation, String idCompteDestination, String auteur);

    /**
     * Suppression d'une opération
     *
     * @param idBudget    identifiant de budget
     * @param idOperation ligne opération
     */
    Uni<BudgetMensuel> deleteOperationInBudget(String idBudget, String idOperation);

    /**
     * Réinitialiser un budget mensuel
     *
     * @param idBudget budget mensuel
     */
    Uni<BudgetMensuel> reinitialiserBudgetMensuel(String idBudget);

    /**
     * Chargement de l'état du budget du mois courant en consultation
     *
     * @param idBudget id budget
     * @return budget mensuel chargé et initialisé à partir des données précédentes
     */
    Uni<Boolean> isBudgetMensuelActif(String idBudget);

    /**
     * Lock/unlock d'un budget
     *
     * @param budgetActif etat du budget
     */
    Uni<BudgetMensuel> setBudgetActif(String idBudgetMensuel, boolean budgetActif);


    /**
     * Calcul du résumé
     *
     * @param budget budget à calculer
     */
    void recalculSoldes(BudgetMensuel budget);

    /**
     * Récupération des libelles des opérations
     *
     * @param idCompte id du compte
     * @param auteur   utilisateur authentifié
     * @return libelles des opérations
     */
    Multi<LibelleCategorieOperation> getLibellesOperations(String idCompte, String auteur);
}
