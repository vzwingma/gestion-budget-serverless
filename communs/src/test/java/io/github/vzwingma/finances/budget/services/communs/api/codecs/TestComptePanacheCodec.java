package io.github.vzwingma.finances.budget.services.communs.api.codecs;

import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de ComptePanacheCodec
 */
class TestComptePanacheCodec {

    private ComptePanacheCodec codec;

    @BeforeEach
    void setUp() {
        codec = new ComptePanacheCodec();
    }

    @Test
    void testGetEncoderClass() {
        assertEquals(CompteBancaire.class, codec.getEncoderClass());
    }

    @Test
    void testDocumentHasIdAvecId() {
        CompteBancaire compte = new CompteBancaire();
        compte.setId("id-123");
        assertTrue(codec.documentHasId(compte));
    }

    @Test
    void testDocumentHasIdSansId() {
        CompteBancaire compte = new CompteBancaire();
        compte.setId(null);
        assertFalse(codec.documentHasId(compte));
    }

    @Test
    void testDocumentHasIdNull() {
        assertFalse(codec.documentHasId(null));
    }

    @Test
    void testGenerateIdIfAbsentFromDocument() {
        CompteBancaire compte = new CompteBancaire();
        compte.setId(null);
        CompteBancaire result = codec.generateIdIfAbsentFromDocument(compte);
        assertNotNull(result.getId());
        assertFalse(result.getId().isEmpty());
    }

    @Test
    void testGetDocumentIdAvecId() {
        CompteBancaire compte = new CompteBancaire();
        compte.setId("id-456");
        BsonValue bsonValue = codec.getDocumentId(compte);
        assertNotNull(bsonValue);
        assertEquals("id-456", ((BsonString) bsonValue).getValue());
    }

    @Test
    void testGetDocumentIdSansId() {
        // Si pas d'id, génère un id puis retourne sa BsonValue
        CompteBancaire compte = new CompteBancaire();
        compte.setId(null);
        BsonValue bsonValue = codec.getDocumentId(compte);
        assertNotNull(bsonValue);
        assertNotNull(compte.getId()); // L'id a été généré
    }
}

