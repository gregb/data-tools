package com.github.gregb.mapping;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * Marks an object as being uniquely identified by a certain value
 *
 * @author Greg Bódi <gregb@fastmail.fm>
 *
 * @param <T>
 *            The type of the unique identifier.
 */
public interface Identified<T extends Comparable<?>> extends Serializable {

	/**
	 * The unique id of this object
	 *
	 * @return
	 */
	public T getId();

	/**
	 * Generic comparator available for Identified objects, when nothing more specific is needed.
	 * Sorts by id in the id
	 * type's natural ascending order.
	 *
	 * @param <C>
	 */
	@SuppressWarnings("serial")
	public static class IdComparator<C extends Comparable<C>> implements Comparator<Identified<C>>, Serializable {

		@Override
		public int compare(final Identified<C> o1, final Identified<C> o2) {
			return o1.getId().compareTo(o2.getId());
		}
	}

	public static class Helper {

		public static Class<?> getIdType(final Class<Identified<?>> o) {
			final PropertyHelper ph = PropertyHelper.getFromClass(o).get("id");
			return ph.getType();
		}

		public static <PK extends Comparable<PK>, T extends Identified<PK>> Map<PK, T> mapById(final Iterable<T> items) {
			return Maps.uniqueIndex(items, new Extractor<PK, T>());
		}
	}

	public static class Extractor<PK extends Comparable<PK>, T extends Identified<PK>> implements Function<T, PK> {

		@Override
		public PK apply(final T arg0) {
			return arg0.getId();
		}

	}
}
