package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;

@FunctionalInterface
public interface CustomJavaObjectWrapper {
	Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Class<?> staticType);
}
