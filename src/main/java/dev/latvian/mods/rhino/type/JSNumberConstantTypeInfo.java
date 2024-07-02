package dev.latvian.mods.rhino.type;

// 10, -402.01
public record JSNumberConstantTypeInfo(Number number) implements TypeInfo {
	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}

	@Override
	public String toString() {
		return number.toString();
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		sb.append(number);
	}
}
