package dev.latvian.mods.rhino.util;

public enum DefaultValueTypeHint {
	STRING,
	NUMBER,
	BOOLEAN,
	FUNCTION,
	CLASS;

	public final String name;

	DefaultValueTypeHint() {
		name = name().toLowerCase();
	}

	public String toString() {
		return name;
	}
}
