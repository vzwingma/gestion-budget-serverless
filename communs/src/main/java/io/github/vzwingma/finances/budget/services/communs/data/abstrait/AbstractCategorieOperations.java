package io.github.vzwingma.finances.budget.services.communs.data.abstrait;

import java.io.Serial;
import java.util.UUID;

/**
 * Catégorie d'opérations
 *
 * @author vzwingma
 */
public abstract class AbstractCategorieOperations extends AbstractAPIObjectModel { //

    @Serial
    private static final long serialVersionUID = -2116580840329551756L;
    /**
     * Identifiant
     */
    String id;
    /**
     * Constructeur pour Spring Data MongSB
     */
    public AbstractCategorieOperations() {
        this.id = UUID.randomUUID().toString();
    }
}
