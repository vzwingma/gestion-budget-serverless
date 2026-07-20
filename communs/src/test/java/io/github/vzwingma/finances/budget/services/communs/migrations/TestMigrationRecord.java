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

        MigrationRecord migrationRecord = new MigrationRecord("V001", "Init collection", date, MigrationRecord.MigrationStatutEnum.SUCCES);

        assertEquals("V001", migrationRecord.getVersion());
        assertEquals("Init collection", migrationRecord.getDescription());
        assertEquals(date, migrationRecord.getDateExecution());
        assertEquals(MigrationRecord.MigrationStatutEnum.SUCCES, migrationRecord.getStatut());
    }

    @Test
    void testSetters() {
        MigrationRecord migrationRecord = new MigrationRecord();
        LocalDateTime date = LocalDateTime.now();

        migrationRecord.setVersion("V002");
        migrationRecord.setDescription("Autre migration");
        migrationRecord.setDateExecution(date);
        migrationRecord.setStatut(MigrationRecord.MigrationStatutEnum.ECHEC);

        assertEquals("V002", migrationRecord.getVersion());
        assertEquals("Autre migration", migrationRecord.getDescription());
        assertEquals(date, migrationRecord.getDateExecution());
        assertEquals(MigrationRecord.MigrationStatutEnum.ECHEC, migrationRecord.getStatut());
    }

    @Test
    void testToStringContientLesChampsCles() {
        LocalDateTime date = LocalDateTime.of(2026, 7, 6, 10, 30);
        MigrationRecord migrationRecord = new MigrationRecord("V001", "Init collection", date, MigrationRecord.MigrationStatutEnum.SUCCES);

        String repr = migrationRecord.toString();

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
