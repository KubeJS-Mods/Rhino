package dev.latvian.mods.rhino.type;

// [string, number]
public record JSFixedArrayTypeInfo(TypeInfo... types) implements TypeInfo {
	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}

	@Override
	public String toString() {
		return TypeStringContext.DEFAULT.toString(this);
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		sb.append('[');

		for (int i = 0; i < types.length; i++) {
			if (i != 0) {
				sb.append(',');
				ctx.appendSpace(sb);
			}

			ctx.append(sb, types[i]);
		}

		sb.append(']');
	}
}
