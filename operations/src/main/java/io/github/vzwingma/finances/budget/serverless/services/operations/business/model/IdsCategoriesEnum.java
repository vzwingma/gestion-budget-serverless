package io.github.vzwingma.finances.budget.serverless.services.operations.business.model;

import lombok.Getter;

/**
 * Id des catégories
 *
 * @author vzwingma
 */
@Getter
public enum IdsCategoriesEnum {

    // Virement interne
    CAT_RENTREES("ea6dcc12-3349-4047-a1e5-cd1d7254f16e", "Rentrées"),
    SS_CAT_VIREMENT_INTERNE("ed3f6100-5dbd-4b68-860e-0c97ae1bbc63", "Virements internes"),
    SS_CAT_RENTREE_VIREMENT_INTERNE("ed3f6100-5dbd-4b68-860e-0c97ae1bbc74", "Virements internes"),

    REMBOURSEMENT("885e0d9a-6f3c-4002-b521-30169baf7123", "Remboursement"),
    FRAIS_REMBOURSABLES("b20a46a5-92ab-47a8-a70d-ecb64ddf02ce", "Frais remboursables"),;



    private final String id;
    private final String libelle;
    /**
     * Constructeur
     *
     * @param id id de l'enum
     */
    IdsCategoriesEnum(String id, String libelle) {

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
