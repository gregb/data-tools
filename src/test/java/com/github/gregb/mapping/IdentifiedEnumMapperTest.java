package com.github.gregb.mapping;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.github.gregb.mapping.Identified;
import com.github.gregb.mapping.IdentifiedEnumMapper;
import com.github.gregb.mapping.fixtures.LongEnum;
import com.github.gregb.mapping.fixtures.StringEnum;

public class IdentifiedEnumMapperTest {

	public static class Dummy implements Comparable<Dummy> {

		@Override
		public int compareTo(final Dummy o) {
			return 0;
		}

	}

	public static enum UnconvertibleIdentityEnum implements Identified<Dummy> {

		A,
		B,
		C;

		@Override
		public Dummy getId() {
			return null;
		}
	}

	private IdentifiedEnumMapper<Long, LongEnum> longMapper;
	private IdentifiedEnumMapper<String, StringEnum> stringMapper;

	@Before
	public void setUp() throws Exception {
		longMapper = new IdentifiedEnumMapper<Long, LongEnum>(Long.class, LongEnum.class);
		stringMapper = new IdentifiedEnumMapper<String, StringEnum>(String.class, StringEnum.class);
	}

	@Test
	public void testLong() {

		longMapper.setAsText(null);
		final LongEnum t1 = (LongEnum) longMapper.getValue();
		assertNull(t1);

		longMapper.setAsText("");
		final LongEnum t2 = (LongEnum) longMapper.getValue();
		assertNull(t2);

		longMapper.setAsText(" ");
		final LongEnum t3 = (LongEnum) longMapper.getValue();
		assertNull(t3);

		longMapper.setAsText("0");
		final LongEnum t4 = (LongEnum) longMapper.getValue();
		assertNull(t4);

		longMapper.setAsText("1 ");
		final LongEnum t10 = (LongEnum) longMapper.getValue();
		assertSame(LongEnum.A, t10);

		longMapper.setAsText("1");
		final LongEnum t11 = (LongEnum) longMapper.getValue();
		assertSame(LongEnum.A, t11);

		longMapper.setAsText(" 42");
		final LongEnum t12 = (LongEnum) longMapper.getValue();
		assertSame(LongEnum.B, t12);

		longMapper.setAsText("-3000");
		final LongEnum t13 = (LongEnum) longMapper.getValue();
		assertSame(LongEnum.C, t13);

	}

	@Test
	public void testString() {

		stringMapper.setAsText(null);
		final StringEnum t1 = (StringEnum) stringMapper.getValue();
		assertNull(t1);

		stringMapper.setAsText("");
		final StringEnum t2 = (StringEnum) stringMapper.getValue();
		assertNull(t2);

		stringMapper.setAsText(" ");
		final StringEnum t3 = (StringEnum) stringMapper.getValue();
		assertNull(t3);

		stringMapper.setAsText("0");
		final StringEnum t4 = (StringEnum) stringMapper.getValue();
		assertNull(t4);

		stringMapper.setAsText("FIRST");
		final StringEnum t10 = (StringEnum) stringMapper.getValue();
		assertSame(StringEnum.A, t10);

		stringMapper.setAsText("FIRST\t");
		final StringEnum t11 = (StringEnum) stringMapper.getValue();
		assertSame(StringEnum.A, t11);

		stringMapper.setAsText("  SECOND");
		final StringEnum t12 = (StringEnum) stringMapper.getValue();
		assertSame(StringEnum.B, t12);

		stringMapper.setAsText("THIRD");
		final StringEnum t13 = (StringEnum) stringMapper.getValue();
		assertSame(StringEnum.C, t13);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnconvertable() {
		new IdentifiedEnumMapper<IdentifiedEnumMapperTest.Dummy, IdentifiedEnumMapperTest.UnconvertibleIdentityEnum>(Dummy.class, UnconvertibleIdentityEnum.class);

	}

}
