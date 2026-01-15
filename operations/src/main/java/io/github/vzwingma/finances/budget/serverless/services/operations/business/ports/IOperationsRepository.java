package io.github.vzwingma.finances.budget.serverless.services.operations.business.ports;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.ADMIN.LibelleAvantApres;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.projections.ProjectionBudgetSoldes;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.Document;

import java.time.Month;
import java.util.List;

/**
 * Service Provider Interface pour fournir les opérations
 *
 * @author vzwingma
 */
public interface IOperationsRepository extends ReactivePanacheMongoRepository<BudgetMensuel> {

    /**
     * Cette méthode est utilisée pour charger le budget mensuel d'un compte bancaire spécifique pour un mois et une année donnés.
     * Elle peut également inclure les opérations dans le budget chargé en fonction du paramètre includeOperations.
     *
     * @param compte Le compte bancaire pour lequel le budget mensuel doit être chargé.
     * @param mois Le mois pour lequel le budget mensuel doit être chargé.
     * @param annee L'année pour laquelle le budget mensuel doit être chargé.
     * @return Un objet Uni contenant le budget mensuel si trouvé.
     */
    Uni<BudgetMensuel> chargeBudgetMensuel(CompteBancaire compte, Month mois, int annee);
    /**
     * Retourne le solde et les totaux par catégorie pour un budget mensuel (ou la liste des budgets mensuels) pour un compte et une année donnée
     * @param idCompte identifiant du compte
     * @param mois mois (facultatif)
     * @param annee année
     * @return liste des soldes et totaux par catégorie
     */
    Multi<ProjectionBudgetSoldes> chargeSoldesBudgetMensuel(String idCompte, Month mois, Integer annee);
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
    Multi<Document> getLibellesOperations(String idCompte);

    /**
     * Mise à jour des libellés des opérations d'un compte pour les homogénéiser
     *
     * @param idCompte           id du compte
     * @param libellesToOverride liste des libellés à mettre à jour
     * @return liste des budgets mensuels mis à jour
     */
    Multi<BudgetMensuel> overrideLibellesOperations(String idCompte, List<LibelleAvantApres> libellesToOverride);
}
