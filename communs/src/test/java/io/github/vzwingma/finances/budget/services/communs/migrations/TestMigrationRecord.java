package io.github.vzwingma.finances.budget.services.communs.migrations;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires de {@link MigrationRecord} (document Panache de suivi des migrations).
 */
class TestMigrationRecord {

    @Test
    void testConstructeurEtGetters() {
        LocalDateTime date = LocalDateTime.of(2026, 7, 6, 10, 30);

        MigrationRecord record = new MigrationRecord("V001", "Init collection", date, MigrationRecord.MigrationStatutEnum.SUCCES);

        assertEquals("V001", record.getVersion());
        assertEquals("Init collection", record.getDescription());
        assertEquals(date, record.getDateExecution());
        assertEquals(MigrationRecord.MigrationStatutEnum.SUCCES, record.getStatut());
    }

    @Test
    void testSetters() {
        MigrationRecord record = new MigrationRecord();
        LocalDateTime date = LocalDateTime.now();

        record.setVersion("V002");
        record.setDescription("Autre migration");
        record.setDateExecution(date);
        record.setStatut(MigrationRecord.MigrationStatutEnum.ECHEC);

        assertEquals("V002", record.getVersion());
        assertEquals("Autre migration", record.getDescription());
        assertEquals(date, record.getDateExecution());
        assertEquals(MigrationRecord.MigrationStatutEnum.ECHEC, record.getStatut());
    }

    @Test
    void testToStringContientLesChampsCles() {
        LocalDateTime date = LocalDateTime.of(2026, 7, 6, 10, 30);
        MigrationRecord record = new MigrationRecord("V001", "Init collection", date, MigrationRecord.MigrationStatutEnum.SUCCES);

        String repr = record.toString();

        assertTrue(repr.contains("V001"));
        assertTrue(repr.contains("Init collection"));
        assertTrue(repr.contains("SUCCES"));
    }

    @Test
    void testMigrationStatutEnumValues() {
        assertEquals(2, MigrationRecord.MigrationStatutEnum.values().length);
        assertEquals(MigrationRecord.MigrationStatutEnum.SUCCES, MigrationRecord.MigrationStatutEnum.valueOf("SUCCES"));
        assertEquals(MigrationRecord.MigrationStatutEnum.ECHEC, MigrationRecord.MigrationStatutEnum.valueOf("ECHEC"));
    }
}
