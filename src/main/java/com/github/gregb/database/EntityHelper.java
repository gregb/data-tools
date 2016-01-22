package com.github.gregb.database;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.gregb.database.CopyBehavior.Behavior;
import com.github.gregb.mapping.ObjectBackedPropertyContainer;
import com.github.gregb.mapping.PropertyHelper;
import com.google.common.base.Objects;

/**
 * Helps Entities!
 *
 * @author Greg Bódi <gregb@fastmail.fm>
 *
 */
public class EntityHelper {

	private final static Logger log = LoggerFactory.getLogger(EntityHelper.class);

	/**
	 * Marks empty strings in an object as null, as well as nulling out any
	 * other fields with undesired values.
	 *
	 * @param <T>
	 *            The entity type.
	 * @param originalObject
	 *            The entity to clean up.
	 * @param otherNullValues
	 *            Values which, if encountered, should be set to null too.
	 * @return A *copy* of the original entity, cleaned up.
	 */
	public static <T> T cleanUpEmptyStrings(final T originalObject, final String... otherNullValues) {
		if (originalObject == null) {
			return null;
		}

		final List<String> thingsThatAreNull = Arrays.asList(otherNullValues);

		try {
			final Map<String, PropertyHelper> props = PropertyHelper.getFromInstance(originalObject);

			for (final PropertyHelper prop : props.values()) {
				if (prop.getType().equals(String.class)) {
					String s = (String) prop.getValue(originalObject);

					if (s != null) {
						s = toTrimmedOrNull(s);

						if (s != null) {
							if (thingsThatAreNull.contains(s)) {
								s = null;
							}
						}
					}

					prop.setValue(originalObject, s);
				}
			}
		}
		catch (final Throwable e) {
			throw new RuntimeException("Error while cleaning up object Strings", e);
		}

		return originalObject;
	}

	/**
	 * Returns either null, or a trimmed non-empty String, but never whitespace
	 * or the empty string '';
	 *
	 * @param s
	 * @return
	 */
	public static String toTrimmedOrNull(final String s) {
		if (s == null) {
			return s;
		}

		if (s.length() == 0) {
			return null;
		}

		final String trimmed = s.trim();

		if (trimmed.length() == 0) {
			return null;
		}

		return trimmed;
	}

	public static <T> boolean areEquivalent(final T a, final T b) {

		if (!a.getClass().equals(b.getClass())) {
			return false;
		}

		final ObjectBackedPropertyContainer<?> container = ObjectBackedPropertyContainer.fromClass(a.getClass());

		for (final String propertyName : container) {
			final PropertyHelper propertyHelper = container.getPropertyHelper(propertyName);

			try {
				final Object aValue = propertyHelper.getValue(a);
				final Object bValue = propertyHelper.getValue(b);

				final boolean comparison = Objects.equal(aValue, bValue);
				if (comparison == false) {
					return false;
				}

			}
			catch (final Throwable e) {
				log.error("Error comparing object field in " + propertyName + " objects: \n\t" + a + "\n\t" + b);
			}
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public static <T> T updateMerge(final T existingEntity, final T updatedEntity) {
		try {
			final Map<String, PropertyHelper> columns = PropertyHelper.getFromInstance(updatedEntity);

			final T newObject = (T) existingEntity.getClass().newInstance();

			for (final Entry<String, PropertyHelper> columnEntry : columns.entrySet()) {
				final String fieldName = columnEntry.getKey();
				final PropertyHelper entityColumn = columnEntry.getValue();

				final Object existingValue = entityColumn.getValue(existingEntity);
				Object updatedValue = entityColumn.getValue(updatedEntity);

				if (updatedValue instanceof String) {
					updatedValue = toTrimmedOrNull((String) updatedValue);
				}

				final CopyBehavior copyBehavior = entityColumn.getAnnotation(CopyBehavior.class);
				final CopyBehavior.Behavior behavior = copyBehavior == null ? Behavior.MOST_RECENT_NON_NULL : copyBehavior.value();

				switch (behavior) {
					case IGNORE:
						log.debug("IGNORE - doing nothing to field " + fieldName);
						break;
					case TAKE_UPDATED:
						// new object always gets updated value
						log.debug("TAKE_UPDATED - copying " + fieldName + " = " + updatedValue + " overwriting value " + existingValue);
						entityColumn.setValue(newObject, updatedValue);
						break;
					case TAKE_ORIGINAL:
						// new object always gets existing value
						log.debug("TAKE_ORIGINAL - copying " + fieldName + " = " + existingValue + " ignoring new value " + updatedValue);
						entityColumn.setValue(newObject, existingValue);
						break;
					case MOST_RECENT_NON_NULL:
						// if updated value is not null, use that
						// otherwise use existing value
						if (updatedValue != null) {
							log.debug("MOST_RECENT_NON_NULL - copying " + fieldName + " = " + updatedValue + " overwriting value " + existingValue);
							entityColumn.setValue(newObject, updatedValue);
						}
						else {
							log.debug("MOST_RECENT_NON_NULL - copying " + fieldName + " = " + existingValue + " ignoring new value " + updatedValue);
							entityColumn.setValue(newObject, existingValue);
						}
						break;
					case ALWAYS_NULL:
						// like it says
						log.debug("ALWAYS_NULL - setting " + fieldName + " = null");
						entityColumn.setValue(newObject, null);

				}
			}

			return newObject;
		}
		catch (final Throwable e) {
			log.error("Error copying entity", e);
			throw new RuntimeException("Can't copy entity", e);
		}

	}

}
