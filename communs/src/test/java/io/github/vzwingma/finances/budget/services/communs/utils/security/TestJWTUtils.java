package io.github.vzwingma.finances.budget.services.communs.utils.security;

import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.*;
import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import io.vertx.core.json.Json;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests JWTUtils
 */
class TestJWTUtils {

    private static final String JWKS_GOOGLE_KEYS = """
            {
              "keys": [
                {
                  "e": "AQAB",
                  "n": "nzGsrziOYrMVYMpvUZOwkKNiPWcOPTYRYlDSdRW4UpAHdWPbPlyqaaphYhoMB5DXrVxI3bdvm7DOlo-sHNnulmAFQa-7TsQMxrZCvVdAbyXGID9DZYEqf8mkCV1Ohv7WY5lDUqlybIk1OSHdK7-1et0QS8nn-5LojGg8FK4ssLf3mV1APpujl27D1bDhyRb1MGumXYElwlUms7F9p9OcSp5pTevXCLmXs9MJJk4o9E1zzPpQ9Ko0lH9l_UqFpA7vwQhnw0nbh73rXOX2TUDCUqL4ThKU5Z9Pd-eZCEOatKe0mJTpQ00XGACBME_6ojCdfNIJr84Y_IpGKvkAEksn9w",
                  "alg": "RS256",
                  "kty": "RSA",
                  "kid": "87bbe0815b064e6d449cac999f0e50e72a3e4374",
                  "use": "sig"
                },
                {
                  "kid": "0e345fd7e4a97271dffa991f5a893cd16b8e0827",
                  "use": "sig",
                  "alg": "RS256",
                  "kty": "RSA",
                  "e": "AQAB",
                  "n": "rv95jmy91hibD7cb_BCA25jv5HrX7WoqHv-fh8wrOR5aYcM8Kvsc3mbzs2w1vCUlMRv7NdEGVBEnOZ6tHvUzGLon4ythd5XsX-wTvAtIHPkyHdo5zGpTgATO9CEn78Y-f1E8By63ttv14kXe_RMjt5aKttK4yqqUyzWUexSs7pET2zWiigd0_bGhJGYYEJlEk_JsOBFvloIBaycMfDjK--kgqnlRA8SWUkP3pEJIAo9oHzmvX6uXZTEJK10a1YNj0JVR4wZY3k60NaUX-KCroreU85iYgnecyxSdL-trpKdkg0-2OYks-_2Isymu7jPX-uKVyi-zKyaok3N64mERRQ"
                }
              ]
            }""";

    private Map<String, JwksAuthKey> getValidSignaturesKeys() {
        Map<String, JwksAuthKey> jwksAuthKeyList = new HashMap<>();
        Arrays.stream(Json.decodeValue(JWKS_GOOGLE_KEYS, JwksAuthKeys.class).getKeys())
              .forEach(k -> jwksAuthKeyList.put(k.getKid(), k));
        return jwksAuthKeyList;
    }

    // ====== isNotExpired ======

    @Test
    void testIsNotExpiredAvecTokenNonExpire() {
        JWTAuthPayload payload = new JWTAuthPayload();
        payload.setExp(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now().plusHours(1)));
        payload.setIss("https://accounts.google.com");
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), payload);
        assertTrue(JWTUtils.isNotExpired(token));
    }

    @Test
    void testIsNotExpiredAvecTokenExpire() {
        JWTAuthPayload payload = new JWTAuthPayload();
        payload.setExp(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now().minusHours(1)));
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), payload);
        assertFalse(JWTUtils.isNotExpired(token));
    }

    @Test
    void testIsNotExpiredAvecExpZero() {
        // exp = 0 => expiredAt() retourne null => isExpired=true => isNotExpired=false
        JWTAuthPayload payload = new JWTAuthPayload();
        payload.setExp(0L);
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), payload);
        assertFalse(JWTUtils.isNotExpired(token));
    }

    // ====== hasValidSignature ======

    @Test
    void testHasValidSignatureSansSignature() {
        // hasSignature = false → retourne true (pas de vérification)
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), new JWTAuthPayload(), false, null);
        assertTrue(JWTUtils.hasValidSignature(token, getValidSignaturesKeys()));
    }

    @Test
    void testHasValidSignatureAvecClesVides() {
        // authKeys vide → retourne true (aucune clé à vérifier)
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), new JWTAuthPayload(), true, "content.payload.signature");
        assertTrue(JWTUtils.hasValidSignature(token, Collections.emptyMap()));
    }

    @Test
    void testHasValidSignatureAvecClesNull() {
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), new JWTAuthPayload(), true, "content.payload.signature");
        assertTrue(JWTUtils.hasValidSignature(token, null));
    }

    @Test
    void testHasValidSignatureRawContentNull() {
        // rawContent null → pas de contenu brut → retourne true (non signé)
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), new JWTAuthPayload(), true, null);
        assertTrue(JWTUtils.hasValidSignature(token, getValidSignaturesKeys()));
    }

    // ====== isValid : branche idAppUserContent null ======

    @Test
    void testIsValidAvecIdAppNull() {
        JWTAuthPayload payload = new JWTAuthPayload();
        payload.setIss("https://accounts.google.com");
        payload.setAud("550431928138-edestj28rk5a0emk546p7ii28dl5boc5.apps.googleusercontent.com");
        payload.setExp(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now().plusHours(1)));
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), payload);

        // idAppUserContent est null → isFromUserAppContent retourne false
        assertFalse(JWTUtils.isValid(token, null, getValidSignaturesKeys()));
    }

    // ====== isTokenSignatureValid avec clé invalide ======

    @Test
    void testIsTokenSignatureInvalidAvecMauvaisFormat() {
        // Clé avec des données invalides → GeneralSecurityException → retourne false
        JwksAuthKey badKey = new JwksAuthKey();
        badKey.setKid("bad-kid");
        badKey.setN("invalid-n");
        badKey.setE("invalid-e");

        Map<String, JwksAuthKey> keys = Map.of("bad-kid", badKey);
        assertFalse(JWTUtils.isTokenSignatureValid("header.payload.signature", keys));
    }

    // ====== isValid global avec token sans payload ======

    @Test
    void testIsValidAvecPayloadNull() {
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), null);
        assertFalse(JWTUtils.isValid(token, "someApp", getValidSignaturesKeys()));
    }

    // ====== isValid avec bon issuer mais mauvais aud ======

    @Test
    void testIsValidBonIssuerMauvaisAud() {
        JWTAuthPayload payload = new JWTAuthPayload();
        payload.setIss("https://accounts.google.com");
        payload.setAud("wrong-aud.apps.googleusercontent.com");
        payload.setExp(BudgetDateTimeUtils.getSecondsFromLocalDateTime(LocalDateTime.now().plusHours(1)));
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), payload);

        assertFalse(JWTUtils.isValid(token, "550431928138-edestj28rk5a0emk546p7ii28dl5boc5", getValidSignaturesKeys()));
    }
}

