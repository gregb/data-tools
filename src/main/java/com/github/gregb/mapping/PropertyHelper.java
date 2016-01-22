package com.github.gregb.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Reflection utilities
 *
 * @author Greg Bódi <gregb@fastmail.fm>
 *
 */
public class PropertyHelper {

	private static final Logger log = LoggerFactory.getLogger(PropertyHelper.class);

	private final Class<?> type;
	private Field field;
	private Method getter;
	private Method setter;
	private final static Map<Class<?>, Map<String, PropertyHelper>> GLOBAL_PROPERTY_MAP = new HashMap<Class<?>, Map<String, PropertyHelper>>();

	private PropertyHelper(final Field field) {
		this.field = field;
		type = field.getType();
	}

	private PropertyHelper(final Field field, final Method getter, final Method setter) {
		this.field = field;
		this.getter = getter;
		this.setter = setter;

		if (field != null) {
			this.type = field.getType();
		}
		else {
			if (getter != null) {
				this.type = getter.getReturnType();
			}
			else {
				if (setter != null) {
					this.type = setter.getParameterTypes()[0];
				}
				else {
					throw new IllegalArgumentException("One of field,  setter, or getter may not be null");
				}
			}
		}

	}

	/**
	 * The field representing this property, if present. Not guaranteed to be
	 * public, nor settable (public).
	 *
	 * @return
	 */
	public Field getField() {
		return field;
	}

	/**
	 * The getter for this property, if present. Not guaranteed to be invokable
	 * (public).
	 *
	 * @return
	 */
	public Method getGetter() {
		return getter;
	}

	/**
	 * The setter for this property, if present. Not guaranteed to be invokable
	 * (public).
	 *
	 * @return
	 */
	public Method getSetter() {
		return setter;
	}

	@Override
	public String toString() {
		return "Property [field=" + field + ", getter=" + getter + ", setter=" + setter + "]";
	}

	/**
	 * Get the property value from the provided object.
	 *
	 * @param target
	 *            Where to retrieve the property from.
	 * @return The value.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public Object getValue(final Object target) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (getter != null) {
			return getter.invoke(target, (Object[]) null);
		}

		if (field != null) {
			field.setAccessible(true);
			return field.get(target);
		}

		throw new RuntimeException("Can't get value -- no field or getters available: " + this);
	}

	/**
	 * Set the value of this property.
	 *
	 * @param object
	 *            The object to set the property in.
	 * @param value
	 *            The value to set.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public void setValue(final Object object, final Object value) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (setter != null) {
			setter.invoke(object, value);
		}
		else {
			if (field != null) {
				field.setAccessible(true);
				field.set(object, value);
			}
			else {
				// if we got here, and a setter or field does not exist,
				// then a getter only must exist
				log.warn("Attempting to set read-only property. Only a getter exists: " + this.getter.getName());
			}
		}
	}

	/**
	 * Get properties of the type of the parameter. Results are cached, so call
	 * as often as you want.
	 *
	 * @param o
	 * @return
	 */
	public static Map<String, PropertyHelper> getFromInstance(final Object o) {
		return getFromClass(o.getClass());
	}

	/**
	 * Get properties of the given type. Results are cached, so call as often as
	 * you want.
	 *
	 * @param klass
	 *            The class to examine for properties.
	 * @return A map of the property name, to an instance of the helper.
	 */
	public static Map<String, PropertyHelper> getFromClass(final Class<?> klass) {

		if (GLOBAL_PROPERTY_MAP.containsKey(klass)) {
			return GLOBAL_PROPERTY_MAP.get(klass);
		}

		final Map<String, PropertyHelper> propertyList = new LinkedHashMap<String, PropertyHelper>();

		addHelpersFromFields(klass, propertyList, true);

		// but if there is a getter or setter in the super class for the private
		// field, it will be
		// detected in these
		// loops
		for (final Method method : klass.getMethods()) {
			final String methodName = method.getName();
			if (methodName.startsWith("get") && method.getParameterTypes().length == 0) {
				final String propertyName = getPropertyNameFromMethodName(methodName);

				PropertyHelper prop = propertyList.get(propertyName);

				// make an attempt to look for the field
				final Field field = getField(klass, propertyName);

				if (prop == null) {
					prop = new PropertyHelper(field, method, null);
					propertyList.put(propertyName, prop);
				}
				else {
					prop.getter = method;
					prop.field = field;
				}

			}

			if (methodName.startsWith("set") && method.getParameterTypes().length == 1) {
				final String propertyName = getPropertyNameFromMethodName(methodName);

				PropertyHelper prop = propertyList.get(propertyName);

				// make an attempt to look for the field
				final Field field = getField(klass, propertyName);

				if (prop == null) {
					prop = new PropertyHelper(field, null, method);
					propertyList.put(propertyName, prop);
				}
				else {
					prop.setter = method;
					prop.field = field;
				}

			}
		}

		synchronized (GLOBAL_PROPERTY_MAP) {
			GLOBAL_PROPERTY_MAP.put(klass, propertyList);
		}

		return propertyList;
	}

