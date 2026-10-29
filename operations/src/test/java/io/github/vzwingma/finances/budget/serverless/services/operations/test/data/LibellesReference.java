package io.github.vzwingma.finances.budget.serverless.services.operations.test.data;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LibelleCategorieOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.utils.BudgetDataUtils;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.SsCategorieOperations;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Référence de test (AP004 T1.3) pour la parité C3 des libellés d'opérations : jeu de données brutes et ancien algorithme
 * (un document par opération, comptage en Java), copié à l'identique de la version antérieure à {@code c86abc6}.
 * Le jeu de données ne comporte aucune égalité de compteur entre deux (catégorie, sous-catégorie) d'un même libellé.
 */
public final class LibellesReference {

    private LibellesReference() {
    }

    /** Opération brute : libellé, catégorie, sous-catégorie, tels que stockés dans {@code budgets.listeOperations} */
    public record OperationBrute(String libelle, String categorieId, String ssCategorieId) {
    }

    public static SsCategorieOperations ssCategorie(String id, boolean actif) {
        SsCategorieOperations ss = new SsCategorieOperations(id);
        ss.setLibelle(id);
        ss.setActif(actif);
        return ss;
    }

    /** Paramétrage : c1 {s1, s2 actives, s4 inactive}, c2 {s3 active} */
    public static List<CategorieOperations> categories() {
        CategorieOperations c1 = new CategorieOperations("c1");
        c1.setListeSSCategories(new HashSet<>(List.of(ssCategorie("s1", true), ssCategorie("s2", true), ssCategorie("s4", false))));
        CategorieOperations c2 = new CategorieOperations("c2");
        c2.setListeSSCategories(new HashSet<>(List.of(ssCategorie("s3", true))));
        return List.of(c1, c2);
    }

    public static List<OperationBrute> repeter(int nb, String libelle, String cat, String ssCat) {
        List<OperationBrute> operations = new ArrayList<>();
        for (int i = 0; i < nb; i++) {
            operations.add(new OperationBrute(libelle, cat, ssCat));
        }
        return operations;
    }

    /**
     * Jeu de données : tags {@code [..]} et commentaires {@code - xxx} à normaliser, sous-catégorie inactive ou absente,
     * libellé unique (filtré), libellé réparti sur deux sous-catégories (le plus fréquent gagne).
     */
    public static List<OperationBrute> jeuDeDonnees() {
        List<OperationBrute> operations = new ArrayList<>();
        operations.addAll(repeter(3, "Courses [Intercompte]", "c1", "s1"));
        operations.addAll(repeter(2, "Courses - mars", "c1", "s1"));
        operations.addAll(repeter(2, "Courses", "c1", "s2"));
        operations.addAll(repeter(1, "Boulangerie", "c2", "s3"));
        operations.addAll(repeter(4, "Essence", "c2", "s3"));
        operations.addAll(repeter(6, "Essence", "c1", "s4"));
        operations.addAll(repeter(2, "Cinéma - sortie famille", "c2", "s3"));
        operations.addAll(repeter(1, "Cinéma", "c2", "s3"));
        operations.addAll(repeter(3, "Pharmacie", "c2", null));
        operations.addAll(repeter(2, "Abonnement", "c1", "s2"));
        operations.addAll(repeter(3, "Abonnement", "c2", "s3"));
        operations.addAll(repeter(2, "Abonnement [Auto]", "c2", "s3"));
        return operations;
    }

    /** Ancien algorithme : un document {@code operationLibelleAttributes} par opération, compteur +1 à chaque doublon */
    public static List<LibelleCategorieOperation> ancienAlgorithme(List<OperationBrute> operations, List<CategorieOperations> categories) {
        List<Document> documents = operations.stream().map(op -> {
            Document attributs = new Document("libelle", op.libelle());
            if (op.categorieId() != null) {
                attributs.append("categorieId", op.categorieId());
            }
            if (op.ssCategorieId() != null) {
                attributs.append("ssCategorieId", op.ssCategorieId());
            }
            return new Document("operationLibelleAttributes", attributs);
        }).toList();
        List<SsCategorieOperations> ssCategoriesParams = categories
                .stream().flatMap(cat -> cat.getListeSSCategories().stream())
                .filter(SsCategorieOperations::isActif)
                .toList();

        return documents.stream().map(doc -> {
                    Object attributesObj = doc.get("operationLibelleAttributes");
                    Document attributes = attributesObj instanceof Document document ? document : null;
                    LibelleCategorieOperation libelleCategorieOperation = new LibelleCategorieOperation();
                    if (attributes != null) {
                        libelleCategorieOperation.setLibelle(BudgetDataUtils.deleteTagFromString(attributes.getString("libelle")).split("-")[0].trim());
                        String catId = attributes.getString("categorieId");
                        String ssCatId = attributes.getString("ssCategorieId");
                        if (ssCategoriesParams.stream().anyMatch(ssCatParam -> ssCatParam.getId().equals(ssCatId))) {
                            libelleCategorieOperation.setCategorieId(catId);
                            libelleCategorieOperation.setSsCategorieId(ssCatId);
                        } else {
                            libelleCategorieOperation.setCategorieId(null);
                            libelleCategorieOperation.setSsCategorieId(null);
                        }
                    }
                    return libelleCategorieOperation;
                })
                .filter(l -> l.getCategorieId() != null && l.getSsCategorieId() != null)
                .collect(Collectors.toMap(
                        l -> l.getLibelle() + "|" + l.getCategorieId() + "|" + l.getSsCategorieId(),
                        l -> {
                            l.setNbOccurrences(1);
                            return l;
                        },
                        (existing, replacement) -> {
                            existing.setNbOccurrences(existing.getNbOccurrences() + 1);
                            return existing;
                        }))
                .values().stream()
                .filter(l -> l.getNbOccurrences() > 1)
                .collect(Collectors.toMap(
                        LibelleCategorieOperation::getLibelle,
                        l -> l,
                        (a, b) -> a.getNbOccurrences() >= b.getNbOccurrences() ? a : b))
                .values().stream()
                .sorted(Comparator.comparing(LibelleCategorieOperation::getLibelle))
                .toList();
    }


    public static List<String> resume(List<LibelleCategorieOperation> libelles) {
        return libelles.stream()
                .map(l -> l.getLibelle() + "|" + l.getCategorieId() + "|" + l.getSsCategorieId() + "|" + l.getNbOccurrences())
                .toList();
    }
}
