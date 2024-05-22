package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.type.TypeUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public record WithContext<T>(Context cx, T value) {
	public static WithContext<?> of(Context cx, Object from, Class<?> target, Type genericTarget) {
		if (genericTarget instanceof ParameterizedType parameterizedType) {
			var types = parameterizedType.getActualTypeArguments();

			if (types.length == 1) {
				return new WithContext<>(cx, cx.jsToJava(from, TypeUtils.getRawType(types[0]), types[0]));
			}
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
