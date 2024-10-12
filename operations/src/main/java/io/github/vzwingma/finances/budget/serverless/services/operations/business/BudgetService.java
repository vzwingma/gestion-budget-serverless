package io.github.vzwingma.finances.budget.serverless.services.operations.business;


import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.IdsCategoriesEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationPeriodiciteEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IBudgetAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IComptesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IParametragesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.projections.ProjectionBudgetSoldes;
import io.github.vzwingma.finances.budget.serverless.services.operations.utils.BudgetDataUtils;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContextKeyEnum;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.CompteClosedException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

/**
 * Service fournissant les budgets
 *
 * @author vzwingma
 */
@ApplicationScoped
@NoArgsConstructor
@Setter
public class BudgetService implements IBudgetAppProvider {


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
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.COMPTE, idCompte).put(BusinessTraceContextKeyEnum.BUDGET, BudgetDataUtils.getBudgetId(idCompte, mois, annee));
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
    public Multi<ProjectionBudgetSoldes> getSoldesBudgetMensuel(String idCompte, Month mois, int annee){
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


    /************************************
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

        this.operationsAppProvider.calculSoldes(budget.getListeOperations(), budget.getSoldes(), budget.getTotauxParCategories(), budget.getTotauxParSSCategories());
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

        budgetInitVide.setDateMiseAJour(LocalDateTime.now());

        // MAJ Calculs à partir du mois précédent
        // Recherche du budget précédent
        // Si impossible : on retourne le budget initialisé
        String idBudgetPrecedent = BudgetDataUtils.getBudgetId(compteBancaire.getId(), mois.minus(1), Month.DECEMBER.equals(mois.minus(1)) ? annee - 1 : annee);
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
            recalculSoldes(budgetPrecedent);
            budgetInitVide.setIdCompteBancaire(budgetPrecedent.getIdCompteBancaire());
            // #116 : Le résultat du moins précédent est le compte réel, pas le compte avancé
            budgetInitVide.getSoldes().setSoldeAtFinMoisPrecedent(budgetPrecedent.getSoldes().getSoldeAtFinMoisCourant());
            budgetInitVide.setDateMiseAJour(LocalDateTime.now());
            if (budgetPrecedent.getListeOperations() != null) {

                // Recopie de toutes les opérations reportées, non périodique
                budgetInitVide.getListeOperations().addAll(
                        budgetPrecedent.getListeOperations()
                                .stream()
                                .filter(op -> OperationEtatEnum.REPORTEE.equals(op.getEtat())
                                        && (op.getMensualite() == null || OperationPeriodiciteEnum.PONCTUELLE.equals(op.getMensualite().getPeriode())))
                                .peek(op -> LOGGER.info("Opération reportée à copier : {}", op))
                                .map(BudgetDataUtils::cloneOperationToMoisSuivant)
                                .toList());

                // Recopie de toutes les opérations périodiques
                budgetInitVide.getListeOperations().addAll(
                        budgetPrecedent.getListeOperations()
                                .stream()
                                .filter(op -> op.getMensualite() != null && !OperationPeriodiciteEnum.PONCTUELLE.equals(op.getMensualite().getPeriode()))
                                .peek(op -> LOGGER.info("Opération périodique reportée à copier : {}", op))
                                // Les opérations périodiques peuvent créer de nouvelles opérations (période suivante)
                                .map(BudgetDataUtils::cloneOperationPeriodiqueToMoisSuivant)
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
                    budgetMensuel.setDateMiseAJour(LocalDateTime.now());
                    //  #119 #141 : Toutes les opérations en attente sont reportées
                    if (!budgetActif) {
                        LOGGER.info("Toutes les opérations prévues sont reportées :");
                        budgetMensuel.getListeOperations()
                                .stream()
                                .filter(op -> OperationEtatEnum.PREVUE.equals(op.getEtat()))
                                .peek(op -> LOGGER.info("[idOperation:{}] {} -> REPORTEE", op.getId(), op.getLibelle()))
                                .forEach(op -> op.setEtat(OperationEtatEnum.REPORTEE));
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
        budget.setDateMiseAJour(LocalDateTime.now());
        return dataOperationsProvider.sauvegardeBudgetMensuel(budget);
    }


    /************************************
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

        Uni<BudgetMensuel> budgetSurCompteActif = getBudgetAndCompteActif(idBudget)
                // Si pas d'erreur, update de l'opération
                .onItem()
                .transform(Tuple2::getItem1);
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.OPERATION, ligneOperation.getId());


        return Uni.combine().all().unis(
                        budgetSurCompteActif,
                        Uni.createFrom().item(ligneOperation),
                        // On ne va charger la catégorie Remboursement - que pour un frais remboursable
                        ligneOperation.getCategorie().getId().equals(IdsCategoriesEnum.FRAIS_REMBOURSABLES.getId()) ? this.parametragesService.getCategorieParId(IdsCategoriesEnum.REMBOURSEMENT.getId()) : Uni.createFrom().voidItem())
                .asTuple()
                // Ajout des opérations standard et remboursement (si non nulle)
                .invoke(tuple -> {
                    try {
                        this.operationsAppProvider.addOrReplaceOperation(tuple.getItem1().getListeOperations(), tuple.getItem2(), auteur, (CategorieOperations) tuple.getItem3());
                    } catch (DataNotFoundException e) {
                        tuple.mapItem1(u -> Uni.createFrom().failure(e));
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
     * Création des opérations inter-comptes
     *
     * @param idBudget            identifiant du budget source
     * @param ligneOperation      ligne de dépense à ajouter
     * @param idCompteDestination identifiant du compte de destination
     * @return budget mensuel mis à jour
     */
    @Override
    public Uni<BudgetMensuel> createOperationsIntercomptes(String idBudget, final LigneOperation ligneOperation, String idCompteDestination, String auteur) {

        try {
            final String libelleOperation = ligneOperation.getLibelle();
            String idBudgetDestination = BudgetDataUtils.getBudgetId(idCompteDestination, BudgetDataUtils.getMoisFromBudgetId(idBudget), BudgetDataUtils.getAnneeFromBudgetId(idBudget));
            String idCompteSource = BudgetDataUtils.getCompteFromBudgetId(idBudget);
            Month moisFromBudgetId = BudgetDataUtils.getMoisFromBudgetId(idBudget);
            Integer anneeFromBudgetId = BudgetDataUtils.getAnneeFromBudgetId(idBudget);
            LOGGER.info("Ajout d'un transfert intercompte de {} vers {} ({}) > {} ", idBudget, idBudgetDestination, idCompteDestination, ligneOperation);

            /*
             * Opération sur Compte source
             */
            Uni<BudgetMensuel> budgetCourant =
                    Uni.combine().all().unis(
                                    getBudgetAndCompteActif(idBudget).map(Tuple2::getItem1),
                                    this.comptesService.getCompteById(idCompteDestination))
                            .asTuple()
                            .invoke(tuple -> {
                                BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, idBudget).put(BusinessTraceContextKeyEnum.COMPTE, idCompteDestination);
                                ligneOperation.setLibelle("[vers " + tuple.getItem2().getId() + "] " + libelleOperation);
                                try {
                                    this.operationsAppProvider.addOrReplaceOperation(tuple.getItem1().getListeOperations(), ligneOperation, auteur, null);
                                } catch (DataNotFoundException e) {
                                    tuple.mapItem1(u -> Uni.createFrom().failure(e));
                                }
                            })
                            .map(Tuple2::getItem1)
                            .onItem().ifNotNull()
                            .invoke(this::recalculSoldes)
                            // Sauvegarde du budget
                            .call(this::sauvegardeBudget);

            /*
             * Opération sur Compte cible
             */
            Uni<BudgetMensuel> budgetCible = this.comptesService.getCompteById(idCompteDestination)
                                                .onItem().ifNotNull()
                                                .transformToUni(compteDestination -> chargerBudgetMensuelSurCompteActif(compteDestination, moisFromBudgetId, anneeFromBudgetId))
                            .invoke(tuple -> {
                                BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, idBudgetDestination).put(BusinessTraceContextKeyEnum.COMPTE, idCompteSource);
                                String libelleOperationCible = "[depuis " + idCompteSource+ "] " + libelleOperation;
                                this.operationsAppProvider.addOperationIntercompte(tuple.getListeOperations(), ligneOperation, libelleOperationCible, auteur);
                            })
                            .onItem().ifNotNull()
                            .invoke(this::recalculSoldes)
                            // Sauvegarde du budget
                            .call(this::sauvegardeBudget);

            return Uni.combine().all().unis(budgetCourant, budgetCible)
                    .asTuple()
                    .onItem()
                    .invoke(tuple -> BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, idBudget).put(BusinessTraceContextKeyEnum.COMPTE, idCompteSource))
                    .onItem()
                    .ifNotNull().transform(Tuple2::getItem1);

        } catch (BudgetNotFoundException e) {
            LOGGER.error("Erreur lors de la création de l'opération intercompte", e);
            return Uni.createFrom().failure(e);
        }
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
    public Multi<String> getLibellesOperations(String idCompte, String auteur) {
        return this.operationsAppProvider.getLibellesOperations(idCompte);
    }
}
