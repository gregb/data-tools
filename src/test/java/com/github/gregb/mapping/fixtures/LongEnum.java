package com.github.gregb.mapping.fixtures;

import com.github.gregb.mapping.Identified;

public enum LongEnum implements Identified<Long> {

	A(1L),
	B(42L),
	C(-3000L);

	private Long id;

	private LongEnum(Long id) {
		this.id = id;
	}

	@Override
	public Long getId() {
		return id;
	}

}
