package dev.latvian.mods.rhino.type;

public abstract class ClassTypeInfo extends TypeInfoBase {
	private final Class<?> type;

	ClassTypeInfo(Class<?> type) {
		this.type = type;
	}

	@Override
	public Class<?> asClass() {
		return type;
	}

	@Override
	public boolean shouldConvert() {
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
}
