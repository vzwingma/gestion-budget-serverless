package io.github.vzwingma.finances.budget.serverless.services.parametrages.spi;

import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyWriteRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.stream.Stream;

/**
 * Service de données en MongoDB fournissant les clés de signature des JWT
 * Adapteur du port {@link IJwtSigningKeyWriteRepository} et {@link IJwtSigningKeyReadRepository}
 *
 * @author vzwingma
 */
@ApplicationScoped
public class JwsSigningKeysDatabaseAdaptor implements IJwtSigningKeyReadRepository, IJwtSigningKeyWriteRepository, PanacheMongoRepository<JwksAuthKey> {

    /**
     * Sauvegarde des clés de signature des tokens JWT
     *
     * @param jwksAuthKeys les clés de signature des tokens JWT
     */
    @Override
    public void saveJwksAuthKeys(List<JwksAuthKey> jwksAuthKeys) {
        persistOrUpdate(jwksAuthKeys.stream());
    }

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
