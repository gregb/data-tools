package com.github.gregb.mapping;

import java.beans.PropertyEditorSupport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentifiedEnumMapper<U extends Comparable<?>, T extends Identified<U>> extends PropertyEditorSupport {

	private static final Logger log = LoggerFactory.getLogger(IdentifiedEnumMapper.class);

	private final Map<U, T> byCode = new HashMap<U, T>();
	private final Function<Object, Object> converter;
	private final Class<T> typeParameterClass;
	private boolean exceptionOnNotFound = false;

	public IdentifiedEnumMapper(final Class<U> identityClass, Class<T> typeParameterClass) {
		this(identityClass, typeParameterClass, false);
	}

	@SuppressWarnings("unchecked")
	public IdentifiedEnumMapper(final Class<U> identityClass, Class<T> typeParameterClass, boolean exceptionOnNotFound) {
		super();

		this.exceptionOnNotFound = exceptionOnNotFound;
		this.typeParameterClass = typeParameterClass;

		this.converter = Converters.getConverter(String.class, identityClass);
		if (this.converter == null) {
			throw new IllegalArgumentException("Can't map enums when no converter exists for String --> " + identityClass);
		}

		try {
			final Method method = typeParameterClass.getMethod("values");
			final T[] values = (T[]) method.invoke(null);
			for (final T t : values) {
				byCode.put(t.getId(), t);
			}
		}
		catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalArgumentException("Error mapping enums by code for " + typeParameterClass, e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setAsText(String text) throws IllegalArgumentException {

		if (text == null) {
			return;
		}

		// helps with numeric conversions
		text = text.trim();

		final U id = (U) converter.apply(text);

		if (exceptionOnNotFound && id == null) {
			throw new IllegalArgumentException("Could not get an instance of the id class from String " + text + " for enum " + this.typeParameterClass);
		}

		final T value = byCode.get(id);

		if (exceptionOnNotFound && value == null) {
			throw new IllegalArgumentException("Invalid id " + id + " for enum " + this.typeParameterClass);
		}

		setValue(value);

	}
}