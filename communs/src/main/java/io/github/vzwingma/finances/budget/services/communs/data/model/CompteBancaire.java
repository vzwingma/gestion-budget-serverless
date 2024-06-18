package io.github.vzwingma.finances.budget.services.communs.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractAPIObjectModel;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;

/**
 * Compte bancaire
 *
 * @author vzwingma
 */
@MongoEntity(collection = "comptesbancaires")
@Getter
@Setter
@NoArgsConstructor
public class CompteBancaire extends AbstractAPIObjectModel {

    @Serial
    private static final long serialVersionUID = -5529248923494509860L;


    //Id
    @Schema(description = "Identifiant")
    private String id;

    // Libellé du compte
    @Schema(description = "Libellé du compte")
    private String libelle;
    // Propriétaire du compte
    @JsonIgnore
    private Proprietaire proprietaire;
    // Icone
    @Schema(description = "Icone")
    private String itemIcon;
    // N° d'ordre
    @Schema(description = "n° d'ordre")
    private int ordre;
    // closed
    @Schema(description = "Etat d'activité")
    private Boolean actif;

    public static CompteBancaire getCompteInactif() {
        CompteBancaire compte = new CompteBancaire();
        compte.setId("inactif");
        compte.setLibelle("Compte inactif");
        compte.setActif(false);
        return compte;
    }

    /**
     * @return the actif
     */
    public Boolean isActif() {
        // Vrai par défaut
        return actif != null ? actif : Boolean.TRUE;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("CompteBancaire [id=%s, libelle=%s, actif=%s]", id, libelle, actif);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CompteBancaire other)) {
            return false;
        }
        if (id == null) {
            return other.id == null;
        } else return id.equals(other.id);
    }

    /**
     * Embeded Document Utilisateur (résumé d'un utilisateur)
     *
     * @author vzwingma
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Proprietaire implements Serializable {
        @Serial
        private static final long serialVersionUID = -5548729525207008917L;
        //Id
        private ObjectId id;
        // Login
        private String login;
        // Libellé
        private String libelle;
    }


}
