package io.github.vzwingma.finances.budget.services.communs.data.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de Info
 */
class TestInfo {

    @Test
    void testConstructeur() {
        Info info = new Info("monService", "1.0.0");
        assertEquals("monService", info.getNom());
        assertEquals("1.0.0", info.getVersion());
        assertNotNull(info.getDescription());
        assertTrue(info.getDescription().contains("monService"));
    }

    @Test
    void testSetters() {
        Info info = new Info("monService", "1.0.0");
        info.setNom("autreService");
        info.setVersion("2.0.0");
        info.setDescription("description personnalisée");

        assertEquals("autreService", info.getNom());
        assertEquals("2.0.0", info.getVersion());
        assertEquals("description personnalisée", info.getDescription());
    }
}

