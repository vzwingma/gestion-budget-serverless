package io.github.vzwingma.finances.budget.serverless.services.operations.business;


import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.IdsCategoriesEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.ProjectionBudgetSoldes;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LibelleCategorieOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationPeriodiciteEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IBudgetAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IComptesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IParametragesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.utils.BudgetDataUtils;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyService;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperationTypeEnum;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.data.model.SsCategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContextKeyEnum;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BadParametersException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.CompteClosedException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Service fournissant les budgets
 *
 * @author vzwingma
 */
@ApplicationScoped
@NoArgsConstructor
@Setter
public class BudgetService implements IBudgetAppProvider, IJwtSigningKeyService {


    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetService.class);
    /**
     * Service Provider Interface des données
     */
    @Inject
    IOperationsRepository dataOperationsProvider;

    @RestClient
    @ApplicationScoped
    IComptesServiceProvider comptesService;

    @Inject
    IOperationsAppProvider operationsAppProvider;

    @RestClient
    @ApplicationScoped
    IParametragesServiceProvider parametragesService;

    @Inject
    IJwtSigningKeyReadRepository iJwtSigningKeyReadRepository;

    Clock clock = Clock.systemUTC();
    /**
     * Chargement du budget du mois courant
     *
     * @param idCompte compte
     * @param mois     mois
     * @param annee    année
     * @return budget mensuel chargé et initialisé à partir des données précédentes
     */
    @Override
    public Uni<BudgetMensuel> getBudgetMensuel(String idCompte, Month mois, int annee) {
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.COMPTE, idCompte).put(BusinessTraceContextKeyEnum.BUDGET, BudgetMensuel.getBudgetId(idCompte, mois, annee));
        LOGGER.debug("Chargement du budget de {}/{}", mois, annee);
        return this.comptesService.getCompteById(idCompte)
                .invoke(compte -> LOGGER.debug("-> Compte correspondant : {}", compte))
                .onItem().ifNotNull()
                .transformToUni(compte -> {
                    if (Boolean.TRUE.equals(compte.isActif())) {
                        return chargerBudgetMensuelSurCompteActif(compte, mois, annee);
                    } else {
                        return chargerBudgetMensuelSurCompteInactif(compte, mois, annee);
                    }
                });
    }



    /**
     * Chargement du budget mensuel
     *
     * @param idBudget id du budget
     * @return budget mensuel
     */
    @Override
    public Uni<BudgetMensuel> getBudgetMensuel(String idBudget) {
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, idBudget);
        return this.dataOperationsProvider.chargeBudgetMensuel(idBudget);
    }

    /**
     * Retourne le solde et les totaux par catégorie pour un budget mensuel (ou la liste des budgets mensuels) pour un compte et une année donnée
     * @param idCompte identifiant du compte
     * @param mois mois (facultatif)
     * @param annee année
     * @return liste des soldes et totaux par catégorie
     */
    @Override
    public Multi<ProjectionBudgetSoldes> getSoldesBudgetMensuel(String idCompte, Month mois, Integer annee){
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.COMPTE, idCompte);
        return this.dataOperationsProvider.chargeSoldesBudgetMensuel(idCompte, mois, annee);
    }
    /**
     * Chargement du budget et du compte en double Uni
     *
     * @param idBudget id du budget
     * @return tuple (budget, compte)
     */
    private Uni<Tuple2<BudgetMensuel, CompteBancaire>> getBudgetAndCompteActif(String idBudget) {
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, idBudget);
        return getBudgetMensuel(idBudget)
                .flatMap(budget -> Uni.combine().all()
                        .unis(Uni.createFrom().item(budget),
                                this.comptesService.getCompteById(budget.getIdCompteBancaire()))
                        .asTuple())
                // Si pas d'erreur, vérification du compte
                .onItem().ifNotNull()
                // Vérification du compte
                .transformToUni(tuple -> {
                    CompteBancaire compteBancaire = tuple.getItem2();
                    BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.COMPTE, compteBancaire.getId());

                    if (!Boolean.TRUE.equals(compteBancaire.isActif())) {
                        LOGGER.warn("Impossible de modifier ou créer une opération. Le compte {} est cloturé", tuple.getItem1().getIdCompteBancaire());
                        return Uni.createFrom().failure(new CompteClosedException("Impossible de modifier ou créer une opération. Le compte " + tuple.getItem1().getIdCompteBancaire() + " est cloturé"));
                    }
                    return Uni.createFrom().item(tuple);
                });
    }


    /**
     * Chargement du budget du mois courant pour le compte actif
     *
     * @param compteBancaire compte
     * @param mois           mois
     * @param annee          année
     * @return budget mensuel chargé et initialisé à partir des données précédentes
     */
    private Uni<BudgetMensuel> chargerBudgetMensuelSurCompteActif(CompteBancaire compteBancaire, Month mois, int annee) {
        LOGGER.debug("Chargement du budget de {}/{} sur compte actif ", mois, annee);

        return this.dataOperationsProvider.chargeBudgetMensuel(compteBancaire, mois, annee)
                // Budget introuvable - init d'un nouveau budget
                .onFailure()
                .recoverWithUni(() -> initNewBudget(compteBancaire, mois, annee))
                .invoke(budgetMensuel -> LOGGER.debug("Budget mensuel chargé : {}", budgetMensuel))
                // rechargement du solde mois précédent (s'il a changé)
                .onItem()
                .transformToUni(budgetMensuel -> recalculSoldeAFinMoisPrecedent(budgetMensuel, compteBancaire))
                // recalcul de tous les soldes du budget courant
                .onItem().ifNotNull()
                .invoke(this::recalculSoldes)
                // Sauvegarde du budget
                .call(this::sauvegardeBudget);
    }


    /**
     * Cette méthode est utilisée pour charger le budget mensuel d'un compte bancaire inactif pour un mois et une année spécifiques.
     * Elle utilise le service de fournisseur de données d'opérations pour récupérer le budget mensuel.
     *
     * @param compteBancaire Le compte bancaire inactif pour lequel le budget mensuel doit être chargé.
     * @param mois Le mois pour lequel le budget mensuel doit être chargé.
     * @param annee L'année pour laquelle le budget mensuel doit être chargé.
     * @return Un objet Uni contenant le budget mensuel si trouvé, ou une exception BudgetNotFoundException s'il n'est pas trouvé.
     */
    private Uni<BudgetMensuel> chargerBudgetMensuelSurCompteInactif(CompteBancaire compteBancaire, Month mois, int annee) {
        LOGGER.debug(" Chargement du budget sur compte inactif de {}/{}", mois, annee);

        // Calcul de paramètres pour le recovery
        Month moisPrecedent = mois.minus(1);
        int anneePrecedente = Month.DECEMBER.equals(moisPrecedent) ? annee - 1 : annee;

        // Chargement du budget précédent
        return this.dataOperationsProvider.chargeBudgetMensuel(compteBancaire, mois, annee)
                .onItem()
                // Si le budget n'existe pas, on recherche le dernier
                .ifNull()
                .switchTo(() -> chargerBudgetMensuelSurCompteInactif(compteBancaire, moisPrecedent, anneePrecedente))
                .onItem()
                .transform(budgetMensuel -> {
                    // On reporte l'état inactif du compte sur les anciens budgets
                    budgetMensuel.setIdCompteBancaire(compteBancaire.getId());
                    // L'état du budget est forcé à inactif
                    budgetMensuel.setActif(false);
                    return budgetMensuel;
                })
                .invoke(budgetMensuel -> LOGGER.info("Budget sur compte inactif de {}/{} chargé : {}", mois, annee, budgetMensuel));
    }


    /* ***********************************
     *  			CALCULS
     ***********************************/

    /**
     * Recalcul du solde à la fin du mois précédent
     *
     * @param budgetMensuel  budget mensuel
     * @param compteBancaire compte
     * @return budget mensuel recalculé
     */
    private Uni<BudgetMensuel> recalculSoldeAFinMoisPrecedent(final BudgetMensuel budgetMensuel, CompteBancaire compteBancaire) {
        // Maj du budget ssi budget actif
        if (budgetMensuel != null && budgetMensuel.isActif()) {
            // Recalcul du résultat du mois précédent
            Month moisPrecedent = budgetMensuel.getMois().minus(1);
            int anneePrecedente = Month.DECEMBER.equals(moisPrecedent) ? budgetMensuel.getAnnee() - 1 : budgetMensuel.getAnnee();

            LOGGER.debug("Recalcul du solde à partir du budget du mois précédent du compte actif {} : {}/{}", compteBancaire, moisPrecedent, anneePrecedente);

            return Uni.combine().all().unis(
                            Uni.createFrom().item(budgetMensuel),
                            this.dataOperationsProvider.chargeBudgetMensuel(compteBancaire, moisPrecedent, anneePrecedente)
                    ).asTuple()
                    .invoke(tuple -> tuple.getItem1().getSoldes().setSoldeAtFinMoisPrecedent(tuple.getItem2().getSoldes().getSoldeAtFinMoisCourant()))
                    .map(Tuple2::getItem1);
        } else {
            LOGGER.debug("Budget inactif, pas de recalcul du solde à partir du budget du mois précédent du compte actif {}", compteBancaire);
            return Uni.createFrom().item(budgetMensuel);
        }

    }

    /**
     * Calcul des soldes du budget mensuel
     *
     * @param budget budget à calculer
     */
    @Override
    public void recalculSoldes(BudgetMensuel budget) {

        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, budget.getId());
        LOGGER.info("(Re)Calcul des soldes du budget");
        BudgetDataUtils.razCalculs(budget);

        this.operationsAppProvider.calculSoldes(budget.getListeOperations(), budget.getSoldes(),
                budget.getTotauxParCategories(),
                budget.getTotauxParSSCategories(),
                budget.getTotauxParTypeCategories());
    }


    /**
     * Init new budget
     *
     * @param compteBancaire compte
     * @param mois           mois
     * @param annee          année
     * @return budget nouvellement créé
     */
    protected Uni<BudgetMensuel> initNewBudget(CompteBancaire compteBancaire, Month mois, int annee) {

        //Vérification du compte
        if (compteBancaire == null) {
            return Uni.createFrom().failure(new DataNotFoundException("Compte bancaire non trouvé"));
        } else if (Boolean.FALSE.equals(compteBancaire.isActif())) {
            return Uni.createFrom().failure(new CompteClosedException("Compte bancaire inactif"));
        }
        LOGGER.info("(Ré)initialisation du budget de {}/{}", mois, annee);
        BudgetMensuel budgetInitVide = new BudgetMensuel();
        budgetInitVide.setActif(true);
        budgetInitVide.setAnnee(annee);
        budgetInitVide.setMois(mois);
        budgetInitVide.setIdCompteBancaire(compteBancaire.getId());

        budgetInitVide.setNewBudget(true);
        budgetInitVide.setId();
        budgetInitVide.setDateMiseAJour(LocalDateTime.now(clock));

        // MAJ Calculs à partir du mois précédent
        // Recherche du budget précédent
        // Si impossible : on retourne le budget initialisé
        String idBudgetPrecedent = BudgetMensuel.getBudgetId(compteBancaire.getId(), mois.minus(1), Month.DECEMBER.equals(mois.minus(1)) ? annee - 1 : annee);
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, idBudgetPrecedent);
        LOGGER.debug("Chargement du budget précédent pour initialisation");
        return getBudgetMensuel(idBudgetPrecedent)
                .onFailure()
                .recoverWithUni(() -> chargerBudgetMensuelSurCompteActif(compteBancaire, mois.minus(1), Month.DECEMBER.equals(mois.minus(1)) ? annee - 1 : annee))
                .onItem()
                .transformToUni(budgetPrecedent -> {
                    LOGGER.debug("Budget précédent trouvé : {}. Actif ? : {}", budgetPrecedent, budgetPrecedent.isActif());
                    // #115 : Cloture automatique du mois précédent
                    return budgetPrecedent.isActif() ? setBudgetActif(budgetPrecedent.getId(), false) : Uni.createFrom().item(budgetPrecedent);
                })
                .map(budgetPrecedent -> initBudgetFromBudgetPrecedent(budgetInitVide, budgetPrecedent));
        // La sauvegarde du budget initialisé est réalisée dans le flux suivant
    }

    /**
     * Initialisation du budget à partir du budget du mois précédent
     *
     * @param budgetInitVide  budget à initialiser
     * @param budgetPrecedent budget du mois précédent
     */
    private BudgetMensuel initBudgetFromBudgetPrecedent(BudgetMensuel budgetInitVide, BudgetMensuel budgetPrecedent) {

        // Calcul
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, budgetInitVide.getId());
        if (budgetPrecedent != null) {
            LOGGER.info("Initialisation du budget à partir du budget précédent {}", budgetPrecedent.getId());
            recalculSoldes(budgetPrecedent);
            budgetInitVide.setIdCompteBancaire(budgetPrecedent.getIdCompteBancaire());
            // #116 : Le résultat du moins précédent est le compte réel, pas le compte avancé
            budgetInitVide.getSoldes().setSoldeAtFinMoisPrecedent(budgetPrecedent.getSoldes().getSoldeAtFinMoisCourant());
            budgetInitVide.setDateMiseAJour(LocalDateTime.now(clock));
            if (budgetPrecedent.getListeOperations() != null) {

                // Recopie de toutes les opérations reportées, non périodique
                budgetInitVide.getListeOperations().addAll(
                        budgetPrecedent.getListeOperations()
                                .stream()
                                .filter(op -> OperationEtatEnum.REPORTEE.equals(op.getEtat())
                                        && (op.getMensualite() == null || OperationPeriodiciteEnum.PONCTUELLE.equals(op.getMensualite().getPeriode())))
                                // .peek(op -> LOGGER.info("Opération reportée à copier : {}", op))
                                .map(op -> BudgetDataUtils.cloneOperationToMoisSuivant(op, clock))
                                .toList());

                // Recopie de toutes les opérations périodiques
                budgetInitVide.getListeOperations().addAll(
                        budgetPrecedent.getListeOperations()
                                .stream()
                                .filter(op -> op.getMensualite() != null && !OperationPeriodiciteEnum.PONCTUELLE.equals(op.getMensualite().getPeriode()))
                                // .peek(op -> LOGGER.info("Opération périodique reportée à copier : {}", op))
                                // Les opérations périodiques peuvent créer de nouvelles opérations (période suivante)
                                .map(ligneOperation -> BudgetDataUtils.cloneOperationPeriodiqueToMoisSuivant(ligneOperation, budgetInitVide.getMois(), budgetInitVide.getAnnee(), clock))
                                .flatMap(List::stream)
                                .toList());
            }
        }
        return budgetInitVide;
    }


    /**
     * Réinitialisation du budget
     *
     * @param idBudget budget mensuel
     * @return budget mensuel réinitialisé
     */
    @Override
    public Uni<BudgetMensuel> reinitialiserBudgetMensuel(String idBudget) {
        LOGGER.info("Réinitialisation du budget {}", idBudget);
        // Chargement du budget et compte
        return getBudgetAndCompteActif(idBudget)
                // Si pas d'erreur, réinitialisation du budget
                .onItem()
                .transformToUni(tuple -> initNewBudget(tuple.getItem2(), tuple.getItem1().getMois(), tuple.getItem1().getAnnee()))
                // recalcul de tous les soldes du budget courant
                .onItem().ifNotNull()
                .invoke(this::recalculSoldes)
                // Sauvegarde du budget
                .call(this::sauvegardeBudget);
    }

    /**
     * Budget mensuel actif
     *
     * @param idBudget id budget
     * @return résultat de l'activation
     */
    @Override
    public Uni<Boolean> isBudgetMensuelActif(String idBudget) {
        return this.dataOperationsProvider.isBudgetActif(idBudget);
    }

    /**
     * Dés/Activation du budget mensuel
     *
     * @param idBudgetMensuel id budget mensuel
     * @param budgetActif     etat du budget
     * @return budget mensuel mis à jour
     */
    @Override
    public Uni<BudgetMensuel> setBudgetActif(String idBudgetMensuel, boolean budgetActif) {
        LOGGER.info("{} du budget", budgetActif ? "Réouverture" : "Fermeture");

        return dataOperationsProvider.chargeBudgetMensuel(idBudgetMensuel)
                .map(budgetMensuel -> {
                    budgetMensuel.setActif(budgetActif);
                    budgetMensuel.setDateMiseAJour(LocalDateTime.now(clock));
                    //  #119 #141 : Toutes les opérations en attente sont reportées
                    if (!budgetActif) {
                        LOGGER.info("Toutes les opérations prévues sont reportées :");
                        budgetMensuel.getListeOperations()
                                .stream()
                                .filter(op -> OperationEtatEnum.PREVUE.equals(op.getEtat()))
                                .forEach(op -> {
                                    LOGGER.info("[idOperation:{}] {} -> REPORTEE", op.getId(), op.getLibelle());
                                    op.setEtat(OperationEtatEnum.REPORTEE);
                                });
                    }
                    return budgetMensuel;
                })
                .onItem()
                .ifNotNull()
                .invoke(this::recalculSoldes)
                // Sauvegarde du budget
                .call(this::sauvegardeBudget);

    }


    /**
     * sauvegarde du budget Courant
     *
     * @param budget budget à sauvegarder
     */

    private Uni<BudgetMensuel> sauvegardeBudget(BudgetMensuel budget) {
        budget.setDateMiseAJour(LocalDateTime.now(clock));
        return dataOperationsProvider.sauvegardeBudgetMensuel(budget);
    }


    /* ***********************************
     *  			OPERATIONS
     ***********************************/

    /**
     * Ajout d'une opération dans le budget
     *
     * @param idBudget       identifiant de budget
     * @param ligneOperation ligne de dépense à ajouter
     * @return budget mensuel mis à jour
     */
    @Override
    public Uni<BudgetMensuel> addOrUpdateOperationInBudget(String idBudget, LigneOperation ligneOperation, String auteur) {

        // #125 : Type de la catégorie = ESSENTIEL si non défini
        if (ligneOperation.getSsCategorie() != null && ligneOperation.getSsCategorie().getType() == null) {
            ligneOperation.getSsCategorie().setType(CategorieOperationTypeEnum.ESSENTIEL);
        }

        Uni<BudgetMensuel> budgetSurCompteActif = getBudgetAndCompteActif(idBudget)
                // Si pas d'erreur, update de l'opération
                .onItem()
                .transform(Tuple2::getItem1);
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.OPERATION, ligneOperation.getId());


        return Uni.combine().all().unis(
                        budgetSurCompteActif,
                        Uni.createFrom().item(ligneOperation),
                        // On ne va charger la catégorie Remboursement - que pour un frais remboursable
                        BudgetDataUtils.isSsCategorieRemboursable(ligneOperation.getSsCategorie()) ? this.parametragesService.getSsCategorieParId(IdsCategoriesEnum.SS_CAT_REMBOURSEMENT.getId()) : Uni.createFrom().item((SsCategorieOperations) null))
                .asTuple()
                // Ajout des opérations standard et remboursement (si non nulle)
                .onItem().transformToUni(tuple -> {
                    try {
                        this.operationsAppProvider.addOrReplaceOperation(tuple.getItem1().getListeOperations(), tuple.getItem2(), auteur, tuple.getItem3());
                        return Uni.createFrom().item(tuple);
                    } catch (DataNotFoundException e) {
                        return Uni.createFrom().failure(e);
                    }
                })
                .onItem().transform(Tuple2::getItem1)
                // recalcul de tous les soldes du budget courant
                .onItem().ifNotNull()
                .invoke(this::recalculSoldes)
                // Sauvegarde du budget
                .call(this::sauvegardeBudget);
    }

    /**
     * Création des opérations de virement interne.
     * <p>
     * Écritures <strong>séquentielles</strong> (ADR-007) : opération sur le compte source, puis opération sur le compte
     * de destination. Si l'écriture cible échoue, l'opération source est retirée (compensation) et l'erreur d'origine
     * est propagée. Un échec de la compensation est loggé en ERROR (identifiants source et destination) et n'écrase pas
     * l'erreur d'origine.
     * <p>
     * Gardes avant toute écriture : source = destination (400), compte destination introuvable (404),
     * compte destination clos (406).
     *
     * @param idBudget            identifiant du budget source
     * @param ligneOperation      ligne de dépense à ajouter
     * @param idCompteDestination identifiant du compte de destination
     * @return budget mensuel source mis à jour
     */
    @Override
    public Uni<BudgetMensuel> createOperationsVirementInterne(String idBudget, final LigneOperation ligneOperation, String idCompteDestination, String auteur) {

        try {
            final String idCompteSource = BudgetDataUtils.getCompteFromBudgetId(idBudget);
            final Month moisFromBudgetId = BudgetDataUtils.getMoisFromBudgetId(idBudget);
            final int anneeFromBudgetId = BudgetDataUtils.getAnneeFromBudgetId(idBudget);

            if (ligneOperation.getSsCategorie() == null || !IdsCategoriesEnum.SS_CAT_VIREMENT_INTERNE.getId().equals(ligneOperation.getSsCategorie().getId())) {
                LOGGER.error("L'opération {} n'est pas un virement interne. L'opération est annulée", ligneOperation.getLibelle());
                return Uni.createFrom().failure(new BadParametersException("L'opération " + ligneOperation + " n'est pas un virement interne"));
            }
            // Garde (Q4) : virement vers le même compte -> 400 (BadParametersException, comme les autres paramètres invalides)
            if (idCompteSource.equals(idCompteDestination)) {
                LOGGER.error("Virement interne refusé : le compte source et le compte de destination sont identiques ({})", idCompteSource);
                return Uni.createFrom().failure(new BadParametersException("Virement interne impossible : le compte source et le compte de destination sont identiques"));
            }
            LOGGER.info("Ajout d'un virement interne de {} vers le compte {} > {} ", idBudget, idCompteDestination, ligneOperation);

            // Lecture du budget et du compte source (actif), et un seul appel de lecture du compte de destination
            return Uni.combine().all().unis(
                            getBudgetAndCompteActif(idBudget).map(Tuple2::getItem1),
                            getCompteDestination(idCompteDestination))
                    .asTuple()
                    .chain(tuple -> {
                        CompteBancaire compteDestination = tuple.getItem2();
                        if (compteDestination == null) {
                            LOGGER.error("Virement interne refusé : compte de destination {} introuvable", idCompteDestination);
                            return Uni.createFrom().failure(new DataNotFoundException("Compte de destination " + idCompteDestination + " introuvable"));
                        }
                        // Garde (Q4) : compte de destination clos -> 406
                        if (!Boolean.TRUE.equals(compteDestination.isActif())) {
                            LOGGER.error("Virement interne refusé : le compte de destination {} est clos", idCompteDestination);
                            return Uni.createFrom().failure(new CompteClosedException("Impossible de créer un virement vers le compte " + idCompteDestination + " : il est cloturé"));
                        }
                        return virementInterneSequentiel(tuple.getItem1(), compteDestination, idCompteSource, idBudget, moisFromBudgetId, anneeFromBudgetId, ligneOperation, auteur);
                    })
                    .invoke(budgetSource -> BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, idBudget).put(BusinessTraceContextKeyEnum.COMPTE, idCompteSource));

        } catch (BudgetNotFoundException e) {
            LOGGER.error("Erreur lors de la création des opérations de virements internes", e);
            return Uni.createFrom().failure(e);
        }
    }

    /**
     * Lecture du compte de destination. Un 404 distant du service comptes (le client REST lève une
     * {@link WebApplicationException} de statut 404 pour un compte inconnu) est traduit en {@link DataNotFoundException}
     * (404 côté API) ; toute autre erreur est propagée inchangée.
     */
    private Uni<CompteBancaire> getCompteDestination(String idCompteDestination) {
        return this.comptesService.getCompteById(idCompteDestination)
                .onFailure(erreur -> erreur instanceof WebApplicationException wae
                        && wae.getResponse() != null
                        && wae.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode())
                .transform(erreur -> {
                    LOGGER.error("Virement interne refusé : compte de destination {} introuvable (404 du service comptes)", idCompteDestination);
                    return new DataNotFoundException("Compte de destination " + idCompteDestination + " introuvable");
                });
    }

    /**
     * Écritures séquentielles du virement interne : source, puis cible, avec compensation de la source si la cible échoue.
     *
     * @return budget source sauvegardé
     */
    private Uni<BudgetMensuel> virementInterneSequentiel(BudgetMensuel budgetSource, CompteBancaire compteDestination, String idCompteSource,
                                                         String idBudget, Month mois, int annee, LigneOperation ligneOperation, String auteur) {
        final String libelleOperation = ligneOperation.getLibelle();
        // L'id est garanti avant toute écriture : la compensation ciblée ($pull / $set / $push) en dépend.
        // L'API le génère déjà (UUID, BudgetsResource) ; en appel direct du service on applique le même mécanisme.
        if (ligneOperation.getId() == null || ligneOperation.getId().isBlank()) {
            ligneOperation.setId(UUID.randomUUID().toString());
        }
        final String idOperation = ligneOperation.getId();
        // Ligne d'origine (modification d'une opération existante) et son rang, capturés avant l'écriture source : null / -1 en cas de création.
        // addOrReplaceOperation remplace ou supprime la ligne sans modifier l'objet d'origine.
        final List<LigneOperation> operationsSource = budgetSource.getListeOperations();
        final int rangOriginal = IntStream.range(0, operationsSource.size())
                .filter(i -> idOperation.equals(operationsSource.get(i).getId()))
                .findFirst().orElse(-1);
        final LigneOperation operationOriginale = rangOriginal >= 0 ? operationsSource.get(rangOriginal) : null;

        // 1. Écriture source
        return ecrireOperationVirementSource(budgetSource, compteDestination, idBudget, ligneOperation, libelleOperation, auteur)
                // 2. Écriture cible, puis compensation de la source en cas d'échec (l'erreur d'origine est propagée)
                .chain(budgetSourceSauvegarde -> ecrireOperationVirementCible(compteDestination, idCompteSource, mois, annee, ligneOperation, libelleOperation, auteur)
                        .replaceWith(budgetSourceSauvegarde)
                        .onFailure().recoverWithUni(erreur -> {
                            LOGGER.error("Echec de l'écriture du virement sur le compte {} : compensation de l'opération {} du compte {}", compteDestination.getId(), idOperation, idCompteSource, erreur);
                            return compenserVirementSource(budgetSourceSauvegarde, idOperation, operationOriginale, rangOriginal, idCompteSource, compteDestination.getId())
                                    .chain(() -> Uni.createFrom().<BudgetMensuel>failure(erreur));
                        }));
    }

    /**
     * Ajout de l'opération de virement (dépense) dans le budget du compte source, recalcul et sauvegarde.
     */
    private Uni<BudgetMensuel> ecrireOperationVirementSource(BudgetMensuel budgetSource, CompteBancaire compteDestination, String idBudget,
                                                             LigneOperation ligneOperation, String libelleOperation, String auteur) {
        return Uni.createFrom().item(budgetSource)
                .invoke(budget -> {
                    BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, idBudget).put(BusinessTraceContextKeyEnum.COMPTE, compteDestination.getId());
                    ligneOperation.setLibelle("[vers " + compteDestination.getId() + "] " + libelleOperation);
                })
                .chain(budget -> {
                    try {
                        this.operationsAppProvider.addOrReplaceOperation(budget.getListeOperations(), ligneOperation, auteur, null);
                    } catch (DataNotFoundException e) {
                        // Erreur propagée (F2a)
                        return Uni.createFrom().<BudgetMensuel>failure(e);
                    }
                    recalculSoldes(budget);
                    return sauvegardeBudget(budget);
                });
    }

    /**
     * Ajout de l'opération de virement (rentrée) dans le budget du compte de destination, recalcul et sauvegarde.
     */
    private Uni<BudgetMensuel> ecrireOperationVirementCible(CompteBancaire compteDestination, String idCompteSource, Month mois, int annee,
                                                            LigneOperation ligneOperation, String libelleOperation, String auteur) {
        return chargerBudgetMensuelSurCompteActif(compteDestination, mois, annee)
                .invoke(budgetCible -> {
                    BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, budgetCible.getId()).put(BusinessTraceContextKeyEnum.COMPTE, idCompteSource);
                    String libelleOperationCible = "[depuis " + idCompteSource + "] " + libelleOperation;
                    this.operationsAppProvider.addOperationVirementInterne(budgetCible.getListeOperations(), ligneOperation, libelleOperationCible, auteur);
                })
                .invoke(this::recalculSoldes)
                .call(this::sauvegardeBudget);
    }

    /**
     * Compensation ciblée de l'écriture source (ADR-007) : création -> l'opération est retirée ; modification -> la ligne
     * d'origine est restaurée (ré-insérée à son rang d'origine si l'écriture source l'avait supprimée, cas {@code etat == null}). Seule {@code listeOperations} est modifiée en base (pas de réécriture du budget entier),
     * avec les soldes et totaux recalculés sur le snapshot mémoire. Un échec de compensation est loggé en ERROR avec les
     * identifiants source et destination, et n'est pas propagé.
     */
    private Uni<Void> compenserVirementSource(BudgetMensuel budgetSource, String idOperation, LigneOperation operationOriginale, int rangOriginal,
                                              String idCompteSource, String idCompteDestination) {
        return Uni.createFrom().item(budgetSource)
                .invoke(budget -> {
                    if (operationOriginale == null) {
                        this.operationsAppProvider.deleteOperation(budget.getListeOperations(), idOperation);
                    } else {
                        List<LigneOperation> operations = budget.getListeOperations();
                        boolean restauree = false;
                        for (int i = 0; i < operations.size(); i++) {
                            if (idOperation.equals(operations.get(i).getId())) {
                                operations.set(i, operationOriginale);
                                restauree = true;
                            }
                        }
                        // Ligne supprimée par l'écriture source (etat == null) : ré-insertion à son rang d'origine
                        // (l'ordre de la liste est visible de l'utilisateur), en fin de liste si le rang dépasse la taille
                        if (!restauree) {
                            operations.add(Math.min(Math.max(rangOriginal, 0), operations.size()), operationOriginale);
                        }
                    }
                    recalculSoldes(budget);
                    budget.setDateMiseAJour(LocalDateTime.now(clock));
                })
                .chain(budget -> dataOperationsProvider.compenseOperation(budget, idOperation, operationOriginale, rangOriginal))
                .onFailure().recoverWithUni(erreurCompensation -> {
                    LOGGER.error("ECHEC de la compensation du virement interne : l'opération {} reste modifiée/ajoutée dans le budget du compte source {} alors que le virement vers {} a échoué. Intervention manuelle nécessaire",
                            idOperation, idCompteSource, idCompteDestination, erreurCompensation);
                    return Uni.createFrom().voidItem();
                });
    }

    /**
     * Ajout d'une opération dans le budget
     *
     * @param idBudget    identifiant de budget
     * @param idOperation ligne de dépense à ajouter
     * @return budget mensuel mis à jour
     */
    @Override
    public Uni<BudgetMensuel> deleteOperationInBudget(String idBudget, String idOperation) {

        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.OPERATION, idOperation);
        return getBudgetAndCompteActif(idBudget)
                // Si pas d'erreur, update de l'opération
                .onItem()
                .transform(Tuple2::getItem1)
                .invoke(budget -> this.operationsAppProvider.deleteOperation(budget.getListeOperations(), idOperation))
                // recalcul de tous les soldes du budget courant
                .onItem().ifNotNull()
                .invoke(this::recalculSoldes)
                // Sauvegarde du budget
                .call(this::sauvegardeBudget);
    }


    /**
     * @param idCompte id du compte
     * @param auteur   utilisateur authentifié
     * @return liste des libellés d'opérations
     */
    @Override
    public Multi<LibelleCategorieOperation> getLibellesOperations(String idCompte, String auteur) {
        return this.operationsAppProvider.getLibellesOperations(idCompte);
    }



    /**
     * Récupration de l'intervalle des budgets mensuels pour un compte
     * @param idCompte id du compte
     * @return intervalle des budgets mensuels pour un compte
     */
    @Override
    public Uni<Instant[]> getiIntervalleBudgets(String idCompte){
        return this.operationsAppProvider.getIntervalleBudgets(idCompte);
    }

    /**
     * @return le dépôt des clés de signature
     */
    @Override
    public IJwtSigningKeyReadRepository getSigningKeyReadRepository() {
        return iJwtSigningKeyReadRepository;
    }

}
