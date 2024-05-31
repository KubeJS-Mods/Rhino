package dev.latvian.mods.rhino.type;

import java.util.Map;

// (a: string) => void
public record JSFunctionTypeInfo(Map<String, TypeInfo> fields, TypeInfo returnType) implements TypeInfo {
	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}
}
