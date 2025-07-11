package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.ports.IUtilisateursAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.ports.IUtilisateursRepository;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Service Utilisateurs
 *
 * @author vzwingma
 */
@ApplicationScoped
@NoArgsConstructor
public class UtilisateursService implements IUtilisateursAppProvider, IJwtSigningKeyService {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilisateursService.class);
    /**
     * Utilisateurs
     */
    @Inject
    IUtilisateursRepository dataDBUsers;
    @Inject
    IJwtSigningKeyReadRepository iJwtSigningKeyReadRepository;
    /**
     * Constructeur (pour les tests)
     *
     * @param spiUtilisateurs Service port Interface Utilisateurs
     */
    public UtilisateursService(IUtilisateursRepository spiUtilisateurs) {
        this.dataDBUsers = spiUtilisateurs;
    }

    /**
     * @param loginUtilisateur login de l'utilisateur
     * @return date de dernier accès
     */
    public Uni<Utilisateur> getUtilisateur(String loginUtilisateur) {
        return dataDBUsers.chargeUtilisateur(loginUtilisateur);
    }

    /**
     * Date de dernier accès
     *
     * @param login login de l'utilisateur
     * @return date de dernier accès
     */
    public Uni<LocalDateTime> getLastAccessDate(String login) {
        // Enregistrement de la date du dernier accès à maintenant, en async
        return getUtilisateur(login)
                .onItem().transform(user -> {
                    LOGGER.info("{} accède à l'application", user != null ? user.toFullString() : "Utilisateur inconnu");
                    if(user != null){
                        updateUtilisateurLastConnection(user);
                    }
                    return user;
                })
                .map(Utilisateur::getDernierAcces);
    }

    /**
     * Mise à jour de la date de dernier accès
     *
     * @param utilisateurUni utilisateur connecté
     */
    private void updateUtilisateurLastConnection(Utilisateur utilisateurUni) {
        Utilisateur clone = new Utilisateur(utilisateurUni);
        clone.setDernierAcces(LocalDateTime.now());
        dataDBUsers.majUtilisateur(clone);
    }

    /**
     * @return le dépôt des clés de signature
     */
    @Override
    public IJwtSigningKeyReadRepository getSigningKeyReadRepository() {
        return iJwtSigningKeyReadRepository;
    }

}
