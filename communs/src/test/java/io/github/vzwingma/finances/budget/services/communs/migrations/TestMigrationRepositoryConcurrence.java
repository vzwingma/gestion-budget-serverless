package io.github.vzwingma.finances.budget.services.communs.migrations;

import io.github.vzwingma.finances.budget.services.communs.migrations.scripts.V002_AjoutIndexBudgetsUtilisateurs;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests d'intégration contre un vrai MongoDB (Dev Services, Docker requis, donc exécutés par la CI) de la réservation
 * atomique des migrations (ADR-006) : concurrence de runners, reprise d'un bail périmé, ECHEC puis SUCCES, et création
 * réelle des index de {@link V002_AjoutIndexBudgetsUtilisateurs}.
 * <p>
 * Chaque test utilise des versions uniques ({@code TEST-CONC-<uuid>}) nettoyées par {@code deleteById} : jamais de
 * {@code deleteAll()}, la collection contient les migrations réelles du démarrage (V001, V002).
 *
 * @author QALvin
 */
@QuarkusTest
class TestMigrationRepositoryConcurrence {

    private static final int DELAI_MAX_SECONDES = 20;

    @Inject
    MigrationRepository migrationRepository;

    @Inject
    ReactiveMongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String nomBase;

    private final List<String> versionsCreees = new ArrayList<>();

    @AfterEach
    void nettoyer() {
        versionsCreees.forEach(version -> migrationRepository.deleteById(version).await().indefinitely());
    }

    private String nouvelleVersion() {
        String version = "TEST-CONC-" + UUID.randomUUID();
        versionsCreees.add(version);
        return version;
    }

    // ====== réservation concurrente ======

