package dev.latvian.mods.rhino.type;

// null, undefined, true, false
public record JSBasicConstantTypeInfo(String value) implements TypeInfo {
	public static final JSBasicConstantTypeInfo NULL = new JSBasicConstantTypeInfo("null");
	public static final JSBasicConstantTypeInfo UNDEFINED = new JSBasicConstantTypeInfo("undefined");
	public static final JSBasicConstantTypeInfo TRUE = new JSBasicConstantTypeInfo("true");
	public static final JSBasicConstantTypeInfo FALSE = new JSBasicConstantTypeInfo("false");

	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		sb.append(value);
	}
}
