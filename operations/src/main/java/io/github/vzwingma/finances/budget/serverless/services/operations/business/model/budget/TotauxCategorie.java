package io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Totaux par catégories
 *
 * @author vzwingma
 */
@Getter @Setter
@NoArgsConstructor
@Schema(description = "Totaux par catégorie")
public class TotauxCategorie implements Serializable {

    @Serial
    private static final long serialVersionUID = 1726925483789601358L;
    @Setter
    @Schema(description = "Libellé de la catégorie")
    private String libelleCategorie;
    @Schema(description = "Total à date")
    private Double totalAtMaintenant = 0D;
    @Schema(description = "Total à la fin du mois")
    private Double totalAtFinMoisCourant = 0D;


    /**
     * Cette méthode est utilisée pour ajouter un montant au total actuel à ce jour.
     * Le total est ensuite arrondi à deux décimales en utilisant la méthode HALF_UP pour l'arrondissement.
     *
     * @param montantAAjouter Le montant à ajouter au total actuel.
     */
    public void ajouterATotalAtMaintenant(Double montantAAjouter) {
        BigDecimal bd = new BigDecimal(this.totalAtMaintenant + montantAAjouter).setScale(2, RoundingMode.HALF_UP);
        this.totalAtMaintenant = bd.doubleValue();
    }

    /**
     * Cette méthode est utilisée pour ajouter un montant au total actuel à la fin du mois en cours.
     * Le total est ensuite arrondi à deux décimales en utilisant la méthode HALF_UP pour l'arrondissement.
     *
     * @param montantAAjouter Le montant à ajouter au total actuel.
     */
    public void ajouterATotalAtFinMoisCourant(Double montantAAjouter) {
        BigDecimal bd = new BigDecimal(this.totalAtFinMoisCourant+ montantAAjouter).setScale(2, RoundingMode.HALF_UP);
        this.totalAtFinMoisCourant = bd.doubleValue();
    }

    @Override
    public String toString() {
        return String.format("Totaux Categorie [libelle=%s, total à Maintenant=%s, total A FinMoisCourant=%s]", libelleCategorie, totalAtMaintenant, totalAtFinMoisCourant);
    }


}
