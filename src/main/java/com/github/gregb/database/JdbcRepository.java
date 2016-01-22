package com.github.gregb.database;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.persistence.Id;
import javax.persistence.Table;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.github.gregb.mapping.Converters;
import com.github.gregb.mapping.Identified;
import com.github.gregb.mapping.PropertyHelper;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Iterables;

public abstract class JdbcRepository<T extends Identified<Long>> extends RowMappingRepository<T> {

	@Resource
	protected JdbcTemplate jdbcTemplate;

	@Resource
	protected NamedParameterJdbcTemplate namedTemplate;

	protected String tableName;
	protected String idField;
	protected String idColumn;
	protected String selectById;
	protected String selectByIds;
	protected String selectAll;
	protected String insertStatement;
	protected String updateStatement;
	protected String deleteStatement;
	protected Sort defaultSort;

	private static final Collector<CharSequence, ?, String> SIMPLE_COMMA_JOINER = Collectors.joining(", ");
	private static final Function<String, String> PREPEND_COLON = s -> ":" + s;
	private static final Function<String, String> SET_PARAMETER = s -> s + "= :" + s;

	private static final Map<Class<? extends JdbcRepository<? extends Identified<Long>>>, JdbcRepository<?>> repositoriesByEntity = new HashMap<>();

	@SuppressWarnings("unchecked")
	public JdbcRepository(final Class<T> entityClass) {
		super(entityClass);
		setupId();
		buildQueries();
		repositoriesByEntity.put((Class<? extends JdbcRepository<? extends Identified<Long>>>) entityClass, this);
	}

	@SuppressWarnings("unchecked")
	public static <U extends Identified<Long>> JdbcRepository<U> getRepositoryFor(final Class<U> entityClass) {
		return (JdbcRepository<U>) repositoriesByEntity.get(entityClass);
	}

	protected void buildQueries() {
		final Table table = entityClass.getAnnotation(Table.class);
		this.tableName = table != null ? table.name() : CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, entityClass.getSimpleName());
		this.selectById = "SELECT * FROM " + tableName + " WHERE " + idColumn + " = ?";
		this.selectByIds = "SELECT * FROM " + tableName + " WHERE " + idColumn + " = ANY(?)";
		this.selectAll = "SELECT * FROM " + tableName;

		final StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO ");
		sb.append(this.tableName);
		sb.append(" (");
		sb.append(insertList.stream().collect(SIMPLE_COMMA_JOINER));
		sb.append(") VALUES (");
		sb.append(insertList.stream().map(PREPEND_COLON).collect(SIMPLE_COMMA_JOINER));
		sb.append(")");

		this.insertStatement = sb.toString();

		final StringBuilder sbu = new StringBuilder();
		sb.append("UPDATE ");
		sb.append(this.tableName);
		sb.append(" WHERE ");
		sb.append(this.idColumn);
		sb.append(" = :id SET ");
		sb.append(updateList.stream().map(SET_PARAMETER).collect(SIMPLE_COMMA_JOINER));
		sb.append(";");

		this.updateStatement = sbu.toString();

		final StringBuilder sbd = new StringBuilder("DELETE FROM ");
		sbd.append(this.tableName);
		sbd.append(" WHERE ");
		sbd.append(this.idColumn);
		sbd.append(" = :id;");

		this.deleteStatement = sbd.toString();

		final List<Order> orders = new ArrayList<Order>();

		final Map<String, DefaultOrder> defaultOrderAnnotations = container.getAnnotations(DefaultOrder.class, true);
		final Map<DefaultOrder, String> orderings = new TreeMap<DefaultOrder, String>(new DefaultOrder.Comparator());
		for (final Entry<String, DefaultOrder> e : defaultOrderAnnotations.entrySet()) {
			orderings.put(e.getValue(), e.getKey());
		}

		for (final Entry<DefaultOrder, String> e : orderings.entrySet()) {
			final String propertyName = e.getValue();
			final DefaultOrder defaultOrder = e.getKey();
			final String columnName = columnsByPropertyName.get(propertyName);
			final Direction direction = defaultOrder.direction();
			orders.add(new Order(direction, columnName));
		}

		if (orders.size() == 0) {
			final String idColumn = super.columnsByPropertyName.get(idField);
			log.warn("No @DefaultOrder annotations found in {}, default sort will use @Id column '{}'", idColumn);
			orders.add(new Order(Direction.ASC, idColumn));
		}

