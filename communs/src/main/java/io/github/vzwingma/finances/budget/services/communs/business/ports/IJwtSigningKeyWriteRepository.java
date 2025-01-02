package io.github.vzwingma.finances.budget.services.communs.business.ports;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import java.util.List;

/**
 * Service de données en écriture en MongoDB fournissant les clés de signature des JWT
 * Adapteur du port {@link IJwtSigningKeyWriteRepository}
 *
 * @author vzwingma
 */

public interface IJwtSigningKeyWriteRepository {

    /**
     * Sauvegarde des clés de signature des tokens JWT
     *
     * @param jwksAuthKeys les clés de signature des tokens JWT
     */
    void saveJwksAuthKeys(List<JwksAuthKey> jwksAuthKeys);

}
