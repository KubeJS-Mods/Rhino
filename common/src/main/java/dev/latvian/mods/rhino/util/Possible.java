package dev.latvian.mods.rhino.util;

import org.jetbrains.annotations.Nullable;

public record Possible(@Nullable Object value) {
	public static final Possible EMPTY = new Possible(null);
	public static final Possible NULL = new Possible(null);

	public static Possible of(@Nullable Object o) {
		return o == null ? NULL : new Possible(o);
	}

	public static Possible empty() {
		return EMPTY;
	}

	public boolean isSet() {
		return this != EMPTY;
	}

	public boolean isEmpty() {
		return this == EMPTY;
	}

	@Override
	public String toString() {
		return this == EMPTY ? "EMPTY" : String.valueOf(value);
	}
}
