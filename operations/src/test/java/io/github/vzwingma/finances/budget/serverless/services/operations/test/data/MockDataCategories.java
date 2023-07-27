package io.github.vzwingma.finances.budget.serverless.services.operations.test.data;

import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;

import java.util.List;
import java.util.Optional;

public class MockDataCategories {



    /**
     * @param id id de la catégorie
     * @param listeCategories liste des catégories
     * @return catégorie correspondante
     */
    public static CategorieOperations getCategorieById(String id, List<CategorieOperations> listeCategories){
        CategorieOperations categorie = null;
        if(id != null && listeCategories != null && !listeCategories.isEmpty()){
            // Recherche parmi les catégories
            Optional<CategorieOperations> cat = listeCategories.parallelStream()
                    .filter(c -> id.equals(c.getId()))
                    .findFirst();
            if(cat.isPresent()){
                categorie = cat.get();
            }
            // Sinon les sous catégories
            else{
                Optional<CategorieOperations> ssCats = listeCategories.parallelStream()
                        .flatMap(c -> c.getListeSSCategories().stream())
                        .filter(ss -> id.equals(ss.getId()))
                        .findFirst();
                if(ssCats.isPresent()){
                    categorie = ssCats.get();
                }
            }
        }
        return categorie;
    }
}
