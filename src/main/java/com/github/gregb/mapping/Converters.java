package com.github.gregb.mapping;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * A home, and repository, for stateless type conversion functions.
 *
 * @author Greg Bodi
 *
 */
public class Converters {

	private static final Logger log = LoggerFactory.getLogger(Converters.class);

	private static Table<Class<?>, Class<?>, Function<?, ?>> repository = HashBasedTable.create();

	private static final List<DateTimeFormatter> formatterParsers = new LinkedList<DateTimeFormatter>();

	/*
	 * Possible patterns for parsing. Order is important!
	 *
	 * The first pattern is the most common input pattern AND the output pattern. The remaining patterns are input only and will be tried in order. Specify most common expected patterns first for best
	 * performance.
	 */
	private static String[] patterns = { "MM/dd/yyyy", "MM-dd-yyyy", "MM dd yyyy", "MMM dd yyyy", "dd MMM yyyy", "dd-MMM-yyyy", "dd/MMM/yyyy", "ddMMMyyyy", "yyyy/MM/dd", "yyyy-MM-dd", "yyyy MM dd", "yyyyMMdd" };

	static {

		for (final String pattern : patterns) {
			formatterParsers.add(DateTimeFormat.forPattern(pattern));
		}

		formatterParsers.add(ISODateTimeFormat.dateTime());
		formatterParsers.add(ISODateTimeFormat.dateTimeNoMillis());

		repository.put(Long.class, Integer.class, new LongToInteger());
		repository.put(Integer.class, Long.class, new IntegerToLong());
		repository.put(Enum.class, String.class, new EnumToString());
		repository.put(String.class, Integer.class, new StringToInteger());
		repository.put(Integer.class, String.class, new IntegerToString());
		repository.put(String.class, Long.class, new StringToLong());
		repository.put(Long.class, String.class, new LongToString());
		repository.put(ReadableInstant.class, String.class, new InstantToString());
		repository.put(ReadablePartial.class, String.class, new PartialToString());
		repository.put(String.class, LocalDate.class, new StringToLocalDate());
		repository.put(String.class, DateTime.class, new StringToDateTime());
		repository.put(Date.class, LocalDate.class, new DateToLocalDate());
		repository.put(Date.class, DateTime.class, new DateToDateTime());
		repository.put(Timestamp.class, DateTime.class, new TimestampToDateTime());
		repository.put(DateTime.class, Timestamp.class, new DateTimeToTimestamp());
		repository.put(LocalDate.class, Date.class, new LocalDateToDate());
		repository.put(DateTime.class, LocalDate.class, new DateTimeToLocalDate());
		repository.put(LocalDate.class, DateTime.class, new LocalDateToDateTime());
		repository.put(BigDecimal.class, DateTime.class, new BigDecimalToDateTime());
		repository.put(BigDecimal.class, Long.class, new BigDecimalToLong());
		repository.put(Character.class, String.class, new CharacterToString());
		repository.put(String.class, Character.class, new StringToCharacter());

	}

	public static <Source, Target> void register(final Class<Source> sourceClass, final Class<Target> targetClass, final Function<Source, Target> converter) {
		repository.put(sourceClass, targetClass, converter);
	}

	@SuppressWarnings("unchecked")
	public static <Source, Target> Function<Object, Object> getConverter(final Class<Source> sourceClass, final Class<Target> targetClass) {

		if (sourceClass.equals(targetClass)) {
			return o -> o;
		}

		Function<?, ?> converter = repository.get(sourceClass, targetClass);

		if (converter != null) {
			return (Function<Object, Object>) converter;
		}

		final Set<Class<?>> rows = repository.rowKeySet();
		final Set<Class<?>> columns = repository.columnKeySet();

		for (final Class<?> rowClass : rows) {
			for (final Class<?> columnClass : columns) {
				converter = repository.get(rowClass, columnClass);
				if (converter != null) {
					if (rowClass.isAssignableFrom(sourceClass)) {
						if (columnClass.isAssignableFrom(targetClass)) {
							// compatible converter found
							repository.put(sourceClass, targetClass, converter);
							log.warn("Converter mapping added: " + sourceClass + " --> " + targetClass + " = " + converter.getClass());
							log.info("Consider adding this to the hard coded mapping so that searching is not required");
							return (Function<Object, Object>) converter;
						}
					}
				}
			}
		}

		// last effort, if destination is a string
		if (targetClass.equals(String.class)) {
			return o -> o.toString();
		}

		return null;
	}

