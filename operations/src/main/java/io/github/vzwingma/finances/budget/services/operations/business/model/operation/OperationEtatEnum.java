/*

 */
package io.github.vzwingma.finances.budget.services.operations.business.model.operation;

import lombok.Getter;

/**
 * Type de dépenses
 * @author vzwingma
 *
 */
@Getter
public enum OperationEtatEnum {

	// Ligne prévue pour ce mois ci
	PREVUE("prevue", "Prévue"),
	// Ligne passée
	REALISEE("realisee", "Réalisée"),
	// Ligne planifiée pour une échéance dans le futur
	PLANIFIEE("planifiee" , "Planifiée"),
	// Ligne reportée pour le mois suivant
	REPORTEE("reportee" , "Reportée"),
	// Ligne annulée
	ANNULEE("annulee", "Annulée");


	private final String id;
	private final String libelle;

	/**
	 * Constructeur
	 * @param id : id de l'enum
	 * @param libelle : libellé de l'enum
	 */
	OperationEtatEnum(String id, String libelle){
		this.id = id;
		this.libelle = libelle;
	}


	public static OperationEtatEnum getEnum(String idEnum){
		for (OperationEtatEnum enums : values()) {
			if(enums.getId().equals(idEnum)){
				return enums;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString(){
		return getLibelle();
	}
}
