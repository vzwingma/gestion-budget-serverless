package io.github.vzwingma.finances.budget.serverless.services.operations.business.model;

import lombok.Getter;

/**
 * Id des catégories
 *
 * @author vzwingma
 */
@Getter
public enum IdsCategoriesEnum {

    TRANSFERT_INTERCOMPTE("ed3f6100-5dbd-4b68-860e-0c97ae1bbc63"),
    SALAIRE("d005de34-f768-4e96-8ccd-70399792c48f"),
    REMBOURSEMENT("885e0d9a-6f3c-4002-b521-30169baf7123"),
    PRELEVEMENTS_MENSUELS("504beea7-ed52-438a-aced-15e9603b82ab"),
    FRAIS_REMBOURSABLES("b20a46a5-92ab-47a8-a70d-ecb64ddf02ce"),
    VIREMENT("ea6dcc12-3349-4047-a1e5-cd1d7254f16e");


    private final String id;

    /**
     * Constructeur
     *
     * @param id id de l'enum
     */
    IdsCategoriesEnum(String id) {
        this.id = id;
    }


    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return name();
    }
}
