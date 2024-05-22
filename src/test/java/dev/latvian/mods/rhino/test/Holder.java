package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.type.TypeUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public record Holder<T>(T value) {
	public static Holder of(Context cx, Object from, Class<?> target, Type genericTarget) {
		if (genericTarget instanceof ParameterizedType pt) {
			var types = pt.getActualTypeArguments();

			if (types.length == 1) {
				return new Holder(cx.jsToJava(from, TypeUtils.getRawType(types[0]), types[0]));
			}
		}

		return new Holder(from);
	}
}
