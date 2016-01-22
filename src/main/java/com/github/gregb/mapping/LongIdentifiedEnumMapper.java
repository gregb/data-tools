package com.github.gregb.mapping;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class LongIdentifiedEnumMapper {

	public static Table<Class<? extends Enum<?>>, Long, Enum<?>> table = HashBasedTable.create();

	@SuppressWarnings("unchecked")
	public static <E extends Enum<E> & Identified<Long>> void add(final E e) {
		table.put((Class<? extends Enum<?>>) e.getClass(), e.getId(), e);
	}

	@SuppressWarnings("unchecked")
	public static <E extends Enum<E> & Identified<Long>> E lookup(final Class<E> klass, final Long id) {
		return (E) table.get(klass, id);
	}
}