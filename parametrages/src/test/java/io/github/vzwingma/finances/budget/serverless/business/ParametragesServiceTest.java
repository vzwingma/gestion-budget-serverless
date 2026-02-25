package io.github.vzwingma.finances.budget.serverless.business;

import io.github.vzwingma.finances.budget.serverless.data.MockDataCategoriesOperations;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ParametragesService;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports.IParametrageAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports.IParametragesRepository;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.spi.IJwtAuthSigningKeyServiceProvider;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyWriteRepository;
import io.github.vzwingma.finances.budget.services.communs.data.abstrait.AbstractCategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.SsCategorieOperations;
import io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKeys;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ParametragesServiceTest {

    private IParametrageAppProvider parametrageAppProvider;
    private IParametragesRepository parametrageServiceProvider;
    private ParametragesService parametragesService;
    private IJwtSigningKeyReadRepository signingKeyRRepository;

    @BeforeEach
    void setup() {
        parametrageServiceProvider = Mockito.mock(IParametragesRepository.class);
        IJwtSigningKeyWriteRepository signingKeyRepository = Mockito.mock(IJwtSigningKeyWriteRepository.class);
        signingKeyRRepository = Mockito.mock(IJwtSigningKeyReadRepository.class);
        parametragesService = Mockito.spy(new ParametragesService(parametrageServiceProvider, signingKeyRepository, signingKeyRRepository));
        parametrageAppProvider = parametragesService;

        Mockito.when(parametrageServiceProvider.chargeCategories()).thenReturn(Multi.createFrom().items(MockDataCategoriesOperations.getListeTestCategories().stream()));
    }

    @Test
    void testGetListeCategories() {
        // Lancement du test
        List<CategorieOperations> listeCat = parametrageAppProvider.getCategories().await().indefinitely();
        // Vérification
        assertNotNull(listeCat);
        assertEquals(1, listeCat.size());
        // 1 seul appel à la BDD
        Mockito.verify(parametrageServiceProvider, Mockito.times(1)).chargeCategories();
        assertEquals(1, listeCat.getFirst().getListeSSCategories().size());
    }


    @Test
    void testGetCategorieById() {
        // Lancement du test
        AbstractCategorieOperations cat = parametrageAppProvider.getCategorieById("8f1614c9-503c-4e7d-8cb5-0c9a9218b84a").await().indefinitely();
        // Vérification
        assertNotNull(cat);
        assertInstanceOf(CategorieOperations.class, cat);
        CategorieOperations catOp = (CategorieOperations) cat;
        assertEquals("Alimentation", catOp.getLibelle());
        // 1 seul appel à la BDD
        Mockito.verify(parametrageServiceProvider, Mockito.times(1)).chargeCategories();
        assertEquals(1, catOp.getListeSSCategories().size());
    }


    @Test
    void testGetSsCategorieById() {
        // Lancement du test
        AbstractCategorieOperations cat = parametrageAppProvider.getCategorieById("467496e4-9059-4b9b-8773-21f230c8c5c6").await().indefinitely();
        // Vérification
        assertNotNull(cat);
        assertInstanceOf(SsCategorieOperations.class, cat);
        SsCategorieOperations ssCat = (SsCategorieOperations) cat;
        assertEquals("Courses", ssCat.getLibelle());
        assertNotNull(ssCat.getCategorieParente());
        // 1 seul appel à la BDD
        Mockito.verify(parametrageServiceProvider, Mockito.times(1)).chargeCategories();
    }


    @Test
    void testGetNoCategorieById() {
        // Lancement du test
        CompletionException exception = assertThrows(CompletionException.class,
                () -> parametrageAppProvider.getCategorieById("unknown-id").await().indefinitely());
        // Vérification
        assertNotNull(exception);
        assertEquals("io.github.vzwingma.finances.budget.services.communs.utils.exceptions.DataNotFoundException", exception.getMessage());
        // 1 seul appel à la BDD
        Mockito.verify(parametrageServiceProvider, Mockito.times(1)).chargeCategories();
    }

    @Test
    void testGetCategoriesAvecCategorieInactive() {
        // Catégorie inactive → filtrée, non retournée
        CategorieOperations catInactive = new CategorieOperations();
        catInactive.setId("inactive-id");
        catInactive.setActif(false);
        catInactive.setLibelle("Inactive");
        SsCategorieOperations ssCat = new SsCategorieOperations();
        ssCat.setId("ss-inactive");
        ssCat.setActif(true);
        ssCat.setLibelle("SsInactive");
        catInactive.setListeSSCategories(new HashSet<>());
        catInactive.getListeSSCategories().add(ssCat);

        Mockito.when(parametrageServiceProvider.chargeCategories())
                .thenReturn(Multi.createFrom().item(catInactive));
        List<CategorieOperations> liste = parametrageAppProvider.getCategories().await().indefinitely();
        assertTrue(liste.isEmpty());
    }

    @Test
    void testGetCategoriesSansSsCategories() {
        // Catégorie sans sous-catégories → filtrée
        CategorieOperations catSansSsCat = new CategorieOperations();
        catSansSsCat.setId("no-ss-cat");
        catSansSsCat.setActif(true);
        catSansSsCat.setLibelle("SansSsCat");
        catSansSsCat.setListeSSCategories(new HashSet<>());

        Mockito.when(parametrageServiceProvider.chargeCategories())
                .thenReturn(Multi.createFrom().item(catSansSsCat));
        List<CategorieOperations> liste = parametrageAppProvider.getCategories().await().indefinitely();
        assertTrue(liste.isEmpty());
    }

    @Test
    void testGetSigningKeyReadRepository() {
        assertNotNull(parametragesService.getSigningKeyReadRepository());
        assertEquals(signingKeyRRepository, parametragesService.getSigningKeyReadRepository());
    }

    @Test
    void testRefreshJwksSigningKeys() throws Exception {
        IJwtAuthSigningKeyServiceProvider jwtProvider = Mockito.mock(IJwtAuthSigningKeyServiceProvider.class);
        IJwtSigningKeyWriteRepository writeRepo = Mockito.mock(IJwtSigningKeyWriteRepository.class);

        JwksAuthKeys keys = new JwksAuthKeys();
        keys.setKeys(new io.github.vzwingma.finances.budget.services.communs.data.model.jwt.JwksAuthKey[0]);

        Mockito.when(jwtProvider.getJwksAuthKeys()).thenReturn(Uni.createFrom().item(keys));
        Mockito.when(writeRepo.saveJwksAuthKeys(Mockito.anyList())).thenReturn(Uni.createFrom().voidItem());

        ParametragesService service2 = Mockito.spy(new ParametragesService(parametrageServiceProvider, writeRepo, signingKeyRRepository));
        // Injection par réflexion du champ @RestClient
        java.lang.reflect.Field field = ParametragesService.class.getDeclaredField("jwtAuthSigningKeyServiceProvider");
        field.setAccessible(true);
        field.set(service2, jwtProvider);

        assertDoesNotThrow(() -> service2.refreshJwksSigningKeys().await().indefinitely());
    }
}
