package io.github.vzwingma.finances.budget.services.communs.migrations.scripts;

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests unitaires de la migration exemple {@link V001_InitMigrationsCollection}. Migration no-op :
 * on vérifie le contrat (version/description stables) et le résultat réel de l'exécution de
 * {@code migrate()} — complétion effective de l'{@link io.smallrye.mutiny.Uni} sans échec ni valeur
 * (contrat {@code Uni<Void>}), pas seulement l'absence d'exception levée au niveau de l'appelant.
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
    void testMigrateSeTermineAvecSuccesEtSansValeur() {
        // Act : souscription explicite au résultat réel de la migration (pas d'.await().indefinitely()
        // qui masquerait le résultat derrière un simple "ne throw pas").
        UniAssertSubscriber<Void> resultat = migration.migrate()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // Assert : la migration se termine avec succès (pas d'échec) et sans valeur (contrat Uni<Void>).
        resultat.assertCompleted().assertItem(null);
    }
}
