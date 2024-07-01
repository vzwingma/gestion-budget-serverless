package io.github.vzwingma.finances.budget.serverless.services.operations.spi.projections;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.TotauxCategorie;
import io.quarkus.mongodb.panache.common.ProjectionFor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;


@ProjectionFor(BudgetMensuel.class)
@Getter @Setter
public class ProjectionBudgetSoldes {



    /**
     * Mois du budget (au sens CALENDAR)
     */
    private Month mois;
    /**
     * année du budget
     */
    private int annee;
    /**
     * Budget actif
     */
    private boolean actif = false;

    /**
     * Date de mise à jour
     */
    private LocalDateTime dateMiseAJour;
    /**
     * Compte bancaire
     */
    private String idCompteBancaire;
    /**
     * Résultats Totaux
     */
    private BudgetMensuel.Soldes soldes = new BudgetMensuel.Soldes();
    /**
     * Totaux par catégories
     */
    private Map<String, TotauxCategorie> totauxParCategories = new HashMap<>();

}
