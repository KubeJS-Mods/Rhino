package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.type.TypeInfo;

@FunctionalInterface
public interface CustomJavaToJsWrapper {
	Scriptable convertJavaToJs(Context cx, Scriptable scope, TypeInfo staticType);
}
