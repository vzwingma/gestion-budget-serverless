package io.github.vzwingma.finances.budget.serverless.services.operations.test.aval;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Profil de test (AP004 T2.8) : les clients REST pointent vers les faux avals locaux ({@link FauxAval}), <b>sans toucher aux timeouts</b>
 * (connexion 1,5 s, lecture 4 s, ceux de la configuration réelle) : le test de durée « aval lent → 503 en moins de 6 s » porte donc
 * sur les valeurs livrées, pas sur des valeurs réduites pour les besoins du test.
 */
public class ProfilFauxAvalTimeoutsReels implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.rest-client.comptes-service.url", "http://127.0.0.1:" + FauxAval.port(FauxAval.PROP_PORT_COMPTES),
                "quarkus.rest-client.parametrages-service.url", "http://127.0.0.1:" + FauxAval.port(FauxAval.PROP_PORT_PARAMETRAGES));
    }
}
