/*

 */
package io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation;

import lombok.Getter;

/**
 * Libellé dans les opérations
 *
 * @author vzwingma
 */
@Getter
public enum LibellesOperationEnum {

    // Libellé en retard
    EN_RETARD("retard", "[En Retard]");


    private final String id;
    private final String libelle;

    /**
     * Constructeur
     *
     * @param id      : id de l'enum
     * @param libelle : libellé de l'enum
     */
    LibellesOperationEnum(String id, String libelle) {
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
