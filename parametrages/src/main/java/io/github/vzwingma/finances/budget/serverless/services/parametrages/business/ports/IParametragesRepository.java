package io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports;

import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;

/**
 * Service Provider Interface pour fournir les paramètres
 *
 * @author vzwingma
 */
public interface IParametragesRepository extends ReactivePanacheMongoRepository<CategorieOperations> {


    /**
     * @return la liste des catégories
     */
    Multi<CategorieOperations> chargeCategories();
}
