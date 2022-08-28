package dev.latvian.mods.rhino.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DefaultRemapper implements Remapper {
	public static final DefaultRemapper INSTANCE = new DefaultRemapper();

	private DefaultRemapper() {
	}

	@Override
	public String remapClass(Class<?> from, String className) {
		return "";
	}

	@Override
	public String unmapClass(String from) {
		return "";
	}

	@Override
	public String remapField(Class<?> from, Field field, String fieldName) {
		return "";
	}

	@Override
	public String remapMethod(Class<?> from, Method method, String methodString) {
		return "";
	}
}
