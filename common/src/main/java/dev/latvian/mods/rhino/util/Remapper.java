package dev.latvian.mods.rhino.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface Remapper {
	String remapClass(Class<?> from);

	String unmapClass(String from);

	String remapField(Class<?> from, Field field);

	String remapMethod(Class<?> from, Method method);

	default String getMappedField(Class<?> from, Field field) {
		if (from == null || from == Object.class) {
			return field.getName();
		}

		String s = remapField(from, field);

		if (!s.isEmpty()) {
			return s;
		}

		String ss = getMappedField(from.getSuperclass(), field);

		if (!ss.isEmpty()) {
			return ss;
		}

		for (Class<?> c : from.getInterfaces()) {
			String si = getMappedField(c, field);

			if (!si.isEmpty()) {
				return si;
			}
		}

		return field.getName();
	}

	default String getMappedMethod(Class<?> from, Method method) {
		if (from == null || from == Object.class) {
			return method.getName();
		}

		String s = remapMethod(from, method);

		if (!s.isEmpty()) {
			return s;
		}

		String ss = getMappedMethod(from.getSuperclass(), method);

		if (!ss.isEmpty()) {
			return ss;
		}

		for (Class<?> c : from.getInterfaces()) {
			String si = getMappedMethod(c, method);

			if (!si.isEmpty()) {
				return si;
			}
		}

		return method.getName();
	}
}