		this.defaultSort = new Sort(orders);
	}

	private void setupId() {
		final Map<String, Id> idAnn = container.getAnnotations(Id.class, true);

		if (idAnn.size() > 0) {
			this.idField = Iterables.getOnlyElement(idAnn.keySet());
			this.idColumn = super.columnsByPropertyName.get(this.idField);

			if (idAnn.size() > 1) {
				log.warn("More than one @Id annotation in {} -- using first encountered: {}", this.entityClass, idField);
			}
		} else {
			log.warn("No @Id annotation found in {} assuming 'id'", this.entityClass);
			this.idField = "id";
		}
	}

	protected void appendOrderByClause(final Pageable p, final StringBuilder sb) {

		final Sort currentSort;

		if (p != null && p.getSort() != null) {
			currentSort = p.getSort().and(defaultSort);
		} else {
			currentSort = defaultSort;
		}

		sb.append(" ORDER BY");
		boolean first = true;

		for (final Order o : currentSort) {
			if (!first) {
				sb.append(",");
			}
			sb.append(" ");
			sb.append(o.getProperty());
			sb.append(" ");
			sb.append(o.getDirection());
			first = false;
		}

		if (p != null) {
			sb.append(" LIMIT ");
			sb.append(p.getPageSize());
			sb.append(" OFFSET ");
			sb.append(p.getOffset());
		}
	}

	public T test() {
		final String sql = "SELECT * FROM " + tableName + " LIMIT 1";
		log.trace("SQL OUT: " + sql);
		return jdbcTemplate.queryForObject(sql, rowMapper);
	}

	public Optional<T> findById(final Long id) {
		log.trace("SQL OUT: " + this.selectById + "; id = " + id);

		try {
			final T o = jdbcTemplate.queryForObject(this.selectById, this.rowMapper, id);
			return Optional.ofNullable(o);
		} catch (final EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	public T findByIdOrException(final Long id) {

		try {
			final T o = jdbcTemplate.queryForObject(this.selectById, this.rowMapper, id);
			return o;
		} catch (final EmptyResultDataAccessException e) {
			throw new AccessException(getEntityClass(), id, e);
		}
	}

	protected List<T> findByIds(final Collection<Long> ids) {
		log.trace("SQL OUT: " + this.selectById + "; id = " + ids);
		final List<T> result = jdbcTemplate.query(this.selectByIds, this.rowMapper, ids);
		return result;
	}

	public Long countAll() {
		final String sql = "SELECT COUNT(*) FROM " + tableName;
		log.trace("SQL OUT: " + sql);
		return jdbcTemplate.queryForLong(sql);
	}

	public List<T> findAll() {
		return jdbcTemplate.query(this.selectAll, this.rowMapper);
	}

	public Long countWhere(final String whereClause, final MapSqlParameterSource parameterSource) {
		final String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause;
		log.trace("SQL OUT: " + sql + "; " + paramsToString(parameterSource));
		return namedTemplate.queryForLong(sql, parameterSource);
	}

	public List<T> selectWhere(final String whereClause, final MapSqlParameterSource parameterSource) {
		// default orderings are still applied even when null passed as Pageable
		return selectWhere(whereClause, null, parameterSource);
	}

	public List<T> selectWhere(final String whereClause, final Pageable p, final MapSqlParameterSource parameterSource) {
		final StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM ");
		sb.append(tableName);
		sb.append(" WHERE ");
		sb.append(whereClause);
		appendOrderByClause(p, sb);

		final String sql = sb.toString();
		log.trace("SQL OUT: " + sql + "; " + paramsToString(parameterSource));
		return namedTemplate.query(sql, parameterSource, rowMapper);
	}

	public Long countFrom(final String from, final String whereClause, final MapSqlParameterSource parameterSource) {
		final String sql = "SELECT COUNT(*) FROM " + from + " WHERE " + whereClause;
		log.trace("SQL OUT: " + sql + "; " + paramsToString(parameterSource));
		return namedTemplate.queryForLong(sql, parameterSource);
	}

	public List<T> selectFrom(final String select, final String from, final String whereClause, final MapSqlParameterSource parameterSource) {
		final String sql = "SELECT " + select + " FROM " + from + " WHERE " + whereClause;
		log.trace("SQL OUT: " + sql + "; " + paramsToString(parameterSource));
		return namedTemplate.query(sql, parameterSource, rowMapper);
	}

	public List<T> selectFrom(final String select, final String from, final String whereClause, final Pageable p, final MapSqlParameterSource parameterSource) {
		final StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		sb.append(select);
		sb.append(" FROM ");
		sb.append(from);
		sb.append(" WHERE ");
		sb.append(whereClause);
		appendOrderByClause(p, sb);

		final String sql = sb.toString();
		log.trace("SQL OUT: " + sql + "; " + paramsToString(parameterSource));
		return namedTemplate.query(sql, parameterSource, rowMapper);
	}

	public int update(final String set, final String where, final Map<String, Object> namedParameters) {
		final String sql = "UPDATE " + tableName + " SET " + set + " WHERE " + where;
		log.trace("SQL OUT: " + sql + "; " + namedParameters);
		final int updated = namedTemplate.update(sql, namedParameters);
		return updated;
	}

	public int updatebyId(final T entity) {
		final MapSqlParameterSource parameterSource = buildParameterMapFromObject(entity);
		log.trace("SQL OUT: " + updateStatement + "; " + paramsToString(parameterSource));
		final int updated = namedTemplate.update(updateStatement, parameterSource);
		return updated;
	}

	public int updateById(final Long id, final Map<String, Object> fields) {

		final MapSqlParameterSource parameterSource = new MapSqlParameterSource(idField, id);
		final List<String> updateColumns = new ArrayList<>(fields.size());
		for (final String propertyName : fields.keySet()) {
			final String columnName = columnsByPropertyName.get(propertyName);
			parameterSource.addValue(columnName, fields.get(propertyName));
			updateColumns.add(columnName);
		}

		final String set = updateColumns.stream().map(SET_PARAMETER).collect(SIMPLE_COMMA_JOINER);
		final String sql = "UPDATE " + tableName + " SET " + set + " WHERE " + idField + " = :id";
		log.trace("SQL OUT: " + sql + "; " + parameterSource);

		final int updated = namedTemplate.update(sql, parameterSource);
		return updated;
	}

	public int partialUpdate(final T updated) {

		if (updated.getId() == null) {
			throw new IllegalArgumentException("Can't update entity without id: " + updated);
		}

		log.debug("Updating object with new data:  " + updated);

		final T dbVersion = findByIdOrException(updated.getId());

		if (dbVersion == null) {
			throw new DataRetrievalFailureException("Unable to locate original record for " + updated);
		}

		log.debug("Loaded original instance " + dbVersion);

		final ConvertingSqlParameterSource paramSource = new ConvertingSqlParameterSource("id", dbVersion.getId());
		final StringBuilder sb = new StringBuilder("UPDATE ");
		sb.append(tableName);

		final Map<String, ColumnChange> changes = scanForChanges(dbVersion, updated);

		final String assignmentClause = changes.values().stream().map(c -> c.assignment).collect(Collectors.joining(", "));

		changes.values().stream().forEach(c -> {
			paramSource.addValue(c.parameterName, c.newValue);
		});

		if (changes.size() > 0) {

			sb.append(" SET ");
			sb.append(assignmentClause);
			sb.append(" WHERE ");
			sb.append(idColumn);
			sb.append(" = :id");
			sb.append(";");

			final String sql = sb.toString();

			log.trace("SQL OUT: {}; {}", sql, paramsToString(paramSource));
			return namedTemplate.update(sql, paramSource);
		}

		log.debug("Object not changed, ignoring update request.");
		return 0;
	}

	Array makeJdbcArray(final Object value, final Object[] elements) {

		final Class<?> elementClass = elements[0].getClass();

		try {
			if (Enum.class.isAssignableFrom(elementClass)) {
				return jdbcTemplate.getDataSource().getConnection().createArrayOf("varchar", elements);
			}

			if (String.class.isAssignableFrom(elementClass)) {
				return jdbcTemplate.getDataSource().getConnection().createArrayOf("varchar", elements);
			}

			if (Integer.class.isAssignableFrom(elementClass)) {
				return jdbcTemplate.getDataSource().getConnection().createArrayOf("int4", elements);
			}

			if (Long.class.isAssignableFrom(elementClass)) {
				return jdbcTemplate.getDataSource().getConnection().createArrayOf("int8", elements);
			}

		} catch (final SQLException e) {
			throw new RuntimeException("Can't make jdbc array from elements", e);
		}

		throw new RuntimeException("Array type not supported: " + elementClass);
	}

	public long insert(final T object) {

		final MapSqlParameterSource parameterSource = buildParameterMapFromObject(object);
		final String sql = this.insertStatement;
		final KeyHolder generatedKeyHolder = new GeneratedKeyHolder();

		log.trace("SQL OUT: " + sql);
		namedTemplate.update(sql, parameterSource, generatedKeyHolder, new String[] { idColumn });

		final long newPrimaryKey = generatedKeyHolder.getKey().longValue();
		final PropertyHelper idProperty = this.propertiesByColumn.get(idColumn);

		try {
			idProperty.setValue(object, newPrimaryKey);
		} catch (final Throwable e) {
			throw new RuntimeException("Unable to set id value for object", e);
		}

		return newPrimaryKey;
	}

	protected MapSqlParameterSource buildParameterMapFromObject(final Object object) {

		final MapSqlParameterSource parameterSource = new MapSqlParameterSource();

		for (final Entry<String, PropertyHelper> entry : propertiesByColumn.entrySet()) {
			try {
				final String parameterName = entry.getKey();
				final PropertyHelper propertyHelper = entry.getValue();
				final Object value = propertyHelper.getValue(object);
				final Object convertedValue = convertParameterValue(value);
				parameterSource.addValue(parameterName, convertedValue);
			} catch (final Throwable e) {
				log.error("Unable to convert " + entry.getKey(), e);
			}

		}
		return parameterSource;
	}

	protected MapSqlParameterSource buildParameterMapFromMap(final Map<String, ?> rawParameters) {
		final MapSqlParameterSource parameterSource = new MapSqlParameterSource();

		for (final Entry<String, ?> entry : rawParameters.entrySet()) {
			try {
				final String parameterName = entry.getKey();
				final Object value = entry.getValue();
				final Object convertedValue = convertParameterValue(value);
				parameterSource.addValue(parameterName, convertedValue);
			} catch (final Throwable e) {
				log.error("Unable to convert " + entry.getKey(), e);
			}

		}
		return parameterSource;
	}

	protected class ConvertingSqlParameterSource extends MapSqlParameterSource {

		public ConvertingSqlParameterSource() {
			super();
		}

		public ConvertingSqlParameterSource(final String name, final Object value) {
			super();
			addValue(name, value);
		}

		@Override
		public MapSqlParameterSource addValue(final String paramName, final Object value) {
			final Object convertedValue = convertParameterValue(value);
			return super.addValue(paramName, convertedValue);
		}

		@Override
		public MapSqlParameterSource addValues(final Map<String, ?> values) {
			if (values != null) {
				for (final Map.Entry<String, ?> entry : values.entrySet()) {
					addValue(entry.getKey(), entry.getValue());
					if (entry.getValue() instanceof SqlParameterValue) {
						final SqlParameterValue value = (SqlParameterValue) entry.getValue();
						registerSqlType(entry.getKey(), value.getSqlType());
					}
				}
			}
			return this;
		}

	}

	protected Object convertParameterValue(final Object value) {

		if (value == null) {
			return null;
		}

		if (value instanceof Collection) {
			final Collection<?> collection = (Collection<?>) value;
			final Object[] elements = collection.toArray();
			return makeJdbcArray(value, elements);
		}

		if (value instanceof Object[]) {
			final Object[] elements = (Object[]) value;
			return makeJdbcArray(value, elements);
		}

		if (value instanceof Enum) {

			if (value instanceof Identified) {
				final Identified<?> i = (Identified<?>) value;
				final Comparable<?> id = i.getId();
				final SqlParameterValue spv = new SqlParameterValue(Types.OTHER, id);
				return spv;
			}

			final Enum<?> asEnum = (Enum<?>) value;
			final String asString = asEnum.name();
			final SqlParameterValue spv = new SqlParameterValue(Types.OTHER, asString);
			return spv;
		}

		final Class<?> fromClass = value.getClass();
		final Class<?> toClass = AUTOMATIC_PARAMETER_CONVERSIONS.get(fromClass);

		if (toClass == null) {
			// no conversion necessary
			return value;
		}

		final Function<Object, Object> converter = Converters.getConverter(fromClass, toClass);
		if (converter == null) {
			throw new RuntimeException("Converter required and not found: " + fromClass + " --> " + toClass);
		}

		return converter.apply(value);
	}

	public Map<Long, T> mapById() {
		final List<T> all = findAll();
		final Map<Long, T> map = all.stream().collect(Collectors.toMap(o -> o.getId(), o -> o));
		return map;
	}

	public String getTableName() {
		return tableName;
	}

	private String paramsToString(final MapSqlParameterSource parameterSource) {
		return parameterSource == null ? null : parameterSource.getValues().toString();
	}

	public int deleteById(final Long id) {
		final MapSqlParameterSource parameterSource = new MapSqlParameterSource("id", id);
		log.trace("SQL OUT: " + deleteStatement + "; " + paramsToString(parameterSource));
		final int updated = namedTemplate.update(deleteStatement, parameterSource);
		return updated;
	}
}
