package com.github.gregb.database;

import java.util.ArrayList;
import java.util.List;


public class Where {

	private final List<String> clauses = new ArrayList<String>();
	private final String joiner;

	public Where(String joiner) {
		this.joiner = joiner;
	}

	public Where with(String... clauses) {
		for (final String s : clauses) {
			this.clauses.add(s);
		}

		return this;
	}

	public static Where and() {
		return new Where(" AND ");
	}

	public static Where or() {
		return new Where(" OR ");
	}

	public static <T> Where inCollection(String columnName, Iterable<T> items, Class<T> klass) {
		final Where w = Where.or();

		for (final T i : items) {
			if (String.class.isAssignableFrom(klass)) {
				w.clauses.add(columnName + " = '" + i + "'");
				continue;
			}

			if (Number.class.isAssignableFrom(klass)) {
				w.clauses.add(columnName + " = " + i);
				continue;
			}

			// deal with other things as needed
			throw new UnsupportedOperationException("This method does not yet support collections of " + klass);
		}

		return w;
	}

	@Override
	public String toString() {
		return String.join(joiner, clauses);
	}
}
