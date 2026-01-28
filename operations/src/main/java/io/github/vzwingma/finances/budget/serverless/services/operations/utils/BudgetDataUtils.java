package io.github.vzwingma.finances.budget.serverless.services.operations.utils;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.IdsCategoriesEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.*;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

/**
 * Utilitaire de data
 *
 * @author vzwingma
 */
public class BudgetDataUtils {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BudgetDataUtils.class);


    private BudgetDataUtils() {
        // constructeur privé
    }


    /**
     * @param budgetId id budget
     * @return la valeur de l'année à partir de l'id
     * @throws BudgetNotFoundException budget introuvable car erreur d'id
     */
    public static Month getMoisFromBudgetId(String budgetId) throws BudgetNotFoundException {
        if (budgetId != null) {
            try {
                return Month.of(Integer.parseInt(budgetId.substring(budgetId.lastIndexOf('_') + 1)));
            } catch (Exception e) {
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
        if (budgetId != null) {
            try {
                return budgetId.substring(0, budgetId.indexOf('_'));
            } catch (Exception e) {
                // Erreur dans l'id
                throw new BudgetNotFoundException("Erreur de compte dans l'id du budget " + budgetId + ". Données (compte) incohérentes");
            }
        }
        return null;
    }

    /**
     * Extrait l'année de l'id budget
     *
     * @param budgetId id budget
     * @return la valeur de l'année à partir de l'id
     * @throws BudgetNotFoundException budget introuvable car erreur d'id
     */
    public static Integer getAnneeFromBudgetId(String budgetId) throws BudgetNotFoundException {
        if (budgetId != null) {
            try {
                return Integer.parseInt(budgetId.substring(budgetId.indexOf('_') + 1, budgetId.lastIndexOf('_')));
            } catch (Exception e) {
                // Erreur dans l'id
                throw new BudgetNotFoundException("Erreur d'année dans l'id du budget " + budgetId + ". Données (année) incohérentes");
            }
        }
        return null;
    }


    /**
     * Raz calculs
     *
     * @param budget : budget à modifier
     */
    public static void razCalculs(BudgetMensuel budget) {
        budget.getTotauxParCategories().clear();
        budget.getTotauxParSSCategories().clear();
        budget.getTotauxParTypeCategories().clear();
        budget.getSoldes().setSoldeAtMaintenant(budget.getSoldes().getSoldeAtFinMoisPrecedent());
        budget.getSoldes().setSoldeAtFinMoisCourant(budget.getSoldes().getSoldeAtFinMoisPrecedent());
    }


    /**
     * Ajout du solde à fin du mois courant
     *
     * @param soldes        soldes du budget à modifier
     * @param soldeAAjouter valeur à ajouter
     */
    public static void ajouteASoldeNow(BudgetMensuel.Soldes soldes, double soldeAAjouter) {
        soldes.setSoldeAtMaintenant(soldes.getSoldeAtMaintenant() + soldeAAjouter);
    }

    /**
     * Ajout du solde à fin du mois courant
     *
     * @param soldes        soldes du budget à modifier
     * @param soldeAAjouter valeur à ajouter
     */
    public static void ajouteASoldeFin(BudgetMensuel.Soldes soldes, double soldeAAjouter) {
        soldes.setSoldeAtFinMoisCourant(soldes.getSoldeAtFinMoisCourant() + soldeAAjouter);
    }


    /**
     * Clone d'une ligne opération
     *
     * @param ligneOperation : ligneOpérations à cloner
     * @return Ligne dépense clonée
     */
    public static LigneOperation cloneOperationToMoisSuivant(LigneOperation ligneOperation) {
        LigneOperation ligneOperationClonee = new LigneOperation();
        ligneOperationClonee.setId(UUID.randomUUID().toString());
        ligneOperationClonee.setLibelle(ligneOperation.getLibelle());
        if (ligneOperation.getCategorie() != null) {
            LigneOperation.Categorie cat = new LigneOperation.Categorie();
            cat.setId(ligneOperation.getCategorie().getId());
            cat.setLibelle(ligneOperation.getCategorie().getLibelle());
            ligneOperationClonee.setCategorie(cat);
        }
        if (ligneOperation.getSsCategorie() != null) {
            LigneOperation.SsCategorie ssCatClonee = new LigneOperation.SsCategorie();
            ssCatClonee.setId(ligneOperation.getSsCategorie().getId());
            ssCatClonee.setLibelle(ligneOperation.getSsCategorie().getLibelle());
            ligneOperationClonee.setSsCategorie(ssCatClonee);
        }
        ligneOperationClonee.setAutresInfos(new LigneOperation.AddInfos());
        ligneOperationClonee.getAutresInfos().setDateMaj(LocalDateTime.now());
        // #73
        LocalDate nextDate = null;
        if(ligneOperation.getAutresInfos() != null && ligneOperation.getAutresInfos().getDateOperation() != null){
            nextDate = ligneOperation.getAutresInfos().getDateOperation().plusMonths(1);
        }
        ligneOperationClonee.getAutresInfos().setDateOperation(nextDate);
        ligneOperationClonee.setEtat(OperationEtatEnum.PREVUE);
        ligneOperationClonee.setTypeOperation(ligneOperation.getTypeOperation());
        ligneOperationClonee.putValeurFromSaisie(Math.abs(ligneOperation.getValeur()));
        // On ne copie pas les statuts (car nouvelle opération) et on recalcule les status
        return ligneOperationClonee;
    }


    /**
     * Clone d'une ligne opération
     *
     * @return Ligne dépense clonée
     */
    public static List<LigneOperation> cloneOperationPeriodiqueToMoisSuivant(final LigneOperation ligneOperation, Month moisCible, int anneeCible) {
        List<LigneOperation> lignesOperationClonees = new ArrayList<>();

        LigneOperation ligneOperationClonee = cloneOperationToMoisSuivant(ligneOperation);

        /*
         *  Recalcul des mensualités et des récurrences
         */
        if (ligneOperation.getMensualite() != null && ligneOperation.getMensualite().getPeriode() != null) {

            LigneOperation.Mensualite mensualiteClonee = new LigneOperation.Mensualite();
            mensualiteClonee.setPeriode(ligneOperation.getMensualite().getPeriode());
            mensualiteClonee.setDateFin(ligneOperation.getMensualite().getDateFin());

            int prochaineMensualite = ligneOperation.getMensualite().getProchaineEcheance() - 1;

            // Si une opération était à échéance, mais a été reportée - on la réinjecte, en retard
            if (ligneOperation.getMensualite().getProchaineEcheance() == ligneOperation.getMensualite().getPeriode().getNbMois()
                    && OperationEtatEnum.REPORTEE.equals(ligneOperation.getEtat())) {
                cloneOperationAEcheanceReportee(lignesOperationClonees, ligneOperation);
            }

            defnirEtatEtEcheance(ligneOperationClonee, mensualiteClonee, prochaineMensualite);

            LocalDate dateBudgetCible = LocalDate.now().withMonth(moisCible.getValue()).withYear(anneeCible);
            if(!gererDateFinMensualite(ligneOperationClonee, ligneOperation.getMensualite().getDateFin(), dateBudgetCible)){
                // La date de fin de mensualité est dépassée - ne pas cloner
                return lignesOperationClonees;
            }
            ligneOperationClonee.setMensualite(mensualiteClonee);
        }

        lignesOperationClonees.add(ligneOperationClonee);
        return lignesOperationClonees;
    }

    /**
     * Définir l'état et l'échéance de la mensualité clonée
     *
     * @param ligneOperationClonee opération clonée
     * @param mensualiteClonee mensualité clonée
     * @param prochaineMensualite prochaine échéance
     */
    private static void defnirEtatEtEcheance(LigneOperation ligneOperationClonee, LigneOperation.Mensualite mensualiteClonee, int prochaineMensualite) {
        // Si la mensualité arrive à échéance, elle est prévue, et la prochaine échéance est réinitalisée
        if (prochaineMensualite == 0) {
            ligneOperationClonee.setEtat(OperationEtatEnum.PREVUE);
            mensualiteClonee.setProchaineEcheance(mensualiteClonee.getPeriode().getNbMois());
        }
        // Si l'échéance est dans le passé, on laisse la mensualité de base et prévue - tagguée en retard
        else if (prochaineMensualite < 0) {
            ligneOperationClonee.setEtat(OperationEtatEnum.PREVUE);
            mensualiteClonee.setProchaineEcheance(prochaineMensualite);
        }
        // Si l'échéance est dans le futur, on laisse la mensualité de base et reportée
        else {
            ligneOperationClonee.setEtat(OperationEtatEnum.PLANIFIEE);
            mensualiteClonee.setProchaineEcheance(prochaineMensualite);
        }
    }

    /**
     * Gérer la date de fin de mensualité
     *
     * @param ligneOperationClonee opération clonée
     * @param dateFinMensualite date de fin de mensualité
     * @param dateBudgetCible date budget cible
     * @return true si on doit continuer le clonage, false sinon
     */
    private static boolean gererDateFinMensualite(LigneOperation ligneOperationClonee, LocalDate dateFinMensualite, LocalDate dateBudgetCible) {
        if(dateFinMensualite == null){
            return true;
        }

        if(dateFinMensualite.getMonthValue() == dateBudgetCible.getMonthValue() && dateFinMensualite.getYear() == dateBudgetCible.getYear()){
            // La date de fin de mensualité est atteinte - dernière échéance
            if (ligneOperationClonee.getStatuts() == null){
                ligneOperationClonee.setStatuts(new ArrayList<>());
            }
            ligneOperationClonee.getStatuts().add(OperationStatutEnum.DERNIERE_ECHEANCE);
            return true;
        }

        return !dateFinMensualite.isBefore(dateBudgetCible);
    }

    /**
     * Opération périodique à échéance qui est reportée en retard
     *
     * @param lignesOperationClonees liste des opérations
     * @param ligneOperation         opération à traiter
     */
    private static void cloneOperationAEcheanceReportee(List<LigneOperation> lignesOperationClonees, LigneOperation ligneOperation) {
        if (LOGGER.isWarnEnabled() && ligneOperation.getMensualite() != null && ligneOperation.getMensualite().getPeriode() != null) {
            LOGGER.warn("L'opération périodique {} est reportée : en retard", ligneOperation.getMensualite().getPeriode().name());
        }
        LigneOperation ligneOperationEcheanceReportee = cloneOperationToMoisSuivant(ligneOperation);
        if (ligneOperationEcheanceReportee.getStatuts() == null){
            ligneOperationEcheanceReportee.setStatuts(new ArrayList<>());
        }
        if(ligneOperationEcheanceReportee.getStatuts().isEmpty()
        || !ligneOperationEcheanceReportee.getStatuts().contains(OperationStatutEnum.EN_RETARD)){
            ligneOperationEcheanceReportee.getStatuts().add(OperationStatutEnum.EN_RETARD);
        }
        LigneOperation.Mensualite echeanceReportee = new LigneOperation.Mensualite();
        echeanceReportee.setPeriode(OperationPeriodiciteEnum.PONCTUELLE);
        echeanceReportee.setProchaineEcheance(-1);
        echeanceReportee.setDateFin(null);
        ligneOperationEcheanceReportee.setMensualite(echeanceReportee);
        lignesOperationClonees.add(ligneOperationEcheanceReportee);
    }

    /**
     * @param listeOperations liste des opérations
     * @return date max d'une liste de dépenses
     */
    public static LocalDate getMaxDateListeOperations(List<LigneOperation> listeOperations) {

        LocalDate localDateDerniereOperation = BudgetDateTimeUtils.localDateNow();

        if (listeOperations != null && !listeOperations.isEmpty()) {
            // Comparaison de date

            Comparator<LigneOperation> comparator = Comparator.comparing(LigneOperation::retrieveDateOperation, (date1, date2) -> {
                if (date1 == null) {
                    return 1;
                } else if (date2 == null) {
                    return -1;
                } else if (date1.equals(date2)) {
                    return 0;
                } else {
                    return date1.isBefore(date2) ? -1 : 1;
                }
            });
            Optional<LigneOperation> maxDate = listeOperations.stream().max(comparator);
            if (maxDate.get().retrieveDateOperation() != null) {
                localDateDerniereOperation = maxDate.get().retrieveDateOperation();
            }
        }
        return localDateDerniereOperation;
    }


    /**
     * @param valeurS valeur en String
     * @return la valeur d'un String en double
     */
    public static Double getValueFromString(String valeurS) {

        if (valeurS != null) {
            valeurS = valeurS.replace(",", ".");
            try {
                return Double.valueOf(valeurS);
            } catch (Exception e) {
                // Erreur de parsing
            }
        }
        return null;
    }


    /**
     * @param valeurS valeur en String
     * @return la valeur du String sans le tag [xxx]
     */
    public static String deleteTagFromString(String valeurS) {

        if (valeurS != null) {
            return valeurS.replaceAll("\\[.*]", "").trim();
        }
        return null;
    }


    /**
     * Liste des sous catégories des frais remboursables
     */
    private static final IdsCategoriesEnum[] sousCatsFraisRemboursables = {
            IdsCategoriesEnum.SS_CAT_FRAIS_REMBOURSABLE_PRO_NDF,
            IdsCategoriesEnum.SS_CAT_FRAIS_REMBOURSABLE_SANTE_DENTISTE,
            IdsCategoriesEnum.SS_CAT_FRAIS_REMBOURSABLE_SANTE_MEDECIN,
            IdsCategoriesEnum.SS_CAT_FRAIS_REMBOURSABLE_SANTE_OPTICIEN,
            IdsCategoriesEnum.SS_CAT_FRAIS_REMBOURSABLE_SANTE_PHARMACIE,
    };

    /**
     *
     * @param sousCategorieOperation sous catégorie de l'opération
     * @return si la sous catégorie est un frais remboursable
     */
    public static boolean isSsCategorieRemboursable(LigneOperation.Categorie sousCategorieOperation){
        return Arrays.stream(sousCatsFraisRemboursables)
                .anyMatch(id -> id.getId().equals(sousCategorieOperation.getId()));
    }
}
