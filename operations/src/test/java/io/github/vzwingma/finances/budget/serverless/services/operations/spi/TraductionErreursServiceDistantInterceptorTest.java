package io.github.vzwingma.finances.budget.serverless.services.operations.spi;

import io.github.vzwingma.finances.budget.serverless.services.operations.spi.TraductionErreursServiceDistantInterceptor.Echec;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AP004 T2.4 : classement des échecs sans réponse HTTP (connexion, timeout, autre).
 * Le comportement avec le vrai client REST est couvert par {@link ServicesDistantsResilienceQuarkusTest}.
 */
class TraductionErreursServiceDistantInterceptorTest {

    /** Équivalent du ConnectTimeoutException de Netty : une ConnectException dont le nom se termine par TimeoutException */
    static class ConnectTimeoutException extends ConnectException {
        ConnectTimeoutException(String message) {
            super(message);
        }
    }

    @Test
    void connexionRefusee_enveloppee_estUneErreurDeConnexion() {
        assertEquals(Echec.CONNEXION, TraductionErreursServiceDistantInterceptor.classer(new ProcessingException(new ConnectException("refused"))));
    }

    @Test
    void hoteInconnu_estUneErreurDeConnexion() {
        assertEquals(Echec.CONNEXION, TraductionErreursServiceDistantInterceptor.classer(new ProcessingException(new UnknownHostException("x"))));
    }

    @Test
    void timeoutDeLecture_estUnTimeout() {
        assertEquals(Echec.TIMEOUT, TraductionErreursServiceDistantInterceptor.classer(new ProcessingException(new TimeoutException("4000 ms"))));
        assertEquals(Echec.TIMEOUT, TraductionErreursServiceDistantInterceptor.classer(new SocketTimeoutException("read")));
    }

    @Test
    void timeoutDeConnexion_estUnTimeout_doncJamaisRejoue() {
        assertEquals(Echec.TIMEOUT, TraductionErreursServiceDistantInterceptor.classer(new ProcessingException(new ConnectTimeoutException("1500 ms"))));
    }

    @Test
    void autresErreurs_traverseesInchangees() {
        assertEquals(Echec.AUTRE, TraductionErreursServiceDistantInterceptor.classer(new IllegalStateException("bug")));
        assertEquals(Echec.AUTRE, TraductionErreursServiceDistantInterceptor.classer(new WebApplicationException(Response.status(418).build())));
        assertEquals(Echec.AUTRE, TraductionErreursServiceDistantInterceptor.classer(new ProcessingException("désérialisation")));
    }

    @Test
    void exceptionMetier_neEstJamaisReclassee() {
        assertEquals(Echec.AUTRE, TraductionErreursServiceDistantInterceptor.classer(new DataNotFoundException("404")));
    }
}
