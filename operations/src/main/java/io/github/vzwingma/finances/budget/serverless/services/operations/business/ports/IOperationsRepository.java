package io.github.vzwingma.finances.budget.serverless.services.operations.business.ports;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LibelleAvantApres;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.time.Month;
import java.util.List;

/**
 * Service Provider Interface pour fournir les opérations
 *
 * @author vzwingma
 */
public interface IOperationsRepository extends ReactivePanacheMongoRepository<BudgetMensuel> {

    /**
     * Chargement du budget mensuel
     *
     * @param mois  mois du budget
     * @param annee année du budget
     * @return budget mensuel
     */
    Uni<BudgetMensuel> chargeBudgetMensuel(CompteBancaire compte, Month mois, int annee);

    /**
     * Activité Budget
     *
     * @param idBudget id budget
     * @return budget actif
     */
    Uni<Boolean> isBudgetActif(String idBudget);


    /**
     * Chargement du budget par id
     *
     * @param idBudget identifiant du budget
     * @return budget mensuel
     */
    Uni<BudgetMensuel> chargeBudgetMensuel(String idBudget);

    /**
     * Chargement des budgets mensuels du compte
     *
     * @param idCompte compte bancaire
     * @return budgets mensuels : flux de budgets mensuels correspondants au compte
     */
    Multi<BudgetMensuel> chargeBudgetsMensuels(String idCompte);
    /**
     * Sauvegarde du budget mensuel
     *
     * @param budget budget à sauvegarder
     * @return résultat de la sauvegarde: id du budget
     */
    Uni<BudgetMensuel> sauvegardeBudgetMensuel(BudgetMensuel budget);

    /**
     * Liste des libellés des opérations d'un compte
     *
     * @param idCompte id du  compte
     * @return libelles des opérations
     */
    Multi<String> getLibellesOperations(String idCompte);

    /**
     * Mise à jour des libellés des opérations d'un compte pour les homogénéiser
     *
     * @param idCompte           id du compte
     * @param libellesToOverride liste des libellés à mettre à jour
     * @return liste des budgets mensuels mis à jour
     */
    Multi<BudgetMensuel> overrideLibellesOperations(String idCompte, List<LibelleAvantApres> libellesToOverride);
}
