package dev.latvian.mods.rhino.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface Remapper {
	static String getTypeName(String type) {
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
			default -> "L" + type.replace('.', '/') + ";";
		};

		return array == 0 ? t : ("[".repeat(array) + t);
	}

	default String getMappedClass(Class<?> from) {
		return "";
	}

	default String getUnmappedClass(String from) {
		return "";
	}

	default String getMappedField(Class<?> from, Field field) {
		return "";
	}

	default String getMappedMethod(Class<?> from, Method method) {
		return "";
	}
}
