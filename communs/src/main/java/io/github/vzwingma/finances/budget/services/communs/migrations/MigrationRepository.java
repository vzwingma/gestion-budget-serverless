package io.github.vzwingma.finances.budget.services.communs.migrations;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Accès Panache à la collection {@code _migrations} de suivi des migrations MongoDB.
 * <p>
 * Hérite de {@code ReactivePanacheMongoRepositoryBase<MigrationRecord, String>} (et non
 * {@code ReactivePanacheMongoRepository<MigrationRecord>}) car le {@code @BsonId} réel de
 * {@link MigrationRecord} (champ {@code version}) est un {@code String}, alors que
 * {@code ReactivePanacheMongoRepository} fige le type d'identifiant générique à
 * {@code org.bson.types.ObjectId}. Ce choix expose correctement {@code findById(String)} /
 * {@code deleteById(String)} avec le bon type.
 * <p>
 * <b>Réservation atomique (ADR-006)</b> : l'exécution d'une version est précédée d'une réservation par un seul
 * {@code findOneAndUpdate} avec upsert, dont l'unicité est garantie par l'index unique sur {@code _id}. Une version
 * ne peut passer à {@code EN_COURS} que si elle est absente, en {@code ECHEC}, ou en {@code EN_COURS} avec un bail
 * périmé ({@link #BAIL}). Une version en {@code SUCCES} ou {@code EN_COURS} valide produit une clé dupliquée
 * (E11000) : la version est « déjà prise ».
 *
 * @author vzwingma
 */
@ApplicationScoped
public class MigrationRepository implements ReactivePanacheMongoRepositoryBase<MigrationRecord, String> {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationRepository.class);

    /**
     * Durée de validité d'une réservation {@code EN_COURS}. Au-delà, un autre runner peut reprendre la version
     * (le runner précédent est considéré comme mort, ex. Lambda gelée ou interrompue).
     */
    static final Duration BAIL = Duration.ofMinutes(5);

    private static final String ATTRIBUT_ID = "_id";
    private static final String ATTRIBUT_STATUT = "statut";
    private static final String ATTRIBUT_OWNER = "owner";
    private static final String ATTRIBUT_DATE_EXECUTION = "dateExecution";
    private static final String ATTRIBUT_DESCRIPTION = "description";

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
        return find(ATTRIBUT_STATUT, MigrationRecord.MigrationStatutEnum.SUCCES.name())
                .project(MigrationRecord.class)
                .list()
                .map(records -> records.stream().map(MigrationRecord::getVersion).toList());
    }

    /**
     * Réserve atomiquement une version pour exécution ({@code findOneAndUpdate} avec upsert).
     * <p>
     * Le filtre ne correspond que si la version est en {@code ECHEC} ou en {@code EN_COURS} avec un bail périmé ;
     * si la version est absente, l'upsert la crée. Dans les autres cas (SUCCES, EN_COURS valide), l'upsert tente
     * d'insérer un {@code _id} déjà présent : l'erreur E11000 signifie « version déjà prise ».
     *
     * @param version     version à réserver (ex. {@code V001})
     * @param description description de la migration
     * @param owner       identifiant du runner demandeur
     * @return {@code true} si la version est réservée par {@code owner}, {@code false} si elle est déjà prise
     */
    public Uni<Boolean> reserver(String version, String description, String owner) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        Bson filtre = Filters.and(
                Filters.eq(ATTRIBUT_ID, version),
                Filters.or(
                        Filters.eq(ATTRIBUT_STATUT, MigrationRecord.MigrationStatutEnum.ECHEC.name()),
                        Filters.and(
                                Filters.eq(ATTRIBUT_STATUT, MigrationRecord.MigrationStatutEnum.EN_COURS.name()),
                                Filters.lt(ATTRIBUT_DATE_EXECUTION, maintenant.minus(BAIL)))));
        Bson miseAJour = Updates.combine(
                Updates.set(ATTRIBUT_STATUT, MigrationRecord.MigrationStatutEnum.EN_COURS.name()),
                Updates.set(ATTRIBUT_OWNER, owner),
                Updates.set(ATTRIBUT_DESCRIPTION, description),
                Updates.set(ATTRIBUT_DATE_EXECUTION, maintenant));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER);

        return mongoCollection().findOneAndUpdate(filtre, miseAJour, options)
                .map(record -> record != null && owner.equals(record.getOwner()))
                .onFailure(MigrationRepository::estCleDupliquee).recoverWithItem(false);
    }

    /**
     * Relit le statut d'une version après une clé dupliquée à la réservation : permet de distinguer une version
     * passée en {@code SUCCES} par un autre runner entre la lecture initiale et la réservation (la chaîne peut
     * continuer) d'une version {@code EN_COURS} chez un autre runner (la chaîne doit s'arrêter).
     *
     * @param version version à contrôler
     * @return {@code true} si la version est en {@code SUCCES}
     */
    public Uni<Boolean> estAppliquee(String version) {
        return findById(version)
                .map(record -> record != null && MigrationRecord.MigrationStatutEnum.SUCCES.equals(record.getStatut()));
    }

    /**
     * Finalise une migration en succès. Sans effet (WARN) si la réservation a été reprise par un autre runner.
     *
     * @param version version exécutée
     * @param owner   identifiant du runner ayant réservé la version
     * @return {@link Uni} terminé une fois le statut persisté
     */
    public Uni<Void> finaliserSucces(String version, String owner) {
        return finaliser(version, owner, MigrationRecord.MigrationStatutEnum.SUCCES);
    }

    /**
     * Finalise une migration en échec (traçabilité — la version reste réservable : une nouvelle tentative
     * est possible au prochain démarrage, y compris vers {@code SUCCES}).
     *
     * @param version version en échec
     * @param owner   identifiant du runner ayant réservé la version
     * @return {@link Uni} terminé une fois le statut persisté
     */
    public Uni<Void> finaliserEchec(String version, String owner) {
        return finaliser(version, owner, MigrationRecord.MigrationStatutEnum.ECHEC);
    }

    private Uni<Void> finaliser(String version, String owner, MigrationRecord.MigrationStatutEnum statut) {
        Bson filtre = Filters.and(Filters.eq(ATTRIBUT_ID, version), Filters.eq(ATTRIBUT_OWNER, owner));
        Bson miseAJour = Updates.combine(
                Updates.set(ATTRIBUT_STATUT, statut.name()),
                Updates.set(ATTRIBUT_DATE_EXECUTION, LocalDateTime.now(clock)));
        return mongoCollection().updateOne(filtre, miseAJour)
                .invoke(resultat -> {
                    if (resultat.getMatchedCount() == 0) {
                        LOG.warn("[MIGRATIONS] Finalisation {} ignorée pour {} : réservation reprise par un autre runner", statut, version);
                    }
                })
                .replaceWithVoid();
    }

    /**
     * @param erreur erreur reçue de MongoDB
     * @return {@code true} si l'erreur (ou sa cause) est une violation de clé unique (E11000)
     */
    private static boolean estCleDupliquee(Throwable erreur) {
        for (Throwable courante = erreur; courante != null; courante = courante.getCause()) {
            if (courante instanceof MongoException mongoException && ErrorCategory.fromErrorCode(mongoException.getCode()) == ErrorCategory.DUPLICATE_KEY) {
                return true;
            }
            if (courante.getCause() == courante) {
                break;
            }
        }
        return false;
    }
}
