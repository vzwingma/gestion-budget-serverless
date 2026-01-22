package io.github.vzwingma.finances.budget.services.communs.data.model;

import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractCategorieOperations;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serial;
import java.util.Set;
import java.util.UUID;

/**
 * Catégorie d'opérations
 *
 * @author vzwingma
 */
@MongoEntity(collection = "categoriesoperations")
@Getter
@Setter
@RegisterForReflection
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class CategorieOperations extends AbstractCategorieOperations implements Comparable<CategorieOperations> { //

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
     * Liste des sous catégories
     */
    @Getter
    @Schema(description = "Liste des sous catégories")
    private Set<SsCategorieOperations> listeSSCategories;
    /**
     * Constructeur pour Spring Data MongSB
     */
    public CategorieOperations() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Constructeur pour le clone
     *
     * @param guidCategorie guidCategorie du parent
     */
    public CategorieOperations(String guidCategorie) {
        this.id = guidCategorie;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
            return this.libelle;
    }

    /**
     * @param o the object to be compared.
     * @return comparison result
     */
    @Override
    public int compareTo(@NonNull  CategorieOperations o) {
        return this.libelle.compareTo(o.getLibelle());
    }
}
