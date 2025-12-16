package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.ports.IUtilisateursAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.ports.IUtilisateursRepository;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.data.MockDataUtilisateur;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.UserAccessForbiddenException;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Classe de test du service UtilisateursService
 */
class UtilisateursServiceTest {


    private IUtilisateursAppProvider appProvider;
    private IUtilisateursRepository serviceDataProvider;

    @BeforeEach
    void setup() {
        serviceDataProvider = Mockito.mock(IUtilisateursRepository.class);
        appProvider = Mockito.spy(new UtilisateursService(serviceDataProvider));

        Mockito.when(serviceDataProvider.chargeUtilisateur("Test")).thenReturn(Uni.createFrom().item(MockDataUtilisateur.getTestUtilisateur()));
        Mockito.when(serviceDataProvider.chargeUtilisateur("Test2")).thenReturn(Uni.createFrom().failure(new DataNotFoundException("Utilisateur non trouvé")));
        Mockito.doNothing().when(serviceDataProvider).majUtilisateur(Mockito.any());
    }

    @Test
    void testGetUtilisateur() {
        // Lancement du test
        Utilisateur utilisateur = appProvider.getUtilisateur("Test").await().indefinitely();
        // Vérification
        assertNotNull(utilisateur);
        assertEquals("54aa7db30bc460e1aeb95596", utilisateur.getId().toString());
        assertNotNull(utilisateur.getDernierAcces());
        // 1 seul appel à la BDD pour charger l'utilisateur et 1 pour le mettre à jour
        Mockito.verify(serviceDataProvider, Mockito.times(1)).chargeUtilisateur(Mockito.anyString());
    }


    @Test
    void testGetUtilisateurKO() {
        // Lancement du test
        Assertions.assertThrows(CompletionException.class, () -> 
            appProvider.getUtilisateur("Test2").await().indefinitely()
        );

        //Vérification
        Mockito.verify(serviceDataProvider, Mockito.times(1)).chargeUtilisateur(Mockito.anyString());
        Mockito.verify(serviceDataProvider, Mockito.never()).majUtilisateur(Mockito.any());
    }


    @Test
    void testGetLastAccessUtilisateur() throws UserAccessForbiddenException {
        // Lancement du test
        LocalDateTime lastAccess = appProvider.getLastAccessDate("Test").await().indefinitely();
        // Vérification
        assertNotNull(lastAccess);
        // 1 seul appel à la BDD pour charger l'utilisateur et 1 pour le mettre à jour
        Mockito.verify(serviceDataProvider, Mockito.times(1)).chargeUtilisateur(Mockito.anyString());
        Mockito.verify(serviceDataProvider, Mockito.times(1)).majUtilisateur(Mockito.any());
    }
}
