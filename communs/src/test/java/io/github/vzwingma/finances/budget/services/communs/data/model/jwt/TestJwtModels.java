package io.github.vzwingma.finances.budget.services.communs.data.model.jwt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires des modèles JWT
 */
class TestJwtModels {

    // ====== JwtAuthHeader ======

    @Test
    void testJwtAuthHeaderGettersSetters() {
        JwtAuthHeader header = new JwtAuthHeader();
        header.setAlg("RS256");
        header.setKid("kid123");
        header.setTyp("JWT");

        assertEquals("RS256", header.getAlg());
        assertEquals("kid123", header.getKid());
        assertEquals("JWT", header.getTyp());
        assertNotNull(header.toString());
    }

    // ====== JWTAuthPayload ======

    @Test
    void testJwtAuthPayloadGettersSetters() {
        JWTAuthPayload payload = new JWTAuthPayload();
        payload.setIss("https://accounts.google.com");
        payload.setAzp("azp-value");
        payload.setAud("aud-value");
        payload.setSub("sub-value");
        payload.setEmail("test@example.com");
        payload.setEmail_verified(true);
        payload.setAt_hash("at_hash-value");
        payload.setName("John Doe");
        payload.setPicture("https://picture.url");
        payload.setGiven_name("John");
        payload.setFamily_name("Doe");
        payload.setLocale("fr");
        payload.setIat(1672660002L);
        payload.setExp(1672663602L);

        assertEquals("https://accounts.google.com", payload.getIss());
        assertEquals("azp-value", payload.getAzp());
        assertEquals("aud-value", payload.getAud());
        assertEquals("sub-value", payload.getSub());
        assertEquals("test@example.com", payload.getEmail());
        assertTrue(payload.isEmail_verified());
        assertEquals("at_hash-value", payload.getAt_hash());
        assertEquals("John Doe", payload.getName());
        assertEquals("https://picture.url", payload.getPicture());
        assertEquals("John", payload.getGiven_name());
        assertEquals("Doe", payload.getFamily_name());
        assertEquals("fr", payload.getLocale());
        assertEquals(1672660002L, payload.getIat());
        assertEquals(1672663602L, payload.getExp());
    }

    @Test
    void testJwtAuthPayloadToString() {
        JWTAuthPayload payload = new JWTAuthPayload();
        payload.setName("Vincent Zwingmann");
        payload.setIat(1672660002L);
        payload.setExp(1672663602L);
        String str = payload.toString();
        assertNotNull(str);
        assertTrue(str.contains("Vincent Zwingmann"));
    }

    // ====== JWTAuthToken ======

    @Test
    void testJwtAuthTokenConstructeurSimple() {
        JwtAuthHeader header = new JwtAuthHeader();
        header.setAlg("RS256");
        JWTAuthPayload payload = new JWTAuthPayload();
        payload.setIat(0L);
        payload.setExp(0L);

        JWTAuthToken token = new JWTAuthToken(header, payload);
        assertEquals(header, token.getHeader());
        assertEquals(payload, token.getPayload());
        assertFalse(token.isHasSignature());
        assertNull(token.getRawContent());
    }

    @Test
    void testJwtAuthTokenConstructeurComplet() {
        JwtAuthHeader header = new JwtAuthHeader();
        JWTAuthPayload payload = new JWTAuthPayload();

        JWTAuthToken token = new JWTAuthToken(header, payload, true, "rawContent");
        assertTrue(token.isHasSignature());
        assertEquals("rawContent", token.getRawContent());
    }

    @Test
    void testJwtAuthTokenIssuedAtNull() {
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), new JWTAuthPayload());
        // iat = 0 => issuedAt() retourne null
        assertNull(token.issuedAt());
    }

    @Test
    void testJwtAuthTokenExpiredAtNull() {
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), new JWTAuthPayload());
        // exp = 0 => expiredAt() retourne null
        assertNull(token.expiredAt());
    }

    @Test
    void testJwtAuthTokenIssuedAtNotNull() {
        JWTAuthPayload payload = new JWTAuthPayload();
        payload.setIat(1672660002L);
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), payload);
        assertNotNull(token.issuedAt());
    }

    @Test
    void testJwtAuthTokenExpiredAtNotNull() {
        JWTAuthPayload payload = new JWTAuthPayload();
        payload.setExp(1672663602L);
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), payload);
        assertNotNull(token.expiredAt());
    }

    @Test
    void testJwtAuthTokenGettersSetters() {
        JWTAuthToken token = new JWTAuthToken(new JwtAuthHeader(), new JWTAuthPayload());
        JwtAuthHeader newHeader = new JwtAuthHeader();
        newHeader.setAlg("HS256");
        token.setHeader(newHeader);
        token.setRawContent("newRaw");
        token.setHasSignature(true);

        assertEquals("HS256", token.getHeader().getAlg());
        assertEquals("newRaw", token.getRawContent());
        assertTrue(token.isHasSignature());
    }

    // ====== JwksAuthKey ======

    @Test
    void testJwksAuthKeyGettersSetters() {
        JwksAuthKey key = new JwksAuthKey();
        key.setKid("kid1");
        key.setE("AQAB");
        key.setN("nzGs...");
        key.setAlg("RS256");
        key.setKty("RSA");
        key.setUse("sig");

        assertEquals("kid1", key.getKid());
        assertEquals("AQAB", key.getE());
        assertEquals("nzGs...", key.getN());
        assertEquals("RS256", key.getAlg());
        assertEquals("RSA", key.getKty());
        assertEquals("sig", key.getUse());
        assertTrue(key.toString().contains("kid1"));
    }

    // ====== JwksAuthKeys ======

    @Test
    void testJwksAuthKeysGettersSetters() {
        JwksAuthKeys keys = new JwksAuthKeys();
        JwksAuthKey k1 = new JwksAuthKey();
        k1.setKid("k1");
        JwksAuthKey k2 = new JwksAuthKey();
        k2.setKid("k2");

        keys.setKeys(new JwksAuthKey[]{k1, k2});
        assertNotNull(keys.getKeys());
        assertEquals(2, keys.getKeys().length);
    }

    // ====== JwtValidationParams ======

    @Test
    void testJwtValidationParamsGettersSetters() {
        JwtValidationParams params = new JwtValidationParams();
        params.setIdAppUserContent("550431928138-edestj28rk5a0emk546p7ii28dl5boc5");
        assertEquals("550431928138-edestj28rk5a0emk546p7ii28dl5boc5", params.getIdAppUserContent());
    }
}

