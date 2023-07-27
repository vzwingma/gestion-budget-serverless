package io.github.vzwingma.finances.budget.serverless.services.operations.business.ports;

import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.time.Month;

/**
 * Service Provider Interface pour fournir les opérations
 * @author vzwingma
 *
 */
public interface IOperationsRepository extends ReactivePanacheMongoRepository<BudgetMensuel> {

    /**
     * Chargement du budget mensuel
     * @param mois mois du budget
     * @param annee année du budget
     * @return budget mensuel
     */
    Uni<BudgetMensuel> chargeBudgetMensuel(CompteBancaire compte, Month mois, int annee);

    /**
     * Activité Budget
     * @param idBudget id budget
     * @return budget actif
     */
    Uni<Boolean> isBudgetActif(String idBudget);


    /**
     * Chargement du budget par id
     * @param idBudget identifiant du budget
     * @return budget mensuel
     */
    Uni<BudgetMensuel> chargeBudgetMensuel(String idBudget) ;

    /**
     * Sauvegarde du budget mensuel
     * @param budget budget à sauvegarder
     * @return résultat de la sauvegarde: id du budget
     */
    Uni<BudgetMensuel> sauvegardeBudgetMensuel(BudgetMensuel budget);


    /**
     * Charge la date du premier budget déclaré pour ce compte pour cet utilisateur
     * @param compte id du compte
     * @return la date du premier budget décrit pour cet utilisateur
     */
    Uni<BudgetMensuel[]> getPremierDernierBudgets(String compte) ;

    /**
     * Chargement des libellés des dépenses
     * @param annee année du budget
     * @param idCompte id du compte
     * @return liste des libellés
     */
    Multi<String> chargeLibellesOperations(String idCompte, int annee);

}
