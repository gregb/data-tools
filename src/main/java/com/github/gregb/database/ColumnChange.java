package com.github.gregb.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColumnChange {

	private static final Logger log = LoggerFactory.getLogger(ColumnChange.class);

	public static enum Type {
		A,
		D,
		U;
	}

	public Type type;
	public String columnName;
	public String parameterName;
	public Object oldValue;
	public Object newValue;
	public String assignment;

	public static ColumnChange add(String columnName, Object newValue) {
		final ColumnChange change = new ColumnChange();

		change.type = Type.A;
		change.columnName = columnName;
		change.parameterName = columnName;
		change.oldValue = null;
		change.newValue = newValue;
		change.assignment = change.columnName + " = :" + change.parameterName;

		log.debug("Generated " + change);
		return change;
	}

	public static ColumnChange delete(String columnName, Object oldValue) {
		final ColumnChange change = new ColumnChange();

		change.type = Type.D;
		change.columnName = columnName;
		change.parameterName = columnName; // doesn't matter
		change.oldValue = oldValue;
		change.newValue = null;
		change.assignment = change.columnName + " = NULL";

		log.debug("Generated " + change);
		return change;
	}

	public static ColumnChange update(String columnName, Object oldValue, Object newValue) {
		final ColumnChange change = new ColumnChange();

		change.type = Type.U;
		change.columnName = columnName;
		change.parameterName = columnName;
		change.oldValue = oldValue;
		change.newValue = newValue;
		change.assignment = change.columnName + " = :" + change.parameterName;

		log.debug("Generated " + change);
		return change;
	}

	@Override
	public String toString() {
		return "ColumnChange [type=" + type + ", columnName=" + columnName + ", parameterName=" + parameterName + ", oldValue=" + oldValue + ", newValue=" + newValue + ", assignment=" + assignment + "]";
	}

}
