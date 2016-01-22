package com.github.gregb.mapping;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class IntegerIdentifiedEnumMapper {

	public static Table<Class<? extends Enum<?>>, Integer, Enum<?>> table = HashBasedTable.create();

	@SuppressWarnings("unchecked")
	public static <E extends Enum<E> & Identified<Integer>> void add(final E e) {
		table.put((Class<? extends Enum<?>>) e.getClass(), e.getId(), e);
	}

	@SuppressWarnings("unchecked")
	public static <E extends Enum<E> & Identified<Integer>> E lookup(final Class<E> klass, final Integer id) {
		return (E) table.get(klass, id);
	}
}