package io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business;

import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.Utilisateur;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.UtilisateurDroitsEnum;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.model.UtilisateurPrefsEnum;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.ports.IUtilisateursAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.business.ports.IUtilisateursRepository;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.config.codec.RegisterPanacheCodecs;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.config.codec.UtilisateurPanacheCodec;
import io.github.vzwingma.finances.budget.serverless.services.utilisateurs.data.MockDataUtilisateur;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.UserAccessForbiddenException;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de test du service UtilisateursService
 */
class UtilisateursServiceTest {

    private IUtilisateursAppProvider appProvider;
    private IUtilisateursRepository serviceDataProvider;
    private UtilisateursService utilisateursService;

    @BeforeEach
    void setup() {
        serviceDataProvider = Mockito.mock(IUtilisateursRepository.class);
        utilisateursService = Mockito.spy(new UtilisateursService(serviceDataProvider));
        appProvider = utilisateursService;

        Mockito.when(serviceDataProvider.chargeUtilisateur("Test")).thenReturn(Uni.createFrom().item(MockDataUtilisateur.getTestUtilisateur()));
        Mockito.when(serviceDataProvider.chargeUtilisateur("Test2")).thenReturn(Uni.createFrom().failure(new DataNotFoundException("Utilisateur non trouvé")));
        Mockito.doNothing().when(serviceDataProvider).majUtilisateur(Mockito.any());
    }

    @Test
    void testGetUtilisateur() {
        Utilisateur utilisateur = appProvider.getUtilisateur("Test").await().indefinitely();
        assertNotNull(utilisateur);
        assertEquals("54aa7db30bc460e1aeb95596", utilisateur.getId().toString());
        assertNotNull(utilisateur.getDernierAcces());
        Mockito.verify(serviceDataProvider, Mockito.times(1)).chargeUtilisateur(Mockito.anyString());
    }

    @Test
    void testGetUtilisateurKO() {
        Assertions.assertThrows(CompletionException.class, () ->
                appProvider.getUtilisateur("Test2").await().indefinitely());
        Mockito.verify(serviceDataProvider, Mockito.times(1)).chargeUtilisateur(Mockito.anyString());
        Mockito.verify(serviceDataProvider, Mockito.never()).majUtilisateur(Mockito.any());
    }

    @Test
    void testGetLastAccessUtilisateur() throws UserAccessForbiddenException {
        LocalDateTime lastAccess = appProvider.getLastAccessDate("Test").await().indefinitely();
        assertNotNull(lastAccess);
        Mockito.verify(serviceDataProvider, Mockito.times(1)).chargeUtilisateur(Mockito.anyString());
        Mockito.verify(serviceDataProvider, Mockito.times(1)).majUtilisateur(Mockito.any());
    }

    @Test
    void testGetLastAccessUtilisateurInconnu() {
        Assertions.assertThrows(CompletionException.class, () ->
                appProvider.getLastAccessDate("Test2").await().indefinitely());
        Mockito.verify(serviceDataProvider, Mockito.times(1)).chargeUtilisateur(Mockito.anyString());
        Mockito.verify(serviceDataProvider, Mockito.never()).majUtilisateur(Mockito.any());
    }

    @Test
    void testGetSigningKeyReadRepositoryNull() {
        // Sans injection CDI (constructeur de test) → null
        assertNull(utilisateursService.getSigningKeyReadRepository());
    }

    // ---- tests modèles ----

    @Test
    void testUtilisateurModele() {
        Utilisateur u = MockDataUtilisateur.getTestUtilisateur();
        assertNotNull(u.toString());
        assertNotNull(u.toFullString());
        assertEquals("Test", u.getLogin());
        assertNotNull(u.getPrefsUtilisateur());
        assertNotNull(u.getDroits());
    }

    @Test
    void testUtilisateurClone() {
        Utilisateur source = MockDataUtilisateur.getTestUtilisateur();
        source.setDroits(new EnumMap<>(UtilisateurDroitsEnum.class));
        source.getDroits().put(UtilisateurDroitsEnum.DROIT_CLOTURE_BUDGET, true);
        Utilisateur clone = new Utilisateur(source);
        assertEquals(source.getId(), clone.getId());
        assertEquals(source.getLogin(), clone.getLogin());
        assertNotNull(clone.getDernierAcces());
        assertEquals(true, clone.getDroits().get(UtilisateurDroitsEnum.DROIT_CLOTURE_BUDGET));
    }

    @Test
    void testUtilisateurSansDernierAcces() {
        Utilisateur u = new Utilisateur();
        u.setLogin("loginTest");
        assertNotNull(u.toFullString());
        assertTrue(u.toFullString().contains("nulle"));
    }

    @Test
    void testUtilisateurPrefsEnum() {
        assertNotNull(UtilisateurPrefsEnum.PREFS_STATUT_NLLE_DEPENSE);
        assertEquals(1, UtilisateurPrefsEnum.values().length);
    }

    @Test
    void testUtilisateurDroitsEnum() {
        assertEquals(2, UtilisateurDroitsEnum.values().length);
        assertNotNull(UtilisateurDroitsEnum.DROIT_CLOTURE_BUDGET);
        assertNotNull(UtilisateurDroitsEnum.DROIT_RAZ_BUDGET);
    }

    @Test
    void testRegisterPanacheCodecs() {
        RegisterPanacheCodecs provider = new RegisterPanacheCodecs();
        assertNotNull(provider.get(Utilisateur.class, null));
        assertInstanceOf(UtilisateurPanacheCodec.class, provider.get(Utilisateur.class, null));
        assertNull(provider.get(String.class, null));
    }

    @Test
    void testUtilisateurId() {
        Utilisateur u = new Utilisateur();
        assertNull(u.getId());
        u.setId(new ObjectId("54aa7db30bc460e1aeb95596"));
        assertEquals("54aa7db30bc460e1aeb95596", u.getId().toString());
    }
}
