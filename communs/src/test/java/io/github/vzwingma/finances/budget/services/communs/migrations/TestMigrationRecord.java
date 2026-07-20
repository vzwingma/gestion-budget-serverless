package io.github.vzwingma.finances.budget.services.communs.migrations;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires de {@link MigrationRecord} (document Panache de suivi des migrations).
 */
class TestMigrationRecord {

    @Test
    void testConstructeurEtGetters() {
        LocalDateTime date = LocalDateTime.of(2026, Month.JULY.getValue(), 6, 10, 30);

        MigrationRecord migration = new MigrationRecord("V001", "Init collection", date, MigrationRecord.MigrationStatutEnum.SUCCES);

        assertEquals("V001", migration.getVersion());
        assertEquals("Init collection", migration.getDescription());
        assertEquals(date, migration.getDateExecution());
        assertEquals(MigrationRecord.MigrationStatutEnum.SUCCES, migration.getStatut());
    }

    @Test
    void testSetters() {
        MigrationRecord migration = new MigrationRecord();
        LocalDateTime date = LocalDateTime.now();

        migration.setVersion("V002");
        migration.setDescription("Autre migration");
        migration.setDateExecution(date);
        migration.setStatut(MigrationRecord.MigrationStatutEnum.ECHEC);

        assertEquals("V002", migration.getVersion());
        assertEquals("Autre migration", migration.getDescription());
        assertEquals(date, migration.getDateExecution());
        assertEquals(MigrationRecord.MigrationStatutEnum.ECHEC, migration.getStatut());
    }

    @Test
    void testToStringContientLesChampsCles() {
        LocalDateTime date = LocalDateTime.of(2026, Month.JULY.getValue(), 6, 10, 30);
        MigrationRecord migration = new MigrationRecord("V001", "Init collection", date, MigrationRecord.MigrationStatutEnum.SUCCES);

        String repr = migration.toString();

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
