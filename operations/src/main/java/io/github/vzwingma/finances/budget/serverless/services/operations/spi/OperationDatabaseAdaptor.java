package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.utils.BudgetDataUtils;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContextKeyEnum;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Month;
import java.util.Optional;

/**
 * Service de données en MongoDB fournissant les opérations.
 * Adapteur du port {@link IOperationsRepository}
 * @author vzwingma
 *
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
	 *
	 * @param idBudget id budget
	 * @return budget actif ?
	 */
	@Override
	public Uni<Boolean> isBudgetActif(String idBudget) {
		LOGGER.trace("Budget {} est actif ?", idBudget);
		return chargeBudgetMensuel(idBudget).map(BudgetMensuel::isActif);
	}

	/**
	 *
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
	 * Liste des lignes opérations d'un budget
	 * @param budget budget concerné
	 * @return liste des opérations

	public Multi<LigneOperation> chargerLignesOperations(String idBudget) {
		return chargeBudgetMensuel(idBudget)
				.map(budget -> budget.getListeOperations())
				.onItem().transformToMulti(ligneOperations -> Multi.createFrom().iterable(ligneOperations));
	}
	 */

	@Override
	public Uni<BudgetMensuel> sauvegardeBudgetMensuel(BudgetMensuel budget) {
		LOGGER.info("Sauvegarde du budget du compte {} du {}/{}", budget.getIdCompteBancaire(), budget.getMois(), budget.getAnnee());
		return persistOrUpdate(budget)
				.invoke(budgetSauvegarde -> LOGGER.debug("-> Budget {} sauvegardé", budgetSauvegarde.getId()));
	}

	@Override
	@Deprecated (forRemoval = true, since = "19.3")
	public Uni<BudgetMensuel[]> getPremierDernierBudgets(String idCompte) {
		return list(ATTRIBUT_COMPTE_ID, Sort.ascending(ATTRIBUT_ANNEE, ATTRIBUT_BUDGET_ID), idCompte)
				.flatMap(b -> {
					if(b != null && !b.isEmpty()){
						return Uni.createFrom().item(new BudgetMensuel[] { b.get(0), b.get(b.size() - 1) });
					}
					else{
						return Uni.createFrom().failure(new DataNotFoundException("Erreur lors du chargement des intervalles de budgets de " + idCompte));
					}
				})
				.invoke(budget -> LOGGER.info("-> Réception de l'intervalle de budgets -> {} / {}", budget[0].getId(), budget[1].getId()));
	}

	@Override
	public Multi<String> chargeLibellesOperations(String idCompte, int annee) {
		LOGGER.info("Chargement des libellés des dépenses du compte pour l'année {}", annee);
		final String query = ATTRIBUT_COMPTE_ID + "=?1 and " + ATTRIBUT_ANNEE + "= ?2";
		return find(query, idCompte, annee)
				.stream()
				.map(BudgetMensuel::getListeOperations)
				.map(listOperations -> listOperations.parallelStream().map(LigneOperation::getLibelle).toList())
				.flatMap(listLibelles -> Multi.createFrom().iterable(listLibelles));
	}
}
