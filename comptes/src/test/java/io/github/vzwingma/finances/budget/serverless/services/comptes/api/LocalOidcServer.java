package io.github.vzwingma.finances.budget.serverless.services.comptes.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Faux serveur OIDC local (JDK {@link HttpServer}, aucun réseau externe) : sert la discovery et un JWKS contenant la clé
 * publique de test. Permet de tester le démarrage à froid réel de quarkus-oidc (discovery + JWKS) sans accès à Google.
 */
final class LocalOidcServer implements AutoCloseable {

    static final String KID = "test-kid";

    private final HttpServer server;
    private final boolean jwksEnDefaut;
    final AtomicInteger discoveryHits = new AtomicInteger();
    final AtomicInteger jwksHits = new AtomicInteger();

    /**
     * @param jwksEnDefaut si vrai, l'endpoint JWKS répond 500 (simule une panne de récupération des clés)
     */
    LocalOidcServer(boolean jwksEnDefaut) {
        this.jwksEnDefaut = jwksEnDefaut;
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/.well-known/openid-configuration", this::discovery);
        server.createContext("/jwks", this::jwks);
        // Compteurs consultables en HTTP : le profil de test est chargé par un autre classloader que la classe de test (pas de statique partagé)
        server.createContext("/_stats", ex -> reply(ex, 200, "{\"discovery\":" + discoveryHits.get() + ",\"jwks\":" + jwksHits.get() + "}"));
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void discovery(HttpExchange ex) throws IOException {
        discoveryHits.incrementAndGet();
        String base = baseUrl();
        reply(ex, 200, "{\"issuer\":\"" + base + "\",\"jwks_uri\":\"" + base + "/jwks\","
                + "\"authorization_endpoint\":\"" + base + "/auth\",\"token_endpoint\":\"" + base + "/token\","
                + "\"id_token_signing_alg_values_supported\":[\"RS256\"]}");
    }

    private void jwks(HttpExchange ex) throws IOException {
        jwksHits.incrementAndGet();
        if (jwksEnDefaut) {
            reply(ex, 500, "{\"error\":\"indisponible\"}");
            return;
        }
        try {
            RSAPrivateCrtKey priv = (RSAPrivateCrtKey) JwtTestSupport.privateKey();
            var pub = (java.security.interfaces.RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(priv.getModulus(), priv.getPublicExponent()));
            Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
            String n = b64.encodeToString(unsigned(pub.getModulus().toByteArray()));
            String e = b64.encodeToString(unsigned(pub.getPublicExponent().toByteArray()));
            reply(ex, 200, "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"" + KID + "\",\"n\":\"" + n + "\",\"e\":\"" + e + "\"}]}");
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Retire l'octet de signe de tête d'un BigInteger.toByteArray(). */
    private static byte[] unsigned(byte[] bytes) {
        if (bytes.length > 1 && bytes[0] == 0) {
            return java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }

    private static void reply(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
