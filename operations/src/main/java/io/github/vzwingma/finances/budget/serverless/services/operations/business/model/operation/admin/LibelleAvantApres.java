package io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.admin;

import lombok.Getter;
import lombok.Setter;


/**
 * Classe permettant de stocker un libellé avant et après modification
  * exemple :
 * [
 *     { "avant": "*** Alignement ***", "apres": "Alignement" },
 *     { "avant": "13 eme mois", "apres": "13ème mois" },
 * ]
 */
@Getter @Setter
public class LibelleAvantApres {
    private String avant;
    private String apres;

}
