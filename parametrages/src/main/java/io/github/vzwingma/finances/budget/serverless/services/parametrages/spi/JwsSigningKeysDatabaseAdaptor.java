package io.github.vzwingma.finances.budget.serverless.services.parametrages.spi;

import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports.IJwtSigningKeyRepository;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports.IParametragesRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service de données en MongoDB fournissant les clés de signature des JWT
 * Adapteur du port {@link IParametragesRepository}
 *
 * @author vzwingma
 */
@ApplicationScoped
public class JwsSigningKeysDatabaseAdaptor implements IJwtSigningKeyRepository {


    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JwsSigningKeysDatabaseAdaptor.class);


    /**
     * Sauvegarde des clés de signature JWT
     *
     * @param jwksAuthKeys les clés de signature JWT
     * @return une {@link Uni} vide
     */
    @Override
    public Uni<Void> saveJwksAuthKeys(List<JwksAuthKey> jwksAuthKeys) {
        LOGGER.info("Sauvegarde des {} clés de signature JWT", jwksAuthKeys.size());
        return persistOrUpdate(jwksAuthKeys.stream());
    }

    /**
     * Récupération des clés de signature JWT
     * @return les clés de signature JWT
     */
    @Override
    public Multi<JwksAuthKey> getJwksSigningAuthKeys() {
        LOGGER.info("Récupération des clés de signature JWT");
        return streamAll();
    }
}