    @Test
    void testReserverEnConcurrence_UnSeulGagnant() throws Exception {
        final int nbRunners = 8;
        String version = nouvelleVersion();
        CountDownLatch depart = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(nbRunners);
        try {
            List<CompletableFuture<Boolean>> reservations = new ArrayList<>();
            for (int i = 0; i < nbRunners; i++) {
                final String owner = "runner-" + i;
                reservations.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        depart.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return migrationRepository.reserver(version, "concurrence", owner).await().atMost(java.time.Duration.ofSeconds(DELAI_MAX_SECONDES));
                }, executor));
            }
            depart.countDown();

            long gagnants = 0;
            for (CompletableFuture<Boolean> reservation : reservations) {
                if (Boolean.TRUE.equals(reservation.get(DELAI_MAX_SECONDES, TimeUnit.SECONDS))) {
                    gagnants++;
                }
            }
            assertEquals(1, gagnants, "Exactement un runner doit obtenir la réservation (index unique sur _id)");
        } finally {
            executor.shutdownNow();
        }
        MigrationRecord record = migrationRepository.findById(version).await().indefinitely();
        assertEquals(MigrationRecord.MigrationStatutEnum.EN_COURS, record.getStatut());
    }

    @Test
    void testDeuxRunnersConcurrents_UneSeuleExecutionDeLaMigration() throws Exception {
        String version = nouvelleVersion();
        AtomicInteger executions = new AtomicInteger();
        IMongoMigration migration = new IMongoMigration() {
            @Override
            public String version() {
                return version;
            }

            @Override
            public String description() {
                return "Migration de test comptée";
            }

            @Override
            public Uni<Void> migrate() {
                return Uni.createFrom().voidItem().invoke(executions::incrementAndGet);
            }
        };
        @SuppressWarnings("unchecked")
        Instance<IMongoMigration> instance = mock(Instance.class);
        when(instance.stream()).thenAnswer(inv -> java.util.stream.Stream.of(migration));
        MongoMigrationRunner runner1 = new MongoMigrationRunner(instance, migrationRepository);
        MongoMigrationRunner runner2 = new MongoMigrationRunner(instance, migrationRepository);

        CountDownLatch depart = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> {
                attendre(depart);
                runner1.onStart(null);
            }, executor);
            CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> {
                attendre(depart);
                runner2.onStart(null);
            }, executor);
            depart.countDown();
            f1.get(DELAI_MAX_SECONDES, TimeUnit.SECONDS);
            f2.get(DELAI_MAX_SECONDES, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        // onStart est asynchrone (subscribe) : on attend l'état final SUCCES
        attendreSucces(version);
        assertEquals(1, executions.get(), "La migration ne doit être exécutée que par un seul des deux runners");
        MigrationRecord record = migrationRepository.findById(version).await().indefinitely();
        assertEquals(MigrationRecord.MigrationStatutEnum.SUCCES, record.getStatut());
        assertNotNull(record.getOwner());
    }

    private static void attendre(CountDownLatch depart) {
        try {
            depart.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void attendreSucces(String version) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(DELAI_MAX_SECONDES);
        while (System.nanoTime() < limite) {
            if (migrationRepository.estAppliquee(version).await().indefinitely()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("La version " + version + " n'est pas passée en SUCCES dans le délai imparti");
    }

    // ====== états de la réservation ======

    @Test
    void testVersionEnSucces_nEstJamaisReservable() {
        String version = nouvelleVersion();
        assertTrue(migrationRepository.reserver(version, "d", "A").await().indefinitely());
        assertTrue(migrationRepository.finaliserSucces(version, "A").await().indefinitely());

        assertFalse(migrationRepository.reserver(version, "d", "B").await().indefinitely(),
                "SUCCES + E11000 : version déjà prise");
        assertTrue(migrationRepository.estAppliquee(version).await().indefinitely());
    }

    @Test
    void testVersionEnCoursAvecBailValide_nEstPasReservablePourUnAutreOwner() {
        String version = nouvelleVersion();
        assertTrue(migrationRepository.reserver(version, "d", "A").await().indefinitely());

        assertFalse(migrationRepository.reserver(version, "d", "B").await().indefinitely());
        assertFalse(migrationRepository.estAppliquee(version).await().indefinitely(), "EN_COURS n'est pas SUCCES : la chaîne s'arrête");
        assertEquals("A", migrationRepository.findById(version).await().indefinitely().getOwner());
    }

    @Test
    void testBailPerime_ReprisParUnAutreRunner_etAncienOwnerNePeutPlusFinaliser() {
        String version = nouvelleVersion();
        // EN_COURS posé il y a 10 minutes (> bail de 5 min) par un runner mort
        MigrationRecord perime = new MigrationRecord(version, "bail périmé", LocalDateTime.now(java.time.ZoneOffset.UTC).minusMinutes(10), MigrationRecord.MigrationStatutEnum.EN_COURS);
        perime.setOwner("A");
        migrationRepository.persist(perime).await().indefinitely();

        assertTrue(migrationRepository.reserver(version, "reprise", "B").await().indefinitely(), "Bail périmé : reprise par B");
        assertFalse(migrationRepository.finaliserSucces(version, "A").await().indefinitely(),
                "L'ancien détenteur ne peut plus finaliser (filtre sur owner)");
        assertTrue(migrationRepository.finaliserSucces(version, "B").await().indefinitely());
        assertEquals(MigrationRecord.MigrationStatutEnum.SUCCES, migrationRepository.findById(version).await().indefinitely().getStatut());
    }

    @Test
    void testEchecPuisSucces_laVersionEnEchecEstRejouable() {
        String version = nouvelleVersion();
        assertTrue(migrationRepository.reserver(version, "d", "A").await().indefinitely());
        migrationRepository.finaliserEchec(version, "A").await().indefinitely();
        assertEquals(MigrationRecord.MigrationStatutEnum.ECHEC, migrationRepository.findById(version).await().indefinitely().getStatut());
        assertFalse(migrationRepository.listerVersionsAppliquees().await().indefinitely().contains(version));

        // Nouveau démarrage : réservable (F6b : plus de E11000 sur l'insert d'une version en ECHEC)
        assertTrue(migrationRepository.reserver(version, "d", "B").await().indefinitely(), "Une version en ECHEC doit être réservable");
        assertTrue(migrationRepository.finaliserSucces(version, "B").await().indefinitely());

        MigrationRecord record = migrationRepository.findById(version).await().indefinitely();
        assertEquals(MigrationRecord.MigrationStatutEnum.SUCCES, record.getStatut());
        assertEquals("B", record.getOwner());
        assertTrue(migrationRepository.listerVersionsAppliquees().await().indefinitely().contains(version));
    }

    @Test
    void testRunnerSurMigrationEnEchec_puisRelanceEnSucces() {
        String version = nouvelleVersion();
        AtomicInteger tentative = new AtomicInteger();
        IMongoMigration migration = new IMongoMigration() {
            @Override
            public String version() {
                return version;
            }

            @Override
            public String description() {
                return "Échoue puis réussit";
            }

            @Override
            public Uni<Void> migrate() {
                return tentative.incrementAndGet() == 1
                        ? Uni.createFrom().failure(new IllegalStateException("échec simulé"))
                        : Uni.createFrom().voidItem();
            }
        };
        @SuppressWarnings("unchecked")
        Instance<IMongoMigration> instance = mock(Instance.class);
        when(instance.stream()).thenAnswer(inv -> java.util.stream.Stream.of(migration));

        new MongoMigrationRunner(instance, migrationRepository).onStart(null);
        assertTrueEventually(() -> MigrationRecord.MigrationStatutEnum.ECHEC.equals(statut(version)), "ECHEC attendu");

        new MongoMigrationRunner(instance, migrationRepository).onStart(null);
        assertTrueEventually(() -> MigrationRecord.MigrationStatutEnum.SUCCES.equals(statut(version)), "SUCCES attendu après relance");
        assertEquals(2, tentative.get());
    }

    private MigrationRecord.MigrationStatutEnum statut(String version) {
        MigrationRecord record = migrationRepository.findById(version).await().indefinitely();
        return record == null ? null : record.getStatut();
    }

    private static void assertTrueEventually(java.util.function.BooleanSupplier condition, String message) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(DELAI_MAX_SECONDES);
        while (System.nanoTime() < limite) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError(message);
    }

    // ====== V002 réel ======

    @Test
    void testV002_creeLesIndexReels_deuxiemeExecutionOk_indexSousAutreNomIgnore() {
        // Base dédiée (jetable, portée par le conteneur Dev Services) : n'interfère pas avec la base de test
        String baseDediee = nomBase + "-v002-" + UUID.randomUUID().toString().substring(0, 8);
        V002_AjoutIndexBudgetsUtilisateurs v002 = new V002_AjoutIndexBudgetsUtilisateurs(mongoClient, baseDediee);

        // 'utilisateurs' : index {login:1} déjà présent sous un autre nom (créé manuellement)
        mongoClient.getDatabase(baseDediee).getCollection("utilisateurs")
                .createIndex(new Document("login", 1), new com.mongodb.client.model.IndexOptions().name("login_manuel"))
                .await().indefinitely();

        v002.migrate().await().indefinitely();
        // 2e exécution : idempotente
        v002.migrate().await().indefinitely();

        List<Document> indexBudgets = mongoClient.getDatabase(baseDediee).getCollection("budgets").listIndexes().collect().asList().await().indefinitely();
        List<Document> indexBudgetsCle = indexBudgets.stream()
                .filter(index -> new Document("idCompteBancaire", 1).append("annee", 1).append("mois", 1).equals(normaliser(index.get("key", Document.class))))
                .toList();
        assertEquals(1, indexBudgetsCle.size(), "Index budgets {idCompteBancaire, annee, mois} créé une seule fois : " + indexBudgets);
        assertFalse(Boolean.TRUE.equals(indexBudgetsCle.getFirst().getBoolean("unique")));

        List<Document> indexUtilisateurs = mongoClient.getDatabase(baseDediee).getCollection("utilisateurs").listIndexes().collect().asList().await().indefinitely();
        List<Document> indexLogin = indexUtilisateurs.stream()
                .filter(index -> new Document("login", 1).equals(normaliser(index.get("key", Document.class))))
                .toList();
        assertEquals(1, indexLogin.size(), "L'index de même clé sous un autre nom doit être réutilisé : " + indexUtilisateurs);
        assertEquals("login_manuel", indexLogin.getFirst().getString("name"));
    }

    @Test
    void testV002_appliqueeParLeRunnerAuDemarrage_indexPresentsSurLaBaseDeTest() throws InterruptedException {
        // V002 est une migration réelle exécutée par MongoMigrationRunner au démarrage Quarkus
        attendreSucces("V002");
        List<Document> indexBudgets = mongoClient.getDatabase(nomBase).getCollection("budgets").listIndexes().collect().asList().await().indefinitely();
        assertTrue(indexBudgets.stream().anyMatch(index -> new Document("idCompteBancaire", 1).append("annee", 1).append("mois", 1)
                .equals(normaliser(index.get("key", Document.class)))), "Index budgets attendu : " + indexBudgets);
        List<Document> indexUtilisateurs = mongoClient.getDatabase(nomBase).getCollection("utilisateurs").listIndexes().collect().asList().await().indefinitely();
        assertTrue(indexUtilisateurs.stream().anyMatch(index -> new Document("login", 1).equals(normaliser(index.get("key", Document.class)))),
                "Index utilisateurs attendu : " + indexUtilisateurs);
    }

    /** Sens d'index normalisé en Integer (Mongo peut les stocker en Integer, Long ou Double) */
    private static Document normaliser(Document cle) {
        Document normalisee = new Document();
        cle.forEach((champ, sens) -> normalisee.append(champ, sens instanceof Number n ? n.intValue() : sens));
        return normalisee;
    }
}
