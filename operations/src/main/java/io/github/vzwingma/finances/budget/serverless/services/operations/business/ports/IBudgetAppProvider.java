package io.github.vzwingma.finances.budget.serverless.services.operations.business.ports;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
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
     * Création d'une opération intercompte
     *
     * @param idBudget            id du budget source
     * @param ligneOperation      opération intercompte source
     * @param idCompteDestination id du compte destination
     * @return budget mensuel source modifié
     */
    Uni<BudgetMensuel> createOperationsIntercomptes(String idBudget, LigneOperation ligneOperation, String idCompteDestination, String auteur);

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
     * Récupération des libelles des opérations
     *
     * @param idCompte id du compte
     * @return libelles des opérations
     */
    Multi<String> getLibellesOperations(String idCompte);
}
