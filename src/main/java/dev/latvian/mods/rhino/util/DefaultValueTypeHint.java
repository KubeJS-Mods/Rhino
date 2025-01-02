package dev.latvian.mods.rhino.util;

import java.util.Locale;

public enum DefaultValueTypeHint {
	STRING,
	NUMBER,
	BOOLEAN,
	FUNCTION,
	CLASS;

	public final String name;

	DefaultValueTypeHint() {
		name = name().toLowerCase(Locale.ROOT);
	}

	public String toString() {
		return name;
	}
}
