package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.UnitVariables;

public class GtOpUnit extends BooleanOpUnit {
	@Override
	public boolean getBoolean(UnitVariables variables) {
		return left.get(variables) > right.get(variables);
	}
}
