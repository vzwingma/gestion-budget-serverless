package io.github.vzwingma.finances.budget.serverless.services.operations.business;

import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.BudgetMensuel;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.budget.ProjectionBudgetSoldes;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.LigneOperation;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.model.operation.OperationEtatEnum;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.business.ports.IOperationsRepository;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IComptesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.IParametragesServiceProvider;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataBudgets;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.data.MockDataOperations;
import io.github.vzwingma.finances.budget.serverless.services.operations.utils.BudgetDataUtils;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.data.model.SsCategorieOperations;
import io.github.vzwingma.finances.budget.serverless.services.operations.api.override.APIExceptionsHandler;
import io.github.vzwingma.finances.budget.serverless.services.operations.spi.SPIExceptionsHandler;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DonneeCorrompueException;
import jakarta.ws.rs.core.Response;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BadParametersException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BudgetNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.CompteClosedException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import com.mongodb.MongoTimeoutException;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@QuarkusTest
class BudgetServiceTest {

    private IOperationsAppProvider operationsAppProvider;

    private BudgetService budgetAppProvider;
    private IOperationsRepository mockOperationDataProvider;
    private IComptesServiceProvider mockCompteServiceProvider;
    private IParametragesServiceProvider mockParametragesServiceProvider;

    /**
     * Horloge fixe (ADR-004) pour rendre déterministes les assertions sur les dates de mise à jour de budget.
     */
    private static final Instant INSTANT_FIXE = Instant.parse("2026-07-10T10:15:30Z");
    private final Clock clockFixe = Clock.fixed(INSTANT_FIXE, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        mockOperationDataProvider = mock(IOperationsRepository.class);
        mockCompteServiceProvider = mock(IComptesServiceProvider.class);
        mockParametragesServiceProvider = mock(IParametragesServiceProvider.class);

        OperationsService operationsService = spy(new OperationsService());
        operationsService.setDataOperationsProvider(mockOperationDataProvider);
        operationsService.setClock(clockFixe);
        operationsAppProvider = operationsService;

        budgetAppProvider = spy(new BudgetService());
        budgetAppProvider.setDataOperationsProvider(mockOperationDataProvider);
        budgetAppProvider.setComptesService(mockCompteServiceProvider);
        budgetAppProvider.setOperationsAppProvider(operationsAppProvider);
        budgetAppProvider.setParametragesService(mockParametragesServiceProvider);
        budgetAppProvider.setClock(clockFixe);

    }

