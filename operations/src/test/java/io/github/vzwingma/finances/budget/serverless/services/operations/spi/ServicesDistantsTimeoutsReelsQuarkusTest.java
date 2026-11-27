package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.test.aval.FauxAval;
import io.github.vzwingma.finances.budget.serverless.services.operations.test.aval.ProfilFauxAvalTimeoutsReels;
import io.github.vzwingma.finances.budget.services.communs.api.metrics.MetricsLog;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.ServiceDistantIndisponibleException;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AP004 T2.8 — clients REST réels face à un faux aval JDK, avec les <b>timeouts réels</b> (1,5 s / 4 s) : complète
 * {@link ServicesDistantsResilienceQuarkusTest} (timeout réduit à 500 ms) sur ce qui n'y est pas couvert :
 * <ul>
 *   <li>aval lent : 503 en moins de 6 s, un seul appel (aucun retry sur timeout) ;</li>
 *   <li>400 / 401 / 403 / 406 / 500 de l'aval : 503, un seul appel ;</li>
 *   <li>429 / 502 / 503 / 504 persistants : un seul retry (2 appels) puis 503 ;</li>
 *   <li>connexion refusée : un retry puis 503 ; 404 : 404 sans retry.</li>
 * </ul>
 */
@QuarkusTest
@TestProfile(ProfilFauxAvalTimeoutsReels.class)
class ServicesDistantsTimeoutsReelsQuarkusTest {

    private static final int PORT_COMPTES = FauxAval.port(FauxAval.PROP_PORT_COMPTES);
    private static final int PORT_PARAMETRAGES = FauxAval.port(FauxAval.PROP_PORT_PARAMETRAGES);
    private static final String COMPTE_JSON = "{\"id\":\"C1\",\"libelle\":\"Compte\",\"actif\":true}";
    /** Délai de l'aval lent : supérieur au read-timeout (4 s) et à la borne de 6 s du critère d'acceptation */
    private static final long DELAI_AVAL_LENT_MS = 6500;
    private static final long BORNE_MS = 6000;

    @Inject
    @RestClient
    IComptesServiceProvider comptes;

    @Inject
    @RestClient
    IParametragesServiceProvider parametrages;

    @CacheName(IParametragesServiceProvider.CATEGORIES_CACHE)
    Cache cacheCategories;

    private FauxAval avalComptes;
    private FauxAval avalParametrages;
    private final List<String> metriques = new CopyOnWriteArrayList<>();
    private Handler handlerMetriques;
    private Logger loggerMetriques;

    @BeforeEach
    void setup() {
        cacheCategories.invalidateAll().await().indefinitely();
        handlerMetriques = new Handler() {
            @Override
            public void publish(LogRecord record) {
                metriques.add(record.getMessage());
            }

            @Override
            public void flush() {
                // sans objet
            }

            @Override
            public void close() {
                // sans objet
            }
        };
        loggerMetriques = Logger.getLogger(MetricsLog.CATEGORIE);
        loggerMetriques.addHandler(handlerMetriques);
    }

    @AfterEach
    void teardown() {
        loggerMetriques.removeHandler(handlerMetriques);
        fermer(avalComptes);
        fermer(avalParametrages);
    }

    @Test
    void avalLent_readTimeout4s_serviceIndisponible_sousLes6Secondes_sansRetry() throws IOException {
        avalComptes = FauxAval.demarrer(PORT_COMPTES, n -> 200, COMPTE_JSON, DELAI_AVAL_LENT_MS);
        long debut = System.nanoTime();
        Throwable e = erreur(() -> comptes.getCompteById("C1").await().indefinitely());
        long dureeMs = (System.nanoTime() - debut) / 1_000_000;

        assertInstanceOf(ServiceDistantIndisponibleException.class, e);
        assertFalse(e instanceof ServiceDistantTransitoireException, "un timeout n'est jamais rejoué");
        assertTrue(dureeMs < BORNE_MS, "503 attendu en moins de 6 s, durée = " + dureeMs + " ms");
        assertTrue(dureeMs >= 3500, "le read-timeout réel de 4 s doit s'appliquer (pas de valeur réduite), durée = " + dureeMs + " ms");
        assertEquals(1, avalComptes.appels(), "aucun retry sur timeout");
    }

