package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.spi;

import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.stream.Stream;

/**
 * Service de données en MongoDB fournissant les clés de signature des JWT
 * Adapteur du port {@link IJwtSigningKeyReadRepository}
 *
 * @author vzwingma
 */
@ApplicationScoped
public class JwsSigningKeysDatabaseAdaptor implements IJwtSigningKeyReadRepository, PanacheMongoRepository<JwksAuthKey> {


    /**
     * Récupération des clés de signature des tokens JWT
     *
     * @return les clés de signature des tokens JWT
     */
    @Override
    public Stream<JwksAuthKey> getJwksSigningAuthKeys() {
        return streamAll();
    }
}
