package io.github.vzwingma.finances.budget.services.communs.migrations.scripts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests unitaires de la migration exemple {@link V001_InitMigrationsCollection}. Migration no-op :
 * on vérifie le contrat (version/description stables, {@code migrate()} se termine avec succès sans effet).
 */
class TestV001InitMigrationsCollection {

    private final V001_InitMigrationsCollection migration = new V001_InitMigrationsCollection();

    @Test
    void testVersion() {
        assertEquals("V001", migration.version());
    }

    @Test
    void testDescriptionNonVide() {
        assertNotNull(migration.description());
        assertEquals(false, migration.description().isBlank());
    }

    @Test
    void testMigrateSeTermineAvecSucces() {
        // No-op : l'Uni doit se résoudre sans erreur ni valeur (Void)
        migration.migrate().await().indefinitely();
    }
}
