package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import java.io.Serial;

/**
 * Service distant (autre µService appelé en REST) indisponible : connexion impossible, timeout, ou réponse en erreur
 * qui n'est pas imputable à l'appelant (403, 429, 5xx...).
 * <p>
 * Aucun cas dédié dans {@code AbstractAPIExceptionsHandler} : cette exception est traduite par son repli final en
 * statut HTTP <b>503</b>. Un 403 renvoyé par l'aval ne doit jamais devenir un 403 vers l'IHM (déconnexion de l'utilisateur).
 *
 * @author vzwingma
 */
public class ServiceDistantIndisponibleException extends AbstractBusinessException {

    @Serial
    private static final long serialVersionUID = 4510347868822036211L;

    /**
     * @param libelleErreur libellé de l'erreur (sans donnée sensible)
     */
    public ServiceDistantIndisponibleException(String libelleErreur) {
        super(libelleErreur);
    }
}
