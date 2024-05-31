package dev.latvian.mods.rhino.type;

// string | number
public record JSOrTypeInfo(TypeInfo... types) implements TypeInfo {
	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}

	@Override
	public TypeInfo or(TypeInfo info) {
		var arr = new TypeInfo[types.length + 1];
		System.arraycopy(types, 0, arr, 0, types.length);
		arr[types.length] = info;
		return new JSOrTypeInfo(arr);
	}
}
