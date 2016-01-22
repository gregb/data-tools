package com.github.gregb.mapping;

import java.util.Set;

/**
 * Marks a class as having named properties which can be get and set.
 *
 * @author Greg Bódi <gregb@fastmail.fm>
 *
 */
public interface PropertyContainer extends Iterable<String>
{
	public Object get(String propertyName);

	public Set<String> getPropertyNames();

	public void set(String propertyName, Object newValue);

	public Class<?> getPropertyType(String propertyName);

}
