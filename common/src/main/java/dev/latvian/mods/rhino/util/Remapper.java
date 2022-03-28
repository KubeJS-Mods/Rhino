package dev.latvian.mods.rhino.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

public interface Remapper {
	String remapClass(Class<?> from, String className);

	String unmapClass(String from);

	String remapField(Class<?> from, Field field, String fieldName);

	String remapMethod(Class<?> from, Method method, String methodString);

	default String getMappedClass(Class<?> from) {
		String n = from.getName();
		String s = remapClass(from, n);
		return s.isEmpty() ? n : s;
	}

	default String getUnmappedClass(String from) {
		String s = unmapClass(from);
		return s.isEmpty() ? from : s;
	}

	default String getMappedField(Class<?> from, Field field) {
		return getMappedField(from, field, field.getName());
	}

	default String getMappedField(Class<?> from, Field field, String fieldName) {
		if (from == null || from == Object.class) {
			return field.getName();
		}

		String s = remapField(from, field, fieldName);

		if (!s.isEmpty()) {
			return s;
		}

		String ss = getMappedField(from.getSuperclass(), field, fieldName);

		if (!ss.isEmpty()) {
			return ss;
		}

		for (Class<?> c : from.getInterfaces()) {
			String si = getMappedField(c, field, fieldName);

			if (!si.isEmpty()) {
				return si;
			}
		}

		return field.getName();
	}

	default String getMappedMethod(Class<?> from, Method method) {
		StringBuilder sb = new StringBuilder(method.getName());
		sb.append('(');

		if (method.getParameterCount() > 0) {
			for (Class<?> param : method.getParameterTypes()) {
				sb.append(Remapper.getTypeName(param.getTypeName()));
			}
		}

		sb.append(')');
		return getMappedMethod(from, method, sb.toString());
	}

	default String getMappedMethod(Class<?> from, Method method, String methodString) {
		if (from == null || from == Object.class) {
			return method.getName();
		}

		String s = remapMethod(from, method, methodString);

		if (!s.isEmpty()) {
			return s;
		}

		String ss = getMappedMethod(from.getSuperclass(), method, methodString);

		if (!ss.isEmpty()) {
			return ss;
		}

		for (Class<?> c : from.getInterfaces()) {
			String si = getMappedMethod(c, method, methodString);

			if (!si.isEmpty()) {
				return si;
			}
		}

		return method.getName();
	}

	static String getTypeName(String type, Function<String, String> remap) {
		int array = 0;

		while (type.endsWith("[]")) {
			array++;
			type = type.substring(0, type.length() - 2);
		}

		String t = switch (type) {
			case "boolean" -> "Z";
			case "byte" -> "B";
			case "short" -> "S";
			case "int" -> "I";
			case "long" -> "J";
			case "float" -> "F";
			case "double" -> "D";
			case "char" -> "C";
			case "void" -> "V";
			default -> "L" + remap.apply(type.replace('/', '.')).replace('.', '/') + ";";
		};

		return array == 0 ? t : ("[".repeat(array) + t);
	}

	static String getTypeName(String type) {
		return getTypeName(type, Function.identity());
	}
}
