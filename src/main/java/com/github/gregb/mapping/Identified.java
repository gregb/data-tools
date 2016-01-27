package com.github.gregb.mapping;

import java.io.Serializable;

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
}
