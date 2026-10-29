package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.ports;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;

import java.time.LocalDateTime;

/**
 * Interface du Service Provider BDD Utilisateurs
 */
public interface IUtilisateursRepository extends ReactivePanacheMongoRepository<Utilisateur> {


    /**
     * @param login : login utilisateur
     * @return Utilisateur
     */
    Uni<Utilisateur> chargeUtilisateur(String login);

    /**
     * Met à jour uniquement la date de dernier accès de l'utilisateur ({@code $set} ciblé sur {@code dernierAcces}).
     * L'écriture est chaînée dans le flux réactif : l'appelant décide de la réponse en cas d'échec.
     *
     * @param login login utilisateur
     * @param date  nouvelle date de dernier accès
     * @return flux vide à la fin de l'écriture
     */
    Uni<Void> majDernierAcces(String login, LocalDateTime date);
}
