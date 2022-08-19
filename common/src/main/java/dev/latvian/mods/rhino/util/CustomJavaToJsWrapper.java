package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.SharedContextData;

@FunctionalInterface
public interface CustomJavaToJsWrapper {
	Scriptable convertJavaToJs(SharedContextData data, Scriptable scope, Class<?> staticType);
}
