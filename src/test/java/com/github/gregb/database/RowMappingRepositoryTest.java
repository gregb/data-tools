package com.github.gregb.database;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.github.gregb.database.ColumnChange;
import com.github.gregb.database.RowMappingRepository;
import com.github.gregb.database.ColumnChange.Type;
import com.github.gregb.database.fixtures.TestObject;
import com.github.gregb.database.fixtures.TestObject.TestEnum;

public class RowMappingRepositoryTest {

	private RowMappingRepository<TestObject> repo;

	@Test
	public void allAdded() {
		final TestObject existing = new TestObject();

		final TestObject updated = new TestObject();
		updated.b = false;
		updated.e = TestEnum.B;
		updated.i = 2;
		updated.l = 3L;
		updated.o = new BigDecimal(4321);
		updated.s = "DIFFERENT";

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, updated);

		assertNotNull(changes);
		assertEquals(6, changes.size());
	}

	@Test
	public void allChangedNotNull() {
		final TestObject existing = new TestObject();
		existing.b = true;
		existing.e = TestEnum.A;
		existing.i = 1;
		existing.l = 2L;
		existing.o = new BigDecimal(1234);
		existing.s = "test";

		final TestObject updated = new TestObject();
		updated.b = false;
		updated.e = TestEnum.B;
		updated.i = 2;
		updated.l = 3L;
		updated.o = new BigDecimal(4321);
		updated.s = "DIFFERENT";

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, updated);

		assertNotNull(changes);
		assertEquals(6, changes.size());
	}

	@Test
	public void annotationsAllNull() {
		final TestObject existing = new TestObject();
		existing.alwaysNull = "not null";
		existing.ignore = "ignore";
		existing.mostRecentNonNull = "also not null";
		existing.takeOriginal = "original";
		existing.takeUpdated = "update me";

		final TestObject updated = new TestObject();

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, updated);

		assertNotNull(changes);
		assertEquals(2, changes.size());
	}

	@Test
	public void annotationsAllValued() {
		final TestObject existing = new TestObject();
		existing.alwaysNull = "not null";
		existing.ignore = "ignore";
		existing.mostRecentNonNull = "also not null";
		existing.takeOriginal = "original";
		existing.takeUpdated = "update me";

		final TestObject updated = new TestObject();
		updated.alwaysNull = "still not null";
		updated.ignore = "shouldn't be minded";
		updated.mostRecentNonNull = "this is more recent";
		updated.takeOriginal = "not original";
		updated.takeUpdated = "i'm updated";

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, updated);

		assertNotNull(changes);
		assertEquals(3, changes.size());
	}

	@Test
	public void columnNames() {
		final TestObject existing = new TestObject();
		existing.notMyColumnName = "qwerty";

		final TestObject updated = new TestObject();
		updated.notMyColumnName = "asdf";

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, updated);

		assertNotNull(changes);
		assertEquals(1, changes.size());

		final ColumnChange cc = changes.get("notMyColumnName");
		assertNotNull(cc);
		assertEquals(Type.U, cc.type);
		assertEquals("qwerty", cc.oldValue);
		assertEquals("asdf", cc.newValue);
		assertEquals("renamed", cc.parameterName);
		assertEquals("renamed", cc.columnName);
		assertEquals("renamed = :renamed", cc.assignment);
	}

	@Test
	public void mixedUpdateNoOverride() {
		final TestObject existing = new TestObject();
		existing.b = true;
		existing.e = TestEnum.A;
		existing.i = 1;
		existing.l = 2L;
		existing.o = null;
		existing.s = "test";

		final TestObject updated = new TestObject();
		updated.b = true; // true -> true, no change
		updated.e = TestEnum.B; // A -> B, update
		updated.i = 1; // 1 -> 1, no change
		updated.l = null; // 2 -> null, delete, but it defaults to MOST_RECENT_NON_NULL, so it will be ignored
		updated.o = new BigDecimal(1234); // null -> instance, add
		updated.s = "DIFFERENT"; // test -> DIFFERENCE, update

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, updated);

		assertNotNull(changes);
		assertEquals(3, changes.size());

		final ColumnChange e = changes.get("e");
		assertNotNull(e);
		assertEquals(Type.U, e.type);
		assertEquals(TestEnum.A, e.oldValue);
		assertEquals(e.newValue, TestEnum.B);
		assertEquals("e", e.parameterName);
		assertEquals("e", e.columnName);
		assertEquals("e = :e", e.assignment);

		final ColumnChange o = changes.get("o");
		assertNotNull(o);
		assertEquals(Type.A, o.type);
		assertEquals(null, o.oldValue);
		assertEquals(new BigDecimal(1234), o.newValue);
		assertEquals("o", o.parameterName);
		assertEquals("o", o.columnName);
		assertEquals("o = :o", o.assignment);

		final ColumnChange s = changes.get("s");
		assertNotNull(s);
		assertEquals(Type.U, s.type);
		assertEquals("test", s.oldValue);
		assertEquals("DIFFERENT", s.newValue);
		assertEquals("s", s.parameterName);
		assertEquals("s", s.columnName);
		assertEquals("s = :s", s.assignment);

	}

	@Test
	public void mixedUpdateWithOverride() {
		final TestObject existing = new TestObject();
		existing.b = true;
		existing.e = TestEnum.A;
		existing.i = 1;
		existing.l = 2L;
		existing.o = null;
		existing.s = "test";

		final TestObject updated = new TestObject();
		updated.b = true; // true -> true, no change
		updated.e = TestEnum.B; // A -> B, update
		updated.i = 1; // 1 -> 1, no change
		updated.l = null; // 2 -> null, delete
		updated.o = new BigDecimal(1234); // null -> instance, add
		updated.s = "DIFFERENT"; // test -> DIFFERENCE, update

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, updated, true);

		assertNotNull(changes);
		assertEquals(4, changes.size());

		final ColumnChange e = changes.get("e");
		assertNotNull(e);
		assertEquals(Type.U, e.type);
		assertEquals(TestEnum.A, e.oldValue);
		assertEquals(e.newValue, TestEnum.B);
		assertEquals("e", e.parameterName);
		assertEquals("e", e.columnName);
		assertEquals("e = :e", e.assignment);

		final ColumnChange l = changes.get("l");
		assertNotNull(l);
		assertEquals(Type.D, l.type);
		assertEquals(2L, l.oldValue);
		assertEquals(null, l.newValue);
		assertEquals("l", l.parameterName);
		assertEquals("l", l.columnName);
		assertEquals("l = NULL", l.assignment);

		final ColumnChange o = changes.get("o");
		assertNotNull(o);
		assertEquals(Type.A, o.type);
		assertEquals(null, o.oldValue);
		assertEquals(new BigDecimal(1234), o.newValue);
		assertEquals("o", o.parameterName);
		assertEquals("o", o.columnName);
		assertEquals("o = :o", o.assignment);

		final ColumnChange s = changes.get("s");
		assertNotNull(s);
		assertEquals(Type.U, s.type);
		assertEquals("test", s.oldValue);
		assertEquals("DIFFERENT", s.newValue);
		assertEquals("s", s.parameterName);
		assertEquals("s", s.columnName);
		assertEquals("s = :s", s.assignment);

	}

	@Test
	public void noChanges() {
		final TestObject existing = new TestObject();
		existing.b = true;
		existing.e = TestEnum.A;
		existing.i = 1;
		existing.l = 2L;
		existing.o = new BigDecimal(1234);
		existing.s = "test";

		final TestObject updated = new TestObject();
		updated.b = true;
		updated.e = TestEnum.A;
		updated.i = 1;
		updated.l = 2L;
		updated.o = new BigDecimal(1234);
		updated.s = "test";

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, updated);

		assertNotNull(changes);
		assertEquals(0, changes.size());
	}

	@Test
	public void nullExisting() {
		final TestObject existing = null;

		final TestObject updated = new TestObject();
		updated.b = false;
		updated.e = TestEnum.B;
		updated.i = 2;
		updated.l = 3L;
		updated.o = new BigDecimal(4321);
		updated.s = "DIFFERENT";

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, updated);

		assertNotNull(changes);
		assertEquals(6, changes.size());

		changes.values().stream().forEach(c -> {
			assertEquals(Type.A, c.type);
			assertNull(c.oldValue);
			assertNotNull(c.newValue);
		});
	}

	@Test
	public void nullUpdatedNoOverride() {
		final TestObject existing = new TestObject();
		existing.b = true;
		existing.e = TestEnum.A;
		existing.i = 1;
		existing.l = 2L;
		existing.o = new BigDecimal(1234);
		existing.s = "test";

		final TestObject updated = null;

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, updated);

		assertNotNull(changes);
		assertEquals(0, changes.size());
	}

	@Test
	public void nullUpdatedWithOverride() {
		final TestObject existing = new TestObject();
		existing.b = true;
		existing.e = TestEnum.A;
		existing.i = 1;
		existing.l = 2L;
		existing.o = new BigDecimal(1234);
		existing.s = "test";

		final TestObject updated = null;

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, updated, true);

		assertNotNull(changes);
		assertEquals(6, changes.size());

		changes.values().stream().forEach(c -> {
			assertEquals(Type.D, c.type);
			assertNotNull(c.oldValue);
			assertNull(c.newValue);
		});
	}

	@Test
	public void sameObject() {
		final TestObject existing = new TestObject();
		existing.b = true;
		existing.e = TestEnum.A;
		existing.i = 1;
		existing.l = 2L;
		existing.o = new BigDecimal(1234);
		existing.s = "test";

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, existing);

		assertNotNull(changes);
		assertEquals(0, changes.size());
	}

	@Before
	public void setUp() throws Exception {
		repo = new RowMappingRepository<TestObject>(TestObject.class);
	}

	@Test
	public void updatable() {
		final TestObject existing = new TestObject();
		existing.obeysUpdatable = "original";

		final TestObject updated = new TestObject();
		updated.obeysUpdatable = "new value";

		final Map<String, ColumnChange> changes = repo.scanForChanges(existing, updated);

		assertNotNull(changes);
		assertEquals(0, changes.size());
	}

}
