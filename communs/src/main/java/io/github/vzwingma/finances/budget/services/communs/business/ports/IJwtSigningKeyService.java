package io.github.vzwingma.finances.budget.services.communs.business.ports;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
  * Interface définissant les services pour la gestion des clés de signature JWT.
  * Cette interface fournit des méthodes pour accéder aux clés de signature JWT
  * et les charger depuis un dépôt de lecture dédié.
  */

public interface IJwtSigningKeyService {



    /**
     * Logger
     */
    Logger logger = LoggerFactory.getLogger(IJwtSigningKeyService.class);

    IJwtSigningKeyReadRepository getSigningKeyReadRepository();

    /**
     * Les clés de signature JWT.
     */
    Map<String,JwksAuthKey> jwksAuthKeyList = new HashMap<>();

    /**
     *
     * @return la liste des clés
     */
    default Map<String,JwksAuthKey> getJwksAuthKeyList() {
        logger.trace("getJwksAuthKeyList : {} clés", jwksAuthKeyList.size());
        return jwksAuthKeyList;
    }


    /**
     *
     * @return chargement des clés JWKS
     */
    default Uni<Map<String, JwksAuthKey>> loadJwksSigningKeys() {
        logger.info("(Re)chargement des clés de signature JWT [{}] ", jwksAuthKeyList.size());
        return getSigningKeyReadRepository().getJwksSigningAuthKeys().collect().asMap(JwksAuthKey::getKid).invoke(jwksAuthKeyList::putAll);

    }
}
