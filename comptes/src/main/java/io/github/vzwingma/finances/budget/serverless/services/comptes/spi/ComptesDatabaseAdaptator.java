package io.github.vzwingma.finances.budget.serverless.services.comptes.spi;

import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ports.IComptesRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service de données en MongoDB fournissant les comptes.
 * Adapteur du port {@link IComptesRepository}
 *
 * @author vzwingma
 */
@ApplicationScoped
public class ComptesDatabaseAdaptator implements IComptesRepository {


    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ComptesDatabaseAdaptator.class);

    /**
     * Chargement des comptes
     *
     * @param idUtilisateur utilisateur
     * @return liste des comptes associés
     */
    public Multi<CompteBancaire> chargeComptes(String idUtilisateur) {
        try {
            LOGGER.info("Chargement des comptes de l'utilisateur");
            return find("proprietaire.login", idUtilisateur)
                    .stream()
                    .invoke(compte -> LOGGER.debug("Chargement du compte [{}] en BDD terminé", compte.getLibelle()));
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la connexion à la BDD lors de la recherche de comptes", e);
            return Multi.createFrom().failure(new DataNotFoundException("Erreur lors de la recherche des comptes "));
        }
    }


    /**
     * Chargement d'un compte par un id
     *
     * @param idCompte      id du compte
     * @param idUtilisateur utilisateur associé
     * @return compte
     */
    public Uni<CompteBancaire> chargeCompteParId(String idCompte, String idUtilisateur) {
        try {
            LOGGER.info("Chargement du compte");
            return find("_id", idCompte)
                    .singleResult()
                    .invoke(compte -> LOGGER.debug("Chargement du compte [{}] en BDD terminé", compte.getLibelle()));
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la connexion à la BDD lors de la recherche de compte", e);
            return Uni.createFrom().failure(new DataNotFoundException("Erreur lors de la recherche du compte " + idCompte));
        }
    }


    /**
     * Chargement d'un compte par un id
     *
     * @param idCompte id du compte
     * @return compte
     */
    public Uni<Boolean> isCompteActif(String idCompte) {
        try {
            return find("id = ?1 and actif = ?2", idCompte, true)
                    .singleResultOptional()
                    .onItem()
                    .ifNull()
                    .failWith(new DataNotFoundException("Compte non trouvé"))
                    .map(compte -> compte.orElse(CompteBancaire.getCompteInactif()).getActif())
                    .invoke(compteActif -> LOGGER.info("Compte actif ? {}", compteActif));
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la connexion à la BDD lors de la recherche d'activité de comptes", e);
            return Uni.createFrom().failure(new DataNotFoundException("Erreur lors de la recherche du compte " + idCompte));
        }
    }
}
