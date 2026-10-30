package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.Map;

/**
 * Démarre un MongoDB Testcontainers dédié aux tests d'intégration et surcharge {@code quarkus.mongodb.connection-string}.
 * <p>
 * Pourquoi pas Quarkus Dev Services : Dev Services ne démarre aucun conteneur dès qu'une chaîne de connexion est configurée.
 * Or le profil Maven {@code native} (build CI « native snapshot apps ») embarque {@code src/main/resources/prod/application.properties}
 * qui la définit ({@code mongodb://localhost:27017} par défaut), et le {@code application.properties} dev de {@code utilisateurs}
 * la définit sans préfixe {@code %dev.}. Ce gestionnaire rend les tests indépendants du profil Maven (Docker requis).
 * <p>
 * Utilisé uniquement via {@code QuarkusTestProfile#testResources()} des tests d'intégration Mongo : les autres tests
 * {@code @QuarkusTest} ne démarrent aucun conteneur.
 *
 * @author QALvin
 */
public class MongoDbTestResource implements QuarkusTestResourceLifecycleManager {

    private MongoDBContainer mongo;

    @Override
    public Map<String, String> start() {
        mongo = new MongoDBContainer("mongo:7.0");
        mongo.start();
        // directConnection : évite la découverte du replica set (nom d'hôte interne du conteneur non résolvable depuis l'hôte)
        return Map.of("quarkus.mongodb.connection-string", mongo.getConnectionString() + "/?directConnection=true");
    }

    @Override
    public void stop() {
        if (mongo != null) {
            mongo.stop();
        }
    }
}
