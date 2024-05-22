package dev.latvian.mods.rhino.type;

import java.lang.reflect.Array;

public final class ArrayTypeInfo implements TypeInfo {
	private final TypeInfo component;
	private Class<?> asClass;

	public ArrayTypeInfo(TypeInfo component) {
		this.component = component;
	}

	@Override
	public Class<?> asClass() {
		if (asClass == null) {
			asClass = Array.newInstance(component.asClass(), 0).getClass();
		}

		return asClass;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this || obj instanceof ArrayTypeInfo t && component.equals(t.component);
	}

	@Override
	public int hashCode() {
		return component.hashCode();
	}

	@Override
	public String toString() {
		return component + "[]";
	}

	@Override
	public String signature() {
		return component.signature() + "[]";
	}

	@Override
	public TypeInfo componentType() {
		return component;
	}
}
