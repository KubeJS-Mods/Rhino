package dev.latvian.mods.rhino.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DefaultRemapper implements Remapper {
	public static final DefaultRemapper INSTANCE = new DefaultRemapper();

	private DefaultRemapper() {
	}

	@Override
	public String remapClass(Class<?> from) {
		return "";
	}

	@Override
	public String unmapClass(String from) {
		return "";
	}

	@Override
	public String remapField(Class<?> from, Field field) {
		RemapForJS remap = field.getAnnotation(RemapForJS.class);

		if (remap != null) {
			return remap.value();
		}

		return "";
	}

	@Override
	public String remapMethod(Class<?> from, Method method) {
		RemapForJS remap = method.getAnnotation(RemapForJS.class);

		if (remap != null) {
			return remap.value();
		}

		return "";
	}
}
