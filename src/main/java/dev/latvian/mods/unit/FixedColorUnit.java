package dev.latvian.mods.unit;

import dev.latvian.mods.unit.token.UnitToken;

public class FixedColorUnit extends Unit implements UnitToken {
	public static final FixedColorUnit WHITE = new FixedColorUnit(0xFFFFFFFF, true);
	public static final FixedColorUnit BLACK = new FixedColorUnit(0xFF000000, true);
	public static final FixedColorUnit TRANSPARENT = new FixedColorUnit(0x00000000, true);

	public static FixedColorUnit of(int color, boolean alpha) {
		if (color == 0xFFFFFFFF) {
			return WHITE;
		} else if (color == 0xFF000000) {
			return BLACK;
		} else if (color == 0x00000000) {
			return TRANSPARENT;
		}

		return new FixedColorUnit(color, alpha);
	}

	public final int color;
	public final boolean alpha;

	private FixedColorUnit(int c, boolean a) {
		color = c;
		alpha = a;
	}

	@Override
	public double get(UnitVariables variables) {
		return getInt(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		return alpha ? color : (color | 0xFF000000);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return !alpha || ((color >> 24) & 0xFF) != 0;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append(String.format(alpha ? "#%08X" : "#%06X", color));
	}

	@Override
	public Unit withAlpha(Unit a) {
		if (a instanceof FixedNumberUnit u) {
			if (u.value >= 1D) {
				return of(color, false);
			} else if (u.value <= 0D) {
				return of(color & 0xFFFFFF, true);
			} else {
				return of((color & 0xFFFFFF) | ((int) (u.value * 255D) << 24), true);
			}
		}

		return super.withAlpha(a);
	}
}
