package io.github.vzwingma.finances.budget.services.communs.api;

import io.github.vzwingma.finances.budget.services.communs.utils.exceptions.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestExceptionHandler {


    @Test
    void testExceptionHandler() {
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        Assertions.assertEquals(404, exceptionHandler.toResponse(new DataNotFoundException("DataNotFoundException")).getStatus());
        Assertions.assertEquals(404, exceptionHandler.toResponse(new BudgetNotFoundException("BudgetNotFoundException")).getStatus());
        Assertions.assertEquals(406, exceptionHandler.toResponse(new CompteClosedException("CompteClosedException")).getStatus());
        Assertions.assertEquals(401, exceptionHandler.toResponse(new UserNotAuthorizedException("UserNotAuthorizedException")).getStatus());
        Assertions.assertEquals(403, exceptionHandler.toResponse(new UserAccessForbiddenException("UserAccessForbiddenException")).getStatus());
        Assertions.assertEquals(400, exceptionHandler.toResponse(new BadParametersException("BadParametersException")).getStatus());
    }

    // Classe concrète de test
    private static class ExceptionHandler extends AbstractAPIExceptionsHandler {
    }
}
