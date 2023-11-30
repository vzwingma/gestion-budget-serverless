package io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * Totaux par catégories
 *
 * @author vzwingma
 */
@Getter
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


    public void ajouterATotalAtMaintenant(Double montantAAjouter) {
        this.totalAtMaintenant += montantAAjouter;
    }

    public void ajouterATotalAtFinMoisCourant(Double montantAAjouter) {
        this.totalAtFinMoisCourant += montantAAjouter;
    }

    @Override
    public String toString() {
        return String.format("Totaux Categorie [libelle=%s, total à Maintenant=%s, total A FinMoisCourant=%s]", libelleCategorie, totalAtMaintenant, totalAtFinMoisCourant);
    }


}
