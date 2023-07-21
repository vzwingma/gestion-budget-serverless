package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.ports;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.UserAccessForbiddenException;
import io.smallrye.mutiny.Uni;

import java.time.LocalDateTime;

/**
 * Port de l'Application Provider Interface des Utilisateurs
 */
public interface IUtilisateursAppProvider {

    /**
     * Chargement d'un utilisateur
     * @param idUtilisateur login de l'utilisateur
     * @return Utilisateur correspondant au login
     */
    Uni<Utilisateur> getUtilisateur(String idUtilisateur);


    /**
     * Date de dernier accès
     * @param idUtilisateur login de l'utilisateur
     * @return date de dernier accès
     * @throws UserAccessForbiddenException erreur d'accès
     */
    Uni<LocalDateTime> getLastAccessDate(String idUtilisateur) throws UserAccessForbiddenException;
}
