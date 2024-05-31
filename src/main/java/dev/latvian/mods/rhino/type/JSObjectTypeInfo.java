package dev.latvian.mods.rhino.type;

import java.util.Map;

// {a: string, b: number}
public record JSObjectTypeInfo(Map<String, TypeInfo> fields) implements TypeInfo {
	public static JSObjectTypeInfo of(String key, TypeInfo value) {
		return new JSObjectTypeInfo(Map.of(key, value));
	}

	public static JSObjectTypeInfo of(String key1, TypeInfo value1, String key2, TypeInfo value2) {
		return new JSObjectTypeInfo(Map.of(key1, value1, key2, value2));
	}

	public static JSObjectTypeInfo of(String key1, TypeInfo value1, String key2, TypeInfo value2, String key3, TypeInfo value3) {
		return new JSObjectTypeInfo(Map.of(key1, value1, key2, value2, key3, value3));
	}

	public static JSObjectTypeInfo of(Map<String, TypeInfo> fields) {
		return new JSObjectTypeInfo(fields);
	}

	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}
}