	private static void addHelpersFromFields(final Class<?> klass, final Map<String, PropertyHelper> propertyList, final boolean climb) {

		// this loop will not discover super private fields!
		for (final Field field : klass.getDeclaredFields()) {
			if ((field.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) != 0) {
				// don't process static or final fields
				continue;
			}

			final String fieldName = field.getName();
			final String methodSuffix = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

			try {
				final Method getter = klass.getMethod("get" + methodSuffix, (Class[]) null);
				final Method setter = klass.getMethod("set" + methodSuffix, getter.getReturnType());

				// don't map getClass(), it's pretty useless
				// if you have a PropertyHelper, you know the Class anyway.
				if (!getter.getName().equals("getClass")) {
					propertyList.put(fieldName, new PropertyHelper(field, getter, setter));
				}

			} catch (final NoSuchMethodException e) {
				if ((field.getModifiers() & Modifier.PUBLIC) > 0) {
					propertyList.put(fieldName, new PropertyHelper(field));
				}
			}
		}

		if (climb) {
			final Class<?> superclass = klass.getSuperclass();
			if (!superclass.equals(Object.class)) {
				addHelpersFromFields(klass.getSuperclass(), propertyList, true);
			}

		}
	}

	private static String getPropertyNameFromMethodName(final String name) {
		return Character.toLowerCase(name.charAt(3)) + name.substring(4);
	}

	public Class<?> getType() {
		return type;
	}

	private static Field getField(final Class<?> klass, final String fieldName) {
		try {
			return klass.getDeclaredField(fieldName);
		}
		catch (final NoSuchFieldException e) {
			final Class<?> superClass = klass.getSuperclass();
			if (superClass == null) {
				return null;
			}

			return getField(superClass, fieldName);
		}
	}

	/**
	 * Gets an annotation from the property. Checks fields, setters, and
	 * getters, in that order, for the annotation specified.
	 *
	 * @param <T>
	 *            The annotation type.
	 * @param annotationType
	 *            The type of the annotation
	 * @return The first found instance of that annotation, per above.
	 */
	public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
		T annotation = null;

		if (field != null) {
			annotation = this.field.getAnnotation(annotationType);

			if (annotation != null) {
				return annotation;
			}
		}

		if (setter != null) {
			annotation = setter.getAnnotation(annotationType);

			if (annotation != null) {
				return annotation;
			}
		}

		if (getter != null) {
			return getter.getAnnotation(annotationType);
		}

		return null;
	}

	public <A extends Annotation> Map<String, A> getAnnotations(final Class<A> annotationType, final boolean climbTree) {
		final Map<String, A> annotations = new HashMap<String, A>();

		getAnnotations(this.type, annotationType, annotations, climbTree);

		return annotations;
	}

	public static <A extends Annotation> void getAnnotations(final Class<?> fromClass, final Class<A> annotationType, final Map<String, A> annotations, final boolean climbTree) {
		final Map<String, PropertyHelper> properties = PropertyHelper.getFromClass(fromClass);

		for (final Entry<String, PropertyHelper> entry : properties.entrySet()) {
			final PropertyHelper ph = entry.getValue();
			final A annotation = ph.getAnnotation(annotationType);

			if (annotation != null) {
				annotations.put(entry.getKey(), annotation);
			}
		}

		if (climbTree && !fromClass.getSuperclass().equals(Object.class)) {
			getAnnotations(fromClass.getSuperclass(), annotationType, annotations, true);
		}
	}

	public String getName() {
		if (this.field != null) {
			return field.getName();
		}

		if (this.getter != null) {
			return getPropertyNameFromMethodName(this.getter.getName());
		}

		// we don't support setter-only properties... code will probably never
		// reach here
		if (this.setter != null) {
			return getPropertyNameFromMethodName(this.setter.getName());
		}

		// and if all three were null, we would not have even created this
		// object
		throw new RuntimeException("Unable to get name from field, getter, or setter");
	}

	public class AccessorFunction<S, T> implements Function<S, T> {

		@SuppressWarnings("unchecked")
		@Override
		public T apply(final S source) {
			try {
				return (T) PropertyHelper.this.getValue(source);
			}
			catch (final Throwable e) {
				throw new RuntimeException("Error using: " + PropertyHelper.this);
			}
		}
	}

	public <S, T> Function<S, T> asAccessorFunction(final Class<S> sourceClass, final Class<T> propertyClass) {
		return new AccessorFunction<S, T>();
	}
}
