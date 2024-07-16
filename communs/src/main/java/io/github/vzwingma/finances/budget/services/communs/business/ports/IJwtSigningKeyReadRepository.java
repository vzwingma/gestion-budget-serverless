package io.github.vzwingma.finances.budget.services.communs.business.ports;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import java.util.stream.Stream;

/**
 * Service de données en lecture en MongoDB fournissant les clés de signature des JWT
 * Adapteur du port {@link IJwtSigningKeyReadRepository}
 *
 * @author vzwingma
 */

public interface IJwtSigningKeyReadRepository {

    /**
     * Récupération des clés de signature des tokens JWT
     *
     * @return les clés de signature des tokens JWT
     */
    Stream<JwksAuthKey> getJwksSigningAuthKeys() ;
}