    /**
     * Test d'un chargement de budget sans compte
     */
    @Test
    void testGetBudgetWithNoCompte() {

        // Initialisation
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Compte introuvable")));

        // Test
        CompletionException thrown = assertThrows(CompletionException.class, () -> budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2020)
                .await().indefinitely());
        assertEquals("io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException", thrown.getMessage());
    }






    /**
     * Test du chargement nominal d'un budget actif sur compte actif
     */
    @Test
    void testGetBudgetInactifSurCompteActif() {

        // Initialisation
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.JANUARY), eq(2022)))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetInactifCompteC1()));
        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class))).thenReturn(Uni.createFrom().item(new BudgetMensuel()));
        // Test
        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2022)
                .await().indefinitely();

        // Assertion
        assertNotNull(budgetCharge);
        assertEquals("C1_2022_1", budgetCharge.getId());
        verify(budgetAppProvider, times(1)).getBudgetMensuel(eq("C1"), any(Month.class), anyInt());
    }


    /**
     * Test du chargement nominal d'un budget actif sur compte actif
     */
    @Test
    void testGetBudgetActifSurCompteActif() {

        // Initialisation
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        BudgetMensuel b1 = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        BudgetMensuel b0 = MockDataBudgets.getBudgetPrecedentCompteC1();

        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.JANUARY), eq(2022)))
                .thenReturn(Uni.createFrom().item(b1));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.DECEMBER), eq(2021)))
                .thenReturn(Uni.createFrom().item(b0));
        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class))).thenReturn(Uni.createFrom().item(new BudgetMensuel()));

        // Test
        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2022)
                .await().indefinitely();

        // Assertion
        assertNotNull(budgetCharge);
        assertEquals("C1_2022_01", budgetCharge.getId());
        // bien le recacul du solde de fin de budget précédent
        assertEquals(b0.getSoldes().getSoldeAtFinMoisCourant(), budgetCharge.getSoldes().getSoldeAtFinMoisPrecedent());
        // bien le recalcul global des soldes
        assertEquals(1000D, budgetCharge.getSoldes().getSoldeAtMaintenant());
        assertEquals(1123D, budgetCharge.getSoldes().getSoldeAtFinMoisCourant());

        verify(mockOperationDataProvider, times(2)).chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt());
        verify(mockOperationDataProvider, times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    @SuppressWarnings("unchecked")
@Test
    void testInitNewBudgetOnCompteActif() {

        // Initialisation
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        BudgetMensuel bcree = MockDataBudgets.getBudgetPrecedentCompteC1();
        bcree.setId("C1_2022_04");
        bcree.setAnnee(2022);
        bcree.setMois(Month.APRIL);
        bcree.setActif(true);
        bcree.setNewBudget(true);
        bcree.getListeOperations().clear();
        BudgetDataUtils.razCalculs(bcree);

        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().failure(new BudgetNotFoundException("Budget introuvable")));
        // F4 : le dernier budget <= M-1 (avril) est le mois précédent exact
        when(mockOperationDataProvider.chargeDernierBudgetAvant(eq("C1"), eq(Month.APRIL), eq(2022)))
                .thenReturn(Uni.createFrom().item(bcree));
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(bcree));


        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class))).thenReturn(Uni.createFrom().item(bcree));
        // Test
        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.MAY, 2022)
                .await().indefinitely();

        // Assertion
        assertNotNull(budgetCharge);
        assertEquals("C1_2022_05", budgetCharge.getId());
        verify(budgetAppProvider, times(1)).getBudgetMensuel("C1", Month.MAY, 2022);
        verify(mockOperationDataProvider, times(1)).chargeDernierBudgetAvant("C1", Month.APRIL, 2022);
        verify(mockOperationDataProvider, times(2)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }


    /**
     * F3 : compte inactif, budget du mois demandé absent : 200 avec le dernier budget &le; M (forcé inactif), en un seul appel dépôt
     */
    @Test
    void testGetBudgetSurCompteInactif_budgetDuMoisAbsent_retourneLeDernierBudget() {

        // Initialisation : le dernier budget <= janvier 2022 est celui de décembre 2021
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteInactif()));
        BudgetMensuel decembre = MockDataBudgets.getBudgetPrecedentCompteC1();
        decembre.setActif(true);
        when(mockOperationDataProvider.chargeDernierBudgetAvant(anyString(), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().item(decembre));
        // Test
        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2022)
                .await().indefinitely();

        // Assertion
        assertNotNull(budgetCharge);
        assertEquals("C1_2021_12", budgetCharge.getId());
        assertFalse(budgetCharge.isActif());
        verify(mockOperationDataProvider, times(1)).chargeDernierBudgetAvant("C2", Month.JANUARY, 2022);
        verify(mockOperationDataProvider, never()).chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt());
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    /**
     * F3 : compte inactif sans aucun budget : BudgetNotFoundException (404)
     */
    @Test
    void testGetBudgetSurCompteInactif_aucunBudget_budgetNotFound() {

        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteInactif()));
        when(mockOperationDataProvider.chargeDernierBudgetAvant(anyString(), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().nullItem());

        CompletionException exception = assertThrows(CompletionException.class,
                () -> budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2022).await().indefinitely());
        assertEquals(BudgetNotFoundException.class, exception.getCause().getClass());
        verify(mockOperationDataProvider, times(1)).chargeDernierBudgetAvant("C2", Month.JANUARY, 2022);
    }

    /**
     * F4 : compte actif sans aucun budget : budget vierge, sans récursion (un seul appel "dernier budget"), solde précédent à 0
     */
    @Test
    void testGetBudgetSurCompteActif_aucunBudget_budgetVierge() {

        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().failure(new BudgetNotFoundException("Budget introuvable")));
        when(mockOperationDataProvider.chargeDernierBudgetAvant(anyString(), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().nullItem());
        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class)))
                .thenAnswer(inv -> Uni.createFrom().item(inv.getArgument(0, BudgetMensuel.class)));

        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.MARCH, 2022)
                .await().indefinitely();

        assertNotNull(budgetCharge);
        assertEquals("C1_2022_03", budgetCharge.getId());
        assertTrue(budgetCharge.isActif());
        assertEquals(0D, budgetCharge.getSoldes().getSoldeAtFinMoisPrecedent());
        assertTrue(budgetCharge.getListeOperations().isEmpty());
        // Appels dépôt : lecture du mois, dernier budget <= M-1, sauvegarde. Pas de récursion.
        verify(mockOperationDataProvider, times(1)).chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt());
        verify(mockOperationDataProvider, times(1)).chargeDernierBudgetAvant("C1", Month.FEBRUARY, 2022);
        verify(mockOperationDataProvider, times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
        verify(mockOperationDataProvider, never()).chargeBudgetMensuel(anyString());
    }

    /**
     * F4 : budget existant du premier mois du compte (aucun budget précédent) : pas d'échec, solde précédent conservé à 0
     */
    @Test
    void testGetBudgetSurCompteActif_premierMois_pasDeBudgetPrecedent_sansEchec() {

        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        BudgetMensuel janvier = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        janvier.getSoldes().setSoldeAtFinMoisPrecedent(0D);
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.JANUARY), eq(2022)))
                .thenReturn(Uni.createFrom().item(janvier));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.DECEMBER), eq(2021)))
                .thenReturn(Uni.createFrom().failure(new BudgetNotFoundException("Budget introuvable")));
        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class)))
                .thenAnswer(inv -> Uni.createFrom().item(inv.getArgument(0, BudgetMensuel.class)));

        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2022)
                .await().indefinitely();

        assertNotNull(budgetCharge);
        assertEquals(0D, budgetCharge.getSoldes().getSoldeAtFinMoisPrecedent());
        verify(mockOperationDataProvider, never()).chargeDernierBudgetAvant(anyString(), any(Month.class), anyInt());
        verify(mockOperationDataProvider, times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    /**
     * F4 : écart de 3 mois (février, mars, avril à créer entre janvier et mai).
     * Appels au dépôt : 1 lecture du mois M, 1 "dernier budget &le; M-1", 1 relecture par id du dernier budget avant sa clôture,
     * écritures : 1 clôture de janvier + 3 mois créés (déjà clos) + 1 sauvegarde de M = 5.
     */
    @Test
    void testGetBudgetSurCompteActif_ecartDe3Mois_creeLesMoisIntermediaires() {

        Map<String, BudgetMensuel> base = initBaseBudgetsFictive(budgetC1(Month.JANUARY, 2022, true));

        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.MAY, 2022)
                .await().indefinitely();

        assertEquals("C1_2022_05", budgetCharge.getId());
        // Janvier + février, mars, avril, mai
        assertEquals(5, base.size());
        assertTrue(base.containsKey("C1_2022_02") && base.containsKey("C1_2022_03") && base.containsKey("C1_2022_04"));
        assertFalse(base.get("C1_2022_01").isActif());
        assertFalse(base.get("C1_2022_04").isActif());
        assertTrue(base.get("C1_2022_05").isActif());
        verify(mockOperationDataProvider, times(1)).chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt());
        verify(mockOperationDataProvider, times(1)).chargeDernierBudgetAvant(eq("C1"), eq(Month.APRIL), eq(2022));
        // Une relecture par id : le dernier budget (janvier) est relu juste avant sa clôture
        verify(mockOperationDataProvider, times(1)).chargeBudgetMensuel(eq("C1_2022_01"));
        verify(mockOperationDataProvider, times(1)).chargeBudgetMensuel(anyString());
        verify(mockOperationDataProvider, times(5)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    /**
     * F4 : l'écart de 24 mois est la limite incluse : tous les mois intermédiaires sont créés, avec un nombre de lectures
     * constant (identique à l'écart de 3 mois) et 1 écriture par mois créé.
     */
    @Test
    void testGetBudgetSurCompteActif_ecartDe24Mois_creeLesMoisIntermediaires() {

        Map<String, BudgetMensuel> base = initBaseBudgetsFictive(budgetC1(Month.JANUARY, 2020, true));

        // M-1 = janvier 2022 : écart de 24 mois avec janvier 2020
        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.FEBRUARY, 2022)
                .await().indefinitely();

        assertEquals("C1_2022_02", budgetCharge.getId());
        assertEquals(1 + 24 + 1, base.size());
        assertTrue(base.containsKey("C1_2022_01"));
        assertFalse(base.get("C1_2022_01").isActif());
        verify(mockOperationDataProvider, times(1)).chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt());
        verify(mockOperationDataProvider, times(1)).chargeDernierBudgetAvant(eq("C1"), eq(Month.JANUARY), eq(2022));
        verify(mockOperationDataProvider, times(1)).chargeBudgetMensuel(eq("C1_2020_01"));
        verify(mockOperationDataProvider, times(1)).chargeBudgetMensuel(anyString());
        // 1 clôture de janvier 2020 + 24 mois créés + 1 sauvegarde de M
        verify(mockOperationDataProvider, times(26)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    /**
     * F4 : écart &gt; 24 mois : aucun mois intermédiaire créé, le solde du dernier budget connu est repris (pas 0)
     */
    @Test
    void testGetBudgetSurCompteActif_ecartDe25Mois_soldeRepris() {

        BudgetMensuel dernier = budgetC1(Month.JANUARY, 2020, true);
        dernier.getSoldes().setSoldeAtFinMoisPrecedent(500D);
        Map<String, BudgetMensuel> base = initBaseBudgetsFictive(dernier);

        // M-1 = février 2022 : écart de 25 mois avec janvier 2020
        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.MARCH, 2022)
                .await().indefinitely();

        assertEquals("C1_2022_03", budgetCharge.getId());
        assertEquals(500D, budgetCharge.getSoldes().getSoldeAtFinMoisPrecedent(), "Le solde du dernier budget connu est repris");
        assertEquals(2, base.size());
        assertTrue(base.get("C1_2020_01").isActif(), "Le budget de janvier 2020 n'est pas modifié ni clos");
        verify(mockOperationDataProvider, times(1)).chargeDernierBudgetAvant("C1", Month.FEBRUARY, 2022);
        verify(mockOperationDataProvider, times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    /**
     * Erreur technique sur la lecture du mois M : elle est propagée (type conservé), jamais d'initialisation ni de sauvegarde d'un budget vierge
     */
    @Test
    void testGetBudgetSurCompteActif_erreurTechniqueSurM_propageeSansSauvegarde() {

        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().failure(new MongoTimeoutException("timeout")));

        assertThrows(MongoTimeoutException.class, () -> budgetAppProvider.getBudgetMensuel("C1", Month.MARCH, 2022).await().indefinitely());

        verify(mockOperationDataProvider, never()).chargeDernierBudgetAvant(anyString(), any(Month.class), anyInt());
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    /**
     * Erreur technique sur "dernier budget &le; M-1" lors de l'initialisation : propagée, aucune sauvegarde
     */
    @Test
    void testGetBudgetSurCompteActif_erreurTechniqueSurDernierBudget_propageeSansSauvegarde() {

        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().failure(new BudgetNotFoundException("Budget introuvable")));
        when(mockOperationDataProvider.chargeDernierBudgetAvant(anyString(), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().failure(new MongoTimeoutException("timeout")));

        assertThrows(MongoTimeoutException.class, () -> budgetAppProvider.getBudgetMensuel("C1", Month.MARCH, 2022).await().indefinitely());

        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    /**
     * Erreur technique sur la lecture de M-1 à l'affichage d'un budget M existant : elle ne fait pas échouer le chargement de M.
     * M est servi avec le solde stocké du mois précédent, n'est pas enregistré, et l'erreur est loggée en ERROR.
     */
    @Test
    void testGetBudgetSurCompteActif_erreurTechniqueSurMoisPrecedent_budgetServiSansSauvegarde() {

        BudgetMensuel janvier = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        janvier.getSoldes().setSoldeAtFinMoisPrecedent(777D);
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.JANUARY), eq(2022)))
                .thenReturn(Uni.createFrom().item(janvier));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.DECEMBER), eq(2021)))
                .thenReturn(Uni.createFrom().failure(new MongoTimeoutException("timeout")));

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
        final java.util.logging.Logger loggerService = java.util.logging.Logger.getLogger(BudgetService.class.getName());
        loggerService.addHandler(capture);
        BudgetMensuel budgetCharge;
        try {
            budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2022).await().indefinitely();
        } finally {
            loggerService.removeHandler(capture);
        }

        assertNotNull(budgetCharge);
        assertEquals("C1_2022_01", budgetCharge.getId());
        assertEquals(777D, budgetCharge.getSoldes().getSoldeAtFinMoisPrecedent(), "Le solde stocké du mois précédent est conservé");
        // Les soldes du mois restent recalculés en mémoire
        assertEquals(777D + 123D, budgetCharge.getSoldes().getSoldeAtFinMoisCourant());
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
        assertTrue(logs.stream().anyMatch(l -> l.getLevel().intValue() >= Level.SEVERE.intValue()
                        && l.getMessage() != null && l.getMessage().contains("Erreur technique lors du chargement du budget précédent")),
                "Une trace ERROR est attendue pour l'erreur sur M-1");
    }

    /**
     * Mois absent sur le budget M existant (recalcul du solde de fin de mois précédent) : donnée corrompue, 500 typé, aucune lecture de M-1 ni sauvegarde
     */
    @Test
    void testGetBudgetSurCompteActif_budgetSansMois_donneeCorrompue500() {

        BudgetMensuel sansMois = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        sansMois.setMois(null);
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.JANUARY), eq(2022)))
                .thenReturn(Uni.createFrom().item(sansMois));

        CompletionException exception = assertThrows(CompletionException.class,
                () -> budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2022).await().indefinitely());
        assertDonneeCorrompue500(exception.getCause());
        verify(mockOperationDataProvider, times(1)).chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt());
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    /**
     * Dernier budget connu sans mois (calcul de l'écart) : donnée corrompue, 500 typé, aucune sauvegarde
     */
    @Test
    void testGetBudgetSurCompteActif_dernierBudgetSansMois_donneeCorrompue500() {

        BudgetMensuel sansMois = budgetC1(Month.JANUARY, 2022, true);
        sansMois.setMois(null);
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().failure(new BudgetNotFoundException("Budget introuvable")));
        when(mockOperationDataProvider.chargeDernierBudgetAvant(anyString(), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().item(sansMois));

        CompletionException exception = assertThrows(CompletionException.class,
                () -> budgetAppProvider.getBudgetMensuel("C1", Month.MARCH, 2022).await().indefinitely());
        assertDonneeCorrompue500(exception.getCause());
        assertTrue(((DonneeCorrompueException) exception.getCause()).getLibelle().contains("n'a pas de mois"));
        verify(mockOperationDataProvider, never()).chargeBudgetMensuel(anyString());
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    /**
     * Clôture du dernier budget : la version relue (active) n'a pas de mois : donnée corrompue, 500 typé, la version relue n'est pas enregistrée
     */
    @Test
    void testGetBudgetSurCompteActif_clotureBudgetReluSansMois_donneeCorrompue500() {

        initBaseBudgetsFictive(budgetC1(Month.JANUARY, 2022, true));
        BudgetMensuel reluSansMois = budgetC1(Month.JANUARY, 2022, true);
        reluSansMois.setMois(null);
        when(mockOperationDataProvider.chargeBudgetMensuel(eq("C1_2022_01"))).thenReturn(Uni.createFrom().item(reluSansMois));

        CompletionException exception = assertThrows(CompletionException.class,
                () -> budgetAppProvider.getBudgetMensuel("C1", Month.FEBRUARY, 2022).await().indefinitely());
        assertDonneeCorrompue500(exception.getCause());
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    /**
     * Création des mois intermédiaires : la version relue du dernier budget, déjà close, n'a pas de mois : donnée corrompue, 500 typé
     */
    @Test
    void testGetBudgetSurCompteActif_creationMoisIntermediairesSurBudgetReluSansMois_donneeCorrompue500() {

        initBaseBudgetsFictive(budgetC1(Month.JANUARY, 2022, true));
        BudgetMensuel recluSansMois = budgetC1(Month.JANUARY, 2022, false);
        recluSansMois.setMois(null);
        when(mockOperationDataProvider.chargeBudgetMensuel(eq("C1_2022_01"))).thenReturn(Uni.createFrom().item(recluSansMois));

        // M-1 = février 2022 : écart de 1 mois avec janvier 2022, un mois intermédiaire à créer
        CompletionException exception = assertThrows(CompletionException.class,
                () -> budgetAppProvider.getBudgetMensuel("C1", Month.MARCH, 2022).await().indefinitely());
        assertDonneeCorrompue500(exception.getCause());
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    /**
     * Le dernier budget est relu par id juste avant sa clôture : une opération ajoutée entre-temps est conservée
     * (reportée), et c'est la version relue qui est enregistrée, pas l'instantané lu au début.
     */
    @Test
    void testGetBudgetSurCompteActif_clotureRelitLeMoisPrecedent_operationAjouteeEntreTempsConservee() {

        BudgetMensuel instantane = budgetC1(Month.JANUARY, 2022, true);
        Map<String, BudgetMensuel> base = initBaseBudgetsFictive(instantane);
        // Version en base au moment de la clôture : une opération prévue a été ajoutée depuis la lecture de l'instantané
        BudgetMensuel relu = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        assertTrue(instantane.getListeOperations().isEmpty());
        when(mockOperationDataProvider.chargeBudgetMensuel(eq("C1_2022_01"))).thenReturn(Uni.createFrom().item(relu));

        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.FEBRUARY, 2022)
                .await().indefinitely();

        assertEquals("C1_2022_02", budgetCharge.getId());
        // La version relue est celle enregistrée, close, avec l'opération PREVUE -> REPORTEE
        assertSame(relu, base.get("C1_2022_01"));
        assertFalse(relu.isActif());
        assertEquals(1, relu.getListeOperations().size());
        assertEquals(OperationEtatEnum.REPORTEE, relu.getListeOperations().getFirst().getEtat());
        assertTrue(instantane.getListeOperations().isEmpty(), "L'instantané initial n'est pas enregistré");
        // L'opération reportée est bien reprise dans le nouveau budget
        assertEquals(1, budgetCharge.getListeOperations().size());
        verify(mockOperationDataProvider, times(1)).chargeBudgetMensuel(eq("C1_2022_01"));
    }

    /**
     * Le dernier budget a été clos entre-temps par un autre appel : la version relue n'est pas réécrite
     */
    @Test
    void testGetBudgetSurCompteActif_clotureReluDejaClos_pasDeReecriture() {

        initBaseBudgetsFictive(budgetC1(Month.JANUARY, 2022, true));
        BudgetMensuel reluClos = budgetC1(Month.JANUARY, 2022, false);
        when(mockOperationDataProvider.chargeBudgetMensuel(eq("C1_2022_01"))).thenReturn(Uni.createFrom().item(reluClos));

        BudgetMensuel budgetCharge = budgetAppProvider.getBudgetMensuel("C1", Month.FEBRUARY, 2022)
                .await().indefinitely();

        assertEquals("C1_2022_02", budgetCharge.getId());
        // Seule la sauvegarde de M : le budget de janvier (relu clos) n'est pas réécrit
        verify(mockOperationDataProvider, times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(reluClos);
    }

    /**
     * R5 : budget absent par id : BudgetNotFoundException (404)
     */
    @Test
    void testGetBudgetMensuelById_absent_budgetNotFound404() {
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().failure(new BudgetNotFoundException("Budget introuvable")));

        CompletionException exception = assertThrows(CompletionException.class,
                () -> budgetAppProvider.getBudgetMensuel("C1_2022_01").await().indefinitely());
        assertInstanceOf(BudgetNotFoundException.class, exception.getCause());
        assertEquals(404, new APIExceptionsHandler().toResponse((BudgetNotFoundException) exception.getCause()).getStatus());
    }

    /**
     * R5 : erreur Mongo sur la lecture par id : propagée (jamais transformée en 404), 503 pour un timeout
     */
    @Test
    void testGetBudgetMensuelById_erreurMongo_propagee503() {
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().failure(new MongoTimeoutException("timeout")));

        assertThrows(MongoTimeoutException.class, () -> budgetAppProvider.getBudgetMensuel("C1_2022_01").await().indefinitely());
        try (Response response = new SPIExceptionsHandler().toResponse(new MongoTimeoutException("timeout"))) {
            assertEquals(503, response.getStatus());
        }
    }

    /**
     * R5 : budget absent sur l'état actif : BudgetNotFoundException (404)
     */
    @Test
    void testIsBudgetActif_absent_budgetNotFound404() {
        when(mockOperationDataProvider.isBudgetActif(anyString()))
                .thenReturn(Uni.createFrom().failure(new BudgetNotFoundException("Budget introuvable")));

        CompletionException exception = assertThrows(CompletionException.class,
                () -> budgetAppProvider.isBudgetMensuelActif("C1_2022_01").await().indefinitely());
        assertInstanceOf(BudgetNotFoundException.class, exception.getCause());
    }

    /**
     * R5 : erreur Mongo sur l'état actif : propagée (503 pour un timeout)
     */
    @Test
    void testIsBudgetActif_erreurMongo_propagee() {
        when(mockOperationDataProvider.isBudgetActif(anyString()))
                .thenReturn(Uni.createFrom().failure(new MongoTimeoutException("timeout")));

        assertThrows(MongoTimeoutException.class, () -> budgetAppProvider.isBudgetMensuelActif("C1_2022_01").await().indefinitely());
    }

    /**
     * R5 : erreur Mongo à la lecture dans setBudgetActif : propagée, aucune sauvegarde
     */
    @Test
    void testSetBudgetActif_erreurMongo_propageeSansSauvegarde() {
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().failure(new MongoTimeoutException("timeout")));

        assertThrows(MongoTimeoutException.class, () -> budgetAppProvider.setBudgetActif("C1_2022_01", false).await().indefinitely());
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    private static void assertDonneeCorrompue500(Throwable cause) {
        assertInstanceOf(DonneeCorrompueException.class, cause);
        assertEquals(500, new APIExceptionsHandler().toResponse((DonneeCorrompueException) cause).getStatus());
    }

    private static BudgetMensuel budgetC1(Month mois, int annee, boolean actif) {
        BudgetMensuel budget = MockDataBudgets.getBudgetPrecedentCompteC1();
        budget.setMois(mois);
        budget.setAnnee(annee);
        budget.setActif(actif);
        budget.setId(BudgetMensuel.getBudgetId("C1", mois, annee));
        budget.getListeOperations().clear();
        return budget;
    }

    /**
     * Base de budgets fictive en mémoire derrière le dépôt mocké (lecture exacte, par id, "dernier budget &le;" et sauvegarde)
     */
    private Map<String, BudgetMensuel> initBaseBudgetsFictive(BudgetMensuel... initiaux) {
        Map<String, BudgetMensuel> base = new TreeMap<>();
        for (BudgetMensuel b : initiaux) {
            base.put(b.getId(), b);
        }
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt()))
                .thenAnswer(inv -> chargeFictif(base, BudgetMensuel.getBudgetId("C1", inv.getArgument(1), inv.getArgument(2))));
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenAnswer(inv -> chargeFictif(base, inv.getArgument(0, String.class)));
        when(mockOperationDataProvider.chargeDernierBudgetAvant(anyString(), any(Month.class), anyInt()))
                .thenAnswer(inv -> {
                    String borne = BudgetMensuel.getBudgetId(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2));
                    return Uni.createFrom().item(base.entrySet().stream()
                            .filter(e -> e.getKey().compareTo(borne) <= 0)
                            .reduce((premier, second) -> second)
                            .map(Map.Entry::getValue)
                            .orElse(null));
                });
        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class)))
                .thenAnswer(inv -> {
                    BudgetMensuel b = inv.getArgument(0, BudgetMensuel.class);
                    base.put(b.getId(), b);
                    return Uni.createFrom().item(b);
                });
        return base;
    }

    private static Uni<BudgetMensuel> chargeFictif(Map<String, BudgetMensuel> base, String idBudget) {
        BudgetMensuel b = base.get(idBudget);
        return b != null ? Uni.createFrom().item(b) : Uni.createFrom().failure(new BudgetNotFoundException("Budget introuvable " + idBudget));
    }


    /**
     * Test #121
     */
    @Test
    void testCalculBudget() {
        assertNotNull(this.operationsAppProvider);
        BudgetMensuel budget = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        assertNotNull(budget);

        this.budgetAppProvider.recalculSoldes(budget);
        assertEquals(0, budget.getSoldes().getSoldeAtMaintenant().intValue());
        assertEquals(123, budget.getSoldes().getSoldeAtFinMoisCourant().intValue());

        budget.getSoldes().setSoldeAtFinMoisPrecedent(0D);
        this.budgetAppProvider.recalculSoldes(budget);
        assertEquals(0, budget.getSoldes().getSoldeAtMaintenant().intValue());
        assertEquals(123, budget.getSoldes().getSoldeAtFinMoisCourant().intValue());
    }

    @Test
    void testReinitBudget() {
        // When
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(
                        Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt()))
                .thenReturn(
                        Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));

        // Aucun budget antérieur : budget vierge (F4)
        when(mockOperationDataProvider.chargeDernierBudgetAvant(anyString(), any(Month.class), anyInt()))
                .thenReturn(Uni.createFrom().nullItem());
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class))).thenReturn(Uni.createFrom().item(new BudgetMensuel()));
        //Test
        BudgetMensuel budgetReinit = budgetAppProvider.reinitialiserBudgetMensuel(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue().getId()).await().indefinitely();
        // Assertion
        assertNotNull(budgetReinit);
        // Horodatage déterministe (ADR-004, Clock.fixed injecté dans setup())
        assertEquals(LocalDateTime.ofInstant(INSTANT_FIXE, ZoneOffset.UTC), budgetReinit.getDateMiseAJour());
        verify(mockOperationDataProvider, times(1)).sauvegardeBudgetMensuel(budgetReinit);
    }


    @Test
    void testReinitCompteClosed() {
        // When
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(
                        Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), any(Month.class), anyInt()))
                .thenReturn(
                        Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteInactif()));
        //Test

        CompletionException exception = Assertions.assertThrows(CompletionException.class,
                () -> budgetAppProvider.reinitialiserBudgetMensuel(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue().getId()).await().indefinitely());
        assertEquals(CompteClosedException.class, exception.getCause().getClass());
    }


    @Test
    void testReinitCompteUnknown() {
        // When
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(
                        Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));

        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Compte introuvable")));
        //Test

        CompletionException exception = Assertions.assertThrows(CompletionException.class,
                () -> budgetAppProvider.reinitialiserBudgetMensuel(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue().getId()).await().indefinitely());
        assertEquals(DataNotFoundException.class, exception.getCause().getClass());
    }


    @Test
    void testIsBudgetActif() {
        // When
        when(mockOperationDataProvider.isBudgetActif(anyString())).thenReturn(Uni.createFrom().item(true));

        // Test
        assertTrue(budgetAppProvider.isBudgetMensuelActif(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue().getId()).await().indefinitely());
    }


    @Test
    void testSetBudgetInactif() {
        // When
        BudgetMensuel budgetADesactiver = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString())).thenReturn(Uni.createFrom().item(budgetADesactiver));
        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class))).thenReturn(Uni.createFrom().item(budgetADesactiver));
        // Test
        BudgetMensuel budgetDesactive = budgetAppProvider.setBudgetActif(budgetADesactiver.getId(), false).await().indefinitely();

        assertFalse(budgetDesactive.isActif());
        assertEquals(OperationEtatEnum.REPORTEE, budgetDesactive.getListeOperations().getFirst().getEtat());
        // Horodatage déterministe (ADR-004, Clock.fixed injecté dans setup())
        assertEquals(LocalDateTime.ofInstant(INSTANT_FIXE, ZoneOffset.UTC), budgetDesactive.getDateMiseAJour());

        verify(mockOperationDataProvider, times(1)).sauvegardeBudgetMensuel(budgetDesactive);
    }


    @Test
    void testUpdateBudgetCompteClos() {
        // When
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetInactifCompteC1()));

        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteInactif()));

        // Test
        CompletionException exception = Assertions.assertThrows(CompletionException.class,
                () -> budgetAppProvider.addOrUpdateOperationInBudget("idBudget", MockDataOperations.getOperationPrelevement(), "userTest").await().indefinitely());
        assertEquals(CompteClosedException.class, exception.getCause().getClass());

    }

    @Test
    void testUpdateBudget() {
        // When
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));

        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class))).thenReturn(Uni.createFrom().item(new BudgetMensuel()));
        // Test
        LigneOperation ligneOperation = MockDataOperations.getOperationPrelevement();
        BudgetMensuel budgetMensuelAJour = budgetAppProvider.addOrUpdateOperationInBudget("C1_2022_01", ligneOperation, "userTest").await().indefinitely();
        assertEquals(1, budgetMensuelAJour.getListeOperations().size());

        verify(budgetAppProvider, times(1)).recalculSoldes(any(BudgetMensuel.class));
        verify(mockOperationDataProvider, times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));

    }

    /**
     * T E.2 - Vérifie que DataNotFoundException levée par operationsAppProvider.addOrReplaceOperation
     * (ex: catégorie inconnue) est bien propagée comme échec dans la chaîne réactive
     * (et non silencieusement avalée), sans recalcul ni sauvegarde du budget.
     */
    @Test
    void testUpdateBudgetOperationAddOrReplaceDataNotFound() {
        // When
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));

        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        IOperationsAppProvider mockOperationsAppProvider = mock(IOperationsAppProvider.class);
        Assertions.assertDoesNotThrow(() ->
            doThrow(new DataNotFoundException("Catégorie introuvable"))
                    .when(mockOperationsAppProvider)
                    .addOrReplaceOperation(anyList(), any(LigneOperation.class), anyString(), isNull()));
        budgetAppProvider.setOperationsAppProvider(mockOperationsAppProvider);

        // Test
        LigneOperation ligneOperation = MockDataOperations.getOperationPrelevement();
        CompletionException exception = Assertions.assertThrows(CompletionException.class,
                () -> budgetAppProvider.addOrUpdateOperationInBudget("C1_2022_01", ligneOperation, "userTest").await().indefinitely());
        assertEquals(DataNotFoundException.class, exception.getCause().getClass());

        verify(budgetAppProvider, never()).recalculSoldes(any(BudgetMensuel.class));
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    @Test
    void testUpdateBudgetRemboursable() {
        // Préparation : budget actif avec 1 opération existante
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class)))
                .thenReturn(Uni.createFrom().item(new BudgetMensuel()));

        // Mock de la sous-catégorie remboursement retournée par le service de parametrages
        SsCategorieOperations ssCatRemboursement = new SsCategorieOperations("remboursement-id");
        ssCatRemboursement.setLibelle("Remboursement");
        when(mockParametragesServiceProvider.getSsCategorieParId(anyString()))
                .thenReturn(Uni.createFrom().item(ssCatRemboursement));

        // Test : ajout d'une nouvelle opération remboursable
        LigneOperation ligneRemboursable = MockDataOperations.getOperationRemboursement();
        BudgetMensuel budgetMensuelAJour = budgetAppProvider.addOrUpdateOperationInBudget("C1_2022_01", ligneRemboursable, "userTest").await().indefinitely();

        // L'opération remboursable + son remboursement automatique = 2 nouvelles opérations (+ 1 existante = 3)
        assertEquals(3, budgetMensuelAJour.getListeOperations().size());
        verify(mockParametragesServiceProvider, times(1)).getSsCategorieParId(anyString());
        verify(budgetAppProvider, times(1)).recalculSoldes(any(BudgetMensuel.class));
        verify(mockOperationDataProvider, times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    @Test
    void testUpdateBudgetRemboursableSansSsCategorie() {
        // Préparation : budget actif avec 1 opération existante
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));

        // Le service de parametrages est indisponible pour la catégorie de remboursement
        when(mockParametragesServiceProvider.getSsCategorieParId(anyString()))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Catégorie introuvable")));

        // Test : l'ajout d'une opération remboursable doit échouer avec DataNotFoundException
        LigneOperation ligneRemboursable = MockDataOperations.getOperationRemboursement();
        CompletionException exception = Assertions.assertThrows(CompletionException.class,
                () -> budgetAppProvider.addOrUpdateOperationInBudget("C1_2022_01", ligneRemboursable, "userTest").await().indefinitely());
        assertEquals(DataNotFoundException.class, exception.getCause().getClass());

        verify(mockParametragesServiceProvider, times(1)).getSsCategorieParId(anyString());
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    @Test
    void testDeleteOperationInBudget() {
        // When
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));

        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class))).thenReturn(Uni.createFrom().item(new BudgetMensuel()));
        // Test
        BudgetMensuel budgetMensuelAJour = budgetAppProvider.deleteOperationInBudget("C1_2022_01", "TEST1").await().indefinitely();
        assertEquals(0, budgetMensuelAJour.getListeOperations().size());

        verify(budgetAppProvider, times(1)).recalculSoldes(any(BudgetMensuel.class));
        verify(mockOperationDataProvider, times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));

    }


    @Test
    void testDeleteOperationUnkownInBudget() {
        // When
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));

        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class))).thenReturn(Uni.createFrom().item(new BudgetMensuel()));
        // Test
        BudgetMensuel budgetMensuelAJour = budgetAppProvider.deleteOperationInBudget("C1_2022_01", "noId").await().indefinitely();
        assertEquals(1, budgetMensuelAJour.getListeOperations().size());

        verify(budgetAppProvider, times(1)).recalculSoldes(any(BudgetMensuel.class));
        verify(mockOperationDataProvider, times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    @Test
    void testGetBudgetMensuelById() {
        when(mockOperationDataProvider.chargeBudgetMensuel(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));

        BudgetMensuel budget = budgetAppProvider.getBudgetMensuel("C1_2022_01").await().indefinitely();
        assertNotNull(budget);
        assertEquals("C1_2022_01", budget.getId());
    }

    @Test
    void testGetSoldesBudgetMensuel() {
        ProjectionBudgetSoldes projection = new ProjectionBudgetSoldes();
        projection.setIdCompteBancaire("C1");
        projection.setAnnee(2022);
        projection.setMois(Month.JANUARY);
        projection.getSoldes().setSoldeAtFinMoisCourant(123.45D);
        when(mockOperationDataProvider.chargeSoldesBudgetMensuel(anyString(), any(), any()))
                .thenReturn(Multi.createFrom().item(projection));

        List<ProjectionBudgetSoldes> result = budgetAppProvider
                .getSoldesBudgetMensuel("C1", Month.JANUARY, 2022)
                .collect().asList().await().indefinitely();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("C1", result.getFirst().getIdCompteBancaire());
        assertEquals(2022, result.getFirst().getAnnee());
        assertEquals(Month.JANUARY, result.getFirst().getMois());
    }

    @Test
    void testGetiIntervalleBudgets() {
        Instant[] intervalle = {Instant.now().minusSeconds(3600), Instant.now()};
        when(mockOperationDataProvider.chargeIntervalleBudgets(anyString()))
                .thenReturn(Uni.createFrom().item(intervalle));

        Instant[] result = budgetAppProvider.getiIntervalleBudgets("C1").await().indefinitely();
        assertNotNull(result);
        assertEquals(2, result.length);
    }

    @Test
    void testGetSigningKeyReadRepository() {
        assertNull(budgetAppProvider.getSigningKeyReadRepository());
    }

    @Test
    void testVirementInterneEtatNull_refuse400AvantToutAcces() {
        LigneOperation operation = new LigneOperation();
        operation.setLibelle("Virement sans état");
        LigneOperation.SsCategorie ssCategorie = new LigneOperation.SsCategorie();
        ssCategorie.setId(io.github.vzwingma.finances.budget.serverless.services.operations.business.model.IdsCategoriesEnum.SS_CAT_VIREMENT_INTERNE.getId());
        operation.setSsCategorie(ssCategorie);
        operation.setEtat(null);

        Uni<BudgetMensuel> resultat = budgetAppProvider.createOperationsVirementInterne("C1_2022_1", operation, "C2", "userTest");

        CompletionException erreur = assertThrows(CompletionException.class, () -> resultat.await().indefinitely());
        assertInstanceOf(BadParametersException.class, erreur.getCause());
        verify(mockCompteServiceProvider, never()).getCompteById(anyString());
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
        verify(mockOperationDataProvider, never()).compenseOperation(any(), any(), any(), anyInt());
    }

    /**
     * Virement interne : erreur technique sur M-1 du compte destination. Le mode dégradé est réservé à l'affichage :
     * l'écriture échoue (503 via SPIExceptionsHandler), la source est compensée, le budget M de la destination n'est pas enregistré.
     */
    @Test
    void testVirementInterne_erreurTechniqueSurMoisPrecedentDestination_echecEtCompensationSource() {

        BudgetMensuel budgetSource = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        BudgetMensuel budgetDestination = MockDataBudgets.getBudgetActifCompteC2et0operationPrevue();
        when(mockCompteServiceProvider.getCompteById("C1")).thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockCompteServiceProvider.getCompteById("C2")).thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC2()));
        when(mockOperationDataProvider.chargeBudgetMensuel(budgetSource.getId())).thenReturn(Uni.createFrom().item(budgetSource));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.JANUARY), eq(2022)))
                .thenReturn(Uni.createFrom().item(budgetDestination));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.DECEMBER), eq(2021)))
                .thenReturn(Uni.createFrom().failure(new MongoTimeoutException("timeout")));
        when(mockOperationDataProvider.sauvegardeBudgetMensuel(any(BudgetMensuel.class)))
                .thenAnswer(invocation -> Uni.createFrom().item((BudgetMensuel) invocation.getArgument(0)));
        when(mockOperationDataProvider.compenseOperation(any(), any(), any(), anyInt())).thenReturn(Uni.createFrom().voidItem());

        LigneOperation virement = MockDataOperations.getOperationIntercompte();
        virement.setEtat(OperationEtatEnum.PREVUE);

        MongoTimeoutException exception = assertThrows(MongoTimeoutException.class,
                () -> budgetAppProvider.createOperationsVirementInterne(budgetSource.getId(), virement, "C2", "userTest").await().indefinitely());

        assertEquals(503, new SPIExceptionsHandler().toResponse(exception).getStatus());
        // Seule la source est enregistrée (écriture initiale) ; aucune sauvegarde du budget de la destination
        verify(mockOperationDataProvider, times(1)).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(budgetDestination);
        // La source est compensée
        verify(mockOperationDataProvider, times(1)).compenseOperation(any(BudgetMensuel.class), eq(virement.getId()), isNull(), anyInt());
    }

    /**
     * NPE / donnée corrompue sur M-1 (soldes absents) à l'affichage : pas de mode dégradé, DonneeCorrompueException (500), aucune sauvegarde
     */
    @Test
    void testGetBudgetSurCompteActif_moisPrecedentSansSoldes_donneeCorrompue500() {

        BudgetMensuel janvier = MockDataBudgets.getBudgetActifCompteC1et1operationPrevue();
        BudgetMensuel decembre = MockDataBudgets.getBudgetPrecedentCompteC1();
        decembre.setSoldes(null);
        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.JANUARY), eq(2022)))
                .thenReturn(Uni.createFrom().item(janvier));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.DECEMBER), eq(2021)))
                .thenReturn(Uni.createFrom().item(decembre));

        CompletionException exception = assertThrows(CompletionException.class,
                () -> budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2022).await().indefinitely());

        assertDonneeCorrompue500(exception.getCause());
        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }

    /**
     * Exception non Mongo sur M-1 à l'affichage (ici NPE) : remontée telle quelle, pas de mode dégradé, aucune sauvegarde
     */
    @Test
    void testGetBudgetSurCompteActif_exceptionNonMongoSurMoisPrecedent_remontee() {

        when(mockCompteServiceProvider.getCompteById(anyString()))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getCompteC1()));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.JANUARY), eq(2022)))
                .thenReturn(Uni.createFrom().item(MockDataBudgets.getBudgetActifCompteC1et1operationPrevue()));
        when(mockOperationDataProvider.chargeBudgetMensuel(any(CompteBancaire.class), eq(Month.DECEMBER), eq(2021)))
                .thenReturn(Uni.createFrom().failure(new NullPointerException("npe")));

        assertThrows(NullPointerException.class, () -> budgetAppProvider.getBudgetMensuel("C1", Month.JANUARY, 2022).await().indefinitely());

        verify(mockOperationDataProvider, never()).sauvegardeBudgetMensuel(any(BudgetMensuel.class));
    }
}
