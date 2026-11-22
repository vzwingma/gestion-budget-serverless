package io.github.vzwingma.finances.budget.serverless.services.operations.business;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IComptesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IParametragesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataBudgets;
import io.github.vzwingma.finances.budget.services.communs.api.APIExceptionsHandler;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.CompteClosedException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DonneeCorrompueException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.ServiceDistantIndisponibleException;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AP004 T2.5 : {@code getBudgetAndCompteActif} lit le compte (déduit de l'id du budget) et le budget en parallèle ;
 * garde de cohérence (500), 404 du budget prioritaire, compte clos inchangé (406). Exercé via {@code reinitialiserBudgetMensuel}.
 */
class BudgetServiceLectureParalleleTest {

    private BudgetService budgetService;
    private IOperationsRepository repository;
    private IComptesServiceProvider comptes;
    private BudgetMensuel budget;

    @BeforeEach
    void setup() {
        repository = mock(IOperationsRepository.class);
        comptes = mock(IComptesServiceProvider.class);
        budgetService = new BudgetService();
        budgetService.setDataOperationsProvider(repository);
        budgetService.setComptesService(comptes);
        budgetService.setParametragesService(mock(IParametragesServiceProvider.class));
        budgetService.setClock(Clock.fixed(Instant.parse("2026-07-10T10:15:30Z"), ZoneOffset.UTC));
        budget = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
    }

    private Throwable erreur(Uni<?> uni) {
        Throwable e = assertThrows(Throwable.class, () -> uni.await().indefinitely());
        return e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
    }

    @Test
    void compteLuSansAttendreLeBudget_parallele() {
        // Le budget n'émet jamais : si le compte n'était lu qu'après le budget, getCompteById ne serait jamais appelé
        when(repository.chargeBudgetMensuel(budget.getId())).thenReturn(Uni.createFrom().nothing());
        when(comptes.getCompteById("C1")).thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        budgetService.reinitialiserBudgetMensuel(budget.getId());

        verify(comptes).getCompteById("C1");
        verify(repository).chargeBudgetMensuel(budget.getId());
    }

    @Test
    void budgetDuCompteDifferent_donneeCorrompue500() {
        BudgetMensuel incoherent = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        incoherent.setIdCompteBancaire("C2");
        when(repository.chargeBudgetMensuel(budget.getId())).thenReturn(Uni.createFrom().item(incoherent));
        when(comptes.getCompteById("C1")).thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        Throwable erreur = erreur(budgetService.reinitialiserBudgetMensuel(budget.getId()));

        assertInstanceOf(DonneeCorrompueException.class, erreur);
        assertEquals(500, new APIExceptionsHandler().toResponse((DonneeCorrompueException) erreur).getStatus());
        verify(repository, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    @Test
    void budget404_prioritaireSurUneErreurDuCompteQuiArriveEnPremier() {
        BudgetNotFoundException introuvable = new BudgetNotFoundException("budget introuvable");
        // le compte échoue immédiatement, le budget échoue plus tard : le 404 du budget doit rester l'erreur renvoyée
        when(comptes.getCompteById("C1")).thenReturn(Uni.createFrom().failure(new ServiceDistantIndisponibleException("aval en panne")));
        when(repository.chargeBudgetMensuel(budget.getId())).thenReturn(
                Uni.createFrom().<BudgetMensuel>failure(introuvable).onItem().delayIt().by(Duration.ofMillis(100)));

        assertSame(introuvable, erreur(budgetService.reinitialiserBudgetMensuel(budget.getId())));
    }

    @Test
    void budgetTrouve_erreurDuCompte_propagee() {
        ServiceDistantIndisponibleException panne = new ServiceDistantIndisponibleException("aval en panne");
        when(repository.chargeBudgetMensuel(budget.getId())).thenReturn(Uni.createFrom().item(budget));
        when(comptes.getCompteById("C1")).thenReturn(Uni.createFrom().failure(panne));

        assertSame(panne, erreur(budgetService.reinitialiserBudgetMensuel(budget.getId())));
    }

    @Test
    void compteClos_406_inchange() {
        when(repository.chargeBudgetMensuel(budget.getId())).thenReturn(Uni.createFrom().item(budget));
        when(comptes.getCompteById("C1")).thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteInactif()));

        Throwable erreur = erreur(budgetService.reinitialiserBudgetMensuel(budget.getId()));

        assertInstanceOf(CompteClosedException.class, erreur);
        assertEquals(406, new APIExceptionsHandler().toResponse((CompteClosedException) erreur).getStatus());
    }
}
