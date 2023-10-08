package io.github.vzwingma.finances.budget.serverless.services.operations.business;


import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.IdsCategoriesEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.utils.BudgetDataUtils;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContextKeyEnum;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.TotauxCategorie;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationTypeEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IBudgetAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service fournissant les calculs de budget sur les opérations
 * @author vzwingma
 *
 */
@ApplicationScoped
@NoArgsConstructor @Setter
public class OperationsService implements IOperationsAppProvider {


	// Logger
	private static final Logger LOGGER = LoggerFactory.getLogger(OperationsService.class);

	@Inject
	IOperationsRepository dataOperationsProvider;

	@Inject
	IBudgetAppProvider budgetService;





	/**
	 * Calcul des soldes
	 * @param operations opérations
	 * @param soldes soldes
	 * @param totauxCategorieMap map des totaux par catégorie
	 * @param totauxSsCategoriesMap map des totaux par sous catégorie
	 */
	@Override
	public void calculSoldes(List<LigneOperation> operations, BudgetMensuel.Soldes soldes, Map<String, TotauxCategorie> totauxCategorieMap, Map<String, TotauxCategorie> totauxSsCategoriesMap) {

		for (LigneOperation operation : operations) {
			LOGGER.trace("     > {}", operation);
			Double valeurOperation = operation.getValeur();

			// Calcul par catégorie
			calculBudgetTotalCategories(totauxCategorieMap, operation);
			// Calcul par sous catégories
			calculBudgetTotalSsCategories(totauxSsCategoriesMap, operation);
			// Calcul des totaux
			if(operation.getEtat().equals(OperationEtatEnum.REALISEE)){
				BudgetDataUtils.ajouteASoldeNow(soldes, valeurOperation);
				BudgetDataUtils.ajouteASoldeFin(soldes, valeurOperation);
			}
			else if(operation.getEtat().equals(OperationEtatEnum.PREVUE)){
				BudgetDataUtils.ajouteASoldeFin(soldes, valeurOperation);
			}
		}
		LOGGER.debug("Solde prévu\t| {} | {}", soldes.getSoldeAtMaintenant(), soldes.getSoldeAtFinMoisCourant());

	}


	/**
	 * Calcul du total de la catégorie du budget via l'opération en cours
	 * @param totauxCategorieMap à calculer
	 * @param operation opération à traiter
	 */
	private void calculBudgetTotalCategories(Map<String, TotauxCategorie> totauxCategorieMap, LigneOperation operation) {

		if(operation.getCategorie() != null && operation.getCategorie().getId() != null) {
			Double valeurOperation = operation.getValeur();
			TotauxCategorie valeursCat = new TotauxCategorie();
			if(totauxCategorieMap.get(operation.getCategorie().getId()) != null){
				valeursCat = totauxCategorieMap.get(operation.getCategorie().getId());
			}
			valeursCat.setLibelleCategorie(operation.getCategorie().getLibelle());
			if(operation.getEtat().equals(OperationEtatEnum.REALISEE)){
				valeursCat.ajouterATotalAtMaintenant(valeurOperation);
				valeursCat.ajouterATotalAtFinMoisCourant(valeurOperation);
			}
			else if(operation.getEtat().equals(OperationEtatEnum.PREVUE)){
				valeursCat.ajouterATotalAtFinMoisCourant(valeurOperation);
			}
			LOGGER.trace("Total par catégorie [idCat={} : {}]", operation.getCategorie().getId(), valeursCat);
			totauxCategorieMap.put(operation.getCategorie().getId(), valeursCat);
		}
		else {
			LOGGER.warn("L'opération [{}] n'a pas de catégorie [{}]", operation, operation.getCategorie() );
		}
	}

