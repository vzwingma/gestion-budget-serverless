package io.github.vzwingma.finances.budget.serverless.services.operations.business.ports;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service de données en MongoDB fournissant les clés de signature des JWT
 * Adapteur du port {@link IJwtSigningKeyRepository}
 *
 * @author vzwingma
 */

public interface IJwtSigningKeyRepository extends ReactivePanacheMongoRepository<JwksAuthKey>  {


    /**
     * Logger
     */
     Logger LOGGER = LoggerFactory.getLogger(IJwtSigningKeyRepository.class);

    /**
     * Récupération des clés de signature des tokens JWT
     *
     * @return les clés de signature des tokens JWT
     */
    Multi<JwksAuthKey> getJwksSigningAuthKeys() ;
}
