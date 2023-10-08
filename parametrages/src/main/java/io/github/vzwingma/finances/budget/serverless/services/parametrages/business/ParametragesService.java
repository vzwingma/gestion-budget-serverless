package io.github.vzwingma.finances.budget.serverless.services.parametrages.business;


import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports.IParametrageAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports.IParametragesRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service fournissant les paramètres
 * @author vzwingma
 *
 */
@ApplicationScoped
@NoArgsConstructor
public class ParametragesService implements IParametrageAppProvider {


	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ParametragesService.class);
	/**
	 * Service Provider Interface des données
	 */
	@Inject
	IParametragesRepository dataParams;

	public ParametragesService(IParametragesRepository parametrageRepository){
		this.dataParams = parametrageRepository;
	}


	/**'
	 * @return liste des catégories
	 */
	public Uni<List<CategorieOperations>> getCategories(){

			return dataParams.chargeCategories()
					.filter(c -> c.getListeSSCategories() != null && !c.getListeSSCategories().isEmpty())
					.filter(CategorieOperations::isActif)
					.map(this::cloneCategorie)
					//async call for log
					.invoke(c -> {
						LOGGER.debug("[{}][{}] {}", c.isActif() ? "v" : "X", c.getId(), c);
						c.getListeSSCategories().forEach(s -> LOGGER.debug("[{}][{}]\t\t{}", s.isActif() ? "v" : "X", s.getId(), s));
					})
					.collect().asList();
	}

	/**
	 *
	 * @param idCategorie identifiant de la catégorie
	 * @return  la catégorie correspondante à l'id
	 */
	@Override
	public Uni<CategorieOperations> getCategorieById(String idCategorie) {

		return getCategories()
				.flatMap(categories -> {
					final AtomicReference<CategorieOperations> categorie = new AtomicReference<>();
					categories.forEach(c -> {
						if(c.getId().equals(idCategorie)){
							categorie.set(c);
						}
						c.getListeSSCategories().forEach(s -> {
							if(s.getId().equals(idCategorie)){
								s.setCategorieParente(new CategorieOperations.CategorieParente(c.getId(), c.getLibelle()));
								LOGGER.info("Sous Catégorie trouvée : {}/{}" , c, s);
								categorie.set(s);
							}
						});
					});
					if(categorie.get() == null){
						return Uni.createFrom().failure(new DataNotFoundException("[idCategorie="+idCategorie+"] Categorie non trouvée"));
					}
					else{
						return Uni.createFrom().item(categorie.get());
					}
				});
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	private CategorieOperations cloneCategorie(CategorieOperations categorie) {
		if(categorie != null && categorie.isActif()){
			CategorieOperations clone = new CategorieOperations();
			clone.setId(categorie.getId());
			clone.setActif(categorie.isActif());
			clone.setCategorie(categorie.isCategorie());
			// Pas de clone de la catégorie parente pour éviter les récursions
			clone.setLibelle(categorie.getLibelle());
			Set<CategorieOperations> setSSCatsClones = new HashSet<>();
			if(categorie.getListeSSCategories() != null && !categorie.getListeSSCategories().isEmpty()){

				categorie.getListeSSCategories()
						.stream()
						// #125
						.filter(CategorieOperations::isActif)
						.forEach(ssC -> {
							CategorieOperations ssCClone = cloneCategorie(ssC);
							// Réinjection de la catégorie parente
							ssCClone.setCategorieParente(new CategorieOperations.CategorieParente(clone.getId(), clone.getLibelle()));
							setSSCatsClones.add(ssCClone);
						});
				clone.setListeSSCategories(setSSCatsClones);
			}
			return clone;
		}
		return null;
	}
}
