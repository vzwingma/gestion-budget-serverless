package io.github.vzwingma.finances.budget.services.communs.data.model;

import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractAPIObjectModel;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;

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
public class CategorieOperations extends AbstractAPIObjectModel implements Comparable<CategorieOperations> { //

    @Serial
    private static final long serialVersionUID = 1L;
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
    private Set<CategorieOperations> listeSSCategories;

    /**
     * Catégorie
     */
    @Getter
    @Schema(description = "Catégorie parente")
    private CategorieOperations.CategorieParente categorieParente;

    /**
     * Est ce une catégorie ?
     */
    @Schema(description = "Est ce une catégorie")
    private boolean categorie = true;


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

    /**
     * @param listeSSCategories the listeSSCategories to set
     */
    public void setListeSSCategories(Set<CategorieOperations> listeSSCategories) {
        if (isCategorie()) {
            this.listeSSCategories = listeSSCategories;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (this.isCategorie()) {
            return this.libelle;
        } else {
            return (this.categorieParente != null ? this.categorieParente.libelle : "?") + "/" + this.libelle;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(@NotNull CategorieOperations o) {
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
