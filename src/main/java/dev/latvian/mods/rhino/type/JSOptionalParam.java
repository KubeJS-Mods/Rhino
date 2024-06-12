package dev.latvian.mods.rhino.type;

public record JSOptionalParam(String name, TypeInfo type, boolean optional) {
	public JSOptionalParam(String name, TypeInfo type) {
		this(name, type, false);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		append(TypeStringContext.DEFAULT, sb);
		return sb.toString();
	}

	public void append(TypeStringContext ctx, StringBuilder sb) {
		if (!name.isEmpty()) {
			sb.append(name);

			if (optional) {
				sb.append('?');
			}

			sb.append(':');
			ctx.appendSpace(sb);
		}

		ctx.append(sb, type);

		if (optional && name.isEmpty()) {
			sb.append('?');
		}
	}
}
