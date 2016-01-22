package com.github.gregb.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.gregb.mapping.Described;
import com.github.gregb.mapping.Identified;

public class JSSelectData {

	public static class Label {

		public String primary;
		public String secondary;
		public String tooltip;

		public static Label ANY = new Label();

		static {
			ANY.primary = "(Any)";
			ANY.tooltip = "Matches any selection";
		}
	}

	public Map<String, Label> labels = new HashMap<String, Label>();
	public List<String> order = new ArrayList<String>();

	public static <T extends Described> JSSelectData fromDescribed(final Map<?, T> items, final Label defaultLabel) {

		final JSSelectData data = new JSSelectData();

		if (defaultLabel != null) {
			data.order.add("");
			data.labels.put("", defaultLabel);
		}

		for (final Entry<?, T> e : items.entrySet()) {
			final String key = e.getKey().toString();
			data.order.add(key);
			final Label label = new Label();
			label.primary = e.getValue().getDescription();
			data.labels.put(key, label);
		}

		return data;
	}

	public static <T extends Identified<?> & Described> JSSelectData fromDescribed(final T[] values, final Label defaultLabel) {
		final JSSelectData data = new JSSelectData();

		if (defaultLabel != null) {
			data.order.add("");
			data.labels.put("", defaultLabel);
		}

		for (final T t : values) {
			final String key = t.getId().toString();
			data.order.add(key);
			final Label label = new Label();
			label.primary = t.getDescription();
			data.labels.put(key, label);
		}

		return data;
	}

	public Label add(String id, String primary, String secondary, String tooltip) {

		final Label l = new Label();
		l.primary = primary;
		l.secondary = secondary;
		l.tooltip = tooltip;

		this.order.add(id);
		this.labels.put(id, l);
		return l;

	}

}
