package io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation;

import lombok.Getter;

/**
 * Type de dépenses
 *
 * @author vzwingma
 */
@Getter
public enum OperationTypeEnum {

    // Crédit
    CREDIT("CREDIT", "+"),
    // Dépense
    DEPENSE("DEPENSE", "-");


    private final String id;
    private final String libelle;

    /**
     * Constructeur
     *
     * @param id      : id de l'énum
     * @param libelle : libellé de l'enum
     */
    OperationTypeEnum(String id, String libelle) {
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
