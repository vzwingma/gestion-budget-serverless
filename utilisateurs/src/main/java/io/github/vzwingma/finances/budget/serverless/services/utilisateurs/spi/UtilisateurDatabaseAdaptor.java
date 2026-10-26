package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.spi;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.ports.IUtilisateursRepository;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Service Provider  données en MongoDB fournissant les infos des utilisateurs et comptes
 *
 * @author vzwingma
 */
@ApplicationScoped
public class UtilisateurDatabaseAdaptor implements IUtilisateursRepository {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilisateurDatabaseAdaptor.class);

    /**
     * @param login : login utilisateur
     * @return Utilisateur
     */
    public Uni<Utilisateur> chargeUtilisateur(String login) {
        try {
            LOGGER.info("Recherche de l'utilisateur [{}]", login);
            return find("login", login)
                    .singleResultOptional()
                    .map(utilisateur -> utilisateur.orElse(null));
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la connexion à la BDD", e);
            return Uni.createFrom().failure(new DataNotFoundException("Erreur lors de la recherche d'utilisateur " + login));
        }
    }


    /**
     * Met à jour uniquement la date de dernier accès ({@code $set} ciblé sur {@code dernierAcces}).
     *
     * @param login login utilisateur
     * @param date  date de dernier accès
     * @return flux vide à la fin de l'écriture
     */
    @Override
    public Uni<Void> majDernierAcces(String login, LocalDateTime date) {
        LOGGER.info("Mise à jour de la date de dernier accès de l'utilisateur [{}]", login);
        Date dateMongo = Date.from(Instant.ofEpochMilli(BudgetDateTimeUtils.getMillisecondsFromLocalDateTime(date)));
        return mongoCollection()
                .updateOne(Filters.eq("login", login), Updates.set("dernierAcces", dateMongo))
                .invoke(result -> LOGGER.info("Utilisateur [{}] : dernier accès mis à jour ({} document modifié)", login, result.getModifiedCount()))
                .replaceWithVoid();
    }
}
