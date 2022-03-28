package dev.latvian.mods.rhino.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public record FallbackRemapper(Remapper main, Remapper fallback) implements Remapper {
	@Override
	public String remapClass(Class<?> from, String className) {
		String s = main.remapClass(from, className);
		return s.isEmpty() ? fallback.remapClass(from, className) : s;
	}

	@Override
	public String unmapClass(String from) {
		String s = main.unmapClass(from);
		return s.isEmpty() ? fallback.unmapClass(from) : s;
	}

	@Override
	public String remapField(Class<?> from, Field field, String fieldName) {
		String s = main.remapField(from, field, fieldName);
		return s.isEmpty() ? fallback.remapField(from, field, fieldName) : s;
	}

	@Override
	public String remapMethod(Class<?> from, Method method, String methodString) {
		String s = main.remapMethod(from, method, methodString);
		return s.isEmpty() ? fallback.remapMethod(from, method, methodString) : s;
	}
}
