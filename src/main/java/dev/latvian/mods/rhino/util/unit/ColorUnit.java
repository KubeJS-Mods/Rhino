package dev.latvian.mods.rhino.util.unit;

import org.jetbrains.annotations.Nullable;

public class ColorUnit extends FuncUnit {
	private static int c(Unit u) {
		return Math.min(Math.max(0, u.getAsInt()), 255);
	}

	public final Unit r;
	public final Unit g;
	public final Unit b;
	public final Unit a;
	private final Integer fixed;

	public ColorUnit(Unit _r, Unit _g, Unit _b, @Nullable Unit _a) {
		r = _r;
		g = _g;
		b = _b;
		a = _a;

		if (r.isFixed() && g.isFixed() && b.isFixed() && (a == null || a.isFixed())) {
			fixed = (c(r) << 16) | (c(g) << 8) | c(b) | (a == null ? 0xFF000000 : (c(a) << 24));
		} else {
			fixed = null;
		}
	}

	@Override
	public String getFuncName() {
		return "color";
	}

	@Override
	public float get() {
		return getAsInt();
	}

	@Override
	public int getAsInt() {
		if (fixed != null) {
			return fixed;
		}

		return (c(r) << 16) | (c(g) << 8) | c(b) | (a == null ? 0xFF000000 : (c(a) << 24));
	}

	@Override
	public boolean getAsBoolean() {
		return getAsInt() != 0;
	}

	@Override
	public void append(StringBuilder sb) {
		if (fixed != null) {
			sb.append('#');

			if (a == null) {
				sb.append(String.format("%06X", fixed & 0xFFFFFF));
			} else {
				sb.append(String.format("%08X", fixed));
			}

			return;
		}

		sb.append("color(");
		r.append(sb);
		sb.append(',');
		sb.append(' ');
		g.append(sb);
		sb.append(',');
		sb.append(' ');
		b.append(sb);

		if (a != null) {
			sb.append(',');
			sb.append(' ');
			a.append(sb);
		}

		sb.append(')');
	}
}
