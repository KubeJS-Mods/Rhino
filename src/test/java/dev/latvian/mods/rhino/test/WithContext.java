package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;

import java.util.Objects;

public record WithContext<T>(Context cx, T value) {
	@Override
	public int hashCode() {
		return value == null ? 0 : value.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof WithContext<?> wc && Objects.equals(value, wc.value);
	}

	@Override
	public String toString() {
		return "WithContext[" + value + "]";
	}
}
