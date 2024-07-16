package io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Service de données en MongoDB fournissant les clés de signature des JWT
 * Adapteur du port {@link IParametragesRepository}
 *
 * @author vzwingma
 */
public interface IJwtSigningKeyRepository extends ReactivePanacheMongoRepository<JwksAuthKey>  {

    /**
     * Sauvegarde des clés de signature des tokens JWT
     *
     * @param jwksAuthKeys les clés de signature des tokens JWT
     * @return une {@link Uni} vide résultat de l'opération
     */
    Uni<Void> saveJwksAuthKeys(List<JwksAuthKey> jwksAuthKeys);


    /**
     * Récupération des clés de signature des tokens JWT
     *
     * @return les clés de signature des tokens JWT
     */
    Multi<JwksAuthKey> getJwksSigningAuthKeys();
}
