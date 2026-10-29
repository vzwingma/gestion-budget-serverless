package io.github.vzwingma.finances.budget.serverless.services.operations.business.ports;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.ProjectionBudgetSoldes;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.admin.LibelleAvantApres;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.Document;

import java.time.Instant;
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
     * <p>
     * Contrat : seule l'absence réelle du document donne une {@code BudgetNotFoundException}. Toute erreur technique
     * (Mongo, timeout...) remonte telle quelle et ne doit jamais être interprétée comme une absence.
     *
     * @param compte Le compte bancaire pour lequel le budget mensuel doit être chargé.
     * @param mois Le mois pour lequel le budget mensuel doit être chargé.
     * @param annee L'année pour laquelle le budget mensuel doit être chargé.
     * @return Un objet Uni contenant le budget mensuel si trouvé, ou en échec {@code BudgetNotFoundException} s'il est absent.
     */
    Uni<BudgetMensuel> chargeBudgetMensuel(CompteBancaire compte, Month mois, int annee);

    /**
     * Charge le dernier budget d'un compte dont le mois est inférieur ou égal au mois demandé (le mois demandé inclus),
     * en une seule requête. Absence de budget : {@code Uni} à valeur {@code null}. Erreur technique : elle remonte.
     *
     * @param idCompte identifiant du compte
     * @param mois     mois borne haute (inclus)
     * @param annee    année borne haute (incluse)
     * @return le dernier budget &le; (mois, annee), ou un {@code Uni} à valeur {@code null} si le compte n'a aucun budget antérieur
     */
    Uni<BudgetMensuel> chargeDernierBudgetAvant(String idCompte, Month mois, int annee);

    /**
     * Retourne le solde et les totaux par catégorie pour un budget mensuel (ou la liste des budgets mensuels) pour un compte et une année donnée
     * @param idCompte identifiant du compte
     * @param mois mois (facultatif)
     * @param annee année
     * @return liste des soldes et totaux par catégorie
     */
    Multi<ProjectionBudgetSoldes> chargeSoldesBudgetMensuel(String idCompte, Month mois, Integer annee);



    Uni<Instant[]> chargeIntervalleBudgets(String idCompte);

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
     * Compensation ciblée d'une écriture d'opération : retire (création) ou restaure (modification) une seule
     * opération dans {@code listeOperations}, sans réécrire le budget entier, et met à jour les soldes et totaux
     * stockés depuis le budget recalculé en mémoire.
     *
     * @param budgetRecalcule    budget en mémoire, déjà compensé et recalculé
     * @param idOperation        identifiant de l'opération à retirer ou restaurer (obligatoire, non vide)
     * @param operationOriginale ligne d'origine à restaurer ; {@code null} pour retirer l'opération (cas d'une création)
     * @param rangOriginal       position d'origine de la ligne dans {@code listeOperations}, utilisée si la ligne a
     *                           disparu de la liste (par exemple suppression concurrente) et doit être ré-insérée ;
     *                           valeur négative = inconnue (insertion en fin de liste)
     * @return complétion ; échec si le budget n'existe plus ou si l'identifiant est absent
     */
    Uni<Void> compenseOperation(BudgetMensuel budgetRecalcule, String idOperation, LigneOperation operationOriginale, int rangOriginal);

    /**
     * Liste des libellés bruts des opérations d'un compte, regroupés côté MongoDB avec leur nombre d'occurrences
     * (documents {@code {_id: {libelle, categorieId, ssCategorieId}, n}})
     *
     * @param idCompte id du  compte
     * @return groupes de libellés bruts avec leur compteur
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
