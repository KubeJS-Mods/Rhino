package dev.latvian.mods.rhino.type;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Map;

public abstract class TypeInfoBase implements TypeInfo {
	private TypeInfo asArray;
	private Object emptyArray;
	private Boolean consolidatable = null;

	@Override
	public TypeInfo asArray() {
		if (asArray == null) {
			asArray = new ArrayTypeInfo(this);
		}

		return asArray;
	}

	@Override
	public Object newArray(int length) {
		if (length == 0) {
			if (emptyArray == null) {
				emptyArray = Array.newInstance(asClass(), 0);
			}

			return emptyArray;
		}

		return Array.newInstance(asClass(), length);
	}

	@Override
	public @NotNull TypeInfo consolidate(@NotNull Map<VariableTypeInfo, TypeInfo> mapping) {
		if (consolidatable == null) {
			var consolidated = this.consolidateImpl(mapping);
			consolidatable = consolidated == this;
			return consolidated;
		}
		if (consolidatable) {
			return this.consolidateImpl(mapping);
		}
		return this;
	}

	protected TypeInfo consolidateImpl(@NotNull Map<VariableTypeInfo, TypeInfo> mapping) {
		return this;
	}
}
