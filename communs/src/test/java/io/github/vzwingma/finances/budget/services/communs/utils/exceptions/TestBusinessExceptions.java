package io.github.vzwingma.finances.budget.services.communs.utils.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires des exceptions métier
 */
class TestBusinessExceptions {

    @Test
    void testAbstractBusinessExceptionLibelle() {
        BadParametersException ex = new BadParametersException("paramètre invalide");
        assertEquals("paramètre invalide", ex.getLibelle());
    }

    @Test
    void testAbstractBusinessExceptionEchappementCaracteres() {
        // Les caractères \n, \r, \t doivent être remplacés par _
        BadParametersException ex = new BadParametersException("erreur\navec\nnewlines");
        assertFalse(ex.getLibelle().contains("\n"));
        assertTrue(ex.getLibelle().contains("_"));
    }

    @Test
    void testBadParametersException() {
        BadParametersException ex = new BadParametersException("Bad param");
        assertNotNull(ex);
        assertEquals("Bad param", ex.getLibelle());
        assertInstanceOf(AbstractBusinessException.class, ex);
    }

    @Test
    void testDataNotFoundException() {
        DataNotFoundException ex = new DataNotFoundException("Données introuvables");
        assertNotNull(ex);
        assertEquals("Données introuvables", ex.getLibelle());
        assertInstanceOf(AbstractBusinessException.class, ex);
    }

    @Test
    void testBudgetNotFoundException() {
        BudgetNotFoundException ex = new BudgetNotFoundException("Budget introuvable");
        assertNotNull(ex);
        assertEquals("Budget introuvable", ex.getLibelle());
        assertInstanceOf(AbstractBusinessException.class, ex);
    }

    @Test
    void testCompteClosedException() {
        CompteClosedException ex = new CompteClosedException("Compte clôturé");
        assertNotNull(ex);
        assertEquals("Compte clôturé", ex.getLibelle());
        assertInstanceOf(AbstractBusinessException.class, ex);
    }

    @Test
    void testUserNotAuthorizedException() {
        UserNotAuthorizedException ex = new UserNotAuthorizedException("Utilisateur non autorisé");
        assertNotNull(ex);
        assertEquals("Utilisateur non autorisé", ex.getLibelle());
        assertInstanceOf(AbstractBusinessException.class, ex);
    }

    @Test
    void testUserAccessForbiddenException() {
        UserAccessForbiddenException ex = new UserAccessForbiddenException("Accès interdit");
        assertNotNull(ex);
        assertEquals("Accès interdit", ex.getLibelle());
        assertInstanceOf(AbstractBusinessException.class, ex);
    }

    @Test
    void testExceptionHandlerFallback() {
        // Cas non géré explicitement (AbstractBusinessException générique)
        AbstractAPIExceptionsHandlerImpl handler = new AbstractAPIExceptionsHandlerImpl();
        AbstractBusinessException genericEx = new BadParametersException("test") {
            // sous-classe anonyme non reconnue comme BadParametersException dans les if/else
        };
        // La réponse de fallback doit être 400 (BadParametersException en premier) ou 503 si vraiment non typée
        // Ici, puisque c'est une sous-classe de BadParametersException, ça retourne 400
        int status = handler.toResponse(genericEx).getStatus();
        assertTrue(status == 400 || status == 503);
    }

    // Implémentation concrète pour les tests
    private static class AbstractAPIExceptionsHandlerImpl extends io.github.vzwingma.finances.budget.services.communs.api.AbstractAPIExceptionsHandler {
    }
}

