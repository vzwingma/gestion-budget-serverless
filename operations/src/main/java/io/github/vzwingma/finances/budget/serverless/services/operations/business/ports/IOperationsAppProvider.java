package io.github.vzwingma.finances.budget.serverless.services.operations.business.ports;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.TotauxCategorie;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LibelleCategorieOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.services.communs.data.model.SsCategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.smallrye.mutiny.Multi;

import java.util.List;
import java.util.Map;

/**
 * Application Provider Interface des opérations
 */
public interface IOperationsAppProvider {

    /**
     * Calcul du résumé
     *
     * @param operations            opérations
     * @param soldes                soldes
     * @param totauxCategorieMap    map des totaux par catégorie
     * @param totauxSsCategoriesMap map des totaux par sous catégorie
     */
    void calculSoldes(List<LigneOperation> operations, BudgetMensuel.Soldes soldes, Map<String, TotauxCategorie> totauxCategorieMap, Map<String, TotauxCategorie> totauxSsCategoriesMap, Map<String, TotauxCategorie> totauxTypesCategoriesMap);

    /**
     * Ajout d'une ligne de virement interne (rentrée d'argent)
     *
     * @param operations            liste des opérations à mettre à jour budget
     * @param ligneOperationSource  ligne de dépense, source, pour créer une nouvelle opération de virement
     * @param libelleOperationCible libelle de la nouvelle opération
     * @param auteur                auteur de l'action
     *                              liste des opérations à mettre à jour dans le budget, avec l'intercompte
     */
    void addOperationVirementInterne(List<LigneOperation> operations, LigneOperation ligneOperationSource, String libelleOperationCible, String auteur);


    /**
     * Suppression d'une opération
     *
     * @param operations  identifiant de budget
     * @param idOperation ligne opération
     */
    void deleteOperation(List<LigneOperation> operations, String idOperation);

    /**
     * Mise à jour d'une ligne de dépense dans la liste d'un budget
     *
     * @param operations               liste des opérations à mettre à jour budget
     * @param auteur                   auteur de l'action
     * @param ligneOperation           ligne de dépense
     * @param ssCategorieRemboursement catégorie Remboursement
     */
    void addOrReplaceOperation(List<LigneOperation> operations, LigneOperation ligneOperation, String auteur, SsCategorieOperations ssCategorieRemboursement) throws DataNotFoundException;

    /**
     * Récupération des libellés des opérations
     *
     * @param idCompte id du compte
     * @return liste des libellés des opérations
     */
    Multi<LibelleCategorieOperation> getLibellesOperations(String idCompte);
}
