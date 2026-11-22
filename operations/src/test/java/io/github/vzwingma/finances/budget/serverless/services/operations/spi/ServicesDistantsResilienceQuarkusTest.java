package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import com.sun.net.httpserver.HttpServer;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.ServiceDistantIndisponibleException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.UserNotAuthorizedException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AP004 T2.4 / T2.6 — clients REST réels (intercepteurs CDI : traduction d'erreurs, retry, cache) face à un faux aval
 * JDK {@link HttpServer} : codes traduits, retry restreint (connexion refusée et 429/502/503/504 uniquement),
 * aucun retry sur timeout, cache des catégories sans mise en cache des erreurs.
 * <p>
 * Les tests complets (délais pilotés, mesure de durée, tous modules) relèvent de T2.8.
 */
@QuarkusTest
@TestProfile(ServicesDistantsResilienceQuarkusTest.ProfilFauxAval.class)
class ServicesDistantsResilienceQuarkusTest {

    // Ports partagés via les propriétés système : le profil de test et le test peuvent être chargés par des classloaders différents
    private static final int PORT_COMPTES = port("test.faux-aval.port.comptes");
    private static final int PORT_PARAMETRAGES = port("test.faux-aval.port.parametrages");

    private static final String COMPTE_JSON = "{\"id\":\"C1\",\"libelle\":\"Compte\",\"actif\":true}";
    private static final String CATEGORIES_JSON = "[{\"id\":\"cat1\",\"libelle\":\"Cat 1\",\"listeSSCategories\":[]}]";
    private static final String SS_CATEGORIE_JSON = "{\"id\":\"ss1\",\"libelle\":\"Sous cat 1\"}";

    /** Délai de lecture réduit (500 ms) pour tester le timeout sans attendre 4 s ; connexion 1,5 s inchangée */
    public static class ProfilFauxAval implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.rest-client.comptes-service.url", "http://127.0.0.1:" + PORT_COMPTES,
                    "quarkus.rest-client.parametrages-service.url", "http://127.0.0.1:" + PORT_PARAMETRAGES,
                    "quarkus.rest-client.comptes-service.read-timeout", "500",
                    "quarkus.rest-client.parametrages-service.read-timeout", "500");
        }
    }

    @Inject
    @RestClient
    IComptesServiceProvider comptes;

    @Inject
    @RestClient
    IParametragesServiceProvider parametrages;

    private HttpServer serveurComptes;
    private HttpServer serveurParametrages;
    private final AtomicInteger appelsComptes = new AtomicInteger();
    private final AtomicInteger appelsParametrages = new AtomicInteger();
    private final List<String> metriques = new CopyOnWriteArrayList<>();
    private Handler handlerLog;
    private Logger loggerInterceptor;

    @BeforeEach
    void setup() {
        loggerInterceptor = Logger.getLogger(TraductionErreursServiceDistantInterceptor.class.getName());
        loggerInterceptor.setLevel(Level.ALL);
        handlerLog = new Handler() {
            @Override
            public void publish(LogRecord record) {
                metriques.add(record.getMessage());
            }

            @Override
            public void flush() {
                // sans objet
            }

            @Override
            public void close() {
                // sans objet
            }
        };
        loggerInterceptor.addHandler(handlerLog);
    }

    @AfterEach
    void teardown() {
        loggerInterceptor.removeHandler(handlerLog);
        arreter(serveurComptes);
        arreter(serveurParametrages);
    }

    // ====== traduction des réponses (mapper) ======

    @Test
    void comptes_200() throws IOException {
        serveurComptes = demarrer(PORT_COMPTES, appelsComptes, n -> 200, COMPTE_JSON, 0);
        assertEquals("C1", comptes.getCompteById("C1").await().indefinitely().getId());
        assertEquals(1, appelsComptes.get());
    }

    @Test
    void comptes_404_dataNotFound_sansRetry() throws IOException {
        serveurComptes = demarrer(PORT_COMPTES, appelsComptes, n -> 404, "", 0);
        Throwable e = erreur(() -> comptes.getCompteById("C1").await().indefinitely());
        assertInstanceOf(DataNotFoundException.class, e);
        assertEquals(1, appelsComptes.get());
    }

    @Test
    void comptes_401_userNotAuthorized_sansRetry() throws IOException {
        serveurComptes = demarrer(PORT_COMPTES, appelsComptes, n -> 401, "", 0);
        assertInstanceOf(UserNotAuthorizedException.class, erreur(() -> comptes.getCompteById("C1").await().indefinitely()));
        assertEquals(1, appelsComptes.get());
    }

    @Test
    void comptes_403_deLAval_serviceIndisponible503_jamaisUn403_sansRetry() throws IOException {
        serveurComptes = demarrer(PORT_COMPTES, appelsComptes, n -> 403, "", 0);
        Throwable e = erreur(() -> comptes.getCompteById("C1").await().indefinitely());
        assertInstanceOf(ServiceDistantIndisponibleException.class, e);
        assertFalse(e instanceof ServiceDistantTransitoireException);
        assertEquals(1, appelsComptes.get());
    }

    @Test
    void comptes_500_serviceIndisponible_sansRetry() throws IOException {
        serveurComptes = demarrer(PORT_COMPTES, appelsComptes, n -> 500, "", 0);
        Throwable e = erreur(() -> comptes.getCompteById("C1").await().indefinitely());
        assertInstanceOf(ServiceDistantIndisponibleException.class, e);
        assertEquals(1, appelsComptes.get());
    }

    // ====== retry restreint ======

    @Test
    void comptes_502puis200_unRetryPuisSucces() throws IOException {
        serveurComptes = demarrer(PORT_COMPTES, appelsComptes, n -> n == 1 ? 502 : 200, COMPTE_JSON, 0);
        assertNotNull(comptes.getCompteById("C1").await().indefinitely());
        assertEquals(2, appelsComptes.get());
    }

    @Test
    void comptes_503_429_504_unSeulRetryPuisServiceIndisponible() throws IOException {
        for (int statut : new int[]{503, 429, 504}) {
            appelsComptes.set(0);
            serveurComptes = demarrer(PORT_COMPTES, appelsComptes, n -> statut, "", 0);
            Throwable e = erreur(() -> comptes.getCompteById("C1").await().indefinitely());
            assertInstanceOf(ServiceDistantIndisponibleException.class, e, "statut " + statut);
            assertEquals(2, appelsComptes.get(), "statut " + statut + " : 1 appel + 1 retry");
            arreter(serveurComptes);
        }
    }

    @Test
    void comptes_readTimeout_serviceIndisponible_sansRetry_etMetriqueTimeout() throws IOException {
        serveurComptes = demarrer(PORT_COMPTES, appelsComptes, n -> 200, COMPTE_JSON, 1500);
        Throwable e = erreur(() -> comptes.getCompteById("C1").await().indefinitely());
        assertInstanceOf(ServiceDistantIndisponibleException.class, e);
        assertFalse(e instanceof ServiceDistantTransitoireException, "un timeout n'est jamais rejoué");
        assertEquals(1, appelsComptes.get());
        assertEquals(1, metriques.stream().filter(m -> m.contains("status=timeout")).count(), metriques.toString());
        assertTrue(metriques.get(0).matches("^\\[REST-METRIC] client=comptes method=GET status=timeout durationMs=\\d+$"), metriques.get(0));
    }

    @Test
    void parametrages_connexionRefusee_unRetry_etMetriqueConnectError() {
        // aucun serveur sur PORT_PARAMETRAGES
        Throwable e = erreur(() -> parametrages.getCategories().await().indefinitely());
        assertInstanceOf(ServiceDistantTransitoireException.class, e);
        assertEquals(2, metriques.stream().filter(m -> m.contains("client=parametrages method=GET status=connect-error")).count(), metriques.toString());
    }

    // ====== utilitaires ======

    /** Erreur levée par l'appel ; {@code await()} enveloppe les exceptions vérifiées dans une {@link CompletionException} */
    private static Throwable erreur(Runnable appel) {
        Throwable e = assertThrows(Throwable.class, appel::run);
        return e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
    }

    private static synchronized int port(String propriete) {
        Integer existant = Integer.getInteger(propriete);
        if (existant != null) {
            return existant;
        }
        try (ServerSocket s = new ServerSocket(0)) {
            System.setProperty(propriete, String.valueOf(s.getLocalPort()));
            return s.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Faux aval : statut fonction du numéro d'appel (1, 2...), corps JSON, délai de réponse en ms
     */
    private static HttpServer demarrer(int port, AtomicInteger compteur, IntFunction<Integer> statut, String corps, long delaiMs) throws IOException {
        HttpServer serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        serveur.createContext("/", exchange -> {
            int n = compteur.incrementAndGet();
            try {
                if (delaiMs > 0) {
                    Thread.sleep(delaiMs);
                }
                byte[] octets = corps.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                int code = statut.apply(n);
                if (code == 200) {
                    exchange.sendResponseHeaders(200, octets.length);
                    exchange.getResponseBody().write(octets);
                } else {
                    exchange.sendResponseHeaders(code, -1);
                }
            } catch (InterruptedException | IOException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        serveur.start();
        return serveur;
    }

    private static void arreter(HttpServer serveur) {
        if (serveur != null) {
            serveur.stop(0);
        }
    }
}
