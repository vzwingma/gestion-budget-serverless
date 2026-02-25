package io.github.vzwingma.finances.budget.serverless.services.comptes.test.data;

import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;

import java.util.ArrayList;
import java.util.List;

/**
 * Jeu de données Comptes
 */
public class MockDataComptes {

    public static List<CompteBancaire> getListeComptes() {

        List<CompteBancaire> comptes = new ArrayList<>();
        CompteBancaire c1 = new CompteBancaire();
        c1.setActif(true);
        c1.setId("C1");
        c1.setLibelle("Libelle1");
        c1.setOrdre(1);
        c1.setProprietaires(List.of(new CompteBancaire.Proprietaire()));
        c1.getProprietaires().getFirst().setLogin("Test");
        comptes.add(c1);
        CompteBancaire c2 = new CompteBancaire();
        c2.setActif(true);
        c2.setId("C2");
        c2.setLibelle("Libelle2");
        c2.setOrdre(2);
        c2.setProprietaires(List.of(new CompteBancaire.Proprietaire()));
        c2.getProprietaires().getFirst().setLogin("Test");
        comptes.add(c2);
        CompteBancaire a3 = new CompteBancaire();
        a3.setActif(true);
        a3.setId("A3");
        a3.setLibelle("Libelle0");
        a3.setOrdre(0);
        comptes.add(a3);
        return comptes;
    }

    public static CompteBancaire getCompte1() {
        return getListeComptes().get(0);
    }
}
