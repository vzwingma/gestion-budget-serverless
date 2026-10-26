package io.github.vzwingma.finances.budget.services.communs.migrations;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Exécute au démarrage de l'application les migrations MongoDB ({@link IMongoMigration}) non encore
 * appliquées, dans l'ordre croissant de leur {@link IMongoMigration#version()}.
 * <p>
 * Découverte des migrations via injection CDI standard ({@link Instance}) — <strong>volontairement pas de
 * scan de classpath ni de lecture de fichiers externes</strong>, incompatible avec le build natif GraalVM Lambda.
 * Chaque migration doit être une classe {@code @ApplicationScoped} implémentant {@link IMongoMigration}.
 * <p>
 * Convention de nommage : voir {@link IMongoMigration}.
 *
 * @author vzwingma
 */
@ApplicationScoped
public class MongoMigrationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(MongoMigrationRunner.class);

    private final Instance<IMongoMigration> migrations;

    private final MigrationRepository migrationRepository;

    /**
     * Identifiant de ce runner (unique par instance, donc par cold start), écrit dans le champ {@code owner}
     * de la réservation : seul le détenteur peut finaliser le statut (ADR-006).
     */
    private final String owner = UUID.randomUUID().toString();

    @Inject
    public MongoMigrationRunner(Instance<IMongoMigration> migrations, MigrationRepository migrationRepository) {
        this.migrations = migrations;
        this.migrationRepository = migrationRepository;
    }

    /**
     * Point d'entrée déclenché au démarrage de Quarkus. Applique séquentiellement les migrations non
     * encore exécutées avec succès. Une migration en échec est explicitement loggée en erreur et n'empêche
     * pas de journaliser les migrations suivantes, mais le démarrage n'est pas bloqué (comportement documenté :
     * ne bloque pas silencieusement, l'erreur reste visible dans les logs applicatifs).
     *
     * @param event évènement de démarrage Quarkus
     */
    void onStart(@Observes StartupEvent event) {
        LOG.info("[MIGRATIONS] > Démarrage vérification migrations MongoDB");

        List<IMongoMigration> migrationsTriees = migrations.stream()
                .sorted(Comparator.comparing(IMongoMigration::version))
                .toList();

        if (migrationsTriees.isEmpty()) {
            LOG.info("[MIGRATIONS] < Aucune migration déclarée");
            return;
        }

        migrationRepository.listerVersionsAppliquees()
                .invoke(versionsAppliquees -> LOG.info("[MIGRATIONS] Versions déjà appliquées : {}", versionsAppliquees))
                .chain(versionsAppliquees -> executerSequentiellement(migrationsTriees, versionsAppliquees))
                .subscribe().with(
                        ignore -> LOG.info("[MIGRATIONS] < Vérification migrations MongoDB terminée"),
                        erreur -> LOG.error("[MIGRATIONS] < Echec inattendu du mécanisme de migration", erreur)
                );
    }

    /**
     * Enchaîne l'exécution des migrations non encore appliquées, dans l'ordre.
     */
    private Uni<Void> executerSequentiellement(List<IMongoMigration> migrationsTriees, List<String> versionsAppliquees) {
        Uni<Void> chaine = Uni.createFrom().voidItem();
        for (IMongoMigration migration : migrationsTriees) {
            if (versionsAppliquees.contains(migration.version())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[MIGRATIONS] Migration {} ({}) déjà appliquée, ignorée", migration.version(), migration.description());
                }
                continue;
            }
            chaine = chaine.chain(() -> executerMigration(migration));
        }
        return chaine;
    }

    /**
     * Réserve atomiquement la version puis exécute la migration et finalise son statut dans la collection
     * {@code _migrations}. Si la version est déjà prise (SUCCES, ou EN_COURS non périmé chez un autre runner),
     * on passe à la version suivante sans l'exécuter. Un échec de migration est tracé en {@code ECHEC}
     * (rejouable au prochain démarrage, y compris vers {@code SUCCES}).
     */
    private Uni<Void> executerMigration(IMongoMigration migration) {
        return migrationRepository.reserver(migration.version(), migration.description(), owner)
                .chain(reservee -> {
                    if (Boolean.FALSE.equals(reservee)) {
                        LOG.info("[MIGRATIONS] Migration {} déjà prise par un autre runner ou déjà appliquée, ignorée", migration.version());
                        return Uni.createFrom().voidItem();
                    }
                    if (LOG.isInfoEnabled()) {
                        LOG.info("[MIGRATIONS] Exécution migration {} : {}", migration.version(), migration.description());
                    }
                    return migration.migrate()
                            .chain(() -> migrationRepository.finaliserSucces(migration.version(), owner))
                            .invoke(() -> LOG.info("[MIGRATIONS] Migration {} appliquée avec succès", migration.version()))
                            .onFailure().recoverWithUni(erreur -> {
                                LOG.error("[MIGRATIONS] ECHEC migration {} ({}) : {}", migration.version(), migration.description(), erreur.getMessage(), erreur);
                                return migrationRepository.finaliserEchec(migration.version(), owner);
                            });
                });
    }
}
