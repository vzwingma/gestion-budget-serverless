package io.github.vzwingma.finances.budget.services.communs.data.abstrait;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serial;
import java.util.UUID;

/**
 * Catégorie d'opérations
 *
 * @author vzwingma
 */
@Getter
@Setter
public abstract class AbstractCategorieOperations extends AbstractAPIObjectModel { //

    @Serial
    private static final long serialVersionUID = -2116580840329551756L;
    /**
     * Identifiant
     */
    //@Id
    @Schema(description = "Identifiant")
    private String id;
    /**
     * Libelle
     */
    @EqualsAndHashCode.Include
    @Schema(description = "Libellé")
    private String libelle;
    /**
     * Actif
     */
    @Schema(description = "Etat d'activité")
    private boolean actif;

    /**
     * Constructeur pour Spring Data MongSB
     */
    public AbstractCategorieOperations() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Constructeur pour le clone
     *
     * @param guidCategorie guidCategorie du parent
     */
    public AbstractCategorieOperations(String guidCategorie) {
        this.id = guidCategorie;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.getLibelle();
    }
}
