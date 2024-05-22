package dev.latvian.mods.rhino.type;

import java.util.IdentityHashMap;
import java.util.Map;

public final class ClassTypeInfo implements TypeInfo {
	static final Map<Class<?>, ClassTypeInfo> CACHE = new IdentityHashMap<>();

	private final Class<?> type;
	private final boolean primitive;
	private TypeInfo asArray;

	public ClassTypeInfo(Class<?> type, boolean primitive) {
		this.type = type;
		this.primitive = primitive;
	}

	public ClassTypeInfo(Class<?> type) {
		this(type, false);
	}

	@Override
	public Class<?> asClass() {
		return type;
	}

	@Override
	public boolean isPrimitive() {
		return primitive;
	}

	@Override
	public boolean convert() {
		return type != Object.class;
	}

	@Override
	public int hashCode() {
		return type.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o == this || o instanceof ClassTypeInfo t && type == t.type;
	}

	@Override
	public String toString() {
		return type.getName();
	}

	@Override
	public TypeInfo asArray() {
		if (asArray == null) {
			asArray = new ArrayTypeInfo(this);
		}

		return asArray;
	}
}
