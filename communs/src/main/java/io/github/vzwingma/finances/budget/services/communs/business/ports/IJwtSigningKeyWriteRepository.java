package io.github.vzwingma.finances.budget.services.communs.business.ports;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service de données en MongoDB fournissant les clés de signature des JWT
 * Adapteur du port {@link IJwtSigningKeyWriteRepository}
 *
 * @author vzwingma
 */

public interface IJwtSigningKeyWriteRepository {

    /**
     * Sauvegarde des clés de signature des tokens JWT
     *
     * @param jwksAuthKeys les clés de signature des tokens JWT
     * @return une {@link Uni} vide résultat de l'opération
     */
    Uni<Void> saveJwksAuthKeys(List<JwksAuthKey> jwksAuthKeys);

}
