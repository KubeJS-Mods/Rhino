package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.UnitVariables;

public class RshOpUnit extends OpUnit {
	@Override
	public double get(UnitVariables variables) {
		return getInt(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		return left.getInt(variables) >> right.getInt(variables);
	}
}
