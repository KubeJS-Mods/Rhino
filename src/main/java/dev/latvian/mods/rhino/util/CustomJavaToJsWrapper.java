package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;

import java.lang.reflect.Type;

@FunctionalInterface
public interface CustomJavaToJsWrapper {
	Scriptable convertJavaToJs(Context cx, Scriptable scope, Class<?> staticType, Type genericType);
}
