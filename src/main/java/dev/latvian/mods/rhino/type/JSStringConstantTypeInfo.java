package dev.latvian.mods.rhino.type;

import dev.latvian.mods.rhino.ScriptRuntime;

// "abc"
public record JSStringConstantTypeInfo(String constant) implements TypeInfo {
	public static final JSStringConstantTypeInfo EMPTY = new JSStringConstantTypeInfo("");

	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}

	@Override
	public String toString() {
		return ScriptRuntime.escapeAndWrapString(constant);
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		sb.append(ScriptRuntime.escapeAndWrapString(constant));
	}
}
