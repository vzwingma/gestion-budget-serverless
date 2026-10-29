package io.github.vzwingma.finances.budget.serverless.services.operations.business;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LibelleCategorieOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IParametragesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.LibellesReference;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.LibellesReference.OperationBrute;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Parité (C3) des libellés d'opérations : ancien algorithme (un document par opération, comptage en Java) contre le
 * nouvel algorithme (regroupement {@code $group} côté MongoDB, somme des compteurs en Java).
 * <p>
 * L'ancien algorithme est copié à l'identique de la version antérieure à {@code c86abc6} ; le regroupement MongoDB est
 * simulé par un regroupement Java sur la valeur brute (libellé, catégorie, sous-catégorie). Le jeu de données ne
 * comporte volontairement <em>aucune égalité de compteur</em> entre deux (catégorie, sous-catégorie) d'un même libellé
 * (cas non déterministe par construction, avant comme après). Le regroupement réel de MongoDB est vérifié par
 * {@code OperationDatabaseAdaptorMongoTest} (Dev Services, CI).
 *
 * @author QALvin
 */
class OperationsServiceLibellesTest {

    private OperationsService operationsService;
    private IOperationsRepository repository;
    
    @BeforeEach
    void setup() {
        repository = mock(IOperationsRepository.class);
        IParametragesServiceProvider parametrages = mock(IParametragesServiceProvider.class);
        operationsService = new OperationsService();
        operationsService.setDataOperationsProvider(repository);
        operationsService.setParametragesService(parametrages);
        when(parametrages.getCategories()).thenReturn(Uni.createFrom().item(LibellesReference.categories()));
    }

    /** Simule le {@code $group} MongoDB : {@code {_id: {libelle, categorieId, ssCategorieId}, n}} (un champ absent est omis de {@code _id}) */
    private static List<Document> regroupementMongo(List<OperationBrute> operations) {
        Map<OperationBrute, Integer> groupes = new LinkedHashMap<>();
        operations.forEach(op -> groupes.merge(op, 1, Integer::sum));
        return groupes.entrySet().stream().map(entree -> {
            Document id = new Document("libelle", entree.getKey().libelle());
            if (entree.getKey().categorieId() != null) {
                id.append("categorieId", entree.getKey().categorieId());
            }
            if (entree.getKey().ssCategorieId() != null) {
                id.append("ssCategorieId", entree.getKey().ssCategorieId());
            }
            return new Document("_id", id).append("n", entree.getValue());
        }).toList();
    }

    private List<LibelleCategorieOperation> nouvelAlgorithme(List<Document> groupes) {
        when(repository.getLibellesOperations("C1")).thenReturn(Multi.createFrom().iterable(groupes));
        return operationsService.getLibellesOperations("C1").collect().asList().await().indefinitely();
    }

    @Test
    void testParite_ancienEtNouvelAlgorithme_memeResultat() {
        List<OperationBrute> operations = LibellesReference.jeuDeDonnees();

        List<LibelleCategorieOperation> ancien = LibellesReference.ancienAlgorithme(operations, LibellesReference.categories());
        List<LibelleCategorieOperation> nouveau = nouvelAlgorithme(regroupementMongo(operations));

        assertFalse(ancien.isEmpty(), "Le jeu de données doit produire des libellés");
        assertEquals(LibellesReference.resume(ancien), LibellesReference.resume(nouveau), "Parité C3 : mêmes libellés, catégories et compteurs");
    }

    @Test
    void testParite_valeursAttendues_sommeDesCompteursApresNormalisation() {
        List<LibelleCategorieOperation> nouveau = nouvelAlgorithme(regroupementMongo(LibellesReference.jeuDeDonnees()));

        // Courses : 3 ("[Intercompte]" retiré) + 2 ("- mars" retiré) = 5 sur s1, devant 2 sur s2
        // Abonnement : c2/s3 = 3 + 2 ("[Auto]" retiré) = 5, devant c1/s2 = 2
        // Cinéma : 2 + 1 = 3 ; Essence : 4 (s3 actif, les 6 de s4 inactif sont ignorés)
        // Boulangerie (1 occurrence) et Pharmacie (sans sous-catégorie) absents
        assertEquals(List.of("Abonnement|c2|s3|5", "Cinéma|c2|s3|3", "Courses|c1|s1|5", "Essence|c2|s3|4"), LibellesReference.resume(nouveau));
    }

    @Test
    void testParite_aucunDocument_resultatVide() {
        assertTrue(LibellesReference.ancienAlgorithme(List.of(), LibellesReference.categories()).isEmpty());
        assertTrue(nouvelAlgorithme(List.of()).isEmpty());
    }

    @Test
    void testNouvelAlgorithme_documentSansCompteur_comptePourZero_etFiltre() {
        // Document sans « n » (contrat dégradé) : compteur 0, donc filtré (> 1 requis)
        Document sansCompteur = new Document("_id", new Document("libelle", "Test").append("categorieId", "c1").append("ssCategorieId", "s1"));
        assertTrue(nouvelAlgorithme(List.of(sansCompteur)).isEmpty());
    }

    @Test
    void testNouvelAlgorithme_documentSansId_ignore() {
        // Ni libellé ni catégorie : filtré sans NPE
        Document sansId = new Document("n", 5);
        assertTrue(nouvelAlgorithme(List.of(sansId)).isEmpty());
    }
}
