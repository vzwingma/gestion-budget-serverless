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
import com.mongodb.client.model.PushOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
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
     * Charge le budget mensuel d'un compte, un mois et une année donnés, par {@code _id} (index natif).
     * <ul>
     *     <li>document absent : {@link BudgetNotFoundException} (404 côté HTTP) ;</li>
     *     <li>toute autre erreur (MongoException, timeout...) : elle remonte telle quelle, sans être confondue avec une absence.
     *     Les {@code MongoException} (client, socket, commande, timeout...) sont toutes mappées en 503 par
     *     {@code AbstractBDDExceptionsHandler}. Le service ne doit jamais
     *     initialiser un budget vierge sur une erreur technique.</li>
     * </ul>
     *
     * @param compte Le compte bancaire pour lequel le budget doit être chargé.
     * @param mois Le mois pour lequel le budget doit être chargé.
     * @param annee L'année pour laquelle le budget doit être chargé.
     * @return Un objet Uni contenant le budget mensuel s'il est trouvé, ou une exception BudgetNotFoundException s'il est absent.
     */
    @Override
    public Uni<BudgetMensuel> chargeBudgetMensuel(CompteBancaire compte, Month mois, int annee) {
        final String idBudget = BudgetMensuel.getBudgetId(compte.getId(), mois, annee);
        BusinessTraceContext.get().put(BusinessTraceContextKeyEnum.BUDGET, idBudget);
        LOGGER.info("Chargement du budget {}/{} du compte {} ", mois, annee, compte.getId());
        return find(ATTRIBUT_BUDGET_ID + " = ?1", idBudget)
                .firstResult()
                .onItem().ifNull().failWith(() -> new BudgetNotFoundException("Budget introuvable pour le compte " + compte.getId() + " du mois " + mois + " de l'année " + annee))
                .onFailure(e -> !(e instanceof BudgetNotFoundException))
                .invoke(e -> LOGGER.error("Erreur technique lors du chargement du budget {}", idBudget, e))
                .invoke(budget -> logReceptionBudget(budget));
    }

    /**
     * Log de debug de réception d'un budget : le calcul du nombre d'opérations n'est fait que si DEBUG est actif,
     * et la liste des opérations peut être absente (donnée non conforme).
     */
    private static void logReceptionBudget(BudgetMensuel budget) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("-> Réception du budget {}. {} opérations", budget.getId(),
                    budget.getListeOperations() != null ? budget.getListeOperations().size() : 0);
        }
    }

    /**
     * Dernier budget &le; (mois, année) en une seule requête indexée sur {@code _id}.
     * <p>
     * L'{@code _id} a le format {@code <idCompte>_<AAAA>_<MM>} : {@code AAAA} et {@code MM} sont de longueur fixe, donc le tri
     * lexical de l'{@code _id} d'un même compte est chronologique. Filtre :
     * <ul>
     *     <li>{@code idCompteBancaire == idCompte} (égalité exacte : un compte {@code a} et un compte {@code a_1} peuvent partager
     *     un préfixe d'{@code _id}, la borne seule ne suffit pas)</li>
     *     <li>{@code _id <= <idCompte>_<AAAA>_<MM>} et {@code _id >= <idCompte>_} (borne basse du préfixe du compte)</li>
     * </ul>
     * tri {@code _id} décroissant, premier résultat. Aucune hypothèse n'est faite sur l'absence de {@code _} dans l'id du compte.
     * <p>
     * Aucun budget : {@code Uni} à valeur {@code null}. Erreur technique : elle remonte (voir {@link #chargeBudgetMensuel(CompteBancaire, Month, int)}).
     */
    @Override
    public Uni<BudgetMensuel> chargeDernierBudgetAvant(String idCompte, Month mois, int annee) {
        final String idBorneHaute = BudgetMensuel.getBudgetId(idCompte, mois, annee);
        final String idBorneBasse = idCompte + "_";
        LOGGER.info("Chargement du dernier budget <= {}/{} du compte {}", mois, annee, idCompte);
        final Document filtre = new Document(ATTRIBUT_COMPTE_ID, idCompte)
                .append(ATTRIBUT_BUDGET_ID, new Document("$lte", idBorneHaute).append("$gte", idBorneBasse));
        return find(filtre, new Document(ATTRIBUT_BUDGET_ID, -1))
                .firstResult()
                .onFailure().invoke(e -> LOGGER.error("Erreur lors du chargement du dernier budget <= {} du compte {}", idBorneHaute, idCompte, e))
                .invoke(budget -> LOGGER.debug("-> Dernier budget <= {} : {}", idBorneHaute, budget != null ? budget.getId() : null));
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
     * Charge un budget par son identifiant ({@code _id}).
     * <ul>
     *     <li>document absent : {@link BudgetNotFoundException} (404 côté HTTP) ;</li>
     *     <li>toute autre erreur (MongoException, timeout...) : elle remonte telle quelle, sans être confondue avec une absence
     *     (statut HTTP selon {@code AbstractBDDExceptionsHandler}).</li>
     * </ul>
     *
     * @param idBudget identifiant du budget
     * @return budget mensuel correspondant à l'identifiant
     */
    @Override
    public Uni<BudgetMensuel> chargeBudgetMensuel(String idBudget) {

        LOGGER.info("Chargement du budget ");
        return find(ATTRIBUT_BUDGET_ID + "=?1", idBudget)
                .firstResult()
                .onItem().ifNull().failWith(() -> new BudgetNotFoundException("Budget introuvable " + idBudget))
                .onFailure(e -> !(e instanceof BudgetNotFoundException))
                .invoke(e -> LOGGER.error("Erreur technique lors du chargement du budget {}", idBudget, e))
                .invoke(budget -> logReceptionBudget(budget));
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
     * <p>
     * Limite connue : l'agrégation est bornée uniquement par le {@code quarkus.mongodb.read-timeout=8S} du client ;
     * aucune limite d'exécution côté serveur (pas de {@code maxTimeMS} : {@code AggregateOptions.maxTime} de
     * quarkus-mongodb-client 3.40.1 est appliqué comme {@code maxAwaitTime}).
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
                        , Document.class);
    }

    /**
     * Compensation ciblée d'une écriture d'opération dans un budget : met à jour uniquement {@code listeOperations}
     * (et les champs calculés listés ci-dessous), jamais le document entier.
     * <ul>
     *     <li>{@code operationOriginale == null} (création) : {@code $pull} de l'opération par id</li>
     *     <li>{@code operationOriginale != null} (modification) : {@code $set} positionnel filtré par id
     *     ({@code listeOperations.$[op]} avec {@code op._id == idOperation}) qui restaure la ligne d'origine ;
     *     si la ligne a disparu de la liste en base (par exemple suppression concurrente), elle est
     *     ré-insérée par {@code $push} à son rang d'origine ({@code $position}, ou en fin de liste si le rang est
     *     inconnu ou dépasse la taille courante), uniquement si aucune ligne de même id n'est présente</li>
     * </ul>
     * Sont également réécrits depuis {@code budgetRecalcule} : {@code soldes}, {@code totauxParCategories},
     * {@code totauxParSSCategories}, {@code totauxParTypeCategories} et {@code dateMiseAJour}. Ces valeurs sont
     * calculées sur le snapshot mémoire du budget : une écriture concurrente sur une <em>autre</em> opération du même
     * budget n'est pas écrasée dans {@code listeOperations}, mais n'est pas prise en compte dans ces totaux jusqu'au
     * prochain recalcul.
     *
     * @param budgetRecalcule    budget en mémoire, déjà compensé et recalculé (source des soldes et totaux)
     * @param idOperation        identifiant de l'opération à retirer ou restaurer (obligatoire, jamais {@code null})
     * @param operationOriginale ligne d'origine à restaurer, {@code null} en cas de création
     * @param rangOriginal       rang d'origine de la ligne, pour une ré-insertion ; négatif si inconnu
     * @return complétion, ou échec si le budget n'existe plus ou si l'identifiant est absent
     */
    @Override
    public Uni<Void> compenseOperation(BudgetMensuel budgetRecalcule, String idOperation, LigneOperation operationOriginale, int rangOriginal) {
        // Aucun $pull ni filtre sur un id null : une compensation sans id est refusée avant toute écriture
        if (idOperation == null || idOperation.isBlank()) {
            return Uni.createFrom().failure(new IllegalArgumentException("Compensation impossible : identifiant d'opération absent (budget " + budgetRecalcule.getId() + ")"));
        }
        LOGGER.info("Compensation ciblée de l'opération {} dans le budget {} ({})", idOperation, budgetRecalcule.getId(), operationOriginale == null ? "retrait" : "restauration");
        final String idBudget = budgetRecalcule.getId();
        // dateMiseAJour est toujours renseignée par le service (horloge applicative, ADR-004) : pas d'horloge locale ici
        final LocalDateTime dateMiseAJour = Objects.requireNonNull(budgetRecalcule.getDateMiseAJour(), "dateMiseAJour du budget recalculé");
        final List<Bson> champsCalcules = List.of(
                Updates.set("soldes", budgetRecalcule.getSoldes()),
                Updates.set("totauxParCategories", budgetRecalcule.getTotauxParCategories()),
                Updates.set("totauxParSSCategories", budgetRecalcule.getTotauxParSSCategories()),
                Updates.set("totauxParTypeCategories", budgetRecalcule.getTotauxParTypeCategories()),
                Updates.set("dateMiseAJour", dateMiseAJour));

        if (operationOriginale == null) {
            return appliquerCompensation(new Document(ATTRIBUT_BUDGET_ID, idBudget),
                    Updates.pull(ATTRIBUT_LISTE_OPERATIONS, new Document(ATTRIBUT_ID_OPERATION, idOperation)),
                    champsCalcules, new UpdateOptions())
                    .onItem().transformToUni(trouve -> Boolean.TRUE.equals(trouve) ? Uni.createFrom().<Void>voidItem() : echecBudgetIntrouvable(idBudget));
        }
        // 1. Restauration en place si la ligne est encore dans la liste
        final Document filtreLignePresente = new Document(ATTRIBUT_BUDGET_ID, idBudget)
                .append(ATTRIBUT_LISTE_OPERATIONS, new Document("$elemMatch", new Document(ATTRIBUT_ID_OPERATION, idOperation)));
        final UpdateOptions optionsPositionnel = new UpdateOptions()
                .arrayFilters(List.of(new Document(FILTRE_OPERATION + "." + ATTRIBUT_ID_OPERATION, idOperation)));
        return appliquerCompensation(filtreLignePresente,
                Updates.set(ATTRIBUT_LISTE_OPERATIONS + ".$[" + FILTRE_OPERATION + "]", operationOriginale),
                champsCalcules, optionsPositionnel)
                .onItem().transformToUni(restauree -> {
                    if (Boolean.TRUE.equals(restauree)) {
                        return Uni.createFrom().<Void>voidItem();
                    }
                    // 2. Ligne absente de la liste : ré-insertion à son rang d'origine, si et seulement si l'id est absent
                    LOGGER.warn("Compensation de l'opération {} : ligne absente du budget {}, ré-insertion (rang {})", idOperation, idBudget, rangOriginal);
                    final Document filtreLigneAbsente = new Document(ATTRIBUT_BUDGET_ID, idBudget)
                            .append(ATTRIBUT_LISTE_OPERATIONS + "." + ATTRIBUT_ID_OPERATION, new Document("$ne", idOperation));
                    final PushOptions optionsPush = new PushOptions();
                    if (rangOriginal >= 0) {
                        optionsPush.position(rangOriginal);
                    }
                    return appliquerCompensation(filtreLigneAbsente,
                            Updates.pushEach(ATTRIBUT_LISTE_OPERATIONS, List.of(operationOriginale), optionsPush),
                            champsCalcules, new UpdateOptions())
                            .onItem().transformToUni(reinseree -> Boolean.TRUE.equals(reinseree) ? Uni.createFrom().<Void>voidItem() : echecReinsertion(idBudget, idOperation));
                });
    }

    /**
     * Applique une mise à jour de compensation : la modification de {@code listeOperations} combinée à la réécriture des champs calculés.
     *
     * @param filtre         sélection du budget (et, selon le cas, de la ligne)
     * @param miseAJourListe modification de {@code listeOperations} ({@code $pull}, {@code $set} positionnel ou {@code $push})
     * @param champsCalcules soldes, totaux et date de mise à jour à réécrire
     * @param options        options de mise à jour (filtres de tableau pour le {@code $set} positionnel)
     * @return {@code true} si un document a été trouvé, {@code false} si le filtre n'a rien sélectionné
     */
    private Uni<Boolean> appliquerCompensation(Bson filtre, Bson miseAJourListe, List<Bson> champsCalcules, UpdateOptions options) {
        final List<Bson> misesAJour = new ArrayList<>();
        misesAJour.add(miseAJourListe);
        misesAJour.addAll(champsCalcules);
        return mongoCollection()
                .updateOne(filtre, Updates.combine(misesAJour), options)
                .map(resultat -> {
                    // matched == 1 && modified == 0 : no-op légitime (état déjà conforme) ; matched > 1 impossible avec updateOne
                    return resultat.getMatchedCount() > 0;
                });
    }

    /**
     * Le {@code $push} n'a rien sélectionné : soit le budget a disparu, soit la ligne est réapparue entre-temps
     * (filtre {@code $ne} sur l'id, écriture concurrente). Le type d'erreur n'est pas exploité par l'appelant
     * (échec de compensation loggé, non propagé) : aucune lecture supplémentaire pour distinguer les deux causes.
     */
    private static Uni<Void> echecReinsertion(String idBudget, String idOperation) {
        LOGGER.error("Compensation du budget {} : ré-insertion de l'opération {} non effectuée (budget introuvable ou ligne réapparue : écriture concurrente)", idBudget, idOperation);
        return Uni.createFrom().failure(new IllegalStateException("Compensation impossible : ré-insertion de l'opération " + idOperation + " non effectuée dans le budget " + idBudget + " (budget introuvable ou ligne réapparue)"));
    }

    private static Uni<Void> echecBudgetIntrouvable(String idBudget) {
        LOGGER.error("Compensation du budget {} impossible : budget introuvable", idBudget);
        return Uni.createFrom().failure(new BudgetNotFoundException("Compensation impossible : budget " + idBudget + " introuvable"));
    }

    /**
     * Sauvegarde ou mise à jour du budget
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
