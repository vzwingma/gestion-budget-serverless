package io.github.vzwingma.finances.budget.services.communs.migrations;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.LocalDateTime;

/**
 * Document de suivi d'exécution d'une migration MongoDB, stocké dans la collection {@code _migrations}.
 * Une entrée = une migration {@link IMongoMigration} exécutée avec succès (ou en échec, voir {@link #statut}).
 *
 * @author vzwingma
 */
@MongoEntity(collection = "_migrations")
@RegisterForReflection
@Getter
@Setter
@NoArgsConstructor
public class MigrationRecord {

    /**
     * Version de la migration, ex. {@code V001}. Correspond à {@link IMongoMigration#version()}.
     */
    @BsonId
    private String version;

    /**
     * Description de la migration au moment de son exécution.
     */
    private String description;

    /**
     * Date/heure de réservation puis d'exécution de la migration. Pour un statut {@link MigrationStatutEnum#EN_COURS},
     * sert de début du bail : au-delà de la durée de bail, la réservation est considérée comme périmée et peut être reprise.
     */
    private LocalDateTime dateExecution;

    /**
     * Statut d'exécution de la migration.
     */
    private MigrationStatutEnum statut;

    /**
     * Identifiant du runner qui a réservé la migration (ADR-006) : seul ce runner peut finaliser le statut.
     */
    private String owner;

    public MigrationRecord(String version, String description, LocalDateTime dateExecution, MigrationStatutEnum statut) {
        this.version = version;
        this.description = description;
        this.dateExecution = dateExecution;
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "MigrationRecord{" +
                "version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", dateExecution=" + dateExecution +
                ", statut=" + statut +
                ", owner='" + owner + "'" +
                '}';
    }

    /**
     * Statut d'exécution d'une migration.
     */
    @RegisterForReflection
    public enum MigrationStatutEnum {
        /** Migration réservée par un runner, en cours d'exécution (bail limité dans le temps). */
        EN_COURS,
        SUCCES,
        ECHEC
    }
}
