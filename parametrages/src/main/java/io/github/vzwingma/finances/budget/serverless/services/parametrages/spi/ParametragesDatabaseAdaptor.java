package io.github.vzwingma.finances.budget.serverless.services.parametrages.spi;

import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports.IParametragesRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service de données en MongoDB fournissant les paramètres
 * Adapteur du port {@link IParametragesRepository}
 * @author vzwingma
 *
 */
@ApplicationScoped
public class ParametragesDatabaseAdaptor implements IParametragesRepository {


	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ParametragesDatabaseAdaptor.class);

	/**
	 * @return la liste des catégories
	 */
	public Multi<CategorieOperations> chargeCategories() {

		try {
			LOGGER.info("Chargement des catégories en BDD");
			return findAll().stream()
					.invoke(cat -> LOGGER.debug("Chargement de la catégorie [{}] en BDD terminé", cat));
		} catch (Exception e) {
			LOGGER.error("Erreur lors de la connexion à la BDD", e);
			return Multi.createFrom().failure(new DataNotFoundException("Erreur lors de la connexion à la BDD"));
		}
	}
}
