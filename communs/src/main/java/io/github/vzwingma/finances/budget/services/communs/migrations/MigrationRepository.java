package io.github.vzwingma.finances.budget.services.communs.migrations;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Accès Panache à la collection {@code _migrations} de suivi des migrations MongoDB déjà appliquées.
 * <p>
 * Hérite de {@code ReactivePanacheMongoRepositoryBase<MigrationRecord, String>} (et non
 * {@code ReactivePanacheMongoRepository<MigrationRecord>}) car le {@code @BsonId} réel de
 * {@link MigrationRecord} (champ {@code version}) est un {@code String}, alors que
 * {@code ReactivePanacheMongoRepository} fige le type d'identifiant générique à
 * {@code org.bson.types.ObjectId}. Ce choix expose correctement {@code findById(String)} /
 * {@code deleteById(String)} avec le bon type.
 *
 * @author vzwingma
 */
@ApplicationScoped
public class MigrationRepository implements ReactivePanacheMongoRepositoryBase<MigrationRecord, String> {

    private final Clock clock;

    /**
     * Constructeur CDI — injection par constructeur de l'horloge applicative UTC (ADR-004), pas
     * d'injection par champ (cohérent avec la convention constructor injection déjà appliquée à
     * {@link MongoMigrationRunner}, Phase A).
     *
     * @param clock horloge applicative injectée (voir {@code ClockConfig})
     */
    @Inject
    public MigrationRepository(Clock clock) {
        this.clock = clock;
    }

    /**
     * Liste les versions déjà exécutées avec succès (utilisée pour filtrer les migrations à jouer).
     *
     * @return liste des versions déjà appliquées avec succès
     */
    public Uni<List<String>> listerVersionsAppliquees() {
        return find("statut", MigrationRecord.MigrationStatutEnum.SUCCES)
                .project(MigrationRecord.class)
                .list()
                .map(records -> records.stream().map(MigrationRecord::getVersion).toList());
    }

    /**
     * Enregistre l'exécution réussie d'une migration.
     *
     * @param version     version de la migration exécutée (ex. {@code V001})
     * @param description description de la migration
     * @return {@link Uni} terminé une fois l'enregistrement persisté
     */
    public Uni<Void> enregistrerSucces(String version, String description) {
        return persist(new MigrationRecord(version, description, LocalDateTime.now(clock), MigrationRecord.MigrationStatutEnum.SUCCES))
                .replaceWithVoid();
    }

    /**
     * Enregistre l'échec d'exécution d'une migration (traçabilité — n'empêche pas une nouvelle tentative
     * au prochain démarrage puisque seules les versions en {@code SUCCES} sont considérées comme appliquées).
     *
     * @param version     version de la migration en échec (ex. {@code V001})
     * @param description description de la migration
     * @return {@link Uni} terminé une fois l'enregistrement persisté
     */
    public Uni<Void> enregistrerEchec(String version, String description) {
        return persist(new MigrationRecord(version, description, LocalDateTime.now(clock), MigrationRecord.MigrationStatutEnum.ECHEC))
                .replaceWithVoid();
    }
}
