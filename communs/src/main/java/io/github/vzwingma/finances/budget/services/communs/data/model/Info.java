/**
 *
 */
package io.github.vzwingma.finances.budget.services.communs.data.model;

import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractAPIObjectModel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * API /_info
 * @author vzwingma
 *
 */
@Getter
@Setter
public class Info extends AbstractAPIObjectModel {

    @Serial
    private static final long serialVersionUID = 1415425535189056299L;

    String nom;
    String version;
    String description;

    public Info(String applicationName, String applicationVersion) {
        this.nom = applicationName;
        this.version = applicationVersion;
        this.description = "µService " + applicationName + " de l'application de gestion des finances";
    }
}
