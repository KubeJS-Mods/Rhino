package dev.latvian.mods.rhino.type;

// [string, number]
public record JSFixedArrayTypeInfo(TypeInfo... types) implements TypeInfo {
	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}
}
