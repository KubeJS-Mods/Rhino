package dev.latvian.mods.rhino.type;

public class PrimitiveClassTypeInfo extends ClassTypeInfo {
	PrimitiveClassTypeInfo(Class<?> type) {
		super(type);
	}

	@Override
	public boolean isPrimitive() {
		return true;
	}
}
