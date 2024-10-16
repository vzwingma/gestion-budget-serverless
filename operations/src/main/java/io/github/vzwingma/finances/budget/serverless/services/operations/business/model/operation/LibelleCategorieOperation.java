package io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation;

import lombok.Getter;
import lombok.Setter;


/**
 * * Classe représentant un libellé d'opération et sa catégorie associée.
 *
 */
@Getter @Setter
public class LibelleCategorieOperation {
    private String libelle;
    private String categorieId;
    private String ssCategorieId;
}
