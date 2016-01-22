package com.github.gregb.mapping;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A property container that uses a map to store and retrieve it's values.
 *
 * @author Greg Bódi <gregb@fastmail.fm>
 *
 */
@SuppressWarnings("unchecked")
public class MapBackedPropertyContainer implements PropertyContainer
{
	private Map map;

	public MapBackedPropertyContainer()
	{
		map = new LinkedHashMap();
	}

	public MapBackedPropertyContainer(final Map map)
	{
		this.map = map;
	}

	@Override
	public Object get(final String propertyName)
	{
		return map.get(propertyName);
	}

	public Map getMap()
	{
		return map;
	}

	@Override
	public Set getPropertyNames()
	{
		return map.keySet();
	}

	@Override
	public void set(final String propertyName, final Object newValue)
	{
		map.put(propertyName, newValue);
	}

	public void setMap(final Map data)
	{
		map = data;
	}

	@Override
	public Class<?> getPropertyType(final String propertyName)
	{
		return map.get(propertyName).getClass();
	}

	@Override
	public Iterator<String> iterator()
	{
		return this.map.keySet().iterator();
	}
}
