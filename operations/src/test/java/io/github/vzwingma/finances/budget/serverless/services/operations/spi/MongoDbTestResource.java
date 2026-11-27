package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.Map;

/**
 * Démarre un MongoDB Testcontainers dédié aux tests d'intégration et surcharge {@code quarkus.mongodb.connection-string}.
 * <p>
 * Pourquoi pas Quarkus Dev Services : depuis AP004 T2.3 (profils Quarkus, plus de profils Maven de ressources), la chaîne de connexion
 * n'est définie que pour {@code %dev} et {@code %prod} (configuration commune de {@code communs}) : en profil {@code test}, Dev Services
 * démarrerait un conteneur. Ce gestionnaire est conservé pour fixer l'image ({@code mongo:7.0}) et la connexion directe, et rendre ces
 * tests indépendants de Dev Services (Docker requis, donc CI).
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
