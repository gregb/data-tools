package com.github.gregb.database;

import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

import javax.persistence.Column;
import javax.persistence.Transient;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import com.github.gregb.database.CopyBehavior.Behavior;
import com.github.gregb.mapping.Converters;
import com.github.gregb.mapping.PropertyContainer;
import com.github.gregb.mapping.PropertyHelper;
import com.github.gregb.mapping.ReflectionHelper;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class RowMappingRepository<T> extends ReflectionHelper<T> {

	protected static final Logger log = LoggerFactory.getLogger(RowMappingRepository.class);

	protected RowMapper<T> rowMapper;
	protected final Collection<String> insertList = Sets.newTreeSet();
	protected final Collection<String> updateList = Sets.newTreeSet();
	protected final Map<String, PropertyHelper> propertiesByColumn = Maps.newHashMap();
	protected final Map<String, String> columnsByPropertyName = Maps.newHashMap();

	private Map<String, Function<?, ?>> converters;

	public static final Map<Class<?>, Class<?>> AUTOMATIC_PARAMETER_CONVERSIONS = Maps.newHashMap();

	static {
		AUTOMATIC_PARAMETER_CONVERSIONS.put(LocalDate.class, Date.class);
		AUTOMATIC_PARAMETER_CONVERSIONS.put(DateTime.class, Timestamp.class);
	}

	public RowMappingRepository(final Class<T> entityClass) {
		super(entityClass);
		scanColumns();
		buildReflectingRowMapper();
	}

	private void buildInitialConverterMap(final ResultSetMetaData metadata) throws SQLException, ClassNotFoundException {

		converters = Maps.newHashMap();

		// we only care about things in the resultset
		final int columns = metadata.getColumnCount();

		for (int i = 1; i < columns + 1; i++) {
			final String columnClassName = metadata.getColumnClassName(i);
			final String columnName = metadata.getColumnName(i);
			final Function<Object, Object> converter = selectConverter(columnClassName, columnName);
			converters.put(columnName, converter);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Function<Object, Object> selectConverter(final String columnClassName, final String columnName) throws ClassNotFoundException {
		final PropertyHelper propertyHelper = propertiesByColumn.get(columnName);

		if (propertyHelper != null) {
			final Class<?> classFieldWants = propertyHelper.getType();
			final Class<?> classColumnHas = Class.forName(columnClassName);

			// handle enums special
			if (Enum.class.isAssignableFrom(classFieldWants)) {
				if (classColumnHas.equals(String.class)) {
					return new Converters.StringToEnum(classFieldWants);
				}
				if (classColumnHas.equals(Integer.class)) {
					return new Converters.IntegerIdToEnum(classFieldWants);
				}
				if (classColumnHas.equals(Long.class)) {
					return new Converters.LongIdToEnum(classFieldWants);
				}

				// postgres returns a PGObject, where the toString() method will get you the enum
				// name
				if (classColumnHas.equals(Object.class)) {
					return new Converters.ObjectToStringToEnum(classFieldWants);
				}

				throw new RuntimeException("Unable to convert enum column " + columnName + ": (" + classColumnHas + " --> " + classFieldWants + ")");

			}

			final Function<Object, Object> converter = Converters.getConverter(classColumnHas, classFieldWants);

			if (converter != null) {
				return converter;
			}

			log.warn("Unable to find converter for column " + columnName + " in " + this.entityClass + ": (" + classColumnHas + " --> " + classFieldWants + ")");
			return o -> o;
		}

		log.warn("Discarding all future values from ResultSet column " + columnName + " because " + this.entityClass + " has no mapping for it");

		return o -> null;
	}

	@SuppressWarnings("unchecked")
	private void buildReflectingRowMapper() {

		this.rowMapper = (rs, rowNum) -> {
			try {
				final ResultSetMetaData resultSetMetaData = rs.getMetaData();

				if (converters == null) {
					buildInitialConverterMap(resultSetMetaData);
				}

				final T instance = entityClass.newInstance();

				for (int i = 1; i < resultSetMetaData.getColumnCount() + 1; i++) {
					final String columnName = resultSetMetaData.getColumnName(i);
					final PropertyHelper propertyHelper = propertiesByColumn.get(columnName);

					// first attempt to have the driver coerce the value to what we expect
					Object columnValue;
					try {
						columnValue = rs.getObject(columnName, propertyHelper.getType());
					} catch (final SQLException e) {
						log.warn("Driver coercion to desired type failed: " + e.getMessage());
						// if that fails, just get it as an object and we can try to coerce it
						// ourselves
						columnValue = rs.getObject(columnName);
					}

					if (propertyHelper != null) {
						if (columnValue != null) {
							Function<Object, Object> converter = (Function<Object, Object>) converters.get(columnName);
							if (converter != null) {
								columnValue = converter.apply(columnValue);
							} else {
								log.trace("No converter found for column " + columnName + "; trying again to see if previous queries missed it");
								final String columnClassName = resultSetMetaData.getColumnClassName(i);
								converter = selectConverter(columnClassName, columnName);

								if (converter != null) {
									columnValue = converter.apply(columnValue);
								} else {
									log.error("No converters found after second try -- attempting to set directly");
								}
							}

							// last attempt
							try {
								propertyHelper.setValue(instance, columnValue);
							} catch (final IllegalArgumentException e1) {
								throw new RuntimeException("Error setting member value on: " + entityClass + "." + columnName + " = " + columnValue + "(" + columnValue.getClass() + ")", e1);
							}
						}
					} else {
						if (instance instanceof PropertyContainer) {
							final PropertyContainer container = (PropertyContainer) instance;
							final String propertyName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, columnName);
							container.set(propertyName, columnValue);
						}
					}
				}

				return instance;
			} catch (final InstantiationException e2) {
				throw new RuntimeException("Error instantiating " + entityClass, e2);
			} catch (final IllegalAccessException e3) {
				throw new RuntimeException("Error instantiating " + entityClass, e3);
			} catch (final InvocationTargetException e4) {
				throw new RuntimeException("Error instantiating " + entityClass, e4);
			} catch (final ClassNotFoundException e5) {
				throw new RuntimeException("Error instantiating " + entityClass, e5);
			}
		};
	}

	public Map<String, ColumnChange> scanForChanges(final T existing, final T updated) {
		return this.scanForChanges(existing, updated, false);
	}

	public Map<String, ColumnChange> scanForChanges(final T existing, final T updated, final boolean deleteOverride) {
		final Map<String, ColumnChange> changes = new TreeMap<String, ColumnChange>();

		this.propertiesByColumn.forEach((columnName, ph) -> {
			try {
				final Column column = ph.getAnnotation(Column.class);
				if (column != null && !column.updatable()) {
					return;
				}

				final Object dbValue = existing == null ? null : ph.getValue(existing);
				final Object updatedValue = updated == null ? null : ph.getValue(updated);
				final String propertyName = ph.getName();

				final CopyBehavior copyBehavior = ph.getAnnotation(CopyBehavior.class);
				final CopyBehavior.Behavior behavior = copyBehavior == null ? Behavior.MOST_RECENT_NON_NULL : copyBehavior.value();

				switch (behavior) {
					case IGNORE:
						// TODO: This had a distinction in validstart/end world, not sure here
					case TAKE_ORIGINAL:
						// column is never updated
						break;
					case TAKE_UPDATED:
						// column is updated if value is different
						if (!Objects.deepEquals(dbValue, updatedValue)) {
							if (updatedValue == null) {
								changes.put(propertyName, ColumnChange.delete(columnName, dbValue));
							} else {
								if (dbValue == null) {
									changes.put(propertyName, ColumnChange.add(columnName, updatedValue));
								} else {
									changes.put(propertyName, ColumnChange.update(columnName, dbValue, updatedValue));
								}
							}

							break;
						}
						break;
					case MOST_RECENT_NON_NULL:
						// if updated value is not null, use that
						// otherwise use existing value
						if (updatedValue != null) {
							if (!Objects.deepEquals(dbValue, updatedValue)) {
								if (dbValue == null) {
									changes.put(propertyName, ColumnChange.add(columnName, updatedValue));
								} else {
									changes.put(propertyName, ColumnChange.update(columnName, dbValue, updatedValue));
								}

								break;
							}
						} else {
							// updated == null
							// normally with MOST_RECENT_NON_NULL this would be ignored, but check
							// override
							// if override and we're not just going from null -> null, issue a
							// delete
							if (dbValue != null && deleteOverride) {
								changes.put(propertyName, ColumnChange.delete(columnName, dbValue));
							}
						}
						break;
					case ALWAYS_NULL:
						// like it says
						if (dbValue != null) {
							changes.put(propertyName, ColumnChange.delete(columnName, dbValue));
							break;
						}
				}
			} catch (final Exception e) {
				throw new QueryConstructionException("Error generating assignment statements for entity update", e);
			}
		});

		return changes;
	}

	private void scanColumns() {

		for (final String propertyName : container.getPropertyNames()) {
			final String columnName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, propertyName);

			// 'class' is never automatically mapped
			if (columnName.equals("class")) {
				continue;
			}

			final PropertyHelper propertyHelper = container.getPropertyHelper(propertyName);

			// only properties not marked with @Transient or @Column
			if (propertyHelper.getAnnotation(Transient.class) == null && propertyHelper.getAnnotation(Column.class) == null) {
				log.trace("Automatically mapping (" + columnName + " --> " + entityClass.getSimpleName() + "." + propertyName + ")");
				propertiesByColumn.put(columnName, propertyHelper);
				columnsByPropertyName.put(propertyName, columnName);
				insertList.add(columnName);
				updateList.add(columnName);
			}

		}

		// override fields marked with @Column
		final Map<String, Column> columns = container.getAnnotations(Column.class, true);

		for (final Entry<String, Column> entry : columns.entrySet()) {
			final String propertyName = entry.getKey();
			final Column column = entry.getValue();
			String columnName = column.name();

			if (columnName.trim().length() == 0) {
				columnName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, propertyName);
				log.info("@Column annotation with no name in entity " + this.entityClass + " -- using generated name of <" + columnName + ">");
			}

			final PropertyHelper propertyHelper = container.getPropertyHelper(propertyName);
			log.trace("Manually mapping (" + columnName + " --> " + entityClass.getSimpleName() + "." + propertyName + ")");
			propertiesByColumn.put(columnName, propertyHelper);
			columnsByPropertyName.put(propertyName, columnName);

			if (column.insertable()) {
				insertList.add(columnName);

				if (column.updatable()) {
					updateList.add(columnName);
				}
			}

		}
	}

	public RowMapper<T> getRowMapper() {
		return this.rowMapper;
	}
}
