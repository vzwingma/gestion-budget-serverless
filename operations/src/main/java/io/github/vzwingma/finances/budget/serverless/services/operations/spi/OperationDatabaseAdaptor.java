package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.ADMIN.LibelleAvantApres;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.projections.ProjectionBudgetSoldes;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContext;
import io.github.vzwingma.finances.budget.services.communs.data.trace.BusinessTraceContextKeyEnum;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.security.SecurityUtils;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Month;
import java.util.Arrays;
import java.util.List;
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
        return find(query, idCompte, annee, mois, Sort.by("id"))
                .project(ProjectionBudgetSoldes.class)
                .stream()
                .onFailure()
                .transform(e -> {
                    LOGGER.error("Erreur lors du chargement des budgets de {}", idCompte, e);
                    return new BudgetNotFoundException("Erreur lors du chargement des budgets " + idCompte);
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
        return find(ATTRIBUT_COMPTE_ID + "=?1", idCompte, Sort.by("id"))
                .stream()
                .onItem().transform(budget -> {
                    budget.getListeOperations()
                            .forEach(operation -> {
                                if(libellesToOverride != null){
                                    libellesToOverride.forEach(libelle -> {
                                        if (operation.getLibelle().trim().equalsIgnoreCase(libelle.getAvant().trim())) {
                                            LOGGER.debug("    override du libellé [{}] --> [{}]", libelle.getAvant(), libelle.getApres());
                                            operation.setLibelle(libelle.getApres());
                                        }
                                    });
                                }
                            });
                    return budget;
                })
                .onItem().transformToUniAndConcatenate(this::persistOrUpdate); // on sauvegarde les budgets mis à jour uniquement si des modifications ont été apportées

    }

    /**
     * Liste des libellés des opérations d'un compte
     *
     * @param idCompte id du  compte
     * @return libelles des opérations
     */
    @Override
    public Multi<Document> getLibellesOperations(String idCompte) {

        LOGGER.info("Liste des libellés des opérations du compte {}", idCompte);
        String opattribute = "operationLibelleAttributes";
        return mongoCollection()
                .aggregate(
                        Arrays.asList(
                                new Document("$match",
                                        new Document(ATTRIBUT_COMPTE_ID, idCompte)),
                                // On projette les libellés des opérations et les catégories associées
                                new Document("$project",
                                        new Document(opattribute,
                                                new Document("$map",
                                                        new Document("input", "$listeOperations")
                                                                .append("as", "operation")
                                                                    .append("in",
                                                                            new Document("libelle", "$$operation.libelle")
                                                                                 .append("categorieId"   , "$$operation.categorie._id")
                                                                                 .append("ssCategorieId" , "$$operation.ssCategorie._id"))
                                                )
                                        )
                                ),
                                // et on éclate le tableau en documents séparés
                                new Document("$unwind",
                                        new Document("path", "$" + opattribute)
                                                .append("includeArrayIndex", "string")
                                                .append("preserveNullAndEmptyArrays", false))
                        )
                , Document.class);
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
