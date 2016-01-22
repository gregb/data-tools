package com.github.gregb.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A property container which uses object reflection to get it's values.
 *
 * @author Greg Bódi <gregb@fastmail.fm>
 *
 * @param <T>
 */
public class ObjectBackedPropertyContainer<T> implements PropertyContainer {

	private static final Logger log = LoggerFactory.getLogger(ObjectBackedPropertyContainer.class);

	private T instance;

	private final Class<T> type;
	private final Map<String, PropertyHelper> properties;

	public static <T> ObjectBackedPropertyContainer<T> fromClass(final Class<T> type) {
		final ObjectBackedPropertyContainer<T> obpc = new ObjectBackedPropertyContainer<T>(type, PropertyHelper.getFromClass(type));
		try {
			obpc.instance = type.newInstance();
		}
		catch (final Throwable e) {
			throw new RuntimeException("Error creating container from class " + type + " (No default constructor?)", e);
		}

		return obpc;
	}

	private ObjectBackedPropertyContainer(final Class<T> type, final Map<String, PropertyHelper> properties) {
		this.type = type;
		this.properties = properties;
	}

	/**
	 * Create a new instance.
	 *
	 * @param instance
	 *            The object which properties will be retrieved from.
	 */
	@SuppressWarnings("unchecked")
	public ObjectBackedPropertyContainer(final T instance) {
		this.instance = instance;
		this.type = (Class<T>) instance.getClass();
		this.properties = PropertyHelper.getFromClass(this.type);
	}

	@Override
	public Object get(final String propertyName) {
		try {
			final PropertyHelper propertyHelper = properties.get(propertyName);

			if (propertyHelper == null) {
				throw new IllegalArgumentException("No property found named " + propertyName);
			}

			return propertyHelper.getValue(instance);
		}
		catch (final Throwable e) {
			log.warn("Error trying to get " + propertyName, e);
			return null;
		}
	}

	public <A extends Annotation> A getAnnotation(final String propertyName, final Class<A> annotationType) {
		return this.properties.get(propertyName).getAnnotation(annotationType);
	}

	public <A extends Annotation> Map<String, A> getAnnotations(final Class<A> annotationType, final boolean climbTree) {
		final Map<String, A> annotations = new LinkedHashMap<String, A>();
		PropertyHelper.getAnnotations(this.type, annotationType, annotations, climbTree);
		return annotations;
	}

	public T getInstance() {
		return instance;
	}

	public PropertyHelper getPropertyHelper(final Method method) {
		for (final PropertyHelper helper : properties.values()) {
			if (method.equals(helper.getGetter()) || method.equals(helper.getSetter())) {
				return helper;
			}
		}

		return null;
	}

	public PropertyHelper getPropertyHelper(final String propertyName) {
		return this.properties.get(propertyName);
	}

	@Override
	public Set<String> getPropertyNames() {
		return properties.keySet();
	}

	@Override
	public Class<?> getPropertyType(final String propertyName) {
		final PropertyHelper ph = this.properties.get(propertyName);
		return ph.getType();
	}

	public Class<T> getType() {
		return type;
	}

	@Override
	public void set(final String propertyName, final Object newValue) {
		try {
			final PropertyHelper propertyHelper = properties.get(propertyName);
			if (propertyHelper == null) {
				throw new IllegalArgumentException("No property found named " + propertyName);
			}

			propertyHelper.setValue(this.instance, newValue);
		}
		catch (final Throwable e) {
			log.warn("Error trying to set " + propertyName + " = " + newValue, e);
		}
	}

	public void setInstance(final T instance) {
		this.instance = instance;
	}

	@Override
	public String toString() {
		return "ObjectBackedPropertyContainer: " + this.instance.toString();
	}

	@Override
	public Iterator<String> iterator() {
		return this.properties.keySet().iterator();
	}
}
