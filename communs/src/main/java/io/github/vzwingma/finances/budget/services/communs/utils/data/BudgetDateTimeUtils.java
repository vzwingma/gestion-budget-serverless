package io.github.vzwingma.finances.budget.services.communs.utils.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Utilitaire sur les données
 * @author vzwingma
 *
 */
public class BudgetDateTimeUtils {

	// Format des dates
	public static final String DATE_DAY_PATTERN = "dd/MM/yyyy";
	public static final String DATE_DAY_HOUR_PATTERN = DATE_DAY_PATTERN + " HH:mm";
	public static final String DATE_DAY_HOUR_S_PATTERN = DATE_DAY_HOUR_PATTERN + ":ss";

	public static final String DATE_FULL_TEXT_PATTERN = "dd MMMM yyyy HH:mm";


	private BudgetDateTimeUtils(){
		// Constructeur privé pour classe utilitaire
	}

	public static TimeZone getTzParis(){
		return TimeZone.getTimeZone("Europe/Paris");
	}

	public static ZoneId getZIdParis(){
		return ZoneId.of("Europe/Paris");
	}

	/**
	 * @param utcTime temps en UTC
	 * @return date transformée en local
	 * @throws ParseException erreur de parsing
	 */
	public static String getUtcToLocalTime(String utcTime) throws ParseException{
		SimpleDateFormat sdfutc = new SimpleDateFormat(DATE_DAY_HOUR_PATTERN, Locale.FRENCH);
		sdfutc.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date dateBuild = sdfutc.parse(utcTime);
		SimpleDateFormat sdflocale = new SimpleDateFormat(DATE_DAY_HOUR_PATTERN, Locale.FRENCH);
		sdflocale.setTimeZone(BudgetDateTimeUtils.getTzParis());
		return sdflocale.format(dateBuild);
	}


	/**
	 * @return la date actuelle en LocalDate
	 */
	public static LocalDate localDateNow(){
		return Instant.now().atZone(getZIdParis()).toLocalDate();
	}

	/**
	 * @param localDateTime temps local
	 * @return la date actuelle en LocalDate
	 */
	public static Long getSecondsFromLocalDateTime(LocalDateTime localDateTime){
		if(localDateTime != null){
			return localDateTime.atZone(getZIdParis()).toEpochSecond();
		}
		return null;
	}

	/**
	 * @param localDateTime temps local
	 * @return la date actuelle en LocalDate
	 */
	public static Long getMillisecondsFromLocalDateTime(LocalDateTime localDateTime){
		if(localDateTime != null){
			return localDateTime.atZone(getZIdParis()).toInstant().toEpochMilli();
		}
		return null;
	}

	/**
	 * @param longTime temps en s
	 * @return la date actuelle en LocalDate
	 */
	public static LocalDateTime getLocalDateTimeFromSecond(Long longTime){
		if(longTime != null){
			return LocalDateTime.ofInstant(Instant.ofEpochSecond(longTime), getZIdParis());
		}
		return null;
	}


	public static LocalDateTime getLocalDateTimeFromDHS(String value) throws ParseException {
		SimpleDateFormat sdfutc = new SimpleDateFormat(DATE_DAY_HOUR_S_PATTERN, Locale.FRENCH);
        return Instant.ofEpochMilli( sdfutc.parse(value).getTime() )
                .atZone( getZIdParis() )
                .toLocalDateTime();
	}
	/**
	 * @param longTime temps en ms
	 * @return la date actuelle en LocalDate
	 */
	public static LocalDateTime getLocalDateTimeFromMillisecond(Long longTime){
		if(longTime != null){
			return LocalDateTime.ofInstant(Instant.ofEpochMilli(longTime), getZIdParis());
		}
		return null;
	}


	/**
	 * @param localDate temps local
	 * @return la date actuelle en LocalDate
	 */
	public static Long getNbDayFromLocalDate(LocalDate localDate){
		if(localDate != null){
			return localDate.toEpochDay();
		}
		return null;
	}

	/**
	 * @param longTime temps en ms
	 * @return la date actuelle en LocalDate
	 */
	public static LocalDate getLocalDateFromNbDay(Long longTime){
		if(longTime != null){
			return LocalDate.ofEpochDay(longTime);
		}
		return null;
	}

	/**
	 * @return la date localisée au début du mois
	 */
	public static LocalDate localDateFirstDayOfMonth(){
		return localDateNow().with(ChronoField.DAY_OF_MONTH, 1);
	}

	/**
	 * @param month mois
	 * @return la date localisée en début du mois, au mois positionnée
	 */
	public static LocalDate localDateFirstDayOfMonth(Month month){
		return localDateNow()
				.with(ChronoField.DAY_OF_MONTH, 1)
				.with(ChronoField.MONTH_OF_YEAR, month.getValue());
	}

	/**
	 * @param month mois
	 * @param year année
	 * @return la date localisée en début du mois, au mois positionnée
	 */
	public static LocalDate localDateFirstDayOfMonth(Month month, int year){
		LocalDate date = localDateNow();
		return date
				.with(ChronoField.DAY_OF_MONTH, 1)
				.with(ChronoField.MONTH_OF_YEAR, month.getValue())
				.with(ChronoField.YEAR, year);
	}

	/**
	 * @param date date en local
	 * @return libellé de la date
	 */
	public static String getLibelleDate(LocalDateTime date){
		DateTimeFormatter sdf = new DateTimeFormatterBuilder()
				.appendPattern(BudgetDateTimeUtils.DATE_FULL_TEXT_PATTERN)
				.toFormatter(Locale.FRENCH);
		return date.format(sdf);
	}


	/**
	 * @param dateInMillis date en ms
	 * @return libellé de la date
	 */
	public static String getLibelleDateFromMillis(Long dateInMillis){
		if(dateInMillis != null) {
			DateTimeFormatter sdf = new DateTimeFormatterBuilder()
					.appendPattern(BudgetDateTimeUtils.DATE_DAY_HOUR_S_PATTERN)
					.toFormatter(Locale.FRENCH);
			return getLocalDateTimeFromMillisecond(dateInMillis).format(sdf);
		}
		return null;
	}

	/**
	 * @param date date
	 * @return localdate local date correspondante
	 */
	public static LocalDate asLocalDate(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}

	/**
	 * @param localdate locamdate
	 * @return date local date correspondante
	 */
	public static Date asDate(LocalDate localdate) {
		return new Date(localdate.toEpochDay());
	}
}
