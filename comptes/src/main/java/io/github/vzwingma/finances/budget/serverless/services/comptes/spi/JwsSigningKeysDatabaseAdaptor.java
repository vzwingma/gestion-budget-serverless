package io.github.vzwingma.finances.budget.serverless.services.comptes.spi;

import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service de données en MongoDB fournissant les clés de signature des JWT
 * Adapteur du port {@link IJwtSigningKeyReadRepository}
 *
 * @author vzwingma
 */
@ApplicationScoped
public class JwsSigningKeysDatabaseAdaptor implements IJwtSigningKeyReadRepository, ReactivePanacheMongoRepository<JwksAuthKey> {


    /**
     * Récupération des clés de signature des tokens JWT
     *
     * @return les clés de signature des tokens JWT
     */
    @Override
    public Multi<JwksAuthKey> getJwksSigningAuthKeys() {
        return streamAll();
    }
}
