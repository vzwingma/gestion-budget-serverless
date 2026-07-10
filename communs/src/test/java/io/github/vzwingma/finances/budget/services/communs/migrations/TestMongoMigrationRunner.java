package io.github.vzwingma.finances.budget.services.communs.migrations;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests unitaires purs de {@link MongoMigrationRunner}.
 * <p>
 * Pas de {@code @QuarkusTest} : {@link MongoMigrationRunner} est instancié directement, ses dépendances
 * ({@link Instance} de migrations, {@link MigrationRepository}) sont mockées avec Mockito. Le point d'entrée
 * {@code onStart} est package-private, accessible depuis cette classe de test (même package), et déclenche
 * une chaîne {@link Uni} entièrement synchrone côté mocks : {@code .await().indefinitely()} n'est pas
 * nécessaire côté production (interdit hors tests), mais on résout explicitement les side-effects en
 * attendant sur le mock du repository plutôt que sur le runner (qui ne retourne rien, cf. onStart void).
 * <p>
 * Pour observer le résultat de la chaîne réactive interne (asynchrone via {@code subscribe().with(...)}),
 * chaque test utilise un verrou (CountDownLatch-like via boucle d'attente courte) — ici simplifié car tous
 * les {@link Uni} produits par les mocks sont déjà résolus (item/failure immédiats), donc la chaîne Mutiny
 * s'exécute de manière synchrone lors de l'appel à {@code subscribe().with(...)} et le test peut vérifier
 * l'état immédiatement après l'appel à {@code onStart}.
 *
 * @author QALvin
 */
class TestMongoMigrationRunner {

    private MigrationRepository migrationRepository;
    private MongoMigrationRunner runner;

    @BeforeEach
    void setup() {
        migrationRepository = mock(MigrationRepository.class);
        // runner instancié dans setMigrations() une fois l'Instance<IMongoMigration> mockée disponible
        // (injection désormais par constructeur, cf. MongoMigrationRunner).
    }

    /**
     * Construit un {@link Instance} mocké dont le {@code .stream()} renvoie les migrations fournies,
     * dans l'ordre donné (le runner est responsable de trier, on ne trie pas ici pour garder le test
     * de tri significatif), puis instancie le runner via son constructeur avec cette Instance mockée.
     */
    @SafeVarargs
    private void setMigrations(IMongoMigration... migrations) {
        @SuppressWarnings("unchecked")
        Instance<IMongoMigration> instance = mock(Instance.class);
        when(instance.stream()).thenAnswer(inv -> java.util.Arrays.stream(migrations));
        runner = new MongoMigrationRunner(instance, migrationRepository);
    }

    private IMongoMigration migrationSucces(String version, String description) {
        IMongoMigration migration = mock(IMongoMigration.class);
        when(migration.version()).thenReturn(version);
        when(migration.description()).thenReturn(description);
        when(migration.migrate()).thenReturn(Uni.createFrom().voidItem());
        return migration;
    }

    private IMongoMigration migrationEchec(String version, String description, RuntimeException erreur) {
        IMongoMigration migration = mock(IMongoMigration.class);
        when(migration.version()).thenReturn(version);
        when(migration.description()).thenReturn(description);
        when(migration.migrate()).thenReturn(Uni.createFrom().failure(erreur));
        return migration;
    }

    // ====== 1. Migration appliquée une seule fois (nominal) ======

    @Test
    void testMigrationNonAppliqueeEstExecuteeEtEnregistreeEnSucces() {
        // Arrange
        IMongoMigration migration = migrationSucces("V001", "Init collection");
        setMigrations(migration);

        when(migrationRepository.listerVersionsAppliquees())
                .thenReturn(Uni.createFrom().item(List.of()));
        when(migrationRepository.enregistrerSucces(anyString(), anyString()))
                .thenReturn(Uni.createFrom().voidItem());

        // Act
        runner.onStart(null);

        // Assert
        verify(migration, times(1)).migrate();
        verify(migrationRepository, times(1))
                .enregistrerSucces("V001", "Init collection");
        verify(migrationRepository, never()).enregistrerEchec(anyString(), anyString());
    }

    // ====== 2. Idempotence : migration déjà en SUCCES n'est pas ré-exécutée ======

    @Test
    void testMigrationDejaAppliqueeNestPasReExecutee() {
        // Arrange : V001 déjà présente en SUCCES dans _migrations
        IMongoMigration migration = migrationSucces("V001", "Init collection");
        setMigrations(migration);

        when(migrationRepository.listerVersionsAppliquees())
                .thenReturn(Uni.createFrom().item(List.of("V001")));

        // Act
        runner.onStart(null);

        // Assert : ni exécution, ni nouvel enregistrement
        verify(migration, never()).migrate();
        verify(migrationRepository, never()).enregistrerSucces(anyString(), anyString());
        verify(migrationRepository, never()).enregistrerEchec(anyString(), anyString());
    }

    @Test
    void testAucuneMigrationDeclareeNeDeclencheAucunAppelRepository() {
        // Arrange : Instance<IMongoMigration> vide (aucun bean CDI découvert)
        setMigrations();

        // Act
        runner.onStart(null);

        // Assert : court-circuit avant même d'interroger _migrations
        verifyNoInteractions(migrationRepository);
    }

    // ====== 3. Tri par version croissante ======

    @Test
    void testMigrationsExecuteesDansLordreCroissantDeVersion() {
        // Arrange : déclarées dans le désordre en entrée (V003, V001, V002)
        IMongoMigration v003 = migrationSucces("V003", "Troisieme");
        IMongoMigration v001 = migrationSucces("V001", "Premiere");
        IMongoMigration v002 = migrationSucces("V002", "Deuxieme");
        setMigrations(v003, v001, v002);

        when(migrationRepository.listerVersionsAppliquees())
                .thenReturn(Uni.createFrom().item(List.of()));
        when(migrationRepository.enregistrerSucces(anyString(), anyString()))
                .thenReturn(Uni.createFrom().voidItem());

        List<String> ordreExecution = new java.util.ArrayList<>();
        doAnswer(inv -> {
            ordreExecution.add("migrate:" + v001.version());
            return Uni.createFrom().voidItem();
        }).when(v001).migrate();
        doAnswer(inv -> {
            ordreExecution.add("migrate:" + v002.version());
            return Uni.createFrom().voidItem();
        }).when(v002).migrate();
        doAnswer(inv -> {
            ordreExecution.add("migrate:" + v003.version());
            return Uni.createFrom().voidItem();
        }).when(v003).migrate();

        // Act
        runner.onStart(null);

        // Assert : ordre d'exécution = ordre croissant de version, indépendamment de l'ordre d'injection CDI
        assertEquals(List.of("migrate:V001", "migrate:V002", "migrate:V003"), ordreExecution);

        ArgumentCaptor<String> versionCaptor = ArgumentCaptor.forClass(String.class);
        verify(migrationRepository, times(3))
                .enregistrerSucces(versionCaptor.capture(), anyString());
        assertEquals(List.of("V001", "V002", "V003"), versionCaptor.getAllValues());
    }

    // ====== 4. Echec explicite non bloquant ======

    @Test
    void testMigrationEnEchecEstEnregistreeEnEchecSansBloquerLesSuivantes() {
        // Arrange : V001 échoue (Uni en failure), V002 doit quand même s'exécuter
        RuntimeException erreurSimulee = new RuntimeException("Erreur simulee migration V001");
        IMongoMigration v001 = migrationEchec("V001", "Premiere en echec", erreurSimulee);
        IMongoMigration v002 = migrationSucces("V002", "Deuxieme ok");
        setMigrations(v001, v002);

        when(migrationRepository.listerVersionsAppliquees())
                .thenReturn(Uni.createFrom().item(List.of()));
        when(migrationRepository.enregistrerSucces(anyString(), anyString()))
                .thenReturn(Uni.createFrom().voidItem());
        when(migrationRepository.enregistrerEchec(anyString(), anyString()))
                .thenReturn(Uni.createFrom().voidItem());

        // Act : onStart ne doit lever aucune exception (non bloquant pour le demarrage)
        assertDoesNotThrow(() -> runner.onStart(null));

        // Assert : V001 enregistree en echec, V002 executee et enregistree en succes malgre l'echec precedent
        verify(migrationRepository, times(1))
                .enregistrerEchec("V001", "Premiere en echec");
        verify(migrationRepository, never())
                .enregistrerSucces(eq("V001"), anyString());
        verify(v002, times(1)).migrate();
        verify(migrationRepository, times(1))
                .enregistrerSucces("V002", "Deuxieme ok");
    }

    @Test
    void testEchecAuMomentDeLenregistrementNepropagePasLexceptionAuDemarrage() {
        // Arrange : la migration reussit, mais l'ecriture en base (_migrations) echoue elle-meme.
        // Ce scenario documente le comportement actuel : le runner logge l'erreur inattendue
        // (branche onFailure du subscribe) sans jamais la propager hors de onStart.
        IMongoMigration migration = migrationSucces("V001", "Init collection");
        setMigrations(migration);

        when(migrationRepository.listerVersionsAppliquees())
                .thenReturn(Uni.createFrom().item(List.of()));
        when(migrationRepository.enregistrerSucces(anyString(), anyString()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Ecriture _migrations indisponible")));
        // Le mapper onFailure().recoverWithUni(...) du runner n'intervient qu'en cas d'échec de migrate() ;
        // ici migrate() réussit et c'est enregistrerSucces qui échoue : ce chemin n'appelle donc jamais
        // enregistrerEchec, mais on le stub par prudence pour ne dépendre d'aucun comportement implicite Mockito.
        when(migrationRepository.enregistrerEchec(anyString(), anyString()))
                .thenReturn(Uni.createFrom().voidItem());

        // Act / Assert : aucune exception ne doit remonter au StartupEvent
        assertDoesNotThrow(() -> runner.onStart(null));
    }

    @Test
    void testPlusieursEchecsSontTousEnregistresIndependamment() {
        // Arrange : deux migrations, toutes deux en echec -> les deux doivent etre enregistrees en ECHEC
        IMongoMigration v001 = migrationEchec("V001", "Premiere", new RuntimeException("boom1"));
        IMongoMigration v002 = migrationEchec("V002", "Deuxieme", new RuntimeException("boom2"));
        setMigrations(v001, v002);

        when(migrationRepository.listerVersionsAppliquees())
                .thenReturn(Uni.createFrom().item(List.of()));
        when(migrationRepository.enregistrerEchec(anyString(), anyString()))
                .thenReturn(Uni.createFrom().voidItem());

        // Act
        assertDoesNotThrow(() -> runner.onStart(null));

        // Assert
        verify(migrationRepository, times(1)).enregistrerEchec(eq("V001"), anyString());
        verify(migrationRepository, times(1)).enregistrerEchec(eq("V002"), anyString());
    }

    // ====== Cas mixte additionnel : combinaison tri + idempotence + echec ======

    @Test
    void testCombinaisonTriIdempotenceEtEchec() {
        // V001 deja appliquee (ignoree), V003 et V002 non appliquees, V003 echoue mais V002 doit s'executer
        // (V002 < V003 dans l'ordre de tri, donc executee avant).
        IMongoMigration v001 = migrationSucces("V001", "Deja appliquee");
        IMongoMigration v002 = migrationSucces("V002", "A executer");
        IMongoMigration v003 = migrationEchec("V003", "En echec", new RuntimeException("boom3"));
        setMigrations(v003, v001, v002);

        when(migrationRepository.listerVersionsAppliquees())
                .thenReturn(Uni.createFrom().item(List.of("V001")));
        when(migrationRepository.enregistrerSucces(anyString(), anyString()))
                .thenReturn(Uni.createFrom().voidItem());
        when(migrationRepository.enregistrerEchec(anyString(), anyString()))
                .thenReturn(Uni.createFrom().voidItem());

        // Act
        assertDoesNotThrow(() -> runner.onStart(null));

        // Assert
        verify(v001, never()).migrate();
        verify(v002, times(1)).migrate();
        verify(v003, times(1)).migrate();
        verify(migrationRepository, never()).enregistrerSucces(eq("V001"), anyString());
        verify(migrationRepository, times(1)).enregistrerSucces(eq("V002"), anyString());
        verify(migrationRepository, times(1)).enregistrerEchec(eq("V003"), anyString());
    }
}
