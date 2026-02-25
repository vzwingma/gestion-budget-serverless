package io.github.vzwingma.finances.budget.serverless.services.comptes.business;

import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ports.IComptesAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.comptes.business.ports.IComptesRepository;
import io.github.vzwingma.finances.budget.serverless.services.comptes.test.data.MockDataComptes;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.CompteBancaire;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey;
import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ComptesServiceTest {

    private IComptesAppProvider comptesAppProvider;
    private IComptesRepository comptesRepository;
    private IJwtSigningKeyReadRepository signingKeyReadRepository;

    @BeforeEach
    void setup() {
        comptesRepository = Mockito.mock(IComptesRepository.class);
        signingKeyReadRepository = Mockito.mock(IJwtSigningKeyReadRepository.class);
        comptesAppProvider = Mockito.spy(new ComptesService(comptesRepository, signingKeyReadRepository));
    }

    @Test
    void testGetComptes() {
        Mockito.when(comptesRepository.chargeComptes("test")).thenReturn(Multi.createFrom().items(MockDataComptes.getListeComptes().stream()));

        List<CompteBancaire> comptes = comptesAppProvider.getComptesUtilisateur("test").await().indefinitely();
        Assertions.assertNotNull(comptes);
        Assertions.assertEquals(3, comptes.size());
        // Vérifie le tri par ordre (ordre=0 en premier)
        Assertions.assertEquals("Libelle0", comptes.getFirst().getLibelle());
    }

    @Test
    void testGetComptesTriParOrdre() {
        Mockito.when(comptesRepository.chargeComptes("test")).thenReturn(Multi.createFrom().items(MockDataComptes.getListeComptes().stream()));

        List<CompteBancaire> comptes = comptesAppProvider.getComptesUtilisateur("test").await().indefinitely();
        Assertions.assertNotNull(comptes);
        Assertions.assertEquals(3, comptes.size());
        // Vérifie le tri : ordre 0, 1, 2
        Assertions.assertEquals(0, comptes.get(0).getOrdre());
        Assertions.assertEquals(1, comptes.get(1).getOrdre());
        Assertions.assertEquals(2, comptes.get(2).getOrdre());
    }

    @Test
    void testGetComptesListeVide() {
        Mockito.when(comptesRepository.chargeComptes("test")).thenReturn(Multi.createFrom().empty());

        List<CompteBancaire> comptes = comptesAppProvider.getComptesUtilisateur("test").await().indefinitely();
        Assertions.assertNotNull(comptes);
        Assertions.assertTrue(comptes.isEmpty());
    }

    @Test
    void testGetComptesErreurRepository() {
        Mockito.when(comptesRepository.chargeComptes("test"))
                .thenReturn(Multi.createFrom().failure(new DataNotFoundException("Erreur BDD")));

        Assertions.assertThrows(Exception.class,
                () -> comptesAppProvider.getComptesUtilisateur("test").await().indefinitely());
    }

    @Test
    void testGetCompteById() {
        Mockito.when(comptesRepository.chargeCompteParId(Mockito.eq("A3"), Mockito.anyString())).thenReturn(Uni.createFrom().item(MockDataComptes.getCompte1()));

        CompteBancaire compte = comptesAppProvider.getCompteById("A3", "test").await().indefinitely();
        Assertions.assertNotNull(compte);
        Assertions.assertEquals("Libelle1", compte.getLibelle());
    }

    @Test
    void testGetCompteByIdIntrouvable() {
        Mockito.when(comptesRepository.chargeCompteParId(Mockito.eq("INCONNU"), Mockito.anyString()))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Compte non trouvé")));

        Assertions.assertThrows(Exception.class,
                () -> comptesAppProvider.getCompteById("INCONNU", "test").await().indefinitely());
    }

    @Test
    void testGetCompteActif() {
        Mockito.when(comptesRepository.isCompteActif("A3")).thenReturn(Uni.createFrom().item(Boolean.TRUE));

        Assertions.assertTrue(comptesAppProvider.isCompteActif("A3").await().indefinitely());
    }

    @Test
    void testGetCompteInactif() {
        Mockito.when(comptesRepository.isCompteActif("INACTIF")).thenReturn(Uni.createFrom().item(Boolean.FALSE));

        Assertions.assertFalse(comptesAppProvider.isCompteActif("INACTIF").await().indefinitely());
    }

    @Test
    void testGetCompteActifErreurRepository() {
        Mockito.when(comptesRepository.isCompteActif("ERREUR"))
                .thenReturn(Uni.createFrom().failure(new DataNotFoundException("Compte non trouvé")));

        Assertions.assertThrows(Exception.class,
                () -> comptesAppProvider.isCompteActif("ERREUR").await().indefinitely());
    }

    @Test
    void testLoadJwksSigningKeys() {
        Mockito.when(signingKeyReadRepository.getJwksSigningAuthKeys()).thenReturn(Multi.createFrom().empty());

        ComptesService service = (ComptesService) comptesAppProvider;
        var keys = service.loadJwksSigningKeys().await().indefinitely();
        assertNotNull(keys);
        assertTrue(keys.isEmpty());
    }

    @Test
    void testLoadJwksSigningKeysAvecCles() {
        JwksAuthKey key = new JwksAuthKey();
        key.setKid("kid-test");
        key.setAlg("RS256");
        Mockito.when(signingKeyReadRepository.getJwksSigningAuthKeys()).thenReturn(Multi.createFrom().item(key));

        ComptesService service = (ComptesService) comptesAppProvider;
        var keys = service.loadJwksSigningKeys().await().indefinitely();
        assertNotNull(keys);
        assertEquals(1, keys.size());
        assertTrue(keys.containsKey("kid-test"));
    }

    @Test
    void testGetSigningKeyReadRepository() {
        assertNotNull(((ComptesService) comptesAppProvider).getSigningKeyReadRepository());
    }
}
