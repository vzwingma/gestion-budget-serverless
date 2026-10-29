package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.ProjectionBudgetSoldes;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.admin.LibelleAvantApres;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContextKeyEnum;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.security.SecurityUtils;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.AggregateOptions;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    private static final String ATTRIBUT_MIN_DATE = "minDate";
    private static final String ATTRIBUT_MAX_DATE = "maxDate";
    private static final String ATTRIBUT_LISTE_OPERATIONS = "listeOperations";
    private static final String ATTRIBUT_ID_OPERATION = "_id";
    private static final String FILTRE_OPERATION = "op";

    /**
     * Durée maximale d'exécution côté serveur de l'agrégation des libellés ({@code $unwind}/{@code $group} sur tous
     * les budgets d'un compte). Volontairement inférieure au {@code quarkus.mongodb.read-timeout} (8 s) : MongoDB
     * interrompt alors lui-même l'agrégation (erreur {@code MaxTimeMSExpired}) au lieu de la laisser consommer des
     * ressources après la déconnexion du client.
     */
    static final long LIBELLES_MAX_TIME_SECONDES = 7;


    /**
     * Cette méthode est utilisée pour charger le budget mensuel pour un compte bancaire spécifique, un mois et une année donnés.
     * Elle commence par enregistrer l'ID du budget dans le contexte de trace métier.
     * Ensuite, elle tente de trouver le budget dans la base de données en utilisant l'ID du compte, le mois et l'année.
     * Si le budget est trouvé, il est renvoyé. Sinon, une exception BudgetNotFoundException est levée.
     *
     * @param compte Le compte bancaire pour lequel le budget doit être chargé.
     * @param mois Le mois pour lequel le budget doit être chargé.
     * @param annee L'année pour laquelle le budget doit être chargé.
     * @return Un objet Uni contenant le budget mensuel s'il est trouvé, ou une exception BudgetNotFoundException s'il n'est pas trouvé.
     */
    @Override
    public Uni<BudgetMensuel> chargeBudgetMensuel(CompteBancaire compte, Month mois, int annee) {
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, BudgetMensuel.getBudgetId(compte.getId(), mois, annee));
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
     * Retourne le solde et les totaux par catégorie pour un budget mensuel (ou la liste des budgets mensuels) pour un compte et une année donnée
     * @param idCompte identifiant du compte
     * @param mois mois (facultatif)
     * @param annee année
     * @return liste des soldes et totaux par catégorie
     */
    @Override
    public Multi<ProjectionBudgetSoldes> chargeSoldesBudgetMensuel(String idCompte, Month mois, Integer annee){
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.COMPTE, idCompte);

        String anneeS = annee != null ? annee.toString().replaceAll(SecurityUtils.ESCAPE_INPUT_REGEX, "_") : " de toutes les années";
        String moisS = mois != null ? (mois +"/").replaceAll(SecurityUtils.ESCAPE_INPUT_REGEX, "_") : "";
        LOGGER.info("Chargement des soldes {}{} du compte {} ", moisS, anneeS, idCompte);
        String query = ATTRIBUT_COMPTE_ID + " = ?1";
        if(annee != null) {
            query += " and " + ATTRIBUT_ANNEE + " = ?2";
        }
        if(mois != null){
            query += " and " + ATTRIBUT_MOIS + " = ?3";
        }
        return find(query, Sort.by(ATTRIBUT_BUDGET_ID), idCompte, annee, mois)
                .project(ProjectionBudgetSoldes.class)
                .stream()
                .onFailure()
                .transform(e -> {
                    LOGGER.error("Erreur lors du chargement des budgets de {}", idCompte, e);
                    return new BudgetNotFoundException("Erreur lors du chargement des budgets " + idCompte);
                });
    }

    /**
     * charger l'intervalle des budgets mensuels pour un compte
     * @param idCompte id du compte
     * @return intervalle des budgets mensuels pour un compte
     */
    @Override
    public Uni<Instant[]> chargeIntervalleBudgets(String idCompte) {
        LOGGER.debug("Recherche de l'intervalle des budgets du compte {}", idCompte);

        // Conversion du label du mois (ex: "FEBRUARY") en numéro (1-12) via $indexOfArray (+1 car 0-based)
        List<String> moisLabels = Arrays.stream(Month.values())
                .sorted(Comparator.comparingInt(Month::getValue))
                .map(Month::name)
                .toList();
        Document moisAsInt = new Document("$add", Arrays.asList(
                new Document("$indexOfArray", Arrays.asList(moisLabels, "$" + ATTRIBUT_MOIS)),
                1
        ));

        return mongoCollection()
                .aggregate(
                        Arrays.asList(
                                new Document("$match",
                                        new Document(ATTRIBUT_COMPTE_ID, idCompte)),
                                new Document("$group",
                                        new Document("_id", null)
                                                .append(ATTRIBUT_MIN_DATE, new Document("$min", new Document("$dateFromParts",
                                                        new Document("year", "$" + ATTRIBUT_ANNEE)
                                                                .append("month", moisAsInt)
                                                                .append("day", 1))))
                                                .append(ATTRIBUT_MAX_DATE, new Document("$max", new Document("$dateFromParts",
                                                        new Document("year", "$" + ATTRIBUT_ANNEE)
                                                                .append("month", moisAsInt)
                                                                .append("day", 1)))))
                        )
                        , Document.class)
                .collect().first()
                .onItem().transform(document -> {
                    if(document == null || document.get(ATTRIBUT_MIN_DATE) == null || document.get(ATTRIBUT_MAX_DATE) == null) {
                        LOGGER.warn("Aucun budget trouvé pour le compte {}", idCompte);
                        return new Instant[]{};
                    }
                    Instant minDate = document.get(ATTRIBUT_MIN_DATE, Date.class).toInstant();
                    Instant maxDate = document.get(ATTRIBUT_MAX_DATE, Date.class).toInstant();
                    LOGGER.debug("Intervalle des budgets du compte {} : minDate={}, maxDate={}", idCompte, minDate, maxDate);
                    return new Instant[]{minDate, maxDate};
                })
                .onFailure()
                .transform(e -> {
                    LOGGER.error("Erreur lors du chargement de l'intervalle des budgets du compte {}", idCompte, e);
                    return new BudgetNotFoundException("Erreur lors du chargement de l'intervalle des budgets du compte " + idCompte);
                });

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
     * Mise à jour des libellés des opérations d'un compte pour les homogénéiser
     *
     * @param idCompte           id du compte
     * @param libellesToOverride liste des libellés à mettre à jour
     * @return override des libellés pour les budgets mensuels
     */
    public Multi<BudgetMensuel> overrideLibellesOperations(String idCompte, List<LibelleAvantApres> libellesToOverride) {

        LOGGER.info("Mise à jour des libellés des opérations du compte {} : {} éléments", idCompte, libellesToOverride != null ? libellesToOverride.size() : 0);
        return find(ATTRIBUT_COMPTE_ID + "=?1", Sort.by(ATTRIBUT_BUDGET_ID), idCompte)
                .stream()
                .onItem().transform(budget -> {
                    if (budget.getListeOperations() == null) {
                        LOGGER.warn("Budget {} : liste des opérations null, ignoré", budget.getId());
                        return budget;
                    }
                    budget.getListeOperations()
                            .forEach(operation -> {
                                if(libellesToOverride != null){
                                    libellesToOverride.forEach(libelle -> {
                                        if (operation.getLibelle() != null && operation.getLibelle().trim().toLowerCase().contains(libelle.getAvant().trim().toLowerCase())) {
                                            LOGGER.info("     [{}] : override du libellé  [{}] --> [{}] =  [{}]", operation.getLibelle(), libelle.getAvant(), libelle.getApres(), operation.getLibelle().replace(libelle.getAvant(), libelle.getApres()));
                                            operation.setLibelle(operation.getLibelle().replace(libelle.getAvant(), libelle.getApres()));
                                        }
                                    });
                                }
                            });
                            return budget;
                })
                .onItem().transformToUniAndMerge(this::sauvegardeBudgetMensuel);
        

    }

    /**
     * Liste des libellés des opérations d'un compte, regroupés côté MongoDB (C3) sur la valeur brute
     * (libellé, catégorie, sous-catégorie) avec le nombre d'occurrences de chaque combinaison.
     * La normalisation des libellés (suppression des tags et commentaires) reste côté Java : elle somme ensuite les compteurs.
     * <p>
     * Documents retournés : {@code {_id: {libelle, categorieId, ssCategorieId}, n: <nombre d'opérations>}}
     * (un champ absent de l'opération est absent de {@code _id}).
     *
     * @param idCompte id du  compte
     * @return groupes de libellés bruts avec leur compteur
     */
    @Override
    public Multi<Document> getLibellesOperations(String idCompte) {

        LOGGER.info("Liste des libellés des opérations du compte {}", idCompte);
        String listeOperations = "$listeOperations";
        return mongoCollection()
                .aggregate(
                        Arrays.asList(
                                new Document("$match",
                                        new Document(ATTRIBUT_COMPTE_ID, idCompte)),
                                // on éclate le tableau des opérations en documents séparés
                                new Document("$unwind", new Document("path", listeOperations).append("preserveNullAndEmptyArrays", false)),
                                // et on regroupe sur la valeur brute du libellé et des catégories, avec un compteur
                                new Document("$group",
                                        new Document("_id",
                                                new Document("libelle", listeOperations + ".libelle")
                                                        .append("categorieId", listeOperations + ".categorie._id")
                                                        .append("ssCategorieId", listeOperations + ".ssCategorie._id"))
                                                .append("n", new Document("$sum", 1)))
                        )
                        , Document.class, new AggregateOptions().maxTime(LIBELLES_MAX_TIME_SECONDES, TimeUnit.SECONDS));
    }

    /**
     * Compensation ciblée d'une écriture d'opération dans un budget : met à jour uniquement {@code listeOperations}
     * (et les champs calculés listés ci-dessous), jamais le document entier.
     * <ul>
     *     <li>{@code operationOriginale == null} (création) : {@code $pull} de l'opération par id</li>
     *     <li>{@code operationOriginale != null} (modification) : {@code $set} positionnel filtré par id
     *     ({@code listeOperations.$[op]} avec {@code op._id == idOperation}) qui restaure la ligne d'origine</li>
     * </ul>
     * Sont également réécrits depuis {@code budgetRecalcule} : {@code soldes}, {@code totauxParCategories},
     * {@code totauxParSSCategories}, {@code totauxParTypeCategories} et {@code dateMiseAJour}. Ces valeurs sont
     * calculées sur le snapshot mémoire du budget : une écriture concurrente sur une <em>autre</em> opération du même
     * budget n'est pas écrasée dans {@code listeOperations}, mais n'est pas prise en compte dans ces totaux jusqu'au
     * prochain recalcul.
     *
     * @param budgetRecalcule   budget en mémoire, déjà compensé et recalculé (source des soldes et totaux)
     * @param idOperation       identifiant de l'opération à retirer ou restaurer
     * @param operationOriginale ligne d'origine à restaurer, {@code null} en cas de création
     * @return complétion, ou échec si le budget n'existe plus
     */
    @Override
    public Uni<Void> compenseOperation(BudgetMensuel budgetRecalcule, String idOperation, LigneOperation operationOriginale) {
        LOGGER.info("Compensation ciblée de l'opération {} dans le budget {} ({})", idOperation, budgetRecalcule.getId(), operationOriginale == null ? "retrait" : "restauration");
        List<Bson> misesAJour = new ArrayList<>();
        UpdateOptions options = new UpdateOptions();
        if (operationOriginale == null) {
            misesAJour.add(Updates.pull(ATTRIBUT_LISTE_OPERATIONS, new Document(ATTRIBUT_ID_OPERATION, idOperation)));
        } else {
            misesAJour.add(Updates.set(ATTRIBUT_LISTE_OPERATIONS + ".$[" + FILTRE_OPERATION + "]", operationOriginale));
            options.arrayFilters(List.of(new Document(FILTRE_OPERATION + "." + ATTRIBUT_ID_OPERATION, idOperation)));
        }
        misesAJour.add(Updates.set("soldes", budgetRecalcule.getSoldes()));
        misesAJour.add(Updates.set("totauxParCategories", budgetRecalcule.getTotauxParCategories()));
        misesAJour.add(Updates.set("totauxParSSCategories", budgetRecalcule.getTotauxParSSCategories()));
        misesAJour.add(Updates.set("totauxParTypeCategories", budgetRecalcule.getTotauxParTypeCategories()));
        misesAJour.add(Updates.set("dateMiseAJour", budgetRecalcule.getDateMiseAJour() != null ? budgetRecalcule.getDateMiseAJour() : LocalDateTime.now()));

        return mongoCollection()
                .updateOne(new Document(ATTRIBUT_BUDGET_ID, budgetRecalcule.getId()), Updates.combine(misesAJour), options)
                .onItem().transformToUni(resultat -> {
                    if (resultat.getMatchedCount() == 0) {
                        return Uni.createFrom().<Void>failure(new BudgetNotFoundException("Compensation impossible : budget " + budgetRecalcule.getId() + " introuvable"));
                    }
                    return Uni.createFrom().<Void>voidItem();
                });
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
