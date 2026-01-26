package io.github.vzwingma.finances.budget.services.communs.data.model;

import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractAPIObjectModel;
import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractCategorieOperations;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.NonNull;

import java.io.Serial;

/**
 * Sous Catégorie d'opérations
 *
 * @author vzwingma
 */
@Getter
@Setter
@RegisterForReflection
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class SsCategorieOperations extends AbstractCategorieOperations implements Comparable<SsCategorieOperations> { //

    @Serial
    private static final long serialVersionUID = -2116580858329551756L;
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
     * Catégorie
     */
    @Getter
    @Schema(description = "Catégorie parente")
    private SsCategorieOperations.CategorieParente categorieParente;

    /**
     * Type de catégorie
     */
    @Schema(description = "Type de catégorie")
    private CategorieOperationTypeEnum type;

    /**
     * Constructeur pour Spring Data MongDB
     */
    public SsCategorieOperations() {
        super();
    }

    /**
     * Constructeur pour Spring Data MongDB
     */
    public SsCategorieOperations(String guidCategorie) {
        this.id = guidCategorie;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return (this.categorieParente != null ? this.categorieParente.libelle : "-") + "/" + this.libelle;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(@NonNull SsCategorieOperations o) {
        return this.libelle.compareTo(o.getLibelle());
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @RegisterForReflection
    @Schema(description = "Catégorie parente de la sous catégorie")
    public static class CategorieParente extends AbstractAPIObjectModel {

        @Serial
        private static final long serialVersionUID = 3069367940675936890L;
        @Schema(description = "id Catégorie parente")
        private String id;
        @Schema(description = "Libelle Catégorie parente")
        private String libelle;

        public CategorieParente(String id, String libelle) {
            this.id = id;
            this.libelle = libelle;
        }

        @Override
        public String toString() {
            return libelle;
        }
    }
}
