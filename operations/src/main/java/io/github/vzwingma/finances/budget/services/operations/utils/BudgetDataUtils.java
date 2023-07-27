package io.github.vzwingma.finances.budget.services.operations.utils;

import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import io.github.vzwingma.finances.budget.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.services.operations.business.model.operation.OperationPeriodiciteEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

/**
 * Utilitaire de data
 * @author vzwingma
 *
 */
public class BudgetDataUtils {

	protected static final Logger LOGGER = LoggerFactory.getLogger( BudgetDataUtils.class );


	private BudgetDataUtils(){
		// constructeur privé
	}


	/**
	 * @param idCompte id compte bancaire
	 * @param mois mois
	 * @param annee année
	 * @return id de budget
	 */
	public static String getBudgetId(String idCompte, Month mois, int annee){
		return String.format("%s_%s_%s", idCompte, annee, String.format("%02d", mois.getValue()));
	}
	/**
	 * @param budgetId id budget
	 * @return la valeur de l'année à partir de l'id
	 * @throws BudgetNotFoundException budget introuvable car erreur d'id
	 */
	public static Month getMoisFromBudgetId(String budgetId) throws BudgetNotFoundException {
		if(budgetId != null){
			try {
				return Month.of(Integer.parseInt(budgetId.substring(budgetId.lastIndexOf('_') + 1)));
			}
			catch (Exception e) {
				// Erreur dans l'id
				throw new BudgetNotFoundException("Erreur de mois dans l'id du budget " + budgetId + ". Données (mois) incohérentes");
			}
		}
		return null;
	}
	/**
	 * @param budgetId id budget
	 * @return la valeur de l'année à partir de l'id
	 * @throws BudgetNotFoundException budget introuvable car erreur d'id
	 */
	public static String getCompteFromBudgetId(String budgetId) throws BudgetNotFoundException {
		if(budgetId != null){
			try {
				return budgetId.substring(0, budgetId.indexOf('_'));
			}
			catch (Exception e) {
				// Erreur dans l'id
				throw new BudgetNotFoundException("Erreur de compte dans l'id du budget " + budgetId + ". Données (compte) incohérentes");
			}
		}
		return null;
	}

	/**
	 * Extrait l'année de l'id budget
	 * @param budgetId id budget
	 * @return la valeur de l'année à partir de l'id
	 * @throws BudgetNotFoundException budget introuvable car erreur d'id
	 */
	public static Integer getAnneeFromBudgetId(String budgetId) throws BudgetNotFoundException{
		if(budgetId != null){
			try {
				return Integer.parseInt(budgetId.substring(budgetId.indexOf('_') + 1, budgetId.lastIndexOf('_')));
			}
			catch (Exception e) {
				// Erreur dans l'id
				throw new BudgetNotFoundException("Erreur d'année dans l'id du budget " + budgetId + ". Données (année) incohérentes");
			}
		}
		return null;
	}



	/**
	 * Raz calculs
	 * @param budget : budget à modifier
	 */
	public static void razCalculs(BudgetMensuel budget){
		budget.getTotauxParCategories().clear();
		budget.getTotauxParSSCategories().clear();
		budget.getSoldes().setSoldeAtMaintenant(budget.getSoldes().getSoldeAtFinMoisPrecedent());
		budget.getSoldes().setSoldeAtFinMoisCourant(budget.getSoldes().getSoldeAtFinMoisPrecedent());
	}


	/**
	 * Ajout du solde à fin du mois courant
	 * @param soldes soldes du budget à modifier
	 * @param soldeAAjouter  valeur à ajouter
	 */
	public static void ajouteASoldeNow(BudgetMensuel.Soldes soldes, double soldeAAjouter) {
		soldes.setSoldeAtMaintenant(soldes.getSoldeAtMaintenant() + soldeAAjouter);
	}

	/**
	 * Ajout du solde à fin du mois courant
	 * @param soldes soldes du budget à modifier
	 * @param soldeAAjouter  valeur à ajouter
	 */
	public static void ajouteASoldeFin(BudgetMensuel.Soldes soldes, double soldeAAjouter) {
		soldes.setSoldeAtFinMoisCourant(soldes.getSoldeAtFinMoisCourant() + soldeAAjouter);
	}



	/**
	 * Clone d'une ligne opération
	 * @return Ligne dépense clonée
	 * @param ligneOperation : ligneOpérations à cloner
	 */
	public static LigneOperation cloneOperationToMoisSuivant(LigneOperation ligneOperation) {
		LigneOperation ligneOperationClonee = new LigneOperation();
		ligneOperationClonee.setId(UUID.randomUUID().toString());
		ligneOperationClonee.setLibelle(ligneOperation.getLibelle());
		if(ligneOperation.getCategorie() != null) {
			LigneOperation.Categorie cat = new LigneOperation.Categorie();
			cat.setId(ligneOperation.getCategorie().getId());
			cat.setLibelle(ligneOperation.getCategorie().getLibelle());
			ligneOperationClonee.setCategorie(cat);
		}
		if(ligneOperation.getSsCategorie() != null) {
			LigneOperation.Categorie ssCatClonee = new LigneOperation.Categorie();
			ssCatClonee.setId(ligneOperation.getSsCategorie().getId());
			ssCatClonee.setLibelle(ligneOperation.getSsCategorie().getLibelle());
			ligneOperationClonee.setSsCategorie(ssCatClonee);
		}
		ligneOperationClonee.setAutresInfos(new LigneOperation.AddInfos());
		ligneOperationClonee.getAutresInfos().setDateMaj(LocalDateTime.now());
		ligneOperationClonee.getAutresInfos().setDateOperation(null);
		ligneOperationClonee.setEtat(OperationEtatEnum.PREVUE);
		ligneOperationClonee.setTypeOperation(ligneOperation.getTypeOperation());
		ligneOperationClonee.putValeurFromSaisie(Math.abs(ligneOperation.getValeur()));
		ligneOperationClonee.setTagDerniereOperation(false);
		return ligneOperationClonee;
	}


