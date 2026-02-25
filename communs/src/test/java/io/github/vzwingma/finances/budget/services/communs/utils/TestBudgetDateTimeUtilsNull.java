package io.github.vzwingma.finances.budget.services.communs.utils;

import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests complémentaires de BudgetDateTimeUtils couvrant les cas null
 */
class TestBudgetDateTimeUtilsNull {

    @Test
    void testGetSecondsFromLocalDateTimeNull() {
        assertNull(BudgetDateTimeUtils.getSecondsFromLocalDateTime(null));
    }

    @Test
    void testGetMillisecondsFromLocalDateTimeNull() {
        assertNull(BudgetDateTimeUtils.getMillisecondsFromLocalDateTime(null));
    }

    @Test
    void testGetLocalDateTimeFromSecondNull() {
        assertNull(BudgetDateTimeUtils.getLocalDateTimeFromSecond(null));
    }

    @Test
    void testGetLocalDateTimeFromMillisecondNull() {
        assertNull(BudgetDateTimeUtils.getLocalDateTimeFromMillisecond(null));
    }

    @Test
    void testGetNbDayFromLocalDateNull() {
        Long result = BudgetDateTimeUtils.getNbDayFromLocalDate(null);
        assertNull(result);
    }

    @Test
    void testGetLocalDateFromNbDayNull() {
        java.time.LocalDate result = BudgetDateTimeUtils.getLocalDateFromNbDay(null);
        assertNull(result);
    }

    @Test
    void testGetLibelleDateFromMillisNull() {
        assertNull(BudgetDateTimeUtils.getLibelleDateFromMillis(null));
    }

    @Test
    void testGetZIdParis() {
        assertNotNull(BudgetDateTimeUtils.getZIdParis());
        assertEquals("Europe/Paris", BudgetDateTimeUtils.getZIdParis().getId());
    }
}

