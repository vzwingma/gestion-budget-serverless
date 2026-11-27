package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.test.aval.FauxAval;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.ServiceDistantIndisponibleException;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AP004 T2.8 (T2.6) — TTL du cache des catégories avec une durée courte (1 s) : dans le TTL, 2 lectures = 1 appel HTTP ;
 * après expiration, un nouvel appel HTTP ; une erreur n'est jamais mise en cache (même après expiration).
 * <p>
 * Le TTL de production (1 h) est contrôlé par {@code RestClientTimeoutsConfigTest} ; ici, seul le comportement d'expiration
 * est vérifié, avec un {@code expire-after-write} surchargé par le profil de test.
 */
@QuarkusTest
@TestProfile(CacheCategoriesTtlCourtQuarkusTest.ProfilTtlCourt.class)
class CacheCategoriesTtlCourtQuarkusTest {

    private static final int PORT_PARAMETRAGES = FauxAval.port(FauxAval.PROP_PORT_PARAMETRAGES);
    private static final String CATEGORIES_JSON = "[{\"id\":\"cat1\",\"libelle\":\"Cat 1\",\"listeSSCategories\":[]}]";
    private static final String SS_CATEGORIE_JSON = "{\"id\":\"ss1\",\"libelle\":\"Sous cat 1\"}";
    private static final Duration TTL = Duration.ofSeconds(1);

    /** TTL réduit à 1 s ; les timeouts réels des clients sont conservés */
    public static class ProfilTtlCourt implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.rest-client.comptes-service.url", "http://127.0.0.1:" + FauxAval.port(FauxAval.PROP_PORT_COMPTES),
                    "quarkus.rest-client.parametrages-service.url", "http://127.0.0.1:" + FauxAval.port(FauxAval.PROP_PORT_PARAMETRAGES),
                    "quarkus.cache.caffeine.\"categories\".expire-after-write", "1S");
        }
    }

    @Inject
    @RestClient
    IParametragesServiceProvider parametrages;

    @CacheName(IParametragesServiceProvider.CATEGORIES_CACHE)
    Cache cacheCategories;

    private FauxAval aval;

    @BeforeEach
    void setup() {
        cacheCategories.invalidateAll().await().indefinitely();
    }

    @AfterEach
    void teardown() {
        if (aval != null) {
            aval.close();
        }
    }

    @Test
    void ttlCourtEffectivementApplique() {
        assertEquals(TTL, ConfigProvider.getConfig().getValue("quarkus.cache.caffeine.\"categories\".expire-after-write", Duration.class));
    }

    @Test
    void categories_deuxLecturesDansLeTtl_unAppel_apresExpiration_deuxAppels() throws IOException, InterruptedException {
        aval = FauxAval.demarrer(PORT_PARAMETRAGES, n -> 200, CATEGORIES_JSON, 0);
        var premiere = parametrages.getCategories().await().indefinitely();
        var deuxieme = parametrages.getCategories().await().indefinitely();
        assertEquals(1, aval.appels(), "2 lectures dans le TTL : 1 seul appel HTTP");
        assertSame(premiere, deuxieme, "la 2e lecture est servie par le cache");

        attendreExpiration();

        assertEquals(1, parametrages.getCategories().await().indefinitely().size());
        assertEquals(2, aval.appels(), "après expiration du TTL : nouvel appel HTTP");
        parametrages.getCategories().await().indefinitely();
        assertEquals(2, aval.appels(), "la valeur rechargée est de nouveau en cache");
    }

    @Test
    void ssCategorie_apresExpiration_nouvelAppel() throws IOException, InterruptedException {
        aval = FauxAval.demarrer(PORT_PARAMETRAGES, n -> 200, SS_CATEGORIE_JSON, 0);
        parametrages.getSsCategorieParId("ss1").await().indefinitely();
        parametrages.getSsCategorieParId("ss1").await().indefinitely();
        assertEquals(1, aval.appels());

        attendreExpiration();

        parametrages.getSsCategorieParId("ss1").await().indefinitely();
        assertEquals(2, aval.appels(), "après expiration du TTL : nouvel appel HTTP");
    }

    @Test
    void uneErreurNEstJamaisMiseEnCache_niPendantNiApresLeTtl() throws IOException, InterruptedException {
        // 1er appel : 500 ; ensuite 200
        aval = FauxAval.demarrer(PORT_PARAMETRAGES, n -> n == 1 ? 500 : 200, CATEGORIES_JSON, 0);
        Throwable e = assertThrows(Throwable.class, () -> parametrages.getCategories().await().indefinitely());
        assertInstanceOf(ServiceDistantIndisponibleException.class, e instanceof CompletionException ? e.getCause() : e);
        assertEquals(1, aval.appels());

        // dans le TTL : la lecture suivante rappelle l'aval (l'erreur n'est pas servie depuis le cache) et réussit
        assertEquals(1, parametrages.getCategories().await().indefinitely().size());
        assertEquals(2, aval.appels());
        // le succès, lui, est en cache dans le TTL...
        parametrages.getCategories().await().indefinitely();
        assertEquals(2, aval.appels());
        // ... et expire
        attendreExpiration();
        parametrages.getCategories().await().indefinitely();
        assertEquals(3, aval.appels());
    }

    /** Attente de l'expiration (TTL de 1 s) avec marge ; la lecture après expiration est paresseuse côté Caffeine */
    private static void attendreExpiration() throws InterruptedException {
        Thread.sleep(TTL.toMillis() + 500);
    }
}
