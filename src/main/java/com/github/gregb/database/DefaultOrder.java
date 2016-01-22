package com.github.gregb.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.domain.Sort.Direction;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface DefaultOrder {

	public int order() default 1;

	public Direction direction() default Direction.ASC;

	public static class Comparator implements java.util.Comparator<DefaultOrder> {
		@Override
		public int compare(DefaultOrder o1, DefaultOrder o2) {
			return o1.order() - o2.order();
		}

	}
}
