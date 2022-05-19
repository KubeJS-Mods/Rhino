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
		RemapForJS remap = field.getAnnotation(RemapForJS.class);

		if (remap != null) {
			return remap.value();
		}

		for (RemapPrefixForJS prefix : from.getAnnotationsByType(RemapPrefixForJS.class)) {
			String p = prefix.value();

			if (fieldName.length() > p.length() && fieldName.startsWith(p)) {
				return fieldName.substring(p.length());
			}
		}

		return "";
	}

	@Override
	public String remapMethod(Class<?> from, Method method, String methodString) {
		RemapForJS remap = method.getAnnotation(RemapForJS.class);

		if (remap != null) {
			return remap.value();
		}

		String methodName = method.getName();

		for (RemapPrefixForJS prefix : from.getAnnotationsByType(RemapPrefixForJS.class)) {
			String p = prefix.value();

			if (methodName.length() > p.length() && methodName.startsWith(p)) {
				return methodName.substring(p.length());
			}
		}

		return "";
	}
}
