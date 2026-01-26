package io.github.vzwingma.finances.budget.serverless.services.parametrages.business;


import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports.IParametrageAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports.IParametragesRepository;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.spi.IJwtAuthSigningKeyServiceProvider;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyWriteRepository;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyService;
import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractCategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.SsCategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service fournissant les paramètres
 *
 * @author vzwingma
 */
@ApplicationScoped
public class ParametragesService implements IParametrageAppProvider, IJwtSigningKeyService {


    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ParametragesService.class);
    /**
     * Service Provider Interface des données
     */

    private final IParametragesRepository dataParams;

    private final IJwtSigningKeyWriteRepository signingKeyRepository;

    private final IJwtSigningKeyReadRepository signingKeyReadRepository;
    @Inject
    @RestClient
    IJwtAuthSigningKeyServiceProvider jwtAuthSigningKeyServiceProvider; // Service fournissant les clés de signature JWT.


    /**
     * Constructeur
     *
     * @param parametrageRepository le repository des paramètres
     * @param signingKeyRepository le repository des clés de signature
     */
    @Inject
    public ParametragesService(IParametragesRepository parametrageRepository, IJwtSigningKeyWriteRepository signingKeyRepository, IJwtSigningKeyReadRepository signingKeyReadRepository){
        this.dataParams = parametrageRepository;
        this.signingKeyRepository = signingKeyRepository;
        this.signingKeyReadRepository = signingKeyReadRepository;
    }

    /**
     * Initialisation des clés de signature JWT de Google
     */
    public Uni<Void> refreshJwksSigningKeys() {
        LOGGER.info("Initialisation des clés de signature JWT");
        return jwtAuthSigningKeyServiceProvider.getJwksAuthKeys()
                .map(jwksAuthKeys -> Arrays.stream(jwksAuthKeys.getKeys()).toList())
                .flatMap(signingKeyRepository::saveJwksAuthKeys);
    }


    /**
     * '
     *
     * @return liste des catégories
     */
    public Uni<List<CategorieOperations>> getCategories() {

        return dataParams.chargeCategories()
                .filter(c -> c.getListeSSCategories() != null && !c.getListeSSCategories().isEmpty())
                .filter(CategorieOperations::isActif)
                .map(this::cloneCategorie)
                //async call for log
                .invoke(c -> {
                    LOGGER.debug("[{}][{}] {}", c.isActif() ? "v" : "X", c.getId(), c);
                    c.getListeSSCategories().forEach(s -> LOGGER.debug("[{}][{}]\t{}\t\t[{}]", s.isActif() ? "v" : "X", s.getId(), s, s.getType()));
                })
                .collect().asList();
    }

    /**
     * @param idCategorie identifiant de la catégorie
     * @return la catégorie correspondante à l'id
     */
    @Override
    public Uni<AbstractCategorieOperations> getCategorieById(String idCategorie) {

        return getCategories()
                .flatMap(categories -> {
                    final AtomicReference<AbstractCategorieOperations> categorie = new AtomicReference<>();
                    categories.forEach(c -> {
                        if (c.getId().equals(idCategorie)) {
                            categorie.set(c);
                        }
                        c.getListeSSCategories().forEach(s -> {
                            if (s.getId().equals(idCategorie)) {
                                s.setCategorieParente(new SsCategorieOperations.CategorieParente(c.getId(), c.getLibelle()));
                                LOGGER.info("Sous Catégorie trouvée : {}/{}", s.getCategorieParente(), s);
                                categorie.set(s);
                            }
                        });
                    });
                    if (categorie.get() == null) {
                        return Uni.createFrom().failure(new DataNotFoundException("[idCategorie=" + idCategorie + "] (Ss)Categorie non trouvée"));
                    } else {
                        return Uni.createFrom().item(categorie.get());
                    }
                });
    }


    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    private CategorieOperations cloneCategorie(CategorieOperations categorie) {
        if (categorie != null && categorie.isActif()) {
            CategorieOperations clone = new CategorieOperations();
            clone.setId(categorie.getId());
            clone.setActif(categorie.isActif());
            // Pas de clone de la catégorie parente pour éviter les récursions
            clone.setLibelle(categorie.getLibelle());
            Set<SsCategorieOperations> setSSCatsClones = new HashSet<>();
            if (categorie.getListeSSCategories() != null && !categorie.getListeSSCategories().isEmpty()) {

                categorie.getListeSSCategories()
                        .stream()
                        // #125
                        .filter(SsCategorieOperations::isActif)
                        .forEach(ssC -> {
                            SsCategorieOperations ssCClone = cloneSsCategorie(ssC);
                            // Réinjection de la catégorie parente
                            ssCClone.setCategorieParente(new SsCategorieOperations.CategorieParente(clone.getId(), clone.getLibelle()));
                            setSSCatsClones.add(ssCClone);
                        });
                clone.setListeSSCategories(setSSCatsClones);
            }
            return clone;
        }
        return null;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    private SsCategorieOperations cloneSsCategorie(SsCategorieOperations categorie) {
        if (categorie != null && categorie.isActif()) {
            SsCategorieOperations clone = new SsCategorieOperations();
            clone.setId(categorie.getId());
            clone.setActif(categorie.isActif());
            clone.setType(categorie.getType());
            // Pas de clone de la catégorie parente pour éviter les récursions
            clone.setLibelle(categorie.getLibelle());
            return clone;
        }
        return null;
    }


    /**
     * @return le repository des clés de signature
     */
    @Override
    public IJwtSigningKeyReadRepository getSigningKeyReadRepository() {
       return signingKeyReadRepository;
    }
}
