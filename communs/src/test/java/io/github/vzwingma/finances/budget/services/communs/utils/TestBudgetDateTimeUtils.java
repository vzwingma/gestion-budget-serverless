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


    @Test
    void localDateNowReturnsCurrentDate() {
        LocalDate expected = LocalDate.now(ZoneId.of("Europe/Paris"));
        LocalDate actual = BudgetDateTimeUtils.localDateNow();
        assertEquals(expected, actual);
    }

    @Test
    void getSecondsFromLocalDateTimeReturnsCorrectSeconds() {
        LocalDateTime now = LocalDateTime.now();
        Long expected = now.atZone(ZoneId.of("Europe/Paris")).toEpochSecond();
        Long actual = BudgetDateTimeUtils.getSecondsFromLocalDateTime(now);
        assertEquals(expected, actual);
    }

    @Test
    void getMillisecondsFromLocalDateTimeReturnsCorrectMilliseconds() {
        LocalDateTime now = LocalDateTime.now();
        Long expected = now.atZone(ZoneId.of("Europe/Paris")).toInstant().toEpochMilli();
        Long actual = BudgetDateTimeUtils.getMillisecondsFromLocalDateTime(now);
        assertEquals(expected, actual);
    }

    @Test
    void getLocalDateTimeFromSecondReturnsCorrectLocalDateTime() {
        Long nowInSeconds = LocalDateTime.now().atZone(ZoneId.of("Europe/Paris")).toEpochSecond();
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochSecond(nowInSeconds), ZoneId.of("Europe/Paris"));
        LocalDateTime actual = BudgetDateTimeUtils.getLocalDateTimeFromSecond(nowInSeconds);
        assertEquals(expected, actual);
    }

    @Test
    void getLocalDateTimeFromMillisecondReturnsCorrectLocalDateTime() {
        Long nowInMilliseconds = LocalDateTime.now().atZone(ZoneId.of("Europe/Paris")).toInstant().toEpochMilli();
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowInMilliseconds), ZoneId.of("Europe/Paris"));
        LocalDateTime actual = BudgetDateTimeUtils.getLocalDateTimeFromMillisecond(nowInMilliseconds);
        assertEquals(expected, actual);
    }
}
