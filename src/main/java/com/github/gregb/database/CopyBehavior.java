package com.github.gregb.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify how a field should behave when a dao.update() is requested, and the entity must be copied
 * for insertion into a new row.
 *
 * @author Greg Bódi <gregb@fastmail.fm>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface CopyBehavior {

	/**
	 * The behavior type to use when copying the annotated field.
	 *
	 * @return
	 */
	public Behavior value() default Behavior.MOST_RECENT_NON_NULL;

	public static enum Behavior {
		/**
		 * The IGNORE behavior tells the copier that this field should be skipped over. Used
		 * internally for super classes whose members should not override whichever annotations
		 * subclasses have used
		 */
		IGNORE,
		/**
		 * The TAKE_UPDATED behavior will always use the newest value for the
		 * field, even if the old row had a non-null value, and the new row will
		 * have a null value. Use this when the columns allows nulls, and
		 * changing a value to null is an expected and required action.
		 */
		TAKE_UPDATED,
		/**
		 * The TAKE_ORIGINAL behavior will always use the original value,
		 * regardless of what the new field is set to. Use this for values which
		 * must carry over in all instances of the row, such as validstart,
		 * createuser, or claim focus ids.
		 */
		TAKE_ORIGINAL,
		/**
		 * The ALWAYS_NULL behavior will always set the value to null when
		 * copying an entity for update. Use for fields which are to be assigned
		 * by the database, or for hibernate ids where null indicates the row
		 * must be inserted instead of updated.
		 */
		ALWAYS_NULL,
		/**
		 * The MOST_RECENT_NON_NULL behavior will the original value, if the new
		 * value is null, or the new value if the new value is not null. This is
		 * the default behavior. It allows updating of a previously null value,
		 * but does not allow nullification of a populated value.
		 */
		MOST_RECENT_NON_NULL;
	}
}
