package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.UnitVariables;

public class NeqOpUnit extends BooleanOpUnit {
	@Override
	public boolean getBoolean(UnitVariables variables) {
		return !EqOpUnit.doubleEquals(left.get(variables), right.get(variables));
	}
}
