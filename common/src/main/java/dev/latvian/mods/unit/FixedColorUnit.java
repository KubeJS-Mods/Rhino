package dev.latvian.mods.unit;

import dev.latvian.mods.unit.token.UnitToken;

public class FixedColorUnit extends Unit implements UnitToken {
	public static final FixedColorUnit WHITE = new FixedColorUnit(0xFFFFFFFF, true);
	public static final FixedColorUnit BLACK = new FixedColorUnit(0xFF000000, true);
	public static final FixedColorUnit TRANSPARENT = new FixedColorUnit(0x00000000, true);

	public final int color;
	public final boolean alpha;

	public FixedColorUnit(int c, boolean a) {
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

	public FixedColorUnit withAlpha(double v) {
		if (v >= 1D) {
			return new FixedColorUnit(color, false);
		} else {
			return new FixedColorUnit((color & 0xFFFFFF) | ((int) (v * 255) << 24), true);
		}
	}
}
