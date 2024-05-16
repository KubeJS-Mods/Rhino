package dev.latvian.mods.rhino;

import org.jetbrains.annotations.Nullable;

public enum MemberType {
	UNDEFINED("undefined"),
	OBJECT("object"),
	FUNCTION("function"),
	SYMBOL("symbol"),
	STRING("string"),
	NUMBER("number"),
	BOOLEAN("boolean");

	public static MemberType get(@Nullable Object value, Context cx) {
		if (value == null) {
			return OBJECT;
		}
		if (value == Undefined.INSTANCE) {
			return UNDEFINED;
		}
		if (value instanceof Scriptable) {
			return (value instanceof Callable) ? FUNCTION : ((Scriptable) value).getTypeOf();
		}
		if (value instanceof CharSequence) {
			return STRING;
		}
		if (value instanceof Number) {
			return NUMBER;
		}
		if (value instanceof Boolean) {
			return BOOLEAN;
		}
		throw ScriptRuntime.errorWithClassName("msg.invalid.type", value, cx);
	}

	private final String name;

	MemberType(String n) {
		name = n;
	}

	@Override
	public String toString() {
		return name;
	}
}
