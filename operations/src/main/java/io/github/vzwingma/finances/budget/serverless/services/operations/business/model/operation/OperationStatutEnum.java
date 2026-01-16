/*

 */
package io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation;

import lombok.Getter;

/**
 * Type de dépenses
 *
 * @author vzwingma
 */
@Getter
public enum OperationStatutEnum {

    // Ligne en retard
    EN_RETARD("enretard", "En retard"),
    // Ligne passée
    DERNIERE_ECHEANCE("derniereecheance", "Dernière échéance");


    private final String id;
    private final String libelle;

    /**
     * Constructeur
     *
     * @param id      : id de l'enum
     * @param libelle : libellé de l'enum
     */
    OperationStatutEnum(String id, String libelle) {
        this.id = id;
        this.libelle = libelle;
    }


    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return getLibelle();
    }
}
