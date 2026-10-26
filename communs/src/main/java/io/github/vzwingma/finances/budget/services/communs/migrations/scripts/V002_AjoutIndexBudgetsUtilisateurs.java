package io.github.vzwingma.finances.budget.services.communs.migrations.scripts;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.github.vzwingma.finances.budget.services.communs.migrations.IMongoMigration;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Migration V002 : création des index de lecture des collections {@code budgets} et {@code utilisateurs}.
 * <ul>
 *     <li>{@code budgets} : {@code {idCompteBancaire:1, annee:1, mois:1}} (lecture d'un budget mensuel d'un compte)</li>
 *     <li>{@code utilisateurs} : {@code {login:1}}, <strong>non unique</strong> (un index unique sera ajouté plus tard,
 *     après contrôle des doublons en base)</li>
 * </ul>
 * La migration est défensive : les index existants sont listés d'abord et la création est ignorée si un index de
 * même clé existe déjà, quel que soit son nom (index créé manuellement, ex. sur Atlas). Elle est donc rejouable
 * sans erreur.
 *
 * @author vzwingma
 */
@ApplicationScoped
public class V002_AjoutIndexBudgetsUtilisateurs implements IMongoMigration {

    private static final Logger LOG = LoggerFactory.getLogger(V002_AjoutIndexBudgetsUtilisateurs.class);

    static final String COLLECTION_BUDGETS = "budgets";
    static final String COLLECTION_UTILISATEURS = "utilisateurs";

    private final ReactiveMongoClient mongoClient;
    private final String nomBase;

    /**
     * @param mongoClient client MongoDB réactif
     * @param nomBase     nom de la base ({@code quarkus.mongodb.database})
     */
    @Inject
    public V002_AjoutIndexBudgetsUtilisateurs(ReactiveMongoClient mongoClient,
                                              @ConfigProperty(name = "quarkus.mongodb.database") String nomBase) {
        this.mongoClient = mongoClient;
        this.nomBase = nomBase;
    }

    @Override
    public String version() {
        return "V002";
    }

    @Override
    public String description() {
        return "Index budgets {idCompteBancaire, annee, mois} et utilisateurs {login} (non unique)";
    }

    @Override
    public Uni<Void> migrate() {
        return creerIndexSiAbsent(COLLECTION_BUDGETS, Indexes.ascending("idCompteBancaire", "annee", "mois"),
                cle("idCompteBancaire", "annee", "mois"))
                .chain(() -> creerIndexSiAbsent(COLLECTION_UTILISATEURS, Indexes.ascending("login"), cle("login")));
    }

    /**
     * Crée l'index sur la collection, sauf si un index de même clé (mêmes champs, même ordre, même sens) existe déjà.
     */
    private Uni<Void> creerIndexSiAbsent(String nomCollection, Bson cleIndex, Map<String, Integer> cleAttendue) {
        ReactiveMongoCollection<Document> collection = mongoClient.getDatabase(nomBase).getCollection(nomCollection);
        return collection.listIndexes().collect().asList()
                .chain(indexExistants -> {
                    if (indexExistants.stream().anyMatch(index -> memeCle(index, cleAttendue))) {
                        LOG.info("[MIGRATIONS] V002 : index de clé {} déjà présent sur {}, création ignorée", cleAttendue, nomCollection);
                        return Uni.createFrom().voidItem();
                    }
                    LOG.info("[MIGRATIONS] V002 : création de l'index {} sur {}", cleAttendue, nomCollection);
                    return collection.createIndex(cleIndex, new IndexOptions()).replaceWithVoid();
                });
    }

    /**
     * @param champs noms des champs, dans l'ordre, tous en tri croissant
     * @return clé d'index attendue (ordre conservé)
     */
    private static Map<String, Integer> cle(String... champs) {
        Map<String, Integer> cle = new LinkedHashMap<>();
        for (String champ : champs) {
            cle.put(champ, 1);
        }
        return cle;
    }

    /**
     * @param index       définition d'un index retournée par {@code listIndexes} (champ {@code key})
     * @param cleAttendue clé recherchée
     * @return {@code true} si la clé de l'index est identique (champs, ordre, sens), quel que soit son nom
     */
    static boolean memeCle(Document index, Map<String, Integer> cleAttendue) {
        Object cle = index.get("key");
        if (!(cle instanceof Document documentCle)) {
            return false;
        }
        List<String> champsExistants = new ArrayList<>(documentCle.keySet());
        if (!champsExistants.equals(new ArrayList<>(cleAttendue.keySet()))) {
            return false;
        }
        // Le sens peut être stocké en Integer, Long ou Double selon l'origine de l'index
        return cleAttendue.entrySet().stream().allMatch(attendu ->
                documentCle.get(attendu.getKey()) instanceof Number sens && sens.intValue() == attendu.getValue());
    }
}
