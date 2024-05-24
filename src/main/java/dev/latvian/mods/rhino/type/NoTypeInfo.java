package dev.latvian.mods.rhino.type;

final class NoTypeInfo implements TypeInfo {
	@Override
	public Class<?> asClass() {
		return Object.class;
	}

	@Override
	public boolean shouldConvert() {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return "?";
	}

	@Override
	public TypeInfo asArray() {
		return this;
	}

	@Override
	public TypeInfo withParams(TypeInfo... params) {
		return this;
	}
}
