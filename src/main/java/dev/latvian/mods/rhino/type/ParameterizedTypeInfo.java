package dev.latvian.mods.rhino.type;

import java.util.Arrays;
import java.util.Objects;

public record ParameterizedTypeInfo(TypeInfo rawType, TypeInfo[] params) implements TypeInfo {
	@Override
	public Class<?> asClass() {
		return rawType.asClass();
	}

	@Override
	public TypeInfo param(int index) {
		return index >= 0 && index < params.length && params[index] != TypeInfo.OBJECT ? params[index] : TypeInfo.NONE;
	}

	@Override
	public int hashCode() {
		return Objects.hash(rawType, Arrays.hashCode(params));
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ParameterizedTypeInfo that = (ParameterizedTypeInfo) object;
		return Objects.equals(rawType, that.rawType) && Objects.deepEquals(params, that.params);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder(rawType.toString());
		sb.append('<');

		for (int i = 0; i < params.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}

			sb.append(params[i]);
		}

		sb.append('>');
		return sb.toString();
	}
}