	public static class LongIdToEnum<T extends Enum<T> & Identified<Long>> implements Function<Long, Enum<T>> {

		private final Class<T> enumType;

		public LongIdToEnum(final Class<T> enumType) {
			assert enumType != null;
			this.enumType = enumType;
		}

		@Override
		public T apply(final Long id) {

			assert id != null;
			final T t = LongIdentifiedEnumMapper.lookup(enumType, id);

			if (t == null) {
				throw new IllegalArgumentException(enumType + " has no member with id " + id);
			}

			return t;
		}
	}

	public static class IntegerIdToEnum<T extends Enum<T> & Identified<Integer>> implements Function<Integer, Enum<T>> {

		private final Class<T> enumType;

		public IntegerIdToEnum(final Class<T> enumType) {
			assert enumType != null;
			this.enumType = enumType;
		}

		@Override
		public T apply(final Integer id) {

			assert id != null;
			final T t = IntegerIdentifiedEnumMapper.lookup(enumType, id);

			if (t == null) {
				throw new IllegalArgumentException(enumType + " has no member with id " + id);
			}

			return t;
		}
	}

	public static class StringToEnum<T extends Enum<T>> implements Function<String, Enum<T>> {

		private final Class<T> enumType;

		public StringToEnum(final Class<T> enumType) {
			assert enumType != null;
			this.enumType = enumType;
		}

		@Override
		public T apply(final String s) {
			try {
				assert s != null;
				return Enum.valueOf(enumType, s);
			} catch (final IllegalArgumentException e) {
				try {
					return Enum.valueOf(enumType, s.toUpperCase());
				} catch (final Exception e1) {
					throw new IllegalArgumentException(enumType + " has no member named <" + s + ">", e);
				}
			}
		}
	}

	public static class EnumToString implements Function<Enum<?>, String> {

		@Override
		public String apply(final Enum<?> e) {
			return e.name();
		}
	}

	public static class ObjectToStringToEnum<T extends Enum<T>> implements Function<Object, Enum<T>> {

		private final Class<T> enumType;

		public ObjectToStringToEnum(final Class<T> enumType) {
			assert enumType != null;
			this.enumType = enumType;
		}

		@Override
		public T apply(final Object o) {
			try {
				assert o != null;
				return Enum.valueOf(enumType, o.toString());
			} catch (final IllegalArgumentException e) {
				try {
					return Enum.valueOf(enumType, o.toString().toUpperCase());
				} catch (final Exception e1) {
					throw new IllegalArgumentException(enumType + " has no member named <" + o.toString() + ">", e);
				}
			}
		}
	}

	public static class LongToInteger implements Function<Long, Integer> {

		@Override
		public Integer apply(final Long s) {
			return s.intValue();
		}
	}

	public static class IntegerToLong implements Function<Integer, Long> {

		@Override
		public Long apply(final Integer i) {
			return i.longValue();
		}
	}

	public static class StringToInteger implements Function<String, Integer> {

		@Override
		public Integer apply(final String s) {
			return Integer.parseInt(s);
		}
	}

	public static class IntegerToString implements Function<Integer, String> {

		@Override
		public String apply(final Integer i) {
			return Integer.toString(i);
		}
	}

	public static class StringToLong implements Function<String, Long> {

		@Override
		public Long apply(final String s) {
			if (s == null || s.trim().length() == 0) {
				return null;
			}
			return Long.parseLong(s);
		}
	}

	public static class LongToString implements Function<Long, String> {

		@Override
		public String apply(final Long i) {
			return Long.toString(i);
		}
	}

	public static class InstantToString implements Function<ReadableInstant, String> {

		@Override
		public String apply(final ReadableInstant value) {
			if (value == null) {
				return null;
			}

			try {
				return formatterParsers.get(0).print(value);
			} catch (final Throwable e) {
				// This may not even be possible...
				// As long as the instant is not null, i am not sure what would
				// prevent it from being converted to a string.
				throw new IllegalArgumentException("Cannot convert " + value + " to a string", e);
			}
		}
	}

	public static class PartialToString implements Function<ReadablePartial, String> {

		@Override
		public String apply(final ReadablePartial value) {
			if (value == null) {
				return null;
			}

			try {
				return formatterParsers.get(0).print(value);
			} catch (final Throwable e) {
				// This may not even be possible...
				// As long as the instant is not null, i am not sure what would
				// prevent it from being converted to a string.
				throw new IllegalArgumentException("Cannot convert " + value + " to a string", e);
			}
		}
	}

