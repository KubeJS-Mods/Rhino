package dev.latvian.mods.rhino.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public record FallbackRemapper(Remapper main, Remapper fallback) implements Remapper {
	@Override
	public String remapClass(Class<?> from) {
		String s = main.remapClass(from);
		return s.isEmpty() ? fallback.remapClass(from) : s;
	}

	@Override
	public String remapField(Class<?> from, Field field) {
		String s = main.remapField(from, field);
		return s.isEmpty() ? fallback.remapField(from, field) : s;
	}

	@Override
	public String remapMethod(Class<?> from, Method method) {
		String s = main.remapMethod(from, method);
		return s.isEmpty() ? fallback.remapMethod(from, method) : s;
	}
}
