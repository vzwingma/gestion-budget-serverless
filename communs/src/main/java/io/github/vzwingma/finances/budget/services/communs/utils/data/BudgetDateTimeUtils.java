package io.github.vzwingma.finances.budget.services.communs.utils.data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;


/**
 * Utilitaire sur les données
 *
 * @author vzwingma
 */
public class BudgetDateTimeUtils {

    // Format des dates
    public static final String DATE_DAY_PATTERN = "dd/MM/yyyy";
    public static final String DATE_DAY_HOUR_PATTERN = DATE_DAY_PATTERN + " HH:mm";
    public static final String DATE_DAY_HOUR_S_PATTERN = DATE_DAY_HOUR_PATTERN + ":ss";

    public static final String DATE_FULL_TEXT_PATTERN = "dd MMMM yyyy HH:mm";


    private BudgetDateTimeUtils() {
        // Constructeur privé pour classe utilitaire
    }

    public static ZoneId getZIdParis() {
        return ZoneId.of("Europe/Paris");
    }

    /**
     * @return la date actuelle en LocalDate
     */
    public static LocalDate localDateNow() {
        return Instant.now().atZone(getZIdParis()).toLocalDate();
    }

    /**
     * @param localDateTime temps local
     * @return la date actuelle en LocalDate
     */
    public static Long getSecondsFromLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime != null) {
            return localDateTime.atZone(getZIdParis()).toEpochSecond();
        }
        return null;
    }

    /**
     * @param localDateTime temps local
     * @return la date actuelle en LocalDate
     */
    public static Long getMillisecondsFromLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime != null) {
            return localDateTime.atZone(getZIdParis()).toInstant().toEpochMilli();
        }
        return null;
    }

    /**
     * @param longTime temps en s
     * @return la date actuelle en LocalDate
     */
    public static LocalDateTime getLocalDateTimeFromSecond(Long longTime) {
        if (longTime != null) {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(longTime), getZIdParis());
        }
        return null;
    }

    /**
     * @param longTime temps en ms
     * @return la date actuelle en LocalDate
     */
    public static LocalDateTime getLocalDateTimeFromMillisecond(Long longTime) {
        if (longTime != null) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(longTime), getZIdParis());
        }
        return null;
    }


    /**
     * @param localDate temps local
     * @return la date actuelle en LocalDate
     */
    public static Long getNbDayFromLocalDate(LocalDate localDate) {
        if (localDate != null) {
            return localDate.toEpochDay();
        }
        return null;
    }

    /**
     * @param longTime temps en ms
     * @return la date actuelle en LocalDate
     */
    public static LocalDate getLocalDateFromNbDay(Long longTime) {
        if (longTime != null) {
            return LocalDate.ofEpochDay(longTime);
        }
        return null;
    }

    /**
     * @return la date localisée au début du mois
     */
    public static LocalDate localDateFirstDayOfMonth() {
        return localDateNow().withDayOfMonth(1);
    }

    /**
     * @param date date en local
     * @return libellé de la date
     */
    public static String getLibelleDate(LocalDateTime date) {
        DateTimeFormatter sdf = new DateTimeFormatterBuilder()
                .appendPattern(BudgetDateTimeUtils.DATE_FULL_TEXT_PATTERN)
                .toFormatter(Locale.FRENCH);
        return date.format(sdf);
    }


    /**
     * @param dateInMillis date en ms
     * @return libellé de la date
     */
    public static String getLibelleDateFromMillis(Long dateInMillis) {
        if (dateInMillis != null) {
            DateTimeFormatter sdf = new DateTimeFormatterBuilder()
                    .appendPattern(BudgetDateTimeUtils.DATE_DAY_HOUR_S_PATTERN)
                    .toFormatter(Locale.FRENCH);
            return getLocalDateTimeFromMillisecond(dateInMillis).format(sdf);
        }
        return null;
    }
}
