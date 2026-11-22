package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.AbstractBusinessException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.ServiceDistantIndisponibleException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.UserNotAuthorizedException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import java.util.Set;

/**
 * Traduction des réponses en erreur des services distants (comptes, parametrages) en exceptions typées (AP004 T2.4, ADR-009).
 * <table>
 *   <caption>Réponse de l'aval, exception levée, statut renvoyé par l'API</caption>
 *   <tr><td>404</td><td>{@link DataNotFoundException}</td><td>404</td></tr>
 *   <tr><td>401</td><td>{@link UserNotAuthorizedException}</td><td>401</td></tr>
 *   <tr><td>429, 502, 503, 504</td><td>{@link ServiceDistantTransitoireException} (retry possible)</td><td>503</td></tr>
 *   <tr><td>403, autres 4xx, autres 5xx</td><td>{@link ServiceDistantIndisponibleException}</td><td>503</td></tr>
 * </table>
 * Un 403 renvoyé par l'aval n'est <b>jamais</b> transmis en 403 : l'IHM déconnecterait l'utilisateur. Le corps de la
 * réponse n'est pas lu ni recopié (aucune donnée de l'aval dans les messages).
 */
public class ServicesDistantsResponseExceptionMapper implements ResponseExceptionMapper<AbstractBusinessException> {

    /** Statuts de l'aval pour lesquels un retry rapide a un sens */
    private static final Set<Integer> STATUTS_TRANSITOIRES = Set.of(429, 502, 503, 504);

    @Override
    public AbstractBusinessException toThrowable(Response response) {
        int statut = response.getStatus();
        if (statut == Response.Status.NOT_FOUND.getStatusCode()) {
            return new DataNotFoundException("Ressource non trouvée dans le service distant");
        }
        if (statut == Response.Status.UNAUTHORIZED.getStatusCode()) {
            return new UserNotAuthorizedException("Accès refusé par le service distant (authentification)");
        }
        String libelle = "Service distant indisponible (statut " + statut + ")";
        return STATUTS_TRANSITOIRES.contains(statut)
                ? new ServiceDistantTransitoireException(libelle)
                : new ServiceDistantIndisponibleException(libelle);
    }
}
