package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.UnitVariables;

public class EqOpUnit extends BooleanOpUnit {
	public static boolean doubleEquals(double a, double b) {
		return Math.abs(a - b) < 1e-6;
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return doubleEquals(left.get(variables), right.get(variables));
	}
}
