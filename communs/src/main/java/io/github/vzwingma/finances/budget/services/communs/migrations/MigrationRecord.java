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
     * Date/heure d'exécution (succès ou échec) de la migration.
     */
    private LocalDateTime dateExecution;

    /**
     * Statut d'exécution de la migration.
     */
    private MigrationStatutEnum statut;

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
                '}';
    }

    /**
     * Statut d'exécution d'une migration.
     */
    @RegisterForReflection
    public enum MigrationStatutEnum {
        SUCCES,
        ECHEC
    }
}