    @ParameterizedTest(name = "aval {0} -> 503, un seul appel")
    @ValueSource(ints = {400, 401, 403, 406, 500})
    void statutsAvalNonRejouables_serviceIndisponible_sansRetry(int statut) throws IOException {
        avalComptes = FauxAval.demarrer(PORT_COMPTES, n -> statut, "", 0);
        Throwable e = erreur(() -> comptes.getCompteById("C1").await().indefinitely());
        assertInstanceOf(ServiceDistantIndisponibleException.class, e, "statut " + statut);
        assertFalse(e instanceof ServiceDistantTransitoireException, "statut " + statut + " : pas de retry");
        assertEquals(1, avalComptes.appels(), "statut " + statut);
    }

    @ParameterizedTest(name = "aval {0} persistant -> 1 retry puis 503")
    @ValueSource(ints = {429, 502, 503, 504})
    void statutsAvalTransitoires_unSeulRetryPuisServiceIndisponible(int statut) throws IOException {
        avalComptes = FauxAval.demarrer(PORT_COMPTES, n -> statut, "", 0);
        Throwable e = erreur(() -> comptes.getCompteById("C1").await().indefinitely());
        assertInstanceOf(ServiceDistantIndisponibleException.class, e, "statut " + statut);
        assertEquals(2, avalComptes.appels(), "statut " + statut + " : 1 appel + 1 seul retry");
    }

    @Test
    void connexionRefusee_unRetryPuisServiceIndisponible_rapidement() {
        // aucun serveur sur PORT_COMPTES
        long debut = System.nanoTime();
        Throwable e = erreur(() -> comptes.getCompteById("C1").await().indefinitely());
        long dureeMs = (System.nanoTime() - debut) / 1_000_000;
        assertInstanceOf(ServiceDistantTransitoireException.class, e);
        assertTrue(dureeMs < BORNE_MS, "durée = " + dureeMs + " ms");
        assertEquals(2, metriques.stream().filter(m -> m.contains("client=comptes method=GET status=connect-error")).count(),
                "connexion refusée : 1 appel + 1 seul retry : " + metriques);
    }

    @Test
    void comptes_404_dataNotFound_sansRetry() throws IOException {
        avalComptes = FauxAval.demarrer(PORT_COMPTES, n -> 404, "", 0);
        assertInstanceOf(DataNotFoundException.class, erreur(() -> comptes.getCompteById("C1").await().indefinitely()));
        assertEquals(1, avalComptes.appels());
    }

    @Test
    void parametrages_404_dataNotFound_sansRetry() throws IOException {
        avalParametrages = FauxAval.demarrer(PORT_PARAMETRAGES, n -> 404, "", 0);
        assertInstanceOf(DataNotFoundException.class, erreur(() -> parametrages.getSsCategorieParId("ss1").await().indefinitely()));
        assertEquals(1, avalParametrages.appels());
    }

    @Test
    void parametrages_403_sansRetry_puis504_unRetry() throws IOException {
        avalParametrages = FauxAval.demarrer(PORT_PARAMETRAGES, n -> 403, "", 0);
        assertInstanceOf(ServiceDistantIndisponibleException.class, erreur(() -> parametrages.getCategories().await().indefinitely()));
        assertEquals(1, avalParametrages.appels(), "403 : pas de retry");
        avalParametrages.close();

        avalParametrages = FauxAval.demarrer(PORT_PARAMETRAGES, n -> 504, "", 0);
        assertInstanceOf(ServiceDistantIndisponibleException.class, erreur(() -> parametrages.getCategories().await().indefinitely()));
        assertEquals(2, avalParametrages.appels(), "504 : 1 appel + 1 retry (l'erreur précédente n'a pas été mise en cache)");
    }

    // ====== utilitaires ======

    /** Erreur levée par l'appel ; {@code await()} enveloppe les exceptions vérifiées dans une {@link CompletionException} */
    private static Throwable erreur(Runnable appel) {
        Throwable e = assertThrows(Throwable.class, appel::run);
        return e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
    }

    private static void fermer(FauxAval aval) {
        if (aval != null) {
            aval.close();
        }
    }
}
