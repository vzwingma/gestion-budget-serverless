package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api;

import io.github.vzwingma.finances.budget.services.communs.api.metrics.MetricsLog;
import com.mongodb.MongoException;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.api.enums.UtilisateursAPIEnum;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.UtilisateursService;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.CompteClosedException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DonneeCorrompueException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.BadParametersException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.ConflictException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.ServiceDistantIndisponibleException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.UserAccessForbiddenException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.UserNotAuthorizedException;
import com.mongodb.MongoTimeoutException;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AP004 T2.1 (R1, R10) : les providers mutualisés dans {@code communs} (mappers d'exceptions, filtre de sécurité,
 * filtres de trace) sont bien actifs dans le module utilisateurs. Un module construit contre un SNAPSHOT {@code communs}
 * obsolète, ou qui aurait perdu ses providers, ferait échouer ces tests sans erreur de compilation.
 * <ul>
 *   <li>404 / 406 / 500 / 503 : mappers {@code APIExceptionsHandler} et {@code SPIExceptionsHandler} ;</li>
 *   <li>401 : sécurité quarkus-oidc ;</li>
 *   <li>jeton valide : {@code getAuthenticatedUser()} fournit l'identité (donc {@code JwtSecurityFilter} actif) ;</li>
 *   <li>trace : une seule ligne {@code [HTTP-METRIC]} par requête (pas de filtre de trace en double).</li>
 * </ul>
 */
@QuarkusTest
class CommunsProvidersRestTest {

    private static final String URL = UtilisateursAPIEnum.USERS_BASE + UtilisateursAPIEnum.USERS_ACCESS_DATE;
    private static UtilisateursService serviceMock;

    @BeforeAll
    static void init() {
        serviceMock = mock(UtilisateursService.class);
        QuarkusMock.installMockForType(serviceMock, UtilisateursService.class);
    }

    @BeforeEach
    void setup() {
        reset(serviceMock);
    }

    private static String bearerValide() {
        return OidcTokenForge.bearer(OidcTokenForge.valid(OidcTokenForge.validClaims()));
    }

    private static int statutAvecErreur(Throwable erreur) {
        when(serviceMock.getLastAccessDate(anyString())).thenReturn(Uni.createFrom().failure(erreur));
        return given().header(HttpHeaders.AUTHORIZATION, bearerValide()).when().get(URL).then().extract().statusCode();
    }

    @Test
    void sansJeton_401() {
        assertEquals(401, given().when().get(URL).then().extract().statusCode());
        verify(serviceMock, never()).getLastAccessDate(anyString());
    }

    @Test
    void donneeIntrouvable_404() {
        assertEquals(404, statutAvecErreur(new DataNotFoundException("introuvable")));
    }

    @Test
    void compteClos_406() {
        assertEquals(406, statutAvecErreur(new CompteClosedException("clos")));
    }

    @Test
    void donneeCorrompue_500() {
        assertEquals(500, statutAvecErreur(new DonneeCorrompueException("corrompue")));
    }

    @Test
    void erreurMongo_503() {
        assertEquals(503, statutAvecErreur(new MongoException("base indisponible")));
    }

    @Test
    void parametresInvalides_400() {
        assertEquals(400, statutAvecErreur(new BadParametersException("parametre invalide")));
    }

    @Test
    void conflit_409() {
        assertEquals(409, statutAvecErreur(new ConflictException("conflit")));
    }

    @Test
    void accesInterdit_403() {
        assertEquals(403, statutAvecErreur(new UserAccessForbiddenException("interdit")));
    }

    @Test
    void utilisateurNonAutorise_401() {
        assertEquals(401, statutAvecErreur(new UserNotAuthorizedException("non autorise")));
    }

    @Test
    void serviceDistantIndisponible_503() {
        assertEquals(503, statutAvecErreur(new ServiceDistantIndisponibleException("aval indisponible")));
    }

    @Test
    void timeoutMongo_503() {
        assertEquals(503, statutAvecErreur(new MongoTimeoutException("serveur injoignable")));
    }

    @Test
    void jetonValide_identiteFournieParLeFiltreDeSecurite() {
        when(serviceMock.getLastAccessDate(anyString())).thenReturn(Uni.createFrom().item(LocalDateTime.of(2026, 1, 10, 8, 0)));
        assertEquals(200, given().header(HttpHeaders.AUTHORIZATION, bearerValide()).when().get(URL).then().extract().statusCode());
        verify(serviceMock).getLastAccessDate(OidcTokenForge.VALID_EMAIL);
    }

    @Test
    void uneSeuleLigneDeTraceParRequete() {
        when(serviceMock.getLastAccessDate(anyString())).thenReturn(Uni.createFrom().item(LocalDateTime.of(2026, 1, 10, 8, 0)));
        List<String> lignes = new CopyOnWriteArrayList<>();
        Logger jul = Logger.getLogger(MetricsLog.CATEGORIE);
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                lignes.add(record.getMessage());
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
        Logger julSecurite = Logger.getLogger("io.github.vzwingma.finances.budget.services.communs.api.security.AbstractAPISecurityFilter");
        jul.addHandler(handler);
        julSecurite.addHandler(handler);
        try {
            given().header(HttpHeaders.AUTHORIZATION, bearerValide()).when().get(URL).then().statusCode(200);
        } finally {
            jul.removeHandler(handler);
            julSecurite.removeHandler(handler);
        }
        long metriques = lignes.stream().filter(l -> l.startsWith("[HTTP-METRIC]")).count();
        assertEquals(1, metriques, "une seule ligne [HTTP-METRIC] attendue par requête : " + lignes);
        // Pas de JwtSecurityFilter en double (coquille du module + filtre de communs) : le WARN « Clé API non trouvée » est unique
        long filtresSecurite = lignes.stream().filter(l -> l.startsWith("Clé API non trouvée")).count();
        assertEquals(1, filtresSecurite, "un seul JwtSecurityFilter attendu par requête : " + lignes);
        assertTrue(lignes.stream().noneMatch(l -> l.contains("eyJ")));
    }
}
