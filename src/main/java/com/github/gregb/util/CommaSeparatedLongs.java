package com.github.gregb.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.converter.Converter;

public class CommaSeparatedLongs {

	public static class FromStringConverter implements Converter<String, CommaSeparatedLongs> {
		@Override
		public CommaSeparatedLongs convert(final String source) {
			return new CommaSeparatedLongs(source);
		}
	};

	public List<Long> longs;

	public CommaSeparatedLongs(final String s) {
		this.longs = parse(s);
	}

	public static List<Long> parse(final String s) {
		final String[] strings = s.split(",");

		final List<Long> list = new ArrayList<Long>(strings.length);
		for (final String item : strings) {
			list.add(Long.parseLong(item));
		}

		return list;
	}

}
