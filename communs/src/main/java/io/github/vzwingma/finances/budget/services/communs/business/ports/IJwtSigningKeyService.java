package io.github.vzwingma.finances.budget.services.communs.business.ports;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.smallrye.mutiny.Multi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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


    default Multi<JwksAuthKey> loadJwksSigningKeys(){
        logger.info("Chargement des clés de signature JWT");
        return getSigningKeyReadRepository().getJwksSigningAuthKeys();
    }

}