	/**
	 * Calcul du total de la sous catégorie du budget via l'opération en cours
	 * @param totauxSsCategoriesMap  à calculer
	 * @param operation opération à traiter
	 *
	 * */
	private void calculBudgetTotalSsCategories(Map<String, TotauxCategorie> totauxSsCategoriesMap, LigneOperation operation) {
		if(operation.getSsCategorie() != null && operation.getSsCategorie().getId() != null) {
			Double valeurOperation = operation.getValeur();
			TotauxCategorie valeursSsCat = new TotauxCategorie();
			if( totauxSsCategoriesMap.get(operation.getSsCategorie().getId()) != null){
				valeursSsCat = totauxSsCategoriesMap.get(operation.getSsCategorie().getId());
			}
			valeursSsCat.setLibelleCategorie(operation.getSsCategorie().getLibelle());
			if(operation.getEtat().equals(OperationEtatEnum.REALISEE)){
				valeursSsCat.ajouterATotalAtMaintenant(valeurOperation);
				valeursSsCat.ajouterATotalAtFinMoisCourant(valeurOperation);
			}
			if(operation.getEtat().equals(OperationEtatEnum.PREVUE)){
				valeursSsCat.ajouterATotalAtFinMoisCourant(valeurOperation);
			}
			LOGGER.trace("Total par ss catégorie [idSsCat={} : {}]", operation.getSsCategorie().getId(), valeursSsCat);
			totauxSsCategoriesMap.put(operation.getSsCategorie().getId(), valeursSsCat);
		}
		else {
			LOGGER.warn("L'opération [{}]  n'a pas de sous-catégorie [{}]", operation, operation.getSsCategorie() );
		}
	}


	@Override
	public void addOrReplaceOperation(List<LigneOperation> operations, LigneOperation ligneOperation, String auteur, CategorieOperations ssCategorieRemboursement) throws DataNotFoundException {
		BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.OPERATION, ligneOperation.getId());
		// Si mise à jour d'une opération, on l'enlève
		LigneOperation ligneOperationToUpdate = operations.stream().filter(op -> op.getId().equals(ligneOperation.getId())).findFirst().orElse(null);

		int rangMaj = operations.indexOf(ligneOperation);
		operations.removeIf(op -> op.getId().equals(ligneOperation.getId()));

