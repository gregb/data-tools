package com.github.gregb.database.fixtures;

import javax.persistence.Column;

import com.github.gregb.database.CopyBehavior;
import com.github.gregb.database.CopyBehavior.Behavior;


public class TestObject {

	public static enum TestEnum {
		A,
		B;
	}

	public String s;
	public Long l;
	public Boolean b;
	public int i;
	public Object o;
	public TestEnum e;

	@CopyBehavior(Behavior.ALWAYS_NULL)
	public String alwaysNull;

	@CopyBehavior(Behavior.IGNORE)
	public String ignore;

	@CopyBehavior(Behavior.MOST_RECENT_NON_NULL)
	public String mostRecentNonNull;

	@CopyBehavior(Behavior.TAKE_ORIGINAL)
	public String takeOriginal;

	@CopyBehavior(Behavior.TAKE_UPDATED)
	public String takeUpdated;

	@Column(name = "renamed")
	public String notMyColumnName;

	@Column(name = "not_updatable", updatable = false)
	public String obeysUpdatable;

}
