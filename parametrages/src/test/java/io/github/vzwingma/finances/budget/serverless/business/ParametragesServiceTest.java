package io.github.vzwingma.finances.budget.serverless.business;

import io.github.vzwingma.finances.budget.serverless.data.MockDataCategoriesOperations;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ParametragesService;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports.IParametrageAppProvider;
import io.github.vzwingma.finances.budget.serverless.services.parametrages.business.ports.IParametragesRepository;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyReadRepository;
import io.github.vzwingma.finances.budget.services.communs.business.ports.IJwtSigningKeyWriteRepository;
import io.github.vzwingma.finances.budget.services.communs.data.model.CategorieOperations;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ParametragesServiceTest {

    private IParametrageAppProvider parametrageAppProvider;
    private IParametragesRepository parametrageServiceProvider;

    @BeforeEach
    public void setup() {
        parametrageServiceProvider = Mockito.mock(IParametragesRepository.class);
        IJwtSigningKeyWriteRepository signingKeyRepository = Mockito.mock(IJwtSigningKeyWriteRepository.class);
        IJwtSigningKeyReadRepository signingKeyRRepository = Mockito.mock(IJwtSigningKeyReadRepository.class);
        parametrageAppProvider = Mockito.spy(new ParametragesService(parametrageServiceProvider, signingKeyRepository, signingKeyRRepository));

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
        CategorieOperations cat = parametrageAppProvider.getCategorieById("8f1614c9-503c-4e7d-8cb5-0c9a9218b84a").await().indefinitely();
        // Vérification
        assertNotNull(cat);
        assertEquals("Alimentation", cat.getLibelle());
        assertTrue(cat.isCategorie());
        // 1 seul appel à la BDD
        Mockito.verify(parametrageServiceProvider, Mockito.times(1)).chargeCategories();
        assertEquals(1, cat.getListeSSCategories().size());
    }


    @Test
    void testGetSsCategorieById() {
        // Lancement du test
        CategorieOperations cat = parametrageAppProvider.getCategorieById("467496e4-9059-4b9b-8773-21f230c8c5c6").await().indefinitely();
        // Vérification
        assertNotNull(cat);
        assertEquals("Courses", cat.getLibelle());
        assertFalse(cat.isCategorie());
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
}
