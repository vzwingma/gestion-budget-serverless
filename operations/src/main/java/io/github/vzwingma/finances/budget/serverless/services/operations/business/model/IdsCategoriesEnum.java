package io.github.vzwingma.finances.budget.serverless.services.operations.business.model;

import lombok.Getter;


/**
 * Id des catégories
 *
 * @author vzwingma
 */
@Getter
public enum IdsCategoriesEnum {

    // Virements internes
    CAT_RENTREES("ea6dcc12-3349-4047-a1e5-cd1d7254f16e", "Rentrées"),
    SS_CAT_VIREMENT_INTERNE("ed3f6100-5dbd-4b68-860e-0c97ae1bbc63", "Virements internes"),
    SS_CAT_RENTREE_VIREMENT_INTERNE("ed3f6100-5dbd-4b68-860e-0c97ae1bbc74", "Virements internes"),

    // Actifs & investissements
    CAT_ACTIFS_INVESTS("33bef45d-9494-4a69-94ea-228a22bbc699", "Investissements/Actifs"),

    // Gestion des frais remboursables
    SS_CAT_REMBOURSEMENT("885e0d9a-6f3c-4002-b521-30169baf7123", "Remboursement"),

    SS_CAT_FRAIS_REMBOURSABLE_PRO_NDF("bd28c498-a774-4a67-b6ec-a135d39fca46", "Notes de frais"),
    CAT_FRAIS_REMBOURSABLE_SANTE("b20a46a5-92ab-47a8-a70d-ecb64ddf02ce", "Santé"),
    SS_CAT_FRAIS_REMBOURSABLE_SANTE_DENTISTE("a705ff05-3589-4b9f-8884-ff0716342309", "Dentiste"),
    SS_CAT_FRAIS_REMBOURSABLE_SANTE_MEDECIN("6e96b0c3-ecc5-4be8-8087-0087b5e46baf", "Médecin"),
    SS_CAT_FRAIS_REMBOURSABLE_SANTE_OPTICIEN("347c8e22-b021-4bd6-bffc-0d683925cf29", "Opticien"),
    SS_CAT_FRAIS_REMBOURSABLE_SANTE_PHARMACIE("eeb2f9a5-49b4-4c44-86bf-3bd626412d8e", "Pharmacie");


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
