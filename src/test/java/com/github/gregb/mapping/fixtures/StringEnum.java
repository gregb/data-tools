package com.github.gregb.mapping.fixtures;

import com.github.gregb.mapping.Identified;

public enum StringEnum implements Identified<String> {

	A("FIRST"),
	B("SECOND"),
	C("THIRD");

	private String id;

	private StringEnum(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

}
