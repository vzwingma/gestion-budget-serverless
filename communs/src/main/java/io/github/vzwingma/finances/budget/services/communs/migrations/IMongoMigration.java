package io.github.vzwingma.finances.budget.services.communs.migrations;

import io.smallrye.mutiny.Uni;

/**
 * Port d'une migration de schéma MongoDB.
 * <p>
 * Convention de nommage des classes d'implémentation : {@code V<numéro incrémental sur 3 chiffres>_<description courte>}
 * (ex. {@code V001_InitMigrationsCollection}, {@code V002_AjoutIndexComptes}). Le numéro doit être unique,
 * strictement croissant et jamais réutilisé/modifié une fois publié (une migration déjà exécutée en environnement
 * ne doit plus jamais changer de contenu : toute évolution ultérieure du schéma = nouvelle migration).
 * <p>
 * Chaque implémentation doit être une classe CDI ({@code @ApplicationScoped}) découverte par injection standard
 * ({@code Instance<IMongoMigration>}) — <strong>aucun scan de classpath ou de fichiers externes</strong>, incompatible
 * avec le build natif GraalVM Lambda. Voir {@link MongoMigrationRunner} pour le mécanisme d'exécution.
 * <p>
 * Une migration doit être idempotente autant que possible et ne jamais être bloquante indéfiniment : c'est le
 * runner qui garantit la non ré-exécution via la collection {@code _migrations}, mais le code de la migration
 * elle-même doit rester défensif (ex. vérifier l'existence d'un index avant de le créer).
 *
 * @author vzwingma
 */
public interface IMongoMigration {

    /**
     * Version de la migration (ex. {@code V001}). Sert de clé unique dans la collection {@code _migrations}
     * et de critère de tri d'exécution (ordre lexicographique croissant).
     *
     * @return version de la migration
     */
    String version();

    /**
     * Description courte et humaine de la migration (objet de la migration), utilisée pour le logging et la traçabilité.
     *
     * @return description de la migration
     */
    String description();

    /**
     * Exécute la migration.
     *
     * @return {@link Uni} terminé avec succès si la migration s'est appliquée correctement, en échec sinon
     */
    Uni<Void> migrate();
}
