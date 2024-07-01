package io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.utils.BudgetDataUtils;
import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractAPIObjectModel;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Budget du mois
 *
 * @author vzwingma
 */
@MongoEntity(collection = "budgets")
@Getter
@Setter
@NoArgsConstructor
public class BudgetMensuel extends AbstractAPIObjectModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 4393433203514049021L;

    @Schema(description = "Identifiant")
    public String id;


    /**
     * Mois du budget (au sens CALENDAR)
     */
    @Schema(description = "Mois du budget")
    private Month mois;
    /**
     * année du budget
     */
    @Schema(description = "Année du budget")
    private int annee;
    /**
     * Budget actif
     */
    @Schema(description = "Etat d'activité")
    private boolean actif = false;

    //@Transient
    private transient boolean newBudget = false;
    /**
     * Date de mise à jour
     */
    @Schema(description = "Date de mise à jour")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime dateMiseAJour;
    /**
     * Compte bancaire
     */
    @Schema(description = "Id du compte bancaire")
    private String idCompteBancaire;

    /**
     * Liste des opérations
     */
    @Schema(description = "Liste des opérations")
    private List<LigneOperation> listeOperations = new ArrayList<>();
    /**
     * Résultats Totaux
     */
    @Schema(description = "Soldes")
    private Soldes soldes = new Soldes();
    @Schema(description = "Totaux par catégorie")
    private Map<String, TotauxCategorie> totauxParCategories = new HashMap<>();
    @Schema(description = "Totaux par sous catégories")
    private Map<String, TotauxCategorie> totauxParSSCategories = new HashMap<>();


    /**
     * @return the id
     */
    public String getId() {
        if (id == null) {
            setId();
        }
        return id;
    }

    /**
     * Set id à partir des informations fonctionnelles
     */
    public void setId() {
        this.id = BudgetDataUtils.getBudgetId(this.idCompteBancaire, this.mois, this.annee);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("BudgetMensuel [id=%s, mois=%s, annee=%s, actif=%s, dateMiseAJour=%s, idCompteBancaire=%s, soldes=%s], %s opérations", id, mois, annee, actif, dateMiseAJour != null ? BudgetDateTimeUtils.getLibelleDate(dateMiseAJour) : "null", idCompteBancaire, soldes, listeOperations != null ? listeOperations.size() : 0);
    }

    /**
     * Totaux
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "Soldes")
    public static class Soldes implements Serializable {

        @Serial
        private static final long serialVersionUID = 649769139203031253L;
        @Schema(description = "Solde à la fin du mois précédent")
        private Double soldeAtFinMoisPrecedent = 0D;
        @Schema(description = "Solde à date")
        private Double soldeAtMaintenant = 0D;
        @Schema(description = "Solde à la fin du mois courant")
        private Double soldeAtFinMoisCourant = 0D;


        public void setSoldeAtFinMoisPrecedent(Double soldeAtFinMoisPrecedent) {
            BigDecimal bd = new BigDecimal(soldeAtFinMoisPrecedent).setScale(2, RoundingMode.HALF_UP);
            this.soldeAtFinMoisPrecedent = bd.doubleValue();
        }

        public void setSoldeAtMaintenant(Double soldeAtMaintenant) {
            BigDecimal bd = new BigDecimal(soldeAtMaintenant).setScale(2, RoundingMode.HALF_UP);
            this.soldeAtMaintenant = bd.doubleValue();
        }

        public void setSoldeAtFinMoisCourant(Double soldeAtFinMoisCourant) {
            BigDecimal bd = new BigDecimal(soldeAtFinMoisCourant).setScale(2, RoundingMode.HALF_UP);
            this.soldeAtFinMoisCourant = bd.doubleValue();
        }

        @Override
        public String toString() {
            return String.format("Soldes [soldeAtFinMoisPrecedent=%s, soldeAtMaintenant=%s, soldeAtFinMoisCourant=%s]", soldeAtFinMoisPrecedent, soldeAtMaintenant, soldeAtFinMoisCourant);
        }
    }
}
