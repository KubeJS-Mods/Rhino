package dev.latvian.mods.rhino.type;

public abstract class TypeInfoBase implements TypeInfo {
	private TypeInfo asArray;

	@Override
	public TypeInfo asArray() {
		if (asArray == null) {
			asArray = new ArrayTypeInfo(this);
		}

		return asArray;
	}
}
