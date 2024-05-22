package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.type.TypeInfo;

import java.util.Objects;

public record WithContext<T>(Context cx, T value) {
	public static WithContext<?> of(Context cx, Object from, TypeInfo target) {
		var type = target.param(0);

		if (type.convert()) {
			return new WithContext<>(cx, cx.jsToJava(from, type));
		}

		return new WithContext<>(cx, from);
	}

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
		return "W[" + value + "]";
	}
}
