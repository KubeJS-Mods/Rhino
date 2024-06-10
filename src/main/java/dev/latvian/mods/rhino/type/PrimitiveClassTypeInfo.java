package dev.latvian.mods.rhino.type;

import org.jetbrains.annotations.Nullable;

public class PrimitiveClassTypeInfo extends ClassTypeInfo {
	private final Object defaultValue;

	PrimitiveClassTypeInfo(Class<?> type, @Nullable Object defaultValue) {
		super(type);
		this.defaultValue = defaultValue;
	}

	@Override
	public boolean isPrimitive() {
		return true;
	}

	@Override
	@Nullable
	public Object createDefaultValue() {
		return defaultValue;
	}
}