		if (ligneOperation.getEtat() != null) {
			LigneOperation ligneUpdatedOperation = completeOperationAttributes(ligneOperation, auteur);
			LigneOperation ligneUpdatedPeriodicOperation = completePeriodiciteOperation(ligneUpdatedOperation, ligneOperationToUpdate);
			if (rangMaj >= 0) {
				LOGGER.info("Mise à jour de l'opération : {}", ligneUpdatedPeriodicOperation);
				operations.add(rangMaj, ligneUpdatedPeriodicOperation);
			} else {
				LOGGER.info("Ajout de l'opération : {}", ligneUpdatedPeriodicOperation);
				operations.add(ligneUpdatedPeriodicOperation);

				// Création du remboursement si besoin
				if (ligneUpdatedPeriodicOperation.getSsCategorie() != null
						&& ligneUpdatedPeriodicOperation.getCategorie() != null
						&& IdsCategoriesEnum.FRAIS_REMBOURSABLES.getId().equals(ligneUpdatedPeriodicOperation.getCategorie().getId())) {

					if(ssCategorieRemboursement != null){
						LigneOperation operationRemboursement = createOperationRemboursement(ligneUpdatedPeriodicOperation, auteur, ssCategorieRemboursement);
						LOGGER.info("Ajout de l'opération de remboursement : {}", operationRemboursement);
						operations.add(operationRemboursement);
					}
					else{
						throw new DataNotFoundException("Catégorie Remboursement non trouvée. Impossible de créer l'opération de remboursement");
					}
				}
			}
		} else {
			LOGGER.info("Suppression d'une opération : {}", ligneOperation);
		}
	}



	/**
	 * @param ligneOperation opération à compléter (ou à mettre à jour)
	 * @return ligneOperation màj
	 */
	private LigneOperation completeOperationAttributes(LigneOperation ligneOperation, String auteur) {
		// Autres infos
		if(ligneOperation.getAutresInfos() == null){
			ligneOperation.setAutresInfos(new LigneOperation.AddInfos());
		}
		ligneOperation.getAutresInfos().setDateMaj(LocalDateTime.now());
		ligneOperation.getAutresInfos().setAuteur(auteur);

		// Date opération suivant Etat
		if(OperationEtatEnum.REALISEE.equals(ligneOperation.getEtat())
				&& ligneOperation.getAutresInfos().getDateOperation() == null){
			ligneOperation.getAutresInfos().setDateOperation(LocalDate.now());
		} else if (OperationEtatEnum.REPORTEE.equals(ligneOperation.getEtat())) {
			ligneOperation.getAutresInfos().setDateOperation(null);
		}
		return ligneOperation;
	}




	/**
	 * Calcul de la périodidité d'une opération
	 * @param ligneOperation opération à compléter (ou à mettre à jour)
	 * @param oldOperationToUpdate ancienne version de l'opération si elle existe
	 * @return ligneOperation màj
	 */
	private LigneOperation completePeriodiciteOperation(LigneOperation ligneOperation, LigneOperation oldOperationToUpdate) {

		// Périodicité
		if(ligneOperation.getMensualite() != null){
			LigneOperation.Mensualite mensualite = ligneOperation.getMensualite();

			// Changement de périodicité, on reporte la prochaine échéance
			if(oldOperationToUpdate != null
					&& oldOperationToUpdate.getMensualite() != null
					&& oldOperationToUpdate.getMensualite().getPeriode() != mensualite.getPeriode()){
				LOGGER.debug("L'opération change de périodicité : {} -> {}", oldOperationToUpdate.getMensualite().getPeriode(), mensualite.getPeriode());
				mensualite.setProchaineEcheance(-1);
			}
			// Init de la prochaine échéance
			if(mensualite.getProchaineEcheance() == -1  && mensualite.getPeriode().getNbMois() > 0){
				mensualite.setProchaineEcheance(mensualite.getPeriode().getNbMois());
			}
			// Raz de la prochaine échéance
			else if(mensualite.getPeriode().getNbMois() == 0){
				mensualite.setProchaineEcheance(-1);
			}
		}

		return ligneOperation;
	}


	/**
	 * Si frais remboursable : ajout du remboursement en prévision
	 * #62 : et en mode création
	 * @param operationSource ligne d'opération source, ajoutée
	 * @return ligne de remboursement
	 */

	private LigneOperation createOperationRemboursement(LigneOperation operationSource, String auteur, CategorieOperations ssCategorieRemboursement){
		// Workaround de #26
		if(ssCategorieRemboursement != null && ssCategorieRemboursement.getCategorieParente() == null){
			CategorieOperations.CategorieParente categorieParente = new CategorieOperations.CategorieParente(IdsCategoriesEnum.VIREMENT.getId(), "Virement");
			ssCategorieRemboursement.setCategorieParente(categorieParente);
		}

		// Si l'opération est une opération de remboursement, on ajoute la catégorie de remboursement
		return completeOperationAttributes(new LigneOperation(
							ssCategorieRemboursement,
							operationSource.getLibelle(),
							OperationTypeEnum.CREDIT,
							Math.abs(operationSource.getValeur()),
							OperationEtatEnum.REPORTEE),
							auteur);
	}




	@Override
	public void addOperationIntercompte(List<LigneOperation> operations, LigneOperation ligneOperationSource, String libelleOperationCible, String auteur){

		// #59 : Cohérence des états
		OperationEtatEnum etatDepenseTransfert;
		switch (ligneOperationSource.getEtat()) {
			case ANNULEE -> etatDepenseTransfert = OperationEtatEnum.ANNULEE;
			case REPORTEE -> etatDepenseTransfert = OperationEtatEnum.REPORTEE;
			// pour tous les autres cas, on prend l'état de l'opération source
			default -> etatDepenseTransfert = OperationEtatEnum.PREVUE;
		}
		// fix #28 : Opération source est forcément en DEBIT
		ligneOperationSource.setTypeOperation(OperationTypeEnum.DEPENSE);

		LigneOperation.Mensualite mensualiteTransfert = null;
		if(ligneOperationSource.getMensualite() != null ){
			mensualiteTransfert= new LigneOperation.Mensualite();
			mensualiteTransfert.setPeriode(ligneOperationSource.getMensualite().getPeriode());
			mensualiteTransfert.setProchaineEcheance(ligneOperationSource.getMensualite().getProchaineEcheance());
		}

		LigneOperation ligneTransfert = completeOperationAttributes(
				new LigneOperation(
					ligneOperationSource.getCategorie(),
					ligneOperationSource.getSsCategorie(),
					libelleOperationCible,
					OperationTypeEnum.CREDIT,
					Math.abs(ligneOperationSource.getValeur()),
					etatDepenseTransfert, mensualiteTransfert),
					auteur);
		LOGGER.debug("Ajout de l'opération [{}] dans le budget", ligneTransfert);

		operations.add(ligneTransfert);
	}


	@Override
	public void deleteOperation(List<LigneOperation> operations, String idOperation) {
		// Si suppression d'une opération, on l'enlève
		if(operations.removeIf(op -> op.getId().equals(idOperation))) {
			LOGGER.info("Suppression d'une Opération : {}", idOperation);
		}
		else {
			LOGGER.warn("[idBudget={}][idOperation={}] Impossible de supprimer l'opération. Introuvable", operations, idOperation);
		}
	}
}
