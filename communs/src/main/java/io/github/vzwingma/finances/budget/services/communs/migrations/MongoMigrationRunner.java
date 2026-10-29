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
     * encore exécutées avec succès. Une migration en échec est explicitement loggée en erreur et arrête la chaîne
     * (les versions suivantes ne sont pas jouées) ; le démarrage n'est pas bloqué (l'erreur reste visible dans les
     * logs applicatifs, la version en {@code ECHEC} est rejouée au prochain démarrage).
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
     * Enchaîne l'exécution des migrations non encore appliquées, dans l'ordre. La chaîne s'arrête pour ce démarrage
     * dès qu'une version n'est pas appliquée (tenue par un autre runner, en échec, ou finalisation impossible) : les
     * versions suivantes ne sont pas tentées, pour ne jamais exécuter la version N+1 avant la fin de la version N.
     */
    private Uni<Void> executerSequentiellement(List<IMongoMigration> migrationsTriees, List<String> versionsAppliquees) {
        Uni<Boolean> chaine = Uni.createFrom().item(Boolean.TRUE);
        for (IMongoMigration migration : migrationsTriees) {
            if (versionsAppliquees.contains(migration.version())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("[MIGRATIONS] Migration {} ({}) déjà appliquée, ignorée", migration.version(), migration.description());
                }
                continue;
            }
            chaine = chaine.chain(poursuivre -> Boolean.TRUE.equals(poursuivre)
                    ? executerMigration(migration)
                    : Uni.createFrom().item(Boolean.FALSE));
        }
        return chaine.replaceWithVoid();
    }

    /**
     * Réserve atomiquement la version puis exécute la migration et finalise son statut dans la collection
     * {@code _migrations}. La chaîne n'est poursuivie que si la version est appliquée (par ce runner ou, entre-temps,
     * par un autre) :
     * <ul>
     *     <li>version non réservable : si un autre runner l'a passée en {@code SUCCES} depuis la lecture initiale, la
     *     chaîne continue ; sinon (EN_COURS non expiré chez un autre runner) elle est arrêtée ;</li>
     *     <li>échec de migration : tracé en {@code ECHEC} (rejouable au prochain démarrage), chaîne arrêtée pour ne
     *     jamais exécuter la version N+1 sans la version N ;</li>
     *     <li>échec de la seule finalisation {@code SUCCES} : loggé en ERROR (pas marqué {@code ECHEC}, la version
     *     reste {@code EN_COURS} jusqu'à expiration du bail), chaîne arrêtée.</li>
     * </ul>
     *
     * @return {@code true} si la chaîne peut se poursuivre, {@code false} sinon
     */
    private Uni<Boolean> executerMigration(IMongoMigration migration) {
        return migrationRepository.reserver(migration.version(), migration.description(), owner)
                .chain(reservee -> {
                    if (!Boolean.TRUE.equals(reservee)) {
                        return migrationRepository.estAppliquee(migration.version())
                                .map(appliquee -> {
                                    if (Boolean.TRUE.equals(appliquee)) {
                                        LOG.info("[MIGRATIONS] Migration {} appliquée entre-temps par un autre runner : la chaîne continue", migration.version());
                                        return Boolean.TRUE;
                                    }
                                    LOG.info("[MIGRATIONS] Migration {} déjà prise par un autre runner : arrêt de la chaîne pour ce démarrage", migration.version());
                                    return Boolean.FALSE;
                                });
                    }
                    if (LOG.isInfoEnabled()) {
                        LOG.info("[MIGRATIONS] Exécution migration {} : {}", migration.version(), migration.description());
                    }
                    // La reprise en ECHEC est attachée à migrate() uniquement
                    return migration.migrate()
                            .replaceWith(Boolean.TRUE)
                            .onFailure().recoverWithUni(erreur -> {
                                LOG.error("[MIGRATIONS] ECHEC migration {} ({}) : {}", migration.version(), migration.description(), erreur.getMessage(), erreur);
                                return migrationRepository.finaliserEchec(migration.version(), owner).replaceWith(Boolean.FALSE);
                            })
                            .chain(migrationReussie -> Boolean.TRUE.equals(migrationReussie) ? finaliserSucces(migration) : Uni.createFrom().item(Boolean.FALSE));
                });
    }

    /**
     * Finalise la migration en {@code SUCCES}. Un échec d'écriture est loggé en ERROR sans marquer {@code ECHEC} :
     * la migration a bien été exécutée, la version reste {@code EN_COURS} jusqu'à expiration du bail.
     *
     * @return {@code true} si la finalisation est persistée, {@code false} sinon (échec d'écriture, ou réservation
     * reprise par un autre runner : aucun document finalisé) — la chaîne doit alors s'arrêter
     */
    private Uni<Boolean> finaliserSucces(IMongoMigration migration) {
        return migrationRepository.finaliserSucces(migration.version(), owner)
                // finalisée == false : le WARN (réservation reprise par un autre runner) est déjà émis par
                // MigrationRepository.finaliser, il n'est pas répété ici ; l'appelant arrête la chaîne
                .invoke(finalisee -> {
                    if (Boolean.TRUE.equals(finalisee)) {
                        LOG.info("[MIGRATIONS] Migration {} appliquée avec succès", migration.version());
                    }
                })
                .onFailure().recoverWithUni(erreur -> {
                    LOG.error("[MIGRATIONS] Migration {} exécutée mais finalisation SUCCES impossible : la version reste EN_COURS jusqu'à expiration du bail ({}). Arrêt de la chaîne",
                            migration.version(), MigrationRepository.BAIL, erreur);
                    return Uni.createFrom().item(Boolean.FALSE);
                });
    }
}