	public static class StringToLocalDate implements Function<String, LocalDate> {

		@Override
		public LocalDate apply(final String value) {

			if (value == null || value.trim().equals("")) {
				return null;
			}

			for (final DateTimeFormatter formatter : formatterParsers) {
				try {
					final DateTime parsed = formatter.parseDateTime(value).withZoneRetainFields(DateTimeZone.UTC);
					return parsed.toLocalDate();
				} catch (final Throwable e) {
					log.trace("Error parsing DateTime " + value + " with formatter " + formatter + "; trying next pattern");
				}
			}

			log.error("Could not convert string to DateTime -- no patterns were able to decode " + value);
			throw new IllegalArgumentException("Could not convert string to DateTime -- no patterns were able to decode " + value);
		}
	}

	public static class StringToDateTime implements Function<String, DateTime> {

		@Override
		public DateTime apply(final String value) {

			if (value.trim().equals("")) {
				return null;
			}

			for (final DateTimeFormatter formatter : formatterParsers) {
				try {

					// TODO: This assumes all times entered are local times.
					// Determine if this is correct.
					final DateTime parsed = formatter.parseDateTime(value).withZoneRetainFields(DateTimeZone.UTC);
					return parsed;

				} catch (final Throwable e) {
					log.trace("Error parsing DateTime " + value + " with formatter " + formatter + "; trying next pattern");
				}
			}

			log.error("Could not convert string to DateTime -- no patterns were able to decode " + value);
			throw new IllegalArgumentException("Could not convert string to DateTime -- no patterns were able to decode " + value);
		}
	}

	public static class DateToLocalDate implements Function<Date, LocalDate> {

		@Override
		public LocalDate apply(final Date value) {
			return new LocalDate(value);
		}
	}

	public static class LocalDateToDate implements Function<LocalDate, Date> {

		@Override
		public Date apply(final LocalDate value) {
			return new Date(value.toDateTimeAtStartOfDay().getMillis());
		}
	}

	public static class DateToDateTime implements Function<Date, DateTime> {

		@Override
		public DateTime apply(final Date value) {
			return new DateTime(value);
		}
	}

	public static class TimestampToDateTime implements Function<Timestamp, DateTime> {

		@Override
		public DateTime apply(final Timestamp value) {
			return new DateTime(value);
		}
	}

	public static class DateTimeToTimestamp implements Function<DateTime, Timestamp> {

		@Override
		public Timestamp apply(final DateTime value) {
			return new Timestamp(value.getMillis());
		}
	}

	public static class LocalDateToDateTime implements Function<LocalDate, DateTime> {

		@Override
		public DateTime apply(final LocalDate value) {
			return value.toDateTimeAtStartOfDay();
		}
	}

	public static class DateTimeToLocalDate implements Function<DateTime, LocalDate> {

		@Override
		public LocalDate apply(final DateTime value) {
			return value.toLocalDate();
		}
	}

	public static class BigDecimalToDateTime implements Function<BigDecimal, DateTime> {

		@Override
		public DateTime apply(final BigDecimal value) {
			return value == null ? null : new DateTime(value.longValue());
		}
	}

	public static class BigDecimalToLong implements Function<BigDecimal, Long> {

		@Override
		public Long apply(final BigDecimal value) {
			return value == null ? null : value.longValue();
		}
	}

	public static class CharacterToString implements Function<Character, String> {

		@Override
		public String apply(final Character value) {
			return value == null ? null : value.toString();
		}
	}

	public static class StringToCharacter implements Function<String, Character> {

		@Override
		public Character apply(final String value) {
			if (value == null) {
				return null;
			}

			final Character c = value.charAt(0);

			if (value.length() > 1) {
				log.warn("Data loss in String --> Character conversion: " + value + " --> " + c);
			}

			return c;
		}
	}

	public static class Wrapper implements Function<Object, String> {

		public Wrapper(final String wrapBoth) {
			this.wrapLeft = wrapBoth;
			this.wrapRight = wrapBoth;
		}

		public Wrapper(final String wrapLeft, final String wrapRight) {
			this.wrapLeft = wrapLeft;
			this.wrapRight = wrapRight;
		}

		private final String wrapLeft;
		private final String wrapRight;

		@Override
		public String apply(final Object o) {
			return wrapLeft + o.toString() + wrapRight;
		}
	}
}