	/**
	 * Clone d'une ligne opération
	 * @return Ligne dépense clonée
	 */
	public static List<LigneOperation> cloneOperationPeriodiqueToMoisSuivant(final LigneOperation ligneOperation) {
		List<LigneOperation> lignesOperationClonees = new ArrayList<>();

		LigneOperation ligneOperationClonee = cloneOperationToMoisSuivant(ligneOperation);

		// Recalcul des mensualités
		if(ligneOperation.getMensualite() != null && ligneOperation.getMensualite().getPeriode() != null){
			LigneOperation.Mensualite mensualiteClonee = new LigneOperation.Mensualite();
			mensualiteClonee.setPeriode(ligneOperation.getMensualite().getPeriode());

			int prochaineMensualite = ligneOperation.getMensualite().getProchaineEcheance() -1 ;


			// Si une opération était à échéance, mais a été reportée - on la réinjecte, en retard
			if(ligneOperation.getMensualite().getProchaineEcheance() == ligneOperation.getMensualite().getPeriode().getNbMois()
			&& OperationEtatEnum.REPORTEE.equals(ligneOperation.getEtat())){
				if(LOGGER.isWarnEnabled() && ligneOperation.getMensualite() != null && ligneOperation.getMensualite().getPeriode() != null){
					LOGGER.warn("L'opération périodique {} est reportée : en retard", ligneOperation.getMensualite().getPeriode().name());
				}
				LigneOperation ligneOperationEcheanceReportee = cloneOperationToMoisSuivant(ligneOperation);
				ligneOperationEcheanceReportee.setLibelle("[En Retard] " + ligneOperation.getLibelle());
				LigneOperation.Mensualite echeanceReportee = new LigneOperation.Mensualite();
				echeanceReportee.setPeriode(OperationPeriodiciteEnum.PONCTUELLE);
				echeanceReportee.setProchaineEcheance(-1);
				ligneOperationEcheanceReportee.setMensualite(echeanceReportee);
				lignesOperationClonees.add(ligneOperationEcheanceReportee);
			}
			// Si la mensualité arrive à échéance, elle est prévue, et la prochaine échéance est réinitalisée
			if(prochaineMensualite == 0){
				ligneOperationClonee.setEtat(OperationEtatEnum.PREVUE);
				mensualiteClonee.setProchaineEcheance(mensualiteClonee.getPeriode().getNbMois());
			}
			// Si l'échéance est dans le passé, on laisse la mensualité de base et prévue - tagguée en retard
			else if(prochaineMensualite < 0){
				ligneOperationClonee.setEtat(OperationEtatEnum.PREVUE);
				mensualiteClonee.setProchaineEcheance(prochaineMensualite);
			}
			// Si l'échéance est dans le futur, on laisse la mensualité de base et reportée
			else{
				ligneOperationClonee.setEtat(OperationEtatEnum.PLANIFIEE);
				mensualiteClonee.setProchaineEcheance(prochaineMensualite);
			}
			ligneOperationClonee.setMensualite(mensualiteClonee);
		}

		lignesOperationClonees.add(ligneOperationClonee);
		return lignesOperationClonees;
	}



	/**
	 * @param listeOperations liste des opérations
	 * @return date max d'une liste de dépenses
	 */
	public static LocalDate getMaxDateListeOperations(List<LigneOperation> listeOperations){

		LocalDate localDateDerniereOperation = BudgetDateTimeUtils.localDateNow();

		if(listeOperations != null && !listeOperations.isEmpty()){
			// Comparaison de date

			Comparator <LigneOperation> comparator = Comparator.comparing(LigneOperation::retrieveDateOperation, (date1, date2) -> {
				if(date1 == null){
					return 1;
				}
				else if(date2 == null){
					return -1;
				} else if (date1.equals(date2)){
					return 0;
				} else{
					return date1.isBefore(date2) ? -1 : 1;
				}
			});
			Optional<LigneOperation> maxDate = listeOperations.stream().max(comparator);
			if(maxDate.isPresent() && maxDate.get().retrieveDateOperation() != null){
				localDateDerniereOperation = maxDate.get().retrieveDateOperation();
			}
		}
		return localDateDerniereOperation;
	}


	/**
	 * @param valeurS valeur en String
	 * @return la valeur d'un String en double
	 */
	public static Double getValueFromString(String valeurS){

		if(valeurS != null){
			valeurS = valeurS.replace(",", ".");
			try{
				return Double.valueOf(valeurS);
			}
			catch(Exception e){
				// Erreur de parsing
			}
		}
		return null;
	}



	/**
	 * @param valeurS valeur en String
	 * @return la valeur du String sans le tag [xxx]
	 */
	public static String deleteTagFromString(String valeurS){

		if(valeurS != null){
			return valeurS.replaceAll("\\[.*]", "").trim();
		}
		return null;
	}

}
