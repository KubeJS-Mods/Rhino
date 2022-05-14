package dev.latvian.mods.unit;

import dev.latvian.mods.unit.token.UnitToken;

public class ColorUnit extends Unit implements UnitToken {
	public final int color;
	public final boolean alpha;

	public ColorUnit(int c, boolean a) {
		color = c;
		alpha = a;
	}

	@Override
	public double get(UnitVariables variables) {
		return color;
	}

	@Override
	public int getInt(UnitVariables variables) {
		return color;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append(String.format(alpha ? "#%08X" : "#%06X", color));
	}

	@Override
	public Unit interpret(UnitContext context) {
		return this;
	}
}
