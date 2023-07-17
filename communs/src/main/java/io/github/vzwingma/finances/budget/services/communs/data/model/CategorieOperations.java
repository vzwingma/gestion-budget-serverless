package io.github.vzwingma.finances.budget.services.communs.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractAPIObjectModel;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.beans.Transient;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * Catégorie d'opérations
 * @author vzwingma
 *
 */
@MongoEntity(collection = "categoriesoperations")
@Getter
@Setter
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
	@Schema(description = "Liste des sous catégories")
	private Set<CategorieOperations> listeSSCategories;

	/**
	 * Catégorie
	 */
	@BsonIgnore
	@Schema(description = "Catégorie parente")
	private CategorieOperations.CategorieParente categorieParente;

	/**
	 * Est ce une catégorie ?
	 */
	@Schema(description = "Est ce une catégorie")
	private boolean categorie = true;


	@Getter @Setter @NoArgsConstructor
	@Schema(description = "Catégorie parente de la sous catégorie")
	public static class CategorieParente implements Serializable {

		@Serial
		private static final long serialVersionUID = 3069367940675936890L;

		public CategorieParente(String id, String libelle){
			this.id = id;
			this.libelle = libelle;
		}
		@Schema(description = "id Catégorie parente")
		private String id;

		@Schema(description = "Libelle Catégorie parente")
		private String libelle;
		@Override
		public String toString() {
			return libelle;
		}
	}

	/**
	 * Constructeur pour Spring Data MongSB
	 */
	public CategorieOperations(){
		this.id = UUID.randomUUID().toString();
	}
	
	/**
	 * Constructeur pour le clone
	 * @param guidCategorie guidCategorie du parent
	 */
	public CategorieOperations(String guidCategorie){
		this.id = guidCategorie;
	}

	/**
	 * @return the listeSSCategories
	 */
	public Set<CategorieOperations> getListeSSCategories() {
		return listeSSCategories;
	}

	/**
	 * @param listeSSCategories the listeSSCategories to set
	 */
	public void setListeSSCategories(Set<CategorieOperations> listeSSCategories) {
		if(isCategorie()) {
			this.listeSSCategories = listeSSCategories;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.libelle;
	}


	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(CategorieOperations o) {
		if(o != null){
			return this.libelle.compareTo(o.getLibelle());
		}
		return 0;
	}
}
