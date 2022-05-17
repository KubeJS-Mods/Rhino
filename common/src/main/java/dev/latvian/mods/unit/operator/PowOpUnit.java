package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.UnitVariables;

public class PowOpUnit extends OpUnit {
	@Override
	public int getPrecedence() {
		return 4;
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.pow(left.get(variables), right.get(variables));
	}
}
