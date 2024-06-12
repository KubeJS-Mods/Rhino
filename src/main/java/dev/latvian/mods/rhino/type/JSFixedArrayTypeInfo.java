package dev.latvian.mods.rhino.type;

import java.util.List;

// [string, number]
public record JSFixedArrayTypeInfo(List<JSOptionalParam> types) implements TypeInfo {
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

		for (int i = 0; i < types.size(); i++) {
			if (i != 0) {
				sb.append(',');
				ctx.appendSpace(sb);
			}

			types.get(i).append(ctx, sb);
		}

		sb.append(']');
	}
}
