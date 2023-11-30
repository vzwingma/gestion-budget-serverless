package io.github.vzwingma.finances.budget.services.communs.utils;

import io.github.vzwingma.finances.budget.services.communs.utils.data.BudgetDateTimeUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author vzwingma
 */
class TestBudgetDateTimeUtils {


    @Test
    void testDates() {

        LocalDate now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate dataUtilsNow = BudgetDateTimeUtils.localDateNow();

        assertEquals(now, dataUtilsNow);

        assertEquals(1, BudgetDateTimeUtils.localDateFirstDayOfMonth().getDayOfMonth());
        assertEquals(now.getMonth(), BudgetDateTimeUtils.localDateFirstDayOfMonth().getMonth());
    }


    @Test
    void testTimeMillisLibelle() {
        LocalDateTime t = BudgetDateTimeUtils.getLocalDateTimeFromMillisecond(Calendar.getInstance().getTimeInMillis());
        assertNotNull(t);
        String libelle = BudgetDateTimeUtils.getLibelleDateFromMillis(Calendar.getInstance().getTimeInMillis());
        assertNotNull(libelle);
    }

    @Test
    void testDateLibelle() {
        String libelle = BudgetDateTimeUtils.getLibelleDate(LocalDateTime.now());
        assertNotNull(libelle);
    }

    @Test
    void testLocalDateTime() {
        LocalDateTime t = LocalDateTime.now();
        t = t.minusNanos(t.getNano());
        Long lt = BudgetDateTimeUtils.getSecondsFromLocalDateTime(t);
        assertNotNull(lt);
        LocalDateTime dt = BudgetDateTimeUtils.getLocalDateTimeFromSecond(lt);
        assertNotNull(dt);
        assertEquals(t, dt);
    }


    @Test
    void testLocalDate() {
        LocalDate t = LocalDate.now();
        Long lt = BudgetDateTimeUtils.getNbDayFromLocalDate(t);
        assertNotNull(lt);
        LocalDate dt = BudgetDateTimeUtils.getLocalDateFromNbDay(lt);
        assertNotNull(dt);
        assertEquals(t, dt);
    }
}
