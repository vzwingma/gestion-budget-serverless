package io.github.vzwingma.finances.budget.serverless.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Forge de jetons de test pour les tests de sécurité (AP004 T0.5) : contrairement à {@link JwtTestSupport},
 * permet de construire des en-têtes / payloads arbitraires (claims manquants, iss/aud/exp invalides, alg=none, etc.)
 * et de choisir la clé de signature.
 * <p>
 * ATTENTION : clés de test uniquement.
 */
final class OidcTokenForge {

    static final String VALID_ISS = "https://accounts.google.com";
    static final String VALID_AUD = "test.apps.googleusercontent.com";
    static final String VALID_EMAIL = "toto.toto@world.com";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    private OidcTokenForge() { }

    /** @return payload d'un jeton valide ; les tests le modifient (put / remove) avant signature */
    static Map<String, Object> validClaims() {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", VALID_ISS);
        claims.put("aud", VALID_AUD);
        claims.put("email", VALID_EMAIL);
        claims.put("email_verified", true);
        claims.put("name", "Test");
        claims.put("iat", JwtTestSupport.epochSecondsIn(Duration.ZERO));
        claims.put("exp", JwtTestSupport.epochSecondsIn(Duration.ofHours(1)));
        return claims;
    }

    static Map<String, Object> rs256Header() {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        return header;
    }

    /** @return jeton signé RS256 avec la clé de test acceptée par l'application */
    static String sign(Map<String, Object> header, Map<String, Object> claims) {
        return sign(header, claims, JwtTestSupport.privateKey());
    }

    /** @return jeton signé RS256 par la clé fournie */
    static String sign(Map<String, Object> header, Map<String, Object> claims, PrivateKey key) {
        String unsigned = b64Json(header) + "." + b64Json(claims);
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(unsigned.getBytes(StandardCharsets.US_ASCII));
            return unsigned + "." + B64.encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @return jeton valide (clé de test) avec les claims donnés */
    static String valid(Map<String, Object> claims) {
        return sign(rs256Header(), claims);
    }

    /** @return jeton sans signature (3e segment vide), avec l'en-tête donné */
    static String unsigned(Map<String, Object> header, Map<String, Object> claims) {
        return b64Json(header) + "." + b64Json(claims) + ".";
    }

    /** @return une clé privée RSA 2048 fraîche, différente de la clé de test */
    static PrivateKey otherPrivateKey() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            return pair.getPrivate();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @return le jeton dont le payload est remplacé (signature d'origine conservée => signature invalide) */
    static String withReplacedPayload(String token, Map<String, Object> newClaims) {
        String[] parts = token.split("[.]");
        return parts[0] + "." + b64Json(newClaims) + "." + parts[2];
    }

    static String b64Json(Map<String, Object> map) {
        try {
            return B64.encodeToString(MAPPER.writeValueAsBytes(map));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    static String bearer(String token) {
        return "Bearer " + token;
    }
}
