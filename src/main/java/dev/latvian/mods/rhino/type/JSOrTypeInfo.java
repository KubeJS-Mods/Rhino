package dev.latvian.mods.rhino.type;

import java.util.ArrayList;
import java.util.List;

// string | number
public record JSOrTypeInfo(List<TypeInfo> types) implements TypeInfo {
	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}

	@Override
	public TypeInfo or(TypeInfo info) {
		if (info instanceof JSOrTypeInfo or) {
			var list = new ArrayList<TypeInfo>(types.size() + or.types.size());
			list.addAll(types);
			list.addAll(or.types);
			return new JSOrTypeInfo(List.copyOf(list));
		} else {
			var list = new ArrayList<TypeInfo>(types.size() + 1);
			list.addAll(types);
			list.add(info);
			return new JSOrTypeInfo(List.copyOf(list));
		}
	}

	@Override
	public String toString() {
		return TypeStringContext.DEFAULT.toString(this);
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		for (int i = 0; i < types.size(); i++) {
			if (i != 0) {
				ctx.appendSpace(sb);
				sb.append('|');
				ctx.appendSpace(sb);
			}

			ctx.append(sb, types.get(i));
		}
	}
}
