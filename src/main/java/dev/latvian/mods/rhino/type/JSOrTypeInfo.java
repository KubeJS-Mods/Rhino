package dev.latvian.mods.rhino.type;

// string | number
public record JSOrTypeInfo(TypeInfo... types) implements TypeInfo {
	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}

	@Override
	public TypeInfo or(TypeInfo info) {
		var arr = new TypeInfo[types.length + 1];
		System.arraycopy(types, 0, arr, 0, types.length);
		arr[types.length] = info;
		return new JSOrTypeInfo(arr);
	}

	@Override
	public String toString() {
		return TypeStringContext.DEFAULT.toString(this);
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		for (int i = 0; i < types.length; i++) {
			if (i != 0) {
				ctx.appendSpace(sb);
				sb.append('|');
				ctx.appendSpace(sb);
			}

			ctx.append(sb, types[i]);
		}
	}
}
