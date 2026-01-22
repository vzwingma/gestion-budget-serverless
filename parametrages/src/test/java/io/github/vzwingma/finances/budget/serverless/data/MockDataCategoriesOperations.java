package io.github.vzwingma.finances.budget.serverless.data;

import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.SsCategorieOperations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Jeu de données Catégorie
 */
public class MockDataCategoriesOperations {

    public static List<CategorieOperations> getListeTestCategories() {
        List<CategorieOperations> categoriesFromSPI = new ArrayList<>();
        CategorieOperations catAlimentation = new CategorieOperations();
        catAlimentation.setId("8f1614c9-503c-4e7d-8cb5-0c9a9218b84a");
        catAlimentation.setActif(true);
        catAlimentation.setLibelle("Alimentation");

        SsCategorieOperations ssCatCourse = new SsCategorieOperations();
        ssCatCourse.setActif(true);
        ssCatCourse.setId("467496e4-9059-4b9b-8773-21f230c8c5c6");
        ssCatCourse.setLibelle("Courses");
        catAlimentation.setListeSSCategories(new HashSet<>());
        catAlimentation.getListeSSCategories().add(ssCatCourse);


        SsCategorieOperations ssCatCourseOld = new SsCategorieOperations();
        ssCatCourseOld.setActif(false);
        ssCatCourseOld.setId("5ad7745f-84f1-4a37-ba24-7fd58ebc07db");
        ssCatCourseOld.setLibelle("Courses [Inactif]");
        catAlimentation.getListeSSCategories().add(ssCatCourseOld);

        categoriesFromSPI.add(catAlimentation);
        return categoriesFromSPI;
    }

}
