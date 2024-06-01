package dev.latvian.mods.rhino.type;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

import java.util.Map;

// {a: string, b: number}
public record JSObjectTypeInfo(Map<String, Field> fields) implements TypeInfo {
	public record Field(String name, TypeInfo type, boolean optional) {
		public Field(String name, TypeInfo type) {
			this(name, type, false);
		}
	}

	public static JSObjectTypeInfo of(Field field) {
		return new JSObjectTypeInfo(Map.of(field.name, field));
	}

	public static JSObjectTypeInfo of(Field field1, Field field2) {
		return new JSObjectTypeInfo(Map.of(field1.name, field1, field2.name, field2));
	}

	public static JSObjectTypeInfo of(Field field1, Field field2, Field field3) {
		return new JSObjectTypeInfo(Map.of(field1.name, field1, field2.name, field2, field3.name, field3));
	}

	public static JSObjectTypeInfo of(Field... fields) {
		var map = new Object2ObjectArrayMap<String, Field>(fields.length);

		for (var field : fields) {
			map.put(field.name, field);
		}

		return new JSObjectTypeInfo(map);
	}

	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}
}
