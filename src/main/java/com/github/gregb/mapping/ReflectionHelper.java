package com.github.gregb.mapping;

import java.util.function.Consumer;

public class ReflectionHelper<T> {

	protected Class<T> entityClass;
	protected ObjectBackedPropertyContainer<T> container;

	public ReflectionHelper(Class<T> entityClass) {
		this.entityClass = entityClass;
		this.container = ObjectBackedPropertyContainer.fromClass(entityClass);
	}

	public Class<T> getEntityClass() {
		return entityClass;
	}

	public void forEach(Consumer<String> consumer) {
		container.forEach(consumer);
	}

}
