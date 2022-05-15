package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.UnitVariables;

public class EqOpUnit extends BooleanOpUnit {
	@Override
	public boolean getBoolean(UnitVariables variables) {
		return Math.abs(left.get(variables) - right.get(variables)) < 0.00001D;
	}
}
