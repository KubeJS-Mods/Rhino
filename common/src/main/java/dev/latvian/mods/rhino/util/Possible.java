package dev.latvian.mods.rhino.util;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unchecked")
public record Possible<T>(@Nullable Object value) {
	public static final Possible<?> EMPTY = new Possible<>(null);
	public static final Possible<?> NULL = new Possible<>(null);

	public static <T> Possible<T> of(@Nullable T o) {
		return o == null ? (Possible<T>) NULL : new Possible<>(o);
	}


	public static <T> Possible<T> absent() {
		return (Possible<T>) EMPTY;
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

	public <C> Possible<C> cast(Class<C> type) {
		return (Possible<C>) this;
	}
}
