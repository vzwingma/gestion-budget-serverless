package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IJwtSigningKeyRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Service de données en MongoDB fournissant les clés de signature des JWT
 * Adapteur du port {@link IJwtSigningKeyRepository}
 *
 * @author vzwingma
 */
@ApplicationScoped
public class JwsSigningKeysDatabaseAdaptor implements IJwtSigningKeyRepository {


    /**
     * Récupération des clés de signature des tokens JWT
     *
     * @return les clés de signature des tokens JWT
     */
    @Override
    public Multi<JwksAuthKey> getJwksSigningAuthKeys() {
        LOGGER.info("Récupération des clés de signature JWT");
        return streamAll();
    }
}
