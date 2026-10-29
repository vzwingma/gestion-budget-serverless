package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.ports.IUtilisateursRepository;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.data.MockDataUtilisateur;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du dernier accès (AP004 T1.1a, F1) de {@link UtilisateursService#getLastAccessDate(String)} : écriture
 * chaînée <em>avant</em> la réponse, échec d'écriture loggé en WARN sans empêcher la réponse, utilisateur inconnu en 404.
 *
 * @author QALvin
 */
class UtilisateursServiceDernierAccesTest {

    private static final Instant INSTANT_FIXE = Instant.parse("2026-07-10T10:15:30Z");

    private IUtilisateursRepository repository;
    private UtilisateursService service;

    @BeforeEach
    void setup() {
        repository = mock(IUtilisateursRepository.class);
        service = new UtilisateursService(repository, Clock.fixed(INSTANT_FIXE, ZoneOffset.UTC));
    }

    private static Throwable erreur(Uni<?> uni) {
        Throwable erreur = assertThrows(Throwable.class, () -> uni.await().indefinitely());
        return erreur instanceof CompletionException && erreur.getCause() != null ? erreur.getCause() : erreur;
    }

    @Test
    void testDernierAcces_valeurLueAvantMiseAJour_ecritureCibleeSurLeLoginDeLUtilisateur() {
        Utilisateur utilisateur = MockDataUtilisateur.getTestUtilisateur();
        LocalDateTime ancienAcces = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        utilisateur.setDernierAcces(ancienAcces);
        when(repository.chargeUtilisateur("Test")).thenReturn(Uni.createFrom().item(utilisateur));
        when(repository.majDernierAcces(anyString(), any())).thenReturn(Uni.createFrom().voidItem());

        LocalDateTime reponse = service.getLastAccessDate("Test").await().indefinitely();

        assertEquals(ancienAcces, reponse, "La réponse porte l'ancienne date (valeur lue avant la mise à jour)");
        verify(repository, times(1)).majDernierAcces("Test", LocalDateTime.ofInstant(INSTANT_FIXE, ZoneOffset.UTC));
    }

    @Test
    void testDernierAcces_ecritureAchevee_avantLEmissionDeLaReponse() {
        Utilisateur utilisateur = MockDataUtilisateur.getTestUtilisateur();
        when(repository.chargeUtilisateur("Test")).thenReturn(Uni.createFrom().item(utilisateur));
        // Écriture différée : le flag n'est positionné qu'à la souscription effective de l'écriture
        AtomicBoolean ecritureTerminee = new AtomicBoolean(false);
        when(repository.majDernierAcces(anyString(), any()))
                .thenReturn(Uni.createFrom().voidItem().invoke(() -> ecritureTerminee.set(true)));
        AtomicBoolean ecritureTermineeALaReponse = new AtomicBoolean(false);

        service.getLastAccessDate("Test").invoke(date -> ecritureTermineeALaReponse.set(ecritureTerminee.get())).await().indefinitely();

        assertTrue(ecritureTermineeALaReponse.get(), "L'écriture est chaînée : elle est terminée avant la réponse (plus de fire-and-forget)");
    }

    @Test
    void testDernierAcces_echecEcriture_reponseServieAvecAncienneDate_etWarn() {
        Utilisateur utilisateur = MockDataUtilisateur.getTestUtilisateur();
        LocalDateTime ancienAcces = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        utilisateur.setDernierAcces(ancienAcces);
        when(repository.chargeUtilisateur("Test")).thenReturn(Uni.createFrom().item(utilisateur));
        when(repository.majDernierAcces(anyString(), any())).thenReturn(Uni.createFrom().failure(new IllegalStateException("mongo KO")));

        final List<LogRecord> logs = new ArrayList<>();
        final Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logs.add(record);
            }

            @Override
            public void flush() {
                // rien à vider
            }

            @Override
            public void close() {
                // rien à fermer
            }
        };
        final java.util.logging.Logger loggerService = java.util.logging.Logger.getLogger(UtilisateursService.class.getName());
        loggerService.addHandler(capture);
        LocalDateTime reponse;
        try {
            reponse = service.getLastAccessDate("Test").await().indefinitely();
        } finally {
            loggerService.removeHandler(capture);
        }

        assertEquals(ancienAcces, reponse, "L'échec d'écriture n'empêche pas la réponse (200)");
        LogRecord warn = logs.stream()
                .filter(l -> l.getLevel().intValue() == Level.WARNING.intValue() && l.getMessage() != null
                        && l.getMessage().contains("Impossible de mettre à jour le dernier accès"))
                .findFirst().orElseThrow(() -> new AssertionError("WARN attendu, logs : " + logs.stream().map(LogRecord::getMessage).toList()));
        String parametres = Arrays.toString(warn.getParameters());
        assertTrue(parametres.contains("Test") && parametres.contains("mongo KO"), parametres);
    }

    @Test
    void testDernierAcces_utilisateurInconnu_itemNull_404_sansEcriture() {
        when(repository.chargeUtilisateur("Inconnu")).thenReturn(Uni.createFrom().nullItem());

        Throwable erreur = erreur(service.getLastAccessDate("Inconnu"));

        assertInstanceOf(DataNotFoundException.class, erreur);
        verify(repository, never()).majDernierAcces(anyString(), any());
    }

    @Test
    void testDernierAcces_erreurDeLectureUtilisateur_propagee_sansEcriture() {
        when(repository.chargeUtilisateur("Test")).thenReturn(Uni.createFrom().failure(new DataNotFoundException("lecture KO")));

        assertInstanceOf(DataNotFoundException.class, erreur(service.getLastAccessDate("Test")));
        verify(repository, never()).majDernierAcces(anyString(), any());
    }

    @Test
    void testDernierAcces_utilisateurSansDernierAccesPrecedent_reponseNull() {
        Utilisateur utilisateur = MockDataUtilisateur.getTestUtilisateur();
        utilisateur.setDernierAcces(null);
        when(repository.chargeUtilisateur("Test")).thenReturn(Uni.createFrom().item(utilisateur));
        when(repository.majDernierAcces(anyString(), any())).thenReturn(Uni.createFrom().voidItem());

        assertNull(service.getLastAccessDate("Test").await().indefinitely());
        verify(repository, times(1)).majDernierAcces(anyString(), any());
    }
}
