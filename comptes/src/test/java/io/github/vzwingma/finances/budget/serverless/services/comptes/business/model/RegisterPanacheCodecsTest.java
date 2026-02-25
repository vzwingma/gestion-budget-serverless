package io.github.vzwingma.finances.budget.serverless.services.comptes.business.model;

import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests unitaires de {@link RegisterPanacheCodecs}
 */
class RegisterPanacheCodecsTest {

    private RegisterPanacheCodecs registerPanacheCodecs;
    private CodecRegistry mockRegistry;

    @BeforeEach
    void setup() {
        registerPanacheCodecs = new RegisterPanacheCodecs();
        mockRegistry = Mockito.mock(CodecRegistry.class);
    }

    @Test
    void testGetCodecCompteBancaire() {
        var codec = registerPanacheCodecs.get(CompteBancaire.class, mockRegistry);
        assertNotNull(codec);
    }

    @Test
    void testGetCodecClasseInconnue() {
        var codec = registerPanacheCodecs.get(String.class, mockRegistry);
        assertNull(codec);
    }
}

