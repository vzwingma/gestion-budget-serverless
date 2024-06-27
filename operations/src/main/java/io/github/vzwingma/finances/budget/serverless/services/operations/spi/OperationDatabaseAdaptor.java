package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.utils.BudgetDataUtils;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContextKeyEnum;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Month;
import java.util.Arrays;
import java.util.Optional;

/**
 * Service de données en MongoDB fournissant les opérations.
 * Adapteur du port {@link IOperationsRepository}
 *
 * @author vzwingma
 */
@ApplicationScoped
public class OperationDatabaseAdaptor implements IOperationsRepository {


    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationDatabaseAdaptor.class);

    private static final String ATTRIBUT_BUDGET_ID = "_id";
    private static final String ATTRIBUT_COMPTE_ID = "idCompteBancaire";
    private static final String ATTRIBUT_ANNEE = "annee";
    private static final String ATTRIBUT_MOIS = "mois";

    @Override
    public Uni<BudgetMensuel> chargeBudgetMensuel(CompteBancaire compte, Month mois, int annee) {
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, BudgetDataUtils.getBudgetId(compte.getId(), mois, annee));
        LOGGER.info("Chargement du budget {}/{} du compte {} ", mois, annee, compte.getId());
        return find(ATTRIBUT_COMPTE_ID + " = ?1 and " + ATTRIBUT_MOIS + " = ?2 and " + ATTRIBUT_ANNEE + " = ?3", compte.getId(), mois.toString(), annee)
                .singleResultOptional()
                .onItem().transform(Optional::orElseThrow)
                .onFailure()
                .transform(e -> {
                    LOGGER.error("Erreur lors du chargement du budget", e);
                    return new BudgetNotFoundException("Erreur lors du chargement du budget pour le compte " + compte.getId() + " du mois " + mois + " de l'année " + annee);
                })
                .invoke(budget -> LOGGER.debug("-> Réception du budget {}. {} opérations", budget.getId(), budget.getListeOperations().size()));
    }

    /**
     * @param idBudget id budget
     * @return budget actif ?
     */
    @Override
    public Uni<Boolean> isBudgetActif(String idBudget) {
        LOGGER.trace("Budget {} est actif ?", idBudget);
        return chargeBudgetMensuel(idBudget).map(BudgetMensuel::isActif);
    }

    /**
     * @param idBudget identifiant du budget
     * @return budget mensuel correspondant à l'identifiant
     */
    @Override
    public Uni<BudgetMensuel> chargeBudgetMensuel(String idBudget) {

        LOGGER.info("Chargement du budget ");
        return find(ATTRIBUT_BUDGET_ID + "=?1", idBudget)
                .singleResultOptional()
                .onItem().transform(Optional::orElseThrow)
                .onFailure()
                .transform(e -> {
                    LOGGER.error("Erreur lors du chargement du budget {}", idBudget, e);
                    return new BudgetNotFoundException("Erreur lors du chargement du budget " + idBudget);
                })
                .invoke(budget -> LOGGER.debug("-> Réception du budget {}. {} opérations", budget.getId(), budget.getListeOperations().size()));
    }

    /**
     * Chargement des budgets mensuels du compte
     *
     * @param idCompte compte bancaire
     * @return budgets mensuels : flux de budgets mensuels correspondants au compte
     */
    @Override
    public Multi<BudgetMensuel> chargeBudgetsMensuels(String idCompte){
        LOGGER.info("Chargement des budgets ");
        return find(ATTRIBUT_COMPTE_ID + "=?1", idCompte, Sort.by("id"))
                .stream()
                .onFailure()
                    .transform(e -> {
                        LOGGER.error("Erreur lors du chargement des budgets de {}", idCompte, e);
                        return new BudgetNotFoundException("Erreur lors du chargement des budgets " + idCompte);
                    });
    }

    /**
     * Liste des libellés des opérations d'un compte
     *
     * @param idCompte id du  compte
     * @return libelles des opérations
     */
    @Override
    public Multi<String> getLibellesOperations(String idCompte) {
        LOGGER.info("Liste des libellés des opérations du compte {}", idCompte);

        return mongoCollection()
                .aggregate(
                        Arrays.asList(new Document("$match",
                                        new Document(ATTRIBUT_COMPTE_ID, idCompte)),
                                // Petit trick : on projete le libellé sur un document dont l'attribut sera un String dans BudgetMensuel
                                new Document("$project",
                                        new Document(ATTRIBUT_COMPTE_ID, "$listeOperations.libelle")),
                                // et on le remappe sur un des attributs String  idCompteBancaire dans BudgetMensuel
                                new Document("$unwind",
                                        new Document("path", "$" + ATTRIBUT_COMPTE_ID)
                                                .append("includeArrayIndex", "string")
                                                .append("preserveNullAndEmptyArrays", false))
                        )
                )
                .map(BudgetMensuel::getIdCompteBancaire);
    }

    /**
     * g
     * sauvegarde ou mise à jour du budget
     *
     * @param budget budget à sauvegarder
     * @return budget sauvegardé
     */
    @Override
    public Uni<BudgetMensuel> sauvegardeBudgetMensuel(BudgetMensuel budget) {
        LOGGER.info("Sauvegarde du budget du compte {} du {}/{}", budget.getIdCompteBancaire(), budget.getMois(), budget.getAnnee());
        return persistOrUpdate(budget)
                .invoke(budgetSauvegarde -> LOGGER.debug("-> Budget {} sauvegardé", budgetSauvegarde.getId()));
    }
}
