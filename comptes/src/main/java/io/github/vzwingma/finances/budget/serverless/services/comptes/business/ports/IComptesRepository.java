package io.github.vzwingma.finances.budget.serverless.services.comptes.business.ports;

import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Service Provider Interface pour fournir les paramètres
 * @author vzwingma
 *
 */
public interface IComptesRepository extends ReactivePanacheMongoRepository<CompteBancaire> {


    /**
     * Chargement des comptes
     * @param idUtilisateur utilisateur
     * @return liste des comptes associés
     */
    Multi<CompteBancaire> chargeComptes(String idUtilisateur);


    /**
     * Chargement d'un compte par un id
     * @param idCompte id du compte
     * @param idUtilisateur utilisateur associé
     * @return compte
     */
    Uni<CompteBancaire> chargeCompteParId(String idCompte, String idUtilisateur) ;


    /**
     * Chargement d'un compte par un id
     * @param idCompte id du compte
     * @return compte actif
     */
    Uni<Boolean> isCompteActif(String idCompte) ;
}
