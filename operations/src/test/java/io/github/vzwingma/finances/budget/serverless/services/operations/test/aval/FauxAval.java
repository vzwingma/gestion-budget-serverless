package io.github.vzwingma.finances.budget.serverless.services.operations.test.aval;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/**
 * Faux service aval (comptes, parametrages) pour les tests de résilience (AP004 T2.8) : {@link HttpServer} du JDK, sans nouvelle
 * dépendance. Statut fonction du numéro d'appel (1, 2...), corps JSON fixe, délai de réponse piloté.
 * <p>
 * Les ports sont tirés une fois par JVM et partagés via des propriétés système : le {@code QuarkusTestProfile} (qui injecte
 * l'URL dans la configuration) et le test peuvent être chargés par des classloaders différents.
 */
public final class FauxAval implements AutoCloseable {

    /** Propriété système du port du faux service comptes */
    public static final String PROP_PORT_COMPTES = "test.faux-aval.port.comptes";
    /** Propriété système du port du faux service parametrages */
    public static final String PROP_PORT_PARAMETRAGES = "test.faux-aval.port.parametrages";

    private final HttpServer serveur;
    private final AtomicInteger appels = new AtomicInteger();

    private FauxAval(HttpServer serveur) {
        this.serveur = serveur;
    }

    /**
     * @param propriete nom de la propriété système portant le port
     * @return port libre, identique pour tous les chargeurs de classes de la JVM
     */
    public static synchronized int port(String propriete) {
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
     * Démarre un faux aval.
     *
     * @param port    port d'écoute (127.0.0.1)
     * @param statut  statut HTTP en fonction du numéro d'appel (à partir de 1)
     * @param corps   corps JSON renvoyé si le statut est 200
     * @param delaiMs délai avant réponse (ms), 0 pour aucun
     * @return le faux aval démarré (à fermer en fin de test)
     * @throws IOException port indisponible
     */
    public static FauxAval demarrer(int port, IntFunction<Integer> statut, String corps, long delaiMs) throws IOException {
        HttpServer serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        FauxAval aval = new FauxAval(serveur);
        serveur.createContext("/", exchange -> {
            int n = aval.appels.incrementAndGet();
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
                // client parti (timeout) ou arrêt du serveur en fin de test
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        serveur.start();
        return aval;
    }

    /** @return nombre de requêtes reçues */
    public int appels() {
        return appels.get();
    }

    @Override
    public void close() {
        serveur.stop(0);
    }
}
