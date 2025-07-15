package io.github.vzwingma.finances.budget.services.communs.business.ports;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


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
    List<JwksAuthKey> jwksAuthKeyList = new ArrayList<>();

    /**
     *
     * @return la liste des clés
     */
    default List<JwksAuthKey> getJwksAuthKeyList() {
        logger.debug("getJwksAuthKeyList : {} clés", jwksAuthKeyList.size());
        return jwksAuthKeyList;
    }


    default void loadJwksSigningKeys(){
        logger.info("Chargement des clés de signature JWT");
            getSigningKeyReadRepository().getJwksSigningAuthKeys().onItem().invoke(jwksAuthKey -> {
                jwksAuthKeyList.add(jwksAuthKey);
                logger.info(" - Clé de signature JWKS chargée : {}", jwksAuthKey.getKid());
            });
    }

}
