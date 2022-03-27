package dev.latvian.mods.rhino.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface Remapper {
	String remapClass(Class<?> from);

	String unmapClass(String from);

	String remapField(Class<?> from, Field field);

	String remapMethod(Class<?> from, Method method);
}
